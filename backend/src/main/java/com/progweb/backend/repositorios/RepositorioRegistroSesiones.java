package com.progweb.backend.repositorios;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import com.progweb.backend.respuestas.*;
import com.progweb.backend.solicitudes.*;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioRegistroSesiones {
    private final JdbcTemplate consultas;
    private final NamedParameterJdbcTemplate paginas;
    public RepositorioRegistroSesiones(JdbcTemplate consultas){this.consultas=consultas;this.paginas=new NamedParameterJdbcTemplate(consultas);}
    private static final String BASE="""
        FROM sesiones s JOIN estudiantes e ON e.id=s.estudiante_id JOIN usuarios u ON u.id=e.usuario_id
        JOIN periodos_academicos p ON p.id=s.periodo_id
        WHERE s.tutor_id=:tutor AND s.estado IN ('completada','cancelada','no_asistio')
        """;
    private static final String FILTROS="""
        AND (:estado IS NULL OR s.estado=:estado)
        AND (:desde IS NULL OR COALESCE(s.inicio_real_en,s.inicio_en)>=:desde)
        AND (:hasta IS NULL OR COALESCE(s.inicio_real_en,s.inicio_en)<:hasta)
        AND CONCAT_WS(' ',u.nombre,e.matricula,s.tema) LIKE :patron ESCAPE '!'
        """;
    private MapSqlParameterSource parametros(long tutor,FiltrosSesionesRealizadas filtros){
        String patron="%"+filtros.busqueda().strip().replace("!","!!").replace("%","!%").replace("_","!_")+"%";
        return new MapSqlParameterSource().addValue("tutor",tutor).addValue("estado",filtros.estado(),java.sql.Types.VARCHAR)
            .addValue("desde",filtros.desde(),java.sql.Types.TIMESTAMP).addValue("hasta",filtros.hasta(),java.sql.Types.TIMESTAMP).addValue("patron",patron);
    }
    private static final String COLUMNAS="""
        SELECT s.id,s.estudiante_id,u.nombre estudiante,p.id periodo_id,p.nombre periodo,s.tema,
        COALESCE(s.inicio_real_en,s.inicio_en) inicio,COALESCE(s.fin_real_en,s.fin_en) fin,s.estado,
        COALESCE(s.ubicacion,s.enlace_virtual) lugar,s.observaciones,
        CASE WHEN s.estado='completada' THEN TIMESTAMPDIFF(MINUTE,COALESCE(s.inicio_real_en,s.inicio_en),COALESCE(s.fin_real_en,s.fin_en)) ELSE 0 END minutos
        """;
    private final RowMapper<RespuestaSesionRealizada> convertir=(fila,posicion)->new RespuestaSesionRealizada(fila.getLong("id"),fila.getLong("estudiante_id"),fila.getString("estudiante"),
        fila.getLong("periodo_id"),fila.getString("periodo"),fila.getString("tema"),fila.getObject("inicio",LocalDateTime.class),fila.getObject("fin",LocalDateTime.class),
        fila.getString("estado"),fila.getString("lugar"),fila.getString("observaciones"),fila.getLong("minutos"));
    public long contar(long tutor,FiltrosSesionesRealizadas filtros){return paginas.queryForObject("SELECT COUNT(*) "+BASE+FILTROS,parametros(tutor,filtros),Long.class);}
    public List<RespuestaSesionRealizada> listar(long tutor,FiltrosSesionesRealizadas filtros,int pagina){
        return paginas.query(COLUMNAS+BASE+FILTROS+" ORDER BY inicio DESC,s.id DESC LIMIT :tamano OFFSET :desplazamiento",
            parametros(tutor,filtros).addValue("tamano",filtros.tamano()).addValue("desplazamiento",(pagina-1L)*filtros.tamano()),convertir);
    }
    public RespuestaSesionRealizada buscar(long id,long tutor){
        return paginas.query(COLUMNAS+BASE+" AND s.id=:id",new MapSqlParameterSource().addValue("tutor",tutor).addValue("id",id),convertir).stream().findFirst().orElse(null);
    }
    public PaginaSesionesRealizadas.Resumen resumen(long tutor){
        return consultas.queryForObject("""
            SELECT COUNT(*) total,COALESCE(SUM(estado='completada'),0) completadas,COALESCE(SUM(estado='cancelada'),0) canceladas,
            COALESCE(SUM(estado='no_asistio'),0) no_asistio,
            ROUND(COALESCE(SUM(CASE WHEN estado='completada' THEN TIMESTAMPDIFF(MINUTE,COALESCE(inicio_real_en,inicio_en),COALESCE(fin_real_en,fin_en)) ELSE 0 END),0)/60,2) horas
            FROM sesiones WHERE tutor_id=? AND estado IN ('completada','cancelada','no_asistio')
            """,(fila,posicion)->new PaginaSesionesRealizadas.Resumen(fila.getLong("total"),fila.getLong("completadas"),fila.getLong("canceladas"),fila.getLong("no_asistio"),fila.getBigDecimal("horas")),tutor);
    }
    public List<OpcionRegistroSesion> opciones(long tutor,Long estudiante){
        return consultas.query("""
            SELECT e.id,u.nombre,p.id,p.nombre FROM asignaciones_tutor a JOIN estudiantes e ON e.id=a.estudiante_id
            JOIN usuarios u ON u.id=e.usuario_id JOIN periodos_academicos p ON p.id=a.periodo_id
            JOIN situaciones_academicas s ON s.estudiante_id=e.id AND s.periodo_id=p.id
            WHERE a.tutor_id=? AND a.estado='activa' AND u.estado='activo' AND p.estado='activo' AND s.estado_inscripcion='activo'
            AND (? IS NULL OR e.id=?) ORDER BY u.nombre,p.fecha_inicio DESC,e.id,p.id
            """,(fila,posicion)->new OpcionRegistroSesion(fila.getLong(1),fila.getString(2),fila.getLong(3),fila.getString(4)),tutor,estudiante,estudiante);
    }
    public boolean cruce(long estudiante,long tutor,LocalDateTime inicio,LocalDateTime fin,Long excluir){
        return !consultas.query("""
            SELECT id FROM sesiones WHERE (estudiante_id=? OR tutor_id=?) AND estado IN ('solicitada','confirmada','completada')
              AND COALESCE(inicio_real_en,inicio_en)<? AND COALESCE(fin_real_en,fin_en)>? AND (? IS NULL OR id<>?) FOR UPDATE
            """,(fila,posicion)->fila.getLong(1),estudiante,tutor,fin,inicio,excluir,excluir).isEmpty();
    }
    public long registrar(long tutor,long autor,SolicitudRegistrarSesion solicitud,String modalidad,String ubicacion,String enlace){
        var llave=new GeneratedKeyHolder();consultas.update(conexion->{var sentencia=conexion.prepareStatement("""
            INSERT INTO sesiones(tutor_id,estudiante_id,periodo_id,tema,inicio_en,fin_en,modalidad,ubicacion,enlace_virtual,estado,observaciones,inicio_real_en,fin_real_en,creado_por)
            VALUES (?,?,?,?,?,?,?,?,?,'completada',?,?,?,?)
            """,Statement.RETURN_GENERATED_KEYS);
            Object[] datos={tutor,solicitud.estudianteId(),solicitud.periodoId(),solicitud.tema().strip(),solicitud.inicioEn(),solicitud.finEn(),modalidad,ubicacion,enlace,
                solicitud.observaciones(),solicitud.inicioEn(),solicitud.finEn(),autor};
            for(int indice=0;indice<datos.length;indice++)sentencia.setObject(indice+1,datos[indice]);return sentencia;},llave);return llave.getKey().longValue();
    }
    public void cerrar(long id,SolicitudCerrarSesion solicitud){
        consultas.update("UPDATE sesiones SET estado=?,inicio_real_en=?,fin_real_en=?,observaciones=? WHERE id=?",
            solicitud.estado(),solicitud.estado().equals("completada")?solicitud.inicioRealEn():null,solicitud.estado().equals("completada")?solicitud.finRealEn():null,solicitud.observaciones(),id);
    }
    public void observacion(long sesion,long estudiante,long periodo,long autor,String contenido,LocalDateTime fecha){
        if(contenido==null||contenido.isBlank())return;
        consultas.update("""
            INSERT INTO observaciones_estudiante(situacion_id,estudiante_id,periodo_id,autor_id,sesion_id,contenido,observado_en)
            SELECT id,estudiante_id,periodo_id,?,?,?,? FROM situaciones_academicas WHERE estudiante_id=? AND periodo_id=?
            """,autor,sesion,contenido.strip(),fecha,estudiante,periodo);
    }
}
