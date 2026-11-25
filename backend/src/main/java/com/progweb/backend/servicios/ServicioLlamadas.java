package com.progweb.backend.servicios;

import java.sql.*;
import java.time.*;
import java.util.*;
import com.progweb.backend.errores.AccesoRechazado;
import com.progweb.backend.repositorios.RepositorioMensajesTutor;
import com.progweb.backend.respuestas.RespuestaUsuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@Transactional(isolation=Isolation.READ_COMMITTED)
public class ServicioLlamadas {
    private static final org.slf4j.Logger registro=org.slf4j.LoggerFactory.getLogger(ServicioLlamadas.class);
    public record Llamada(long id,long conversacionId,String tipo,String estado,boolean soyIniciador,String nombre,Instant creadaEn,Instant inicioEn,Instant venceEn) {}
    public record Actual(boolean habilitado,Llamada llamada) {}
    public record Acceso(String url,String token,Instant venceEn,String tipo) {}
    private record Registro(long id,long conversacion,long iniciador,long receptor,String tipo,String estado,String sala,String direccion,Instant creada,Instant inicio,Instant vence) {}
    private final JdbcTemplate consultas;
    private final ServicioAutenticacion autenticacion;
    private final RepositorioMensajesTutor mensajes;
    private final ProveedorLlamadas proveedor;
    private final Clock reloj;
    private final int duracion;

