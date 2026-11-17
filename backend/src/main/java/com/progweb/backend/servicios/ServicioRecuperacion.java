package com.progweb.backend.servicios;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.*;
import java.util.Locale;
import com.progweb.backend.errores.SolicitudRechazada;
import com.progweb.backend.solicitudes.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioRecuperacion {
    private final JdbcTemplate consultas;
    private final PasswordEncoder codificador;
    private final Clock reloj;
    private final EnvioCodigoRecuperacion envio;
    private final SecureRandom azar=new SecureRandom();
    public ServicioRecuperacion(JdbcTemplate consultas,PasswordEncoder codificador,Clock reloj,EnvioCodigoRecuperacion envio) {
        this.consultas=consultas;this.codificador=codificador;this.reloj=reloj;this.envio=envio;
    }
    private LocalDateTime ahora() { return LocalDateTime.ofInstant(reloj.instant(),ZoneOffset.UTC); }
    private String normalizar(String correo) { return correo.strip().toLowerCase(Locale.ROOT); }
    private record Cuenta(long id,String clave,String estado) { }
    private Cuenta bloquear(String correo) {
        return consultas.query("SELECT id,password_hash,estado FROM usuarios WHERE correo=? FOR UPDATE",
            (fila,posicion)->new Cuenta(fila.getLong("id"),fila.getString("password_hash"),fila.getString("estado")),correo).stream().findFirst().orElse(null);
    }
    private boolean elegible(Cuenta cuenta) { return cuenta!=null&&cuenta.clave()!=null&&cuenta.estado().equals("activo"); }

    @Transactional
    public void solicitar(SolicitudCodigoRecuperacion solicitud) {
        // Solo generamos códigos cuando el envío de correo está habilitado.
        if(!envio.habilitado()) throw new SolicitudRechazada(503,"La recuperación por correo todavía no está disponible.","correo");
        String correo=normalizar(solicitud.correo());
        var cuenta=bloquear(correo);
        if(cuenta==null)throw new SolicitudRechazada(422,"No hay una cuenta registrada con ese correo.","correo");
        if(cuenta.estado().equals("inactivo"))throw new SolicitudRechazada(422,"Esta cuenta está inactiva. Contacta a coordinación.","correo");
        if(cuenta.clave()==null)throw new SolicitudRechazada(422,"Esta cuenta usa Google. Recupera el acceso desde Google.","correo");
        var momento=ahora();
        var anteriores=consultas.query("SELECT solicitado_en FROM recuperaciones_contrasena WHERE usuario_id=?",
            (fila,posicion)->fila.getObject(1,LocalDateTime.class),cuenta.id());
        // Repetir la solicitud durante la espera no genera otro correo.
        if(!anteriores.isEmpty()&&momento.isBefore(anteriores.getFirst().plusMinutes(1)))return;
        String codigo=String.format(Locale.ROOT,"%06d",azar.nextInt(1000000));
        consultas.update("""
            INSERT INTO recuperaciones_contrasena(usuario_id,codigo_hash,solicitado_en,vence_en,intentos,utilizado)
            VALUES (?,?,?,?,0,false) ON DUPLICATE KEY UPDATE codigo_hash=VALUES(codigo_hash),
            solicitado_en=VALUES(solicitado_en),vence_en=VALUES(vence_en),intentos=0,utilizado=false
            """,cuenta.id(),codificador.encode(codigo),momento,momento.plusMinutes(10));
        envio.enviar(correo,codigo);
    }
    private record Recuperacion(String codigo,LocalDateTime vence,int intentos,boolean utilizado) { }

    @Transactional(noRollbackFor=SolicitudRechazada.class)
    public void cambiar(SolicitudCambiarContrasena solicitud) {
        if(!solicitud.contrasena().equals(solicitud.confirmacion()))throw new SolicitudRechazada(422,"Las contraseñas no coinciden.","confirmacion");
        if(solicitud.contrasena().getBytes(StandardCharsets.UTF_8).length>72)throw new SolicitudRechazada(422,"La contraseña es demasiado larga.","contrasena");
        var cuenta=bloquear(normalizar(solicitud.correo()));
        if(!elegible(cuenta))throw codigoInvalido();
        var registros=consultas.query("SELECT codigo_hash,vence_en,intentos,utilizado FROM recuperaciones_contrasena WHERE usuario_id=? FOR UPDATE",
            (fila,posicion)->new Recuperacion(fila.getString(1),fila.getObject(2,LocalDateTime.class),fila.getInt(3),fila.getBoolean(4)),cuenta.id());
        if(registros.isEmpty())throw codigoInvalido();
        var registro=registros.getFirst();
        if(registro.utilizado()||registro.intentos()>=5||!ahora().isBefore(registro.vence()))throw codigoInvalido();
        if(!codificador.matches(solicitud.codigo(),registro.codigo())) {
            // El intento se guarda aunque devolvamos un error al formulario.
            consultas.update("UPDATE recuperaciones_contrasena SET intentos=intentos+1 WHERE usuario_id=?",cuenta.id());
            throw codigoInvalido();
        }
        consultas.update("UPDATE usuarios SET password_hash=? WHERE id=?",codificador.encode(solicitud.contrasena()),cuenta.id());
        consultas.update("UPDATE recuperaciones_contrasena SET utilizado=true WHERE usuario_id=?",cuenta.id());
    }
    private SolicitudRechazada codigoInvalido() { return new SolicitudRechazada(422,"El código no es válido o ya venció. Solicita uno nuevo.","codigo"); }
}
