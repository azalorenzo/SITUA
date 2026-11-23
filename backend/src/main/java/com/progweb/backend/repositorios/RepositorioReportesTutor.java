package com.progweb.backend.repositorios;
import java.sql.Statement;
import java.time.*;
import java.math.*;
import java.util.List;
import com.progweb.backend.respuestas.*;
import com.progweb.backend.solicitudes.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
@Repository
public class RepositorioReportesTutor {
    private final JdbcTemplate consultas;
    public RepositorioReportesTutor(JdbcTemplate consultas){this.consultas=consultas;}
    public List<RespuestaMotivoReporte> motivos(){return consultas.query("SELECT id,nombre,activo FROM motivos_reporte WHERE activo=true ORDER BY nombre,id",(fila,posicion)->new RespuestaMotivoReporte(fila.getLong(1),fila.getString(2),fila.getBoolean(3)));}
    public boolean motivoActivo(long id){return !consultas.query("SELECT id FROM motivos_reporte WHERE id=? AND activo=true FOR SHARE",(fila,posicion)->fila.getLong(1),id).isEmpty();}
    public boolean sesionPropia(long id,long tutor,long estudiante,long periodo){
        return !consultas.query("SELECT id FROM sesiones WHERE id=? AND tutor_id=? AND estudiante_id=? AND periodo_id=? AND estado='completada' FOR SHARE",(fila,posicion)->fila.getLong(1),id,tutor,estudiante,periodo).isEmpty();
    }
    public List<RespuestaSesionRealizada> sesiones(long tutor,long estudiante,long periodo){
        return consultas.query("""
            SELECT s.*,u.nombre,p.nombre periodo FROM sesiones s JOIN estudiantes e ON e.id=s.estudiante_id JOIN usuarios u ON u.id=e.usuario_id JOIN periodos_academicos p ON p.id=s.periodo_id
            WHERE s.tutor_id=? AND s.estudiante_id=? AND s.periodo_id=? AND s.estado='completada' ORDER BY s.inicio_en DESC,s.id DESC LIMIT 100
            """,(fila,posicion)->new RespuestaSesionRealizada(fila.getLong("id"),estudiante,fila.getString("nombre"),periodo,fila.getString("periodo"),fila.getString("tema"),
                fila.getObject("inicio_en",LocalDateTime.class),fila.getObject("fin_en",LocalDateTime.class),fila.getString("estado"),fila.getString("ubicacion"),fila.getString("observaciones"),0),tutor,estudiante,periodo);
    }
    public long crear(long tutor,long autor,LocalDateTime ahora,SolicitudReporteTutor solicitud){
        var llave=new GeneratedKeyHolder();consultas.update(conexion->{var sentencia=conexion.prepareStatement("""
            INSERT INTO reportes(estudiante_id,tutor_id,periodo_id,sesion_id,motivo_id,fecha,contenido,estado,completado_en,completado_por) VALUES (?,?,?,?,?,?,?,?,?,?)
            """,Statement.RETURN_GENERATED_KEYS);
            boolean completo=solicitud.estado().equals("completado");Object[] datos={solicitud.estudianteId(),tutor,solicitud.periodoId(),solicitud.sesionId(),solicitud.motivoId(),solicitud.fecha(),solicitud.contenido().strip(),solicitud.estado(),completo?ahora:null,completo?autor:null};
            for(int indice=0;indice<datos.length;indice++)sentencia.setObject(indice+1,datos[indice]);return sentencia;},llave);return llave.getKey().longValue();
    }
    public String bloquear(long id,long tutor){return consultas.query("SELECT estado FROM reportes WHERE id=? AND tutor_id=? FOR UPDATE",(fila,posicion)->fila.getString(1),id,tutor).stream().findFirst().orElse(null);}
    public void completar(long id,long autor,LocalDateTime ahora,String contenido){consultas.update("UPDATE reportes SET contenido=?,estado='completado',completado_en=?,completado_por=? WHERE id=?",contenido.strip(),ahora,autor,id);}
    private static final String ALUMNOS="""
        WITH alumnos AS (
            SELECT x.*,ROW_NUMBER() OVER(PARTITION BY x.estudiante_id ORDER BY p.fecha_inicio DESC,p.id DESC) orden
            FROM v_expediente_actual_por_periodo x JOIN asignaciones_tutor a ON a.estudiante_id=x.estudiante_id AND a.periodo_id=x.periodo_id AND a.estado='activa'
            JOIN periodos_academicos p ON p.id=x.periodo_id AND p.estado='activo'
            JOIN estudiantes e ON e.id=x.estudiante_id JOIN usuarios u ON u.id=e.usuario_id AND u.estado='activo'
            WHERE a.tutor_id=? AND x.estado_inscripcion='activo'
        )
        """;
    public BigDecimal promedio(long tutor){return consultas.queryForObject(ALUMNOS+" SELECT ROUND(AVG(promedio),2) FROM alumnos WHERE orden=1",BigDecimal.class,tutor);}
    public List<AnalisisReportesTutor.Progreso> progreso(long tutor){
        return consultas.query(ALUMNOS+"""
            SELECT nombre,
                (SELECT m.promedio FROM mediciones_academicas m WHERE m.situacion_id=a.situacion_id AND m.promedio IS NOT NULL ORDER BY m.medido_en,m.id LIMIT 1) inicial,
                promedio actual FROM alumnos a WHERE orden=1 AND promedio IS NOT NULL ORDER BY nombre,estudiante_id LIMIT 10
            """,(fila,posicion)->new AnalisisReportesTutor.Progreso(fila.getString("nombre"),fila.getBigDecimal("inicial"),fila.getBigDecimal("actual")),tutor);
    }
    public List<AnalisisReportesTutor.Mes> rendimiento(long tutor,LocalDateTime desde,LocalDateTime hasta){
        return consultas.query(ALUMNOS+"""
            ,mediciones AS (
                SELECT m.promedio,YEAR(m.medido_en) anio,MONTH(m.medido_en) mes,
                    ROW_NUMBER() OVER(PARTITION BY a.estudiante_id,YEAR(m.medido_en),MONTH(m.medido_en) ORDER BY m.medido_en DESC,m.id DESC) ultima
                FROM alumnos a JOIN mediciones_academicas m ON m.situacion_id=a.situacion_id
                WHERE a.orden=1 AND m.medido_en>=? AND m.medido_en<=? AND m.promedio IS NOT NULL
            ) SELECT anio,mes,ROUND(AVG(promedio),2) promedio FROM mediciones WHERE ultima=1 GROUP BY anio,mes ORDER BY anio,mes
            """,(fila,posicion)->new AnalisisReportesTutor.Mes(YearMonth.of(fila.getInt("anio"),fila.getInt("mes")).toString(),fila.getBigDecimal("promedio")),tutor,desde,hasta);
    }
    public long estudiantesAtendidos(long tutor){return consultas.queryForObject("SELECT COUNT(DISTINCT estudiante_id) FROM sesiones WHERE tutor_id=? AND estado='completada'",Long.class,tutor);}
    public BigDecimal asistencia(long tutor){
        return consultas.queryForObject("SELECT ROUND(100.0*SUM(estado='completada')/NULLIF(COUNT(*),0),1) FROM sesiones WHERE tutor_id=? AND estado IN ('completada','no_asistio')",BigDecimal.class,tutor);
    }
}
