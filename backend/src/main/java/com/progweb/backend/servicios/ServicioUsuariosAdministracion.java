package com.progweb.backend.servicios;
import java.util.*;
import java.nio.charset.StandardCharsets;
import com.progweb.backend.errores.*;
import com.progweb.backend.respuestas.*;
import com.progweb.backend.solicitudes.SolicitudUsuarioAdministracion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional(readOnly=true)
public class ServicioUsuariosAdministracion {
 private final JdbcTemplate consultas;private final PasswordEncoder codificador;private final ServicioAutenticacion autenticacion;
 public ServicioUsuariosAdministracion(JdbcTemplate consultas,PasswordEncoder codificador,ServicioAutenticacion autenticacion){this.consultas=consultas;this.codificador=codificador;this.autenticacion=autenticacion;}
 private static final String BASE=" FROM usuarios u LEFT JOIN estudiantes e ON e.usuario_id=u.id LEFT JOIN tutores t ON t.usuario_id=u.id ";
 private static final String COLUMNAS="SELECT u.id,u.nombre,u.correo,u.estado,e.matricula,t.departamento_id,(SELECT GROUP_CONCAT(r.codigo ORDER BY r.codigo) FROM usuario_roles ur JOIN roles r ON r.id=ur.rol_id WHERE ur.usuario_id=u.id) roles,EXISTS(SELECT 1 FROM identidades_externas x WHERE x.usuario_id=u.id AND x.proveedor='google') google ";
 private UsuarioAdministracion convertir(java.sql.ResultSet f,int posicion)throws java.sql.SQLException {String roles=f.getString("roles");return new UsuarioAdministracion(f.getLong("id"),f.getString("nombre"),f.getString("correo"),f.getString("estado"),roles==null?List.of():Arrays.asList(roles.split(",")),f.getString("matricula"),f.getObject("departamento_id",Long.class),f.getBoolean("google"));}
 public PaginaAdministracion<UsuarioAdministracion> listar(int pagina,int tamano,String busqueda,String rol,String estado){
  ServicioCatalogosAdministracion.validarPagina(pagina,tamano,busqueda);
  if(!rol.isEmpty()&&!List.of("coordinador","tutor","estudiante").contains(rol))throw new SolicitudRechazada(422,"Selecciona un rol válido.","rol");
  if(!estado.isEmpty()&&!List.of("activo","inactivo").contains(estado))throw new SolicitudRechazada(422,"Selecciona un estado válido.","estado");
  String filtro=" WHERE (u.nombre LIKE ? OR u.correo LIKE ?) AND (?='' OR u.estado=?) AND (?='' OR EXISTS(SELECT 1 FROM usuario_roles ur JOIN roles r ON r.id=ur.rol_id WHERE ur.usuario_id=u.id AND r.codigo=?))";
  String buscar="%"+busqueda.strip()+"%";Object[] argumentos={buscar,buscar,estado,estado,rol,rol};
  long total=consultas.queryForObject("SELECT COUNT(*)"+BASE+filtro,Long.class,argumentos);int paginas=(int)((total+tamano-1)/tamano),actual=Math.min(pagina,Math.max(1,paginas));
  var parametros=new ArrayList<>(Arrays.asList(argumentos));parametros.add(tamano);parametros.add((actual-1L)*tamano);
  return new PaginaAdministracion<>(consultas.query(COLUMNAS+BASE+filtro+" ORDER BY u.id DESC LIMIT ? OFFSET ?",this::convertir,parametros.toArray()),actual,tamano,total,paginas,Map.of("total",consultas.queryForObject("SELECT COUNT(*) FROM usuarios",Long.class),"activos",consultas.queryForObject("SELECT COUNT(*) FROM usuarios WHERE estado='activo'",Long.class),"roles",consultas.queryForObject("SELECT COUNT(*) FROM roles",Long.class)));
 }
 public UsuarioAdministracion consultar(long id){return consultas.query(COLUMNAS+BASE+" WHERE u.id=?",this::convertir,id).stream().findFirst().orElseThrow(()->new AccesoRechazado(404,"No se encontró el usuario."));}
 private void rechazar(String mensaje,String campo){throw new SolicitudRechazada(409,mensaje,campo);}
 @Transactional public UsuarioAdministracion guardar(Long id,SolicitudUsuarioAdministracion datos,String sujeto){
  var autor=autenticacion.consultarSesion(sujeto);if(!autor.roles().contains("coordinador"))throw new AccesoRechazado(403,"Se requiere coordinación.");
  consultas.queryForList("SELECT u.id FROM usuarios u JOIN usuario_roles ur ON ur.usuario_id=u.id JOIN roles r ON r.id=ur.rol_id WHERE r.codigo='coordinador' ORDER BY u.id FOR UPDATE",Long.class);
  UsuarioAdministracion anterior=null;
  if(id!=null){consultas.queryForList("SELECT id FROM usuarios WHERE id=? FOR UPDATE",Long.class,id);anterior=consultar(id);}
  String correo=datos.correo().strip().toLowerCase(Locale.ROOT);
  if(!consultas.queryForList("SELECT id FROM usuarios WHERE correo=? AND (? IS NULL OR id<>?)",Long.class,correo,id,id).isEmpty())rechazar("Ya hay una cuenta con este correo.","correo");
  if(anterior!=null&&anterior.vinculadoGoogle()&&!anterior.correo().equals(correo))rechazar("El correo de una cuenta vinculada con Google no se puede cambiar.","correo");
  boolean cambioRol=anterior!=null&&(!anterior.roles().equals(List.of(datos.rol())));
  if(anterior!=null&&anterior.id()==autor.id()&&(cambioRol||datos.estado().equals("inactivo")))rechazar("No puedes quitar tu propio acceso a coordinación.","estado");
  if(anterior!=null&&anterior.roles().contains("coordinador")&&(cambioRol||datos.estado().equals("inactivo"))&&consultas.queryForObject("SELECT COUNT(*) FROM usuarios u JOIN usuario_roles ur ON ur.usuario_id=u.id JOIN roles r ON r.id=ur.rol_id WHERE r.codigo='coordinador' AND u.estado='activo' AND u.id<>?",Long.class,id)==0)rechazar("Debe quedar una cuenta de coordinación activa.","rol");
  if(cambioRol){long pendientes=consultas.queryForObject("SELECT COUNT(*) FROM sesiones s LEFT JOIN tutores t ON t.id=s.tutor_id LEFT JOIN estudiantes e ON e.id=s.estudiante_id WHERE (t.usuario_id=? OR e.usuario_id=?) AND s.estado IN ('solicitada','confirmada')",Long.class,id,id);long asignaciones=consultas.queryForObject("SELECT COUNT(*) FROM asignaciones_tutor a LEFT JOIN tutores t ON t.id=a.tutor_id LEFT JOIN estudiantes e ON e.id=a.estudiante_id WHERE (t.usuario_id=? OR e.usuario_id=?) AND a.estado IN ('activa','pendiente')",Long.class,id,id);if(pendientes+asignaciones>0)rechazar("Finaliza las asignaciones y sesiones pendientes antes de cambiar el rol.","rol");}
  if(datos.contrasena()!=null&&datos.contrasena().getBytes(StandardCharsets.UTF_8).length>72)throw new SolicitudRechazada(422,"La contraseña es demasiado larga.","contrasena");
  if(id==null){if(datos.contrasena()==null)throw new SolicitudRechazada(422,"Escribe una contraseña.","contrasena");consultas.update("INSERT INTO usuarios(nombre,correo,password_hash,estado) VALUES (?,?,?,?)",datos.nombre().strip(),correo,codificador.encode(datos.contrasena()),datos.estado());id=consultas.queryForObject("SELECT LAST_INSERT_ID()",Long.class);}
  else {consultas.update("UPDATE usuarios SET nombre=?,correo=?,estado=?,version_sesion=version_sesion+? WHERE id=?",datos.nombre().strip(),correo,datos.estado(),cambioRol||!anterior.estado().equals(datos.estado())||!anterior.correo().equals(correo)||datos.contrasena()!=null?1:0,id);if(!anterior.correo().equals(correo))consultas.update("DELETE FROM recuperaciones_contrasena WHERE usuario_id=?",id);if(datos.contrasena()!=null){consultas.update("UPDATE usuarios SET password_hash=? WHERE id=?",codificador.encode(datos.contrasena()),id);consultas.update("DELETE FROM recuperaciones_contrasena WHERE usuario_id=?",id);}}
  if(datos.rol().equals("tutor")){if((anterior==null||!Objects.equals(anterior.departamentoId(),datos.departamentoId()))&&consultas.queryForObject("SELECT COUNT(*) FROM departamentos WHERE id=? AND activo=1",Long.class,datos.departamentoId())==0)rechazar("Selecciona un departamento activo.","departamentoId");consultas.update("INSERT INTO tutores(usuario_id,departamento_id) VALUES (?,?) ON DUPLICATE KEY UPDATE departamento_id=?",id,datos.departamentoId(),datos.departamentoId());}
  if(datos.rol().equals("estudiante")){String matricula=datos.matricula().strip();if(!consultas.queryForList("SELECT usuario_id FROM estudiantes WHERE matricula=? AND usuario_id<>?",Long.class,matricula,id).isEmpty())rechazar("La matrícula ya está registrada.","matricula");if(consultas.queryForObject("SELECT COUNT(*) FROM estudiantes WHERE usuario_id=?",Long.class,id)==0)consultas.update("INSERT INTO estudiantes(usuario_id,matricula) VALUES (?,?)",id,matricula);else consultas.update("UPDATE estudiantes SET matricula=? WHERE usuario_id=?",matricula,id);}
  consultas.update("DELETE FROM usuario_roles WHERE usuario_id=?",id);consultas.update("INSERT INTO usuario_roles(usuario_id,rol_id) SELECT ?,id FROM roles WHERE codigo=?",id,datos.rol());return consultar(id);
 }
}
