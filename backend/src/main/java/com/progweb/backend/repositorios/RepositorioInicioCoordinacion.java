package com.progweb.backend.repositorios;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.time.YearMonth;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import com.progweb.backend.respuestas.RespuestaInicioCoordinacion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioInicioCoordinacion {
    private final JdbcTemplate consultas;
    public RepositorioInicioCoordinacion(JdbcTemplate consultas){this.consultas=consultas;}
    public RespuestaInicioCoordinacion.Indicadores indicadores(){
        return consultas.queryForObject("""
            SELECT COUNT(*) total_estudiantes,
              COALESCE(SUM(m.nivel_riesgo IN ('medio','alto')),0) estudiantes_en_riesgo,
              COALESCE(SUM(m.nivel_riesgo IS NULL),0) sin_evaluacion,
              (SELECT COUNT(*) FROM tutores t JOIN usuarios u ON u.id=t.usuario_id WHERE u.estado='activo') tutores_activos,
              (SELECT COUNT(*) FROM reportes) total_reportes
            FROM estudiantes e
            LEFT JOIN situaciones_academicas s ON s.id=(
              SELECT situacion.id FROM situaciones_academicas situacion
              JOIN periodos_academicos periodo ON periodo.id=situacion.periodo_id
              WHERE situacion.estudiante_id=e.id
              ORDER BY periodo.fecha_inicio DESC,periodo.id DESC,situacion.id DESC LIMIT 1
            )
            LEFT JOIN v_ultima_medicion_academica m ON m.situacion_id=s.id
            """,(fila,posicion)->new RespuestaInicioCoordinacion.Indicadores(fila.getLong("total_estudiantes"),fila.getLong("tutores_activos"),
                fila.getLong("estudiantes_en_riesgo"),fila.getLong("sin_evaluacion"),fila.getLong("total_reportes")));
    }
    public List<RespuestaInicioCoordinacion.Motivo> motivos(long total){
        return consultas.query("""
            SELECT m.id,m.nombre,COUNT(*) cantidad FROM reportes r JOIN motivos_reporte m ON m.id=r.motivo_id
            GROUP BY m.id,m.nombre ORDER BY cantidad DESC,m.nombre,m.id
            """,(fila,posicion)->new RespuestaInicioCoordinacion.Motivo(fila.getLong("id"),fila.getString("nombre"),fila.getLong("cantidad"),
                total==0?BigDecimal.ZERO:BigDecimal.valueOf(fila.getLong("cantidad")).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(total),1,RoundingMode.HALF_UP)));
    }
    public Map<YearMonth,Long> tutoriasCompletadas(LocalDateTime desde,LocalDateTime hasta){
        var resultado=new HashMap<YearMonth,Long>();
        consultas.query("""
            SELECT YEAR(inicio_en) anio,MONTH(inicio_en) mes,COUNT(*) cantidad FROM sesiones
            WHERE estado='completada' AND inicio_en>=? AND inicio_en<=?
            GROUP BY YEAR(inicio_en),MONTH(inicio_en) ORDER BY anio,mes
            """,(org.springframework.jdbc.core.RowCallbackHandler)fila->{resultado.put(YearMonth.of(fila.getInt("anio"),fila.getInt("mes")),fila.getLong("cantidad"));},desde,hasta);
        return resultado;
    }
}
