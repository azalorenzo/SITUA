package com.progweb.backend.repositorios;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import com.progweb.backend.solicitudes.FiltrosReportes;
import com.progweb.backend.respuestas.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioReportes {
    private final JdbcTemplate consultas;
    private final NamedParameterJdbcTemplate parametros;
    private static final String BASE="""
        FROM reportes r JOIN estudiantes e ON e.id=r.estudiante_id JOIN usuarios u ON u.id=e.usuario_id
        JOIN tutores t ON t.id=r.tutor_id JOIN usuarios ut ON ut.id=t.usuario_id
        JOIN periodos_academicos p ON p.id=r.periodo_id JOIN motivos_reporte m ON m.id=r.motivo_id
        JOIN situaciones_academicas s ON s.estudiante_id=r.estudiante_id AND s.periodo_id=r.periodo_id
        JOIN carreras c ON c.id=s.carrera_id LEFT JOIN usuarios autor ON autor.id=r.completado_por
        """;
    private static final String COLUMNAS="""
        SELECT r.id,r.estudiante_id,u.nombre AS estudiante,e.matricula,r.tutor_id,ut.nombre AS tutor,
        r.periodo_id,p.nombre AS periodo,c.nombre AS carrera,s.semestre,r.motivo_id,m.nombre AS motivo,
        r.fecha,r.estado,r.contenido,r.completado_en,autor.nombre AS completado_por
        """;
    private static final String CONDICIONES="""
        WHERE (:estado IS NULL OR r.estado=:estado)
          AND (:periodoId IS NULL OR r.periodo_id=:periodoId)
          AND (:motivoId IS NULL OR r.motivo_id=:motivoId)
          AND (:desde IS NULL OR r.fecha>=:desde) AND (:hasta IS NULL OR r.fecha<=:hasta)
          AND (:busqueda='' OR CONCAT_WS(' ',u.nombre,e.matricula,ut.nombre) LIKE :patron ESCAPE '!')
        """;
    private final RowMapper<RespuestaReporte> convertir=(fila,posicion)->new RespuestaReporte(fila.getLong("id"),
        fila.getLong("estudiante_id"),fila.getString("estudiante"),fila.getString("matricula"),fila.getLong("tutor_id"),fila.getString("tutor"),
        fila.getLong("periodo_id"),fila.getString("periodo"),fila.getString("carrera"),fila.getInt("semestre"),fila.getLong("motivo_id"),fila.getString("motivo"),
        fila.getObject("fecha",LocalDate.class),fila.getString("estado"),fila.getString("contenido"),fila.getObject("completado_en",LocalDateTime.class),fila.getString("completado_por"));
    public RepositorioReportes(JdbcTemplate consultas){this.consultas=consultas;this.parametros=new NamedParameterJdbcTemplate(consultas);}
    private MapSqlParameterSource valores(FiltrosReportes filtros){
        String termino=filtros.busqueda().strip();String patron="%"+termino.replace("!","!!").replace("%","!%").replace("_","!_")+"%";
        return new MapSqlParameterSource().addValue("estado",filtros.estado(),java.sql.Types.VARCHAR)
            .addValue("periodoId",filtros.periodoId(),java.sql.Types.BIGINT).addValue("motivoId",filtros.motivoId(),java.sql.Types.BIGINT)
            .addValue("desde",filtros.desde(),java.sql.Types.DATE).addValue("hasta",filtros.hasta(),java.sql.Types.DATE)
            .addValue("busqueda",termino).addValue("patron",patron);
    }
    public PaginaReportes.Resumen resumen(FiltrosReportes filtros){
        return parametros.queryForObject("SELECT COUNT(*) total,COALESCE(SUM(r.estado='completado'),0) completados,COALESCE(SUM(r.estado='pendiente'),0) pendientes "+BASE+CONDICIONES,
            valores(filtros),(fila,posicion)->new PaginaReportes.Resumen(fila.getLong("total"),fila.getLong("completados"),fila.getLong("pendientes")));
    }
    public List<PaginaReportes.Motivo> motivos(FiltrosReportes filtros,long total){
        return parametros.query("SELECT m.id,m.nombre,COUNT(*) cantidad "+BASE+CONDICIONES+" GROUP BY m.id,m.nombre ORDER BY cantidad DESC,m.nombre,m.id",
            valores(filtros),(fila,posicion)->new PaginaReportes.Motivo(fila.getLong("id"),fila.getString("nombre"),fila.getLong("cantidad"),
                total==0?BigDecimal.ZERO:BigDecimal.valueOf(fila.getLong("cantidad")).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(total),1,RoundingMode.HALF_UP)));
    }
    public List<RespuestaMotivoReporte> catalogoMotivos(){
        return consultas.query("SELECT id,nombre,activo FROM motivos_reporte ORDER BY nombre,id",(fila,posicion)->new RespuestaMotivoReporte(fila.getLong("id"),fila.getString("nombre"),fila.getBoolean("activo")));
    }
    public List<RespuestaReporte> listar(FiltrosReportes filtros,int pagina){
        return parametros.query(COLUMNAS+BASE+CONDICIONES+" ORDER BY r.fecha DESC,r.id DESC LIMIT :tamano OFFSET :desplazamiento",
            valores(filtros).addValue("tamano",filtros.tamano()).addValue("desplazamiento",(pagina-1L)*filtros.tamano()),convertir);
    }
    public List<RespuestaReporte> exportar(FiltrosReportes filtros){
        return parametros.query(COLUMNAS+BASE+CONDICIONES+" ORDER BY r.fecha DESC,r.id DESC",valores(filtros),convertir);
    }
    public Optional<RespuestaReporte> buscar(long id){return consultas.query(COLUMNAS+BASE+" WHERE r.id=?",convertir,id).stream().findFirst();}

    public PaginaReportes.Resumen resumenTutor(long tutor,FiltrosReportes filtros){
        return parametros.queryForObject("SELECT COUNT(*) total,COALESCE(SUM(r.estado='completado'),0) completados,COALESCE(SUM(r.estado='pendiente'),0) pendientes "+BASE+CONDICIONES+" AND r.tutor_id=:tutor",
            valores(filtros).addValue("tutor",tutor),(fila,posicion)->new PaginaReportes.Resumen(fila.getLong("total"),fila.getLong("completados"),fila.getLong("pendientes")));
    }
    public List<RespuestaReporte> listarTutor(long tutor,FiltrosReportes filtros,int pagina){
        return parametros.query(COLUMNAS+BASE+CONDICIONES+" AND r.tutor_id=:tutor ORDER BY r.fecha DESC,r.id DESC LIMIT :tamano OFFSET :desplazamiento",
            valores(filtros).addValue("tutor",tutor).addValue("tamano",filtros.tamano()).addValue("desplazamiento",(pagina-1L)*filtros.tamano()),convertir);
    }
    public List<RespuestaReporte> exportarTutor(long tutor,FiltrosReportes filtros){
        return parametros.query(COLUMNAS+BASE+CONDICIONES+" AND r.tutor_id=:tutor ORDER BY r.fecha DESC,r.id DESC",valores(filtros).addValue("tutor",tutor),convertir);
    }
    public Optional<RespuestaReporte> buscarTutor(long id,long tutor){return consultas.query(COLUMNAS+BASE+" WHERE r.id=? AND r.tutor_id=?",convertir,id,tutor).stream().findFirst();}
}
