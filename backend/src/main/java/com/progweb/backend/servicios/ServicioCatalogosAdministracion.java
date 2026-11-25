package com.progweb.backend.servicios;
import java.util.*;
import java.time.LocalDate;
import com.progweb.backend.errores.*;
import com.progweb.backend.respuestas.*;
import com.progweb.backend.solicitudes.SolicitudCatalogoAdministracion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional(readOnly=true)
public class ServicioCatalogosAdministracion {
 private final JdbcTemplate consultas;
 public ServicioCatalogosAdministracion(JdbcTemplate consultas){this.consultas=consultas;}
 public static void validarPagina(int pagina,int tamano,String busqueda){if(pagina<1||tamano<1||tamano>100)throw new SolicitudRechazada(422,"Revisa la página solicitada.","pagina");if(busqueda.length()>160)throw new SolicitudRechazada(422,"La búsqueda es demasiado larga.","busqueda");}
 private record Configuracion(String tabla,String activo,String columnas){}
 private Configuracion configuracion(String tipo){return switch(tipo){case "carreras"->new Configuracion("carreras","activa","c.clave,c.departamento_id,d.nombre departamento,NULL inicio,NULL fin");case "departamentos"->new Configuracion("departamentos","activo","NULL clave,NULL departamento_id,NULL departamento,NULL inicio,NULL fin");case "periodos"->new Configuracion("periodos_academicos","estado","c.clave,NULL departamento_id,NULL departamento,c.fecha_inicio inicio,c.fecha_fin fin");case "motivos"->new Configuracion("motivos_reporte","activo","c.codigo clave,NULL departamento_id,NULL departamento,NULL inicio,NULL fin");default->throw new AccesoRechazado(404,"No se encontró ese catálogo.");};}
 private String base(String tipo){return " FROM "+configuracion(tipo).tabla()+" c"+(tipo.equals("carreras")?" JOIN departamentos d ON d.id=c.departamento_id":"");}
 private String estado(String tipo){return tipo.equals("periodos")?"c.estado":"CASE WHEN c."+configuracion(tipo).activo()+"=1 THEN 'activo' ELSE 'inactivo' END";}
 private String columnas(String tipo){return "SELECT c.id,c.nombre,"+configuracion(tipo).columnas()+","+estado(tipo)+" estado";}
 private CatalogoAdministracion convertir(java.sql.ResultSet f,int posicion)throws java.sql.SQLException{return new CatalogoAdministracion(f.getLong("id"),f.getString("nombre"),f.getString("clave"),f.getObject("departamento_id",Long.class),f.getString("departamento"),f.getString("estado"),f.getObject("inicio",LocalDate.class),f.getObject("fin",LocalDate.class));}
 public PaginaAdministracion<CatalogoAdministracion> listar(String tipo,int pagina,int tamano,String busqueda,String filtro){validarPagina(pagina,tamano,busqueda);configuracion(tipo);if(!filtro.isEmpty()&&!(tipo.equals("periodos")?List.of("planeado","activo","cerrado"):List.of("activo","inactivo")).contains(filtro))throw new SolicitudRechazada(422,"Selecciona un estado válido.","estado");String donde=" WHERE c.nombre LIKE ? AND (?='' OR "+estado(tipo)+"=?)";String buscar="%"+busqueda.strip()+"%";long total=consultas.queryForObject("SELECT COUNT(*)"+base(tipo)+donde,Long.class,buscar,filtro,filtro);int paginas=(int)((total+tamano-1)/tamano),actual=Math.min(pagina,Math.max(1,paginas));return new PaginaAdministracion<>(consultas.query(columnas(tipo)+base(tipo)+donde+" ORDER BY c.nombre,c.id LIMIT ? OFFSET ?",this::convertir,buscar,filtro,filtro,tamano,(actual-1L)*tamano),actual,tamano,total,paginas);}
 public CatalogoAdministracion consultar(String tipo,long id){return consultas.query(columnas(tipo)+base(tipo)+" WHERE c.id=?",this::convertir,id).stream().findFirst().orElseThrow(()->new AccesoRechazado(404,"No se encontró el registro."));}
 @Transactional public CatalogoAdministracion guardar(String tipo,Long id,SolicitudCatalogoAdministracion datos){
  var config=configuracion(tipo);if(id!=null){consultas.queryForList("SELECT id FROM "+config.tabla()+" WHERE id=? FOR UPDATE",Long.class,id);consultar(tipo,id);}
  String nombre=datos.nombre().strip();int limite=tipo.equals("periodos")?80:tipo.equals("carreras")?150:120;if(nombre.length()>limite)throw new SolicitudRechazada(422,"El nombre es demasiado largo.","nombre");
  boolean periodo=tipo.equals("periodos");if(!(periodo?List.of("planeado","activo","cerrado"):List.of("activo","inactivo")).contains(datos.estado()))throw new SolicitudRechazada(422,"Selecciona un estado válido.","estado");
  var valores=new ArrayList<Object>();var columnas=new ArrayList<String>();columnas.add("nombre");valores.add(nombre);
  if(!tipo.equals("departamentos")){if(datos.clave()==null||datos.clave().isBlank())throw new SolicitudRechazada(422,"Escribe la clave.","clave");columnas.add(tipo.equals("motivos")?"codigo":"clave");valores.add(datos.clave().strip().toUpperCase(Locale.ROOT));}
  if(tipo.equals("carreras")){if(datos.departamentoId()==null||consultas.queryForObject("SELECT COUNT(*) FROM departamentos WHERE id=? AND activo=1",Long.class,datos.departamentoId())==0)throw new SolicitudRechazada(422,"Selecciona un departamento activo.","departamentoId");columnas.add("departamento_id");valores.add(datos.departamentoId());}
  if(periodo){if(datos.inicio()==null||datos.fin()==null)throw new SolicitudRechazada(422,"Selecciona las fechas del periodo.","inicio");columnas.add("fecha_inicio");valores.add(datos.inicio());columnas.add("fecha_fin");valores.add(datos.fin());if(id!=null&&datos.estado().equals("cerrado")&&consultas.queryForObject("SELECT COUNT(*) FROM sesiones WHERE periodo_id=? AND estado IN ('solicitada','confirmada')",Long.class,id)>0)throw new SolicitudRechazada(409,"Resuelve las sesiones pendientes antes de cerrar el periodo.","estado");}
  columnas.add(config.activo());valores.add(periodo?datos.estado():datos.estado().equals("activo"));
  if(id==null){consultas.update("INSERT INTO "+config.tabla()+" ("+String.join(",",columnas)+") VALUES ("+String.join(",",Collections.nCopies(columnas.size(),"?"))+")",valores.toArray());id=consultas.queryForObject("SELECT LAST_INSERT_ID()",Long.class);}
  else {valores.add(id);consultas.update("UPDATE "+config.tabla()+" SET "+String.join(",",columnas.stream().map(c->c+"=?").toList())+" WHERE id=?",valores.toArray());}
  return consultar(tipo,id);
 }
 @Transactional public CatalogoAdministracion desactivar(String tipo,long id){var actual=consultar(tipo,id);return guardar(tipo,id,new SolicitudCatalogoAdministracion(actual.nombre(),actual.clave(),actual.departamentoId(),tipo.equals("periodos")?"cerrado":"inactivo",actual.inicio(),actual.fin()));}
}
