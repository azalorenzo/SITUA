package com.progweb.backend.repositorios;

import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import com.progweb.backend.respuestas.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioSesionesAgenda {
    private final JdbcTemplate consultas;
    private final NamedParameterJdbcTemplate paginas;
    public RepositorioSesionesAgenda(JdbcTemplate consultas) { this.consultas=consultas;this.paginas=new NamedParameterJdbcTemplate(consultas); }
    public Long estudiante(long usuario) {
        return consultas.query("SELECT id FROM estudiantes WHERE usuario_id=?",(fila,posicion)->fila.getLong(1),usuario).stream().findFirst().orElse(null);
    }
    public Long tutor(long usuario) {
        return consultas.query("SELECT id FROM tutores WHERE usuario_id=?",(fila,posicion)->fila.getLong(1),usuario).stream().findFirst().orElse(null);
    }
    public boolean bloquearEstudiante(long id) {
        return !consultas.query("SELECT e.id FROM estudiantes e JOIN usuarios u ON u.id=e.usuario_id WHERE e.id=? FOR UPDATE",
            (fila,posicion)->fila.getLong(1),id).isEmpty();
    }
    public boolean bloquearTutor(long id) {
        return !consultas.query("SELECT t.id FROM tutores t JOIN usuarios u ON u.id=t.usuario_id WHERE t.id=? FOR UPDATE",
            (fila,posicion)->fila.getLong(1),id).isEmpty();
    }
    public boolean participantesActivos(long estudiante,long tutor) {
        var estudianteActivo=consultas.query("SELECT u.estado FROM estudiantes e JOIN usuarios u ON u.id=e.usuario_id WHERE e.id=? FOR SHARE",
            (fila,posicion)->fila.getString(1),estudiante);
        var tutorActivo=consultas.query("SELECT u.estado FROM tutores t JOIN usuarios u ON u.id=t.usuario_id WHERE t.id=? FOR SHARE",
            (fila,posicion)->fila.getString(1),tutor);
        return estudianteActivo.size()==1&&tutorActivo.size()==1&&estudianteActivo.getFirst().equals("activo")&&tutorActivo.getFirst().equals("activo");
    }
    public boolean inscripcionVigente(long estudiante,long tutor,long periodo) {
        return !consultas.query("""
            SELECT a.id FROM asignaciones_tutor a JOIN situaciones_academicas s ON s.estudiante_id=a.estudiante_id AND s.periodo_id=a.periodo_id
            JOIN periodos_academicos p ON p.id=a.periodo_id
            WHERE a.estudiante_id=? AND a.tutor_id=? AND a.periodo_id=? AND a.estado='activa'
                AND s.estado_inscripcion='activo' AND p.estado='activo' FOR SHARE
            """,(fila,posicion)->fila.getLong(1),estudiante,tutor,periodo).isEmpty();
    }
    public List<OpcionSesionEstudiante> opciones(long estudiante) {
        return consultas.query("""
            SELECT p.id,p.nombre,t.id,u.nombre FROM asignaciones_tutor a
            JOIN situaciones_academicas s ON s.estudiante_id=a.estudiante_id AND s.periodo_id=a.periodo_id
            JOIN periodos_academicos p ON p.id=a.periodo_id
            JOIN tutores t ON t.id=a.tutor_id JOIN usuarios u ON u.id=t.usuario_id
            WHERE a.estudiante_id=? AND a.estado='activa' AND s.estado_inscripcion='activo' AND p.estado='activo' AND u.estado='activo'
            ORDER BY p.fecha_inicio DESC,p.id DESC
            """,(fila,posicion)->new OpcionSesionEstudiante(fila.getLong(1),fila.getString(2),fila.getLong(3),fila.getString(4)),estudiante);
    }
    public record HorarioDatos(long id,long tutor,LocalDateTime inicio,LocalDateTime fin,String modalidad,String ubicacion,String enlace,String estado) { }
    public HorarioDatos horario(long id,boolean bloquear) {
        return consultas.query("SELECT * FROM disponibilidades_tutor WHERE id=?"+(bloquear?" FOR UPDATE":""),
            (fila,posicion)->new HorarioDatos(fila.getLong("id"),fila.getLong("tutor_id"),fila.getObject("inicio_en",LocalDateTime.class),
                fila.getObject("fin_en",LocalDateTime.class),fila.getString("modalidad"),fila.getString("ubicacion"),fila.getString("enlace_virtual"),fila.getString("estado")),id).stream().findFirst().orElse(null);
    }
    private static final String DISPONIBLES="""
        FROM disponibilidades_tutor d
        JOIN asignaciones_tutor a ON a.tutor_id=d.tutor_id AND a.estudiante_id=:estudiante AND a.periodo_id=:periodo AND a.estado='activa'
        JOIN situaciones_academicas sa ON sa.estudiante_id=a.estudiante_id AND sa.periodo_id=a.periodo_id AND sa.estado_inscripcion='activo'
        JOIN periodos_academicos p ON p.id=a.periodo_id AND p.estado='activo'
        JOIN tutores t ON t.id=d.tutor_id JOIN usuarios u ON u.id=t.usuario_id AND u.estado='activo'
        WHERE d.estado='habilitada' AND d.inicio_en>:ahora
          AND NOT EXISTS(SELECT 1 FROM sesiones s WHERE s.disponibilidad_id=d.id AND s.estado NOT IN ('cancelada','rechazada'))
          AND NOT EXISTS(SELECT 1 FROM sesiones s WHERE (s.estudiante_id=:estudiante OR s.tutor_id=d.tutor_id)
            AND s.estado IN ('solicitada','confirmada','completada','no_asistio') AND s.inicio_en<d.fin_en AND s.fin_en>d.inicio_en)
        """;
    private MapSqlParameterSource parametrosDisponibles(long estudiante,long periodo,LocalDateTime ahora) {
        return new MapSqlParameterSource().addValue("estudiante",estudiante).addValue("periodo",periodo).addValue("ahora",ahora);
    }
    public long contarDisponibles(long estudiante,long periodo,LocalDateTime ahora) {
        return paginas.queryForObject("SELECT COUNT(*) "+DISPONIBLES,parametrosDisponibles(estudiante,periodo,ahora),Long.class);
    }
    public List<RespuestaHorario> disponibles(long estudiante,long periodo,LocalDateTime ahora,int tamano,long desplazamiento) {
        return paginas.query("SELECT d.* "+DISPONIBLES+" ORDER BY d.inicio_en,d.id LIMIT :tamano OFFSET :desplazamiento",
            parametrosDisponibles(estudiante,periodo,ahora).addValue("tamano",tamano).addValue("desplazamiento",desplazamiento),
            (fila,posicion)->new RespuestaHorario(fila.getLong("id"),fila.getObject("inicio_en",LocalDateTime.class),fila.getObject("fin_en",LocalDateTime.class),
                fila.getString("modalidad"),fila.getString("ubicacion"),fila.getString("enlace_virtual"),fila.getString("estado"),null));
    }
    public boolean ocupado(long estudiante,long tutor,LocalDateTime inicio,LocalDateTime fin,Long excluir) {
        return !consultas.query("""
            SELECT id FROM sesiones WHERE (estudiante_id=? OR tutor_id=?) AND estado IN ('solicitada','confirmada','completada','no_asistio')
                AND inicio_en<? AND fin_en>? AND (? IS NULL OR id<>?) FOR UPDATE
            """,(fila,posicion)->fila.getLong(1),estudiante,tutor,fin,inicio,excluir,excluir).isEmpty();
    }
    public long crear(long estudiante,long periodo,HorarioDatos horario,String tema,String mensaje,long autor) {
        var llave=new GeneratedKeyHolder();
        consultas.update(conexion->{
            var sentencia=conexion.prepareStatement("""
                INSERT INTO sesiones(tutor_id,estudiante_id,periodo_id,disponibilidad_id,tema,inicio_en,fin_en,modalidad,ubicacion,enlace_virtual,mensaje_solicitud,creado_por)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                """,Statement.RETURN_GENERATED_KEYS);
            sentencia.setLong(1,horario.tutor());sentencia.setLong(2,estudiante);sentencia.setLong(3,periodo);sentencia.setLong(4,horario.id());sentencia.setString(5,tema);
            sentencia.setObject(6,horario.inicio());sentencia.setObject(7,horario.fin());sentencia.setString(8,horario.modalidad());sentencia.setString(9,horario.ubicacion());
            sentencia.setString(10,horario.enlace());sentencia.setString(11,mensaje);sentencia.setLong(12,autor);return sentencia;
        },llave);return llave.getKey().longValue();
    }
    public record SesionDatos(long id,long estudiante,long tutor,long periodo,Long disponibilidad,LocalDateTime inicio,LocalDateTime fin,String estado) { }
    public SesionDatos sesion(long id,long perfil,boolean esTutor,boolean bloquear) {
        String propietario=esTutor?"tutor_id":"estudiante_id";
        return consultas.query("SELECT * FROM sesiones WHERE id=? AND "+propietario+"=?"+(bloquear?" FOR UPDATE":""),
            (fila,posicion)->new SesionDatos(fila.getLong("id"),fila.getLong("estudiante_id"),fila.getLong("tutor_id"),fila.getLong("periodo_id"),fila.getObject("disponibilidad_id",Long.class),
                fila.getObject("inicio_en",LocalDateTime.class),fila.getObject("fin_en",LocalDateTime.class),fila.getString("estado")),id,perfil).stream().findFirst().orElse(null);
    }
    public void cambiarEstado(long id,String estado) { consultas.update("UPDATE sesiones SET estado=? WHERE id=?",estado,id); }
    public void registrarCambio(long id,long autor,String tipo,String anterior,String nuevo,LocalDateTime inicio,LocalDateTime fin) {
        consultas.update("""
            INSERT INTO sesion_cambios(sesion_id,actor_id,tipo,estado_anterior,estado_nuevo,inicio_anterior,fin_anterior,inicio_nuevo,fin_nuevo)
            VALUES (?,?,?,?,?,?,?,?,?)
            """,id,autor,tipo,anterior,nuevo,anterior==null?null:inicio,anterior==null?null:fin,inicio,fin);
    }
    private static final String SESIONES="""
        FROM sesiones s JOIN tutores t ON t.id=s.tutor_id JOIN usuarios ut ON ut.id=t.usuario_id
        JOIN estudiantes e ON e.id=s.estudiante_id JOIN usuarios ue ON ue.id=e.usuario_id
        JOIN periodos_academicos p ON p.id=s.periodo_id
        """;
    private MapSqlParameterSource parametros(long perfil,String estado,LocalDateTime desde,LocalDateTime hasta) {
        return new MapSqlParameterSource().addValue("perfil",perfil).addValue("estado",estado,java.sql.Types.VARCHAR)
            .addValue("desde",desde,java.sql.Types.TIMESTAMP).addValue("hasta",hasta,java.sql.Types.TIMESTAMP);
    }
    private String filtros(boolean esTutor) {
        return " WHERE s."+(esTutor?"tutor_id":"estudiante_id")+"=:perfil AND (:estado IS NULL OR s.estado=:estado) AND (:desde IS NULL OR s.inicio_en>=:desde) AND (:hasta IS NULL OR s.inicio_en<:hasta) ";
    }
    private RowMapper<RespuestaSesionAgenda> convertir(LocalDateTime ahora,boolean esTutor) {
        return (fila,posicion)->{
            String estado=fila.getString("estado");boolean futura=fila.getObject("inicio_en",LocalDateTime.class).isAfter(ahora);
            return new RespuestaSesionAgenda(fila.getLong("id"),fila.getObject("disponibilidad_id",Long.class),fila.getString("tutor"),fila.getString("estudiante"),fila.getString("tema"),
                fila.getObject("inicio_en",LocalDateTime.class),fila.getObject("fin_en",LocalDateTime.class),fila.getString("modalidad"),fila.getString("ubicacion"),fila.getString("enlace_virtual"),
                estado,fila.getLong("periodo_id"),fila.getString("periodo"),fila.getString("mensaje_solicitud"),futura&&List.of("solicitada","confirmada").contains(estado),esTutor&&futura&&estado.equals("solicitada"),esTutor&&!futura&&estado.equals("confirmada"),fila.getString("observaciones"),
                estado.equals("completada")?java.time.Duration.between(
                    fila.getObject("inicio_real_en",LocalDateTime.class)==null?fila.getObject("inicio_en",LocalDateTime.class):fila.getObject("inicio_real_en",LocalDateTime.class),
                    fila.getObject("fin_real_en",LocalDateTime.class)==null?fila.getObject("fin_en",LocalDateTime.class):fila.getObject("fin_real_en",LocalDateTime.class)).toMinutes():0);
        };
    }
    private static final String COLUMNAS="SELECT s.*,ut.nombre tutor,ue.nombre estudiante,p.nombre periodo ";
    public long contar(long perfil,boolean esTutor,String estado,LocalDateTime desde,LocalDateTime hasta) {
        return paginas.queryForObject("SELECT COUNT(*) "+SESIONES+filtros(esTutor),parametros(perfil,estado,desde,hasta),Long.class);
    }
    public List<RespuestaSesionAgenda> listar(long perfil,boolean esTutor,String estado,LocalDateTime desde,LocalDateTime hasta,int tamano,long desplazamiento,LocalDateTime ahora) {
        return paginas.query(COLUMNAS+SESIONES+filtros(esTutor)+" ORDER BY s.inicio_en DESC,s.id DESC LIMIT :tamano OFFSET :desplazamiento",
            parametros(perfil,estado,desde,hasta).addValue("tamano",tamano).addValue("desplazamiento",desplazamiento),convertir(ahora,esTutor));
    }
    public List<RespuestaSesionAgenda> agenda(long estudiante,LocalDateTime desde,LocalDateTime hasta,LocalDateTime ahora) {
        return paginas.query(COLUMNAS+SESIONES+filtros(false)+" ORDER BY s.inicio_en,s.id",parametros(estudiante,null,desde,hasta),convertir(ahora,false));
    }
    public RespuestaSesionAgenda respuesta(long id,long perfil,boolean esTutor,LocalDateTime ahora) {
        return paginas.query(COLUMNAS+SESIONES+filtros(esTutor)+" AND s.id=:id",parametros(perfil,null,null,null).addValue("id",id),convertir(ahora,esTutor)).stream().findFirst().orElse(null);
    }
    public List<RespuestaSesionAgenda> proximas(long estudiante,LocalDateTime ahora) {
        return consultas.query(COLUMNAS+SESIONES+" WHERE s.estudiante_id=? AND s.estado IN ('solicitada','confirmada') AND s.inicio_en>? ORDER BY s.inicio_en,s.id LIMIT 5",convertir(ahora,false),estudiante,ahora);
    }
    public List<ResumenSesionesEstudiante.Actividad> actividades(long estudiante) {
        return consultas.query("""
            SELECT c.id,c.tipo,s.tema,c.estado_nuevo,c.ocurrido_en FROM sesion_cambios c JOIN sesiones s ON s.id=c.sesion_id
            WHERE s.estudiante_id=? ORDER BY c.ocurrido_en DESC,c.id DESC LIMIT 5
            """,(fila,posicion)->new ResumenSesionesEstudiante.Actividad(fila.getLong(1),fila.getString(2),fila.getString(3),fila.getString(4),fila.getObject(5,LocalDateTime.class)),estudiante);
    }
}