    public ServicioLlamadas(JdbcTemplate consultas,ServicioAutenticacion autenticacion,RepositorioMensajesTutor mensajes,
            ProveedorLlamadas proveedor,Clock reloj,@Value("${llamadas.duracion-minutos:5}") int duracion) {
        this.consultas=consultas;this.autenticacion=autenticacion;this.mensajes=mensajes;this.proveedor=proveedor;this.reloj=reloj;
        if(duracion<1||duracion>5)throw new IllegalArgumentException("Las llamadas pueden durar de uno a cinco minutos.");
        this.duracion=duracion;
    }
    private RespuestaUsuario identificar(String sujeto) {
        var usuario=autenticacion.consultarSesion(sujeto);
        if(!usuario.roles().contains("tutor")&&!usuario.roles().contains("estudiante"))throw new AccesoRechazado(403,"No tienes acceso a esta opción.");
        return usuario;
    }
    private void disponible(){if(!proveedor.habilitado())throw new AccesoRechazado(503,"Las llamadas no están disponibles.");}
    private Registro convertir(ResultSet fila,int indice)throws SQLException {
        Timestamp inicio=fila.getTimestamp("inicio_en");
        return new Registro(fila.getLong("id"),fila.getLong("conversacion_id"),fila.getLong("iniciador_id"),fila.getLong("receptor_id"),
            fila.getString("tipo"),fila.getString("estado"),fila.getString("sala"),fila.getString("direccion"),
            fila.getTimestamp("creada_en").toInstant(),inicio==null?null:inicio.toInstant(),fila.getTimestamp("vence_en").toInstant());
    }
    private void bloquear(long primero,long segundo) {
        consultas.queryForList("SELECT id FROM usuarios WHERE id IN (?,?) ORDER BY id FOR UPDATE",Long.class,primero,segundo);
    }
    private Registro buscar(long id,long usuario,boolean bloqueo) {
        var filas=consultas.query("SELECT * FROM llamadas WHERE id=? AND (iniciador_id=? OR receptor_id=?)"+(bloqueo?" FOR UPDATE":""),this::convertir,id,usuario,usuario);
        if(filas.isEmpty())throw new AccesoRechazado(404,"No se encontró la llamada.");
        return filas.getFirst();
    }
    private Registro bloquearLlamada(long id,long usuario) {
        var llamada=buscar(id,usuario,false);bloquear(llamada.iniciador(),llamada.receptor());return buscar(id,usuario,true);
    }
    private boolean vigente(Registro llamada) {return Set.of("pendiente","activa").contains(llamada.estado())&&llamada.vence().isAfter(reloj.instant());}
    private Llamada respuesta(Registro llamada,long usuario) {
        long otro=llamada.iniciador()==usuario?llamada.receptor():llamada.iniciador();
        String nombre=consultas.queryForObject("SELECT nombre FROM usuarios WHERE id=?",String.class,otro);
        return new Llamada(llamada.id(),llamada.conversacion(),llamada.tipo(),llamada.estado(),llamada.iniciador()==usuario,nombre,llamada.creada(),llamada.inicio(),llamada.vence());
    }
    private long contacto(long conversacion,RespuestaUsuario usuario) {
        if(!mensajes.participa(usuario.id(),conversacion))throw new AccesoRechazado(404,"No se encontró la conversación.");
        var participantes=consultas.queryForList("SELECT cp.usuario_id FROM conversacion_participantes cp JOIN conversaciones c ON c.id=cp.conversacion_id WHERE c.id=? AND c.tipo='directa' ORDER BY cp.usuario_id",Long.class,conversacion);
        if(participantes.size()!=2||!participantes.contains(usuario.id()))throw new AccesoRechazado(422,"Esta conversación no permite llamadas.");
        long otro=participantes.getFirst().equals(usuario.id())?participantes.getLast():participantes.getFirst();
        if(!mensajes.contactosAsignados(usuario.id(),usuario.roles().contains("tutor")).contains(otro))throw new AccesoRechazado(403,"Necesitas una asignación activa para llamar.");
        return otro;
    }
    @Transactional(readOnly=true)
    public Actual actual(String sujeto) {
        var usuario=identificar(sujeto);
        if(!proveedor.habilitado())return new Actual(false,null);
        var filas=consultas.query("SELECT * FROM llamadas WHERE (iniciador_id=? OR receptor_id=?) AND estado IN ('pendiente','activa') AND vence_en>? ORDER BY id DESC LIMIT 1",this::convertir,usuario.id(),usuario.id(),Timestamp.from(reloj.instant()));
        return new Actual(true,filas.isEmpty()?null:respuesta(filas.getFirst(),usuario.id()));
    }
    public Llamada iniciar(String sujeto,long conversacion,String tipo) {
        var usuario=identificar(sujeto);disponible();long otro=contacto(conversacion,usuario);bloquear(usuario.id(),otro);
        contacto(conversacion,usuario);
        int ocupadas=consultas.queryForObject("SELECT COUNT(*) FROM llamadas WHERE (iniciador_id IN (?,?) OR receptor_id IN (?,?)) AND estado IN ('pendiente','activa') AND vence_en>?",Integer.class,usuario.id(),otro,usuario.id(),otro,Timestamp.from(reloj.instant()));
        if(ocupadas>0)throw new AccesoRechazado(409,"Uno de los participantes ya tiene una llamada.");
        Instant ahora=reloj.instant();
        var llave=new org.springframework.jdbc.support.GeneratedKeyHolder();
        consultas.update(conexion->{var sentencia=conexion.prepareStatement("INSERT INTO llamadas(conversacion_id,iniciador_id,receptor_id,tipo,creada_en,vence_en) VALUES (?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS);
            sentencia.setLong(1,conversacion);sentencia.setLong(2,usuario.id());sentencia.setLong(3,otro);sentencia.setString(4,tipo);sentencia.setTimestamp(5,Timestamp.from(ahora));sentencia.setTimestamp(6,Timestamp.from(ahora.plusSeconds(45)));return sentencia;},llave);
        return respuesta(buscar(llave.getKey().longValue(),usuario.id(),false),usuario.id());
    }
    public Llamada aceptar(String sujeto,long id) {
        var usuario=identificar(sujeto);disponible();var llamada=bloquearLlamada(id,usuario.id());
        if(llamada.receptor()!=usuario.id())throw new AccesoRechazado(403,"Solo la otra persona puede aceptar.");
        if(!vigente(llamada))throw new AccesoRechazado(409,"La llamada ya terminó.");
        contacto(llamada.conversacion(),usuario);
        if(llamada.estado().equals("activa"))return respuesta(llamada,usuario.id());
        Instant ahora=reloj.instant(),vence=ahora.plusSeconds(duracion*60L);
        var sala=proveedor.crearSala("situa-"+UUID.randomUUID(),llamada.tipo(),vence);
        consultas.update("UPDATE llamadas SET estado='activa',sala=?,direccion=?,inicio_en=?,vence_en=? WHERE id=?",sala.nombre(),sala.direccion(),Timestamp.from(ahora),Timestamp.from(vence),id);
        return respuesta(buscar(id,usuario.id(),false),usuario.id());
    }
    public Actual terminar(String sujeto,long id,boolean rechazo) {
        var usuario=identificar(sujeto);var llamada=bloquearLlamada(id,usuario.id());
        if(rechazo&&llamada.receptor()!=usuario.id())throw new AccesoRechazado(403,"Solo la otra persona puede rechazar.");
        if(rechazo&&llamada.estado().equals("activa"))throw new AccesoRechazado(409,"La llamada ya fue aceptada.");
        if(Set.of("pendiente","activa").contains(llamada.estado())) {
            String estado=llamada.estado().equals("activa")?"finalizada":rechazo?"rechazada":"cancelada";
            consultas.update("UPDATE llamadas SET estado=?,finalizada_en=? WHERE id=?",estado,Timestamp.from(reloj.instant()),id);
            limpiar(llamada);
        }
        return new Actual(proveedor.habilitado(),null);
    }
    public Acceso acceso(String sujeto,long id) {
        var usuario=identificar(sujeto);disponible();var llamada=bloquearLlamada(id,usuario.id());
        if(!llamada.estado().equals("activa")||!vigente(llamada))throw new AccesoRechazado(409,"La llamada no está activa.");
        contacto(llamada.conversacion(),usuario);
        return new Acceso(llamada.direccion(),proveedor.crearAcceso(llamada.sala(),llamada.tipo(),usuario.id(),usuario.nombre(),llamada.vence()),llamada.vence(),llamada.tipo());
    }
    @Scheduled(fixedDelay=15000)
    public void actualizar() { vencerEn(Instant.now()); }
    public void vencer() {
        vencerEn(reloj.instant());
    }
    private void vencerEn(Instant ahora) {
        consultas.update("UPDATE llamadas SET estado=IF(estado='pendiente','sin_respuesta','finalizada'),finalizada_en=vence_en WHERE estado IN ('pendiente','activa') AND vence_en<=?",Timestamp.from(ahora));
        if(proveedor.habilitado())for(var llamada:consultas.query("SELECT * FROM llamadas WHERE estado NOT IN ('pendiente','activa') AND sala IS NOT NULL ORDER BY id LIMIT 5",this::convertir))limpiar(llamada);
    }
    private void limpiar(Registro llamada) {
        if(llamada.sala()==null)return;
        if(proveedor.cerrarSala(llamada.sala(),llamada.iniciador(),llamada.receptor()))consultas.update("UPDATE llamadas SET sala=NULL,direccion=NULL WHERE id=?",llamada.id());
        else registro.warn("No se pudo retirar una sala de llamada; se volverá a intentar.");
    }
    public void cerrarPorSalida(long usuario) {
        var pendientes=consultas.queryForList("SELECT id FROM llamadas WHERE (iniciador_id=? OR receptor_id=?) AND estado IN ('pendiente','activa')",Long.class,usuario,usuario);
        for(long id:pendientes)terminar(Long.toString(usuario),id,false);
    }
}
