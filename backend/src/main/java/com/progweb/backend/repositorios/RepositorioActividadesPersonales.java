package com.progweb.backend.repositorios;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.progweb.backend.respuestas.RespuestaActividadPersonal;
import com.progweb.backend.solicitudes.SolicitudActividadPersonal;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
@Repository
public class RepositorioActividadesPersonales {
    private final JdbcTemplate consultas;
    public RepositorioActividadesPersonales(JdbcTemplate consultas){this.consultas=consultas;}
    private static final String BASE="""
        SELECT a.*,COALESCE(u.nombre,a.responsable_texto) responsable FROM actividades_personales a
        LEFT JOIN tutores t ON t.id=a.tutor_id LEFT JOIN usuarios u ON u.id=t.usuario_id
        """;
    private final RowMapper<RespuestaActividadPersonal> convertir=(fila,posicion)->new RespuestaActividadPersonal(fila.getLong("id"),fila.getString("titulo"),fila.getString("descripcion"),fila.getString("tipo"),
        fila.getObject("fecha",LocalDate.class),fila.getObject("inicio_en",LocalDateTime.class),fila.getObject("fin_en",LocalDateTime.class),fila.getString("responsable"),fila.getString("ubicacion"),fila.getString("estado"));
    public List<RespuestaActividadPersonal> listar(long usuario,String tipo){return consultas.query(BASE+" WHERE a.propietario_id=? AND a.tipo=? AND a.estado='pendiente' ORDER BY COALESCE(a.fecha,DATE(a.inicio_en)) IS NULL,COALESCE(a.fecha,DATE(a.inicio_en)),a.inicio_en,a.id",convertir,usuario,tipo);}
    public RespuestaActividadPersonal buscar(long usuario,long id){return consultas.query(BASE+" WHERE a.propietario_id=? AND a.id=?",convertir,usuario,id).stream().findFirst().orElse(null);}
    public long crear(long usuario,SolicitudActividadPersonal solicitud){
        var llave=new GeneratedKeyHolder();consultas.update(conexion->{var sentencia=conexion.prepareStatement("INSERT INTO actividades_personales(propietario_id,titulo,descripcion,tipo,fecha,inicio_en,fin_en,responsable_texto,ubicacion) VALUES (?,?,?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS);
            Object[] datos={usuario,solicitud.titulo().strip(),solicitud.descripcion(),solicitud.tipo(),solicitud.fecha(),solicitud.inicioEn(),solicitud.finEn(),solicitud.responsable(),solicitud.ubicacion()};
            for(int indice=0;indice<datos.length;indice++)sentencia.setObject(indice+1,datos[indice]);return sentencia;},llave);return llave.getKey().longValue();
    }
    public boolean editar(long usuario,long id,SolicitudActividadPersonal solicitud){return consultas.update("UPDATE actividades_personales SET titulo=?,descripcion=?,tipo=?,fecha=?,inicio_en=?,fin_en=?,responsable_texto=?,tutor_id=NULL,ubicacion=? WHERE id=? AND propietario_id=?",
        solicitud.titulo().strip(),solicitud.descripcion(),solicitud.tipo(),solicitud.fecha(),solicitud.inicioEn(),solicitud.finEn(),solicitud.responsable(),solicitud.ubicacion(),id,usuario)>0;}
    public boolean eliminar(long usuario,long id){return consultas.update("DELETE FROM actividades_personales WHERE id=? AND propietario_id=?",id,usuario)>0;}
}
