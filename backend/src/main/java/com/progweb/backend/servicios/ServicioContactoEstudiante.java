package com.progweb.backend.servicios;

import com.progweb.backend.errores.AccesoRechazado;
import com.progweb.backend.respuestas.RespuestaContactoEstudiante;
import com.progweb.backend.solicitudes.SolicitudContactoEstudiante;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class ServicioContactoEstudiante {
    private final JdbcTemplate consultas;
    private final ServicioAutenticacion autenticacion;
    public ServicioContactoEstudiante(JdbcTemplate consultas,ServicioAutenticacion autenticacion) { this.consultas=consultas;this.autenticacion=autenticacion; }
    private long identificar(String identificador) {
        var usuario=autenticacion.consultarSesion(identificador);
        if(!usuario.roles().contains("estudiante"))throw new AccesoRechazado(403,"Esta opción es para estudiantes.");
        return usuario.id();
    }
    private RespuestaContactoEstudiante datos(long usuario) {
        return consultas.queryForObject("SELECT telefono,acepta_sms FROM usuarios WHERE id=?",
            (fila,posicion)->new RespuestaContactoEstudiante(fila.getString(1),fila.getBoolean(2)),usuario);
    }
    public RespuestaContactoEstudiante consultar(String identificador) { return datos(identificar(identificador)); }
    @Transactional
    public RespuestaContactoEstudiante guardar(String identificador,SolicitudContactoEstudiante solicitud) {
        long usuario=identificar(identificador);
        consultas.update("UPDATE usuarios SET telefono=?,acepta_sms=?,preferencias_contacto_en=UTC_TIMESTAMP(6) WHERE id=?",
            solicitud.telefono().isEmpty()?null:solicitud.telefono(),solicitud.aceptaSms(),usuario);
        return datos(usuario);
    }
}
