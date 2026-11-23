package com.progweb.backend.seguridad;
import java.util.List;
import com.progweb.backend.entidades.Usuario;
import com.progweb.backend.repositorios.*;
import com.progweb.backend.respuestas.RespuestaUsuario;
import com.progweb.backend.servicios.*;
import com.progweb.backend.solicitudes.SolicitudElegirTutor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @Transactional
class EleccionTutorPruebas {
 @Autowired ServicioEleccionTutor servicio;
 @Autowired ServicioEstudiantesTutor tutorados;
 @Autowired RepositorioAsignaciones asignaciones;
 @Autowired RepositorioUsuarios usuarios;
 @Autowired RepositorioRoles roles;
 @Autowired ServicioTokens tokens;
 @Autowired JdbcTemplate consultas;
 @Autowired WebApplicationContext contexto;
 @Autowired FilterChainProxy filtro;
 long usuario,estudiante,carrera,periodo,tutor,usuarioTutor,departamento;MockMvc cliente;
 long insertar(String sql,Object... datos){consultas.update(sql,datos);return consultas.queryForObject("SELECT LAST_INSERT_ID()",Long.class);}
 long cuenta(String nombre,String correo,String rol){var cuenta=new Usuario(nombre,correo,null);cuenta.asignarRol(roles.buscarPorCodigo(rol).orElseThrow());usuarios.saveAndFlush(cuenta);return cuenta.obtenerId();}
 @BeforeEach void preparar(){
  cliente=MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtro).build();
  usuario=cuenta("Elección estudiante","eleccion-estudiante@example.invalid","estudiante");
  estudiante=insertar("INSERT INTO estudiantes(usuario_id,matricula) VALUES (?,?)",usuario,"ELECCION-PRUEBA");
  departamento=insertar("INSERT INTO departamentos(nombre) VALUES (?)","Departamento elección prueba");
  carrera=insertar("INSERT INTO carreras(departamento_id,clave,nombre) VALUES (?,?,?)",departamento,"ELECCION-PRUEBA","Carrera elección prueba");
  periodo=insertar("INSERT INTO periodos_academicos(clave,nombre,fecha_inicio,fecha_fin,estado) VALUES (?,?,?,?,?)","ELECCION-PRUEBA","Periodo elección prueba","2020-01-01","2099-12-31","activo");
  insertar("INSERT INTO situaciones_academicas(estudiante_id,periodo_id,carrera_id,semestre) VALUES (?,?,?,?)",estudiante,periodo,carrera,3);
  usuarioTutor=cuenta("Elección tutor","eleccion-tutor@example.invalid","tutor");
  tutor=insertar("INSERT INTO tutores(usuario_id,departamento_id) VALUES (?,?)",usuarioTutor,departamento);
 }
 String identidad(){return String.valueOf(usuario);}
 String token(String rol){return tokens.crearToken(new RespuestaUsuario(usuario,"Elección","eleccion-estudiante@example.invalid",null,List.of(rol))).token();}
 @Test void guardaAsignacionPropiaVisibleParaCoordinacionYTutor(){
  var respuesta=servicio.elegir(identidad(),new SolicitudElegirTutor(tutor,periodo));
  assertThat(respuesta.asignado().id()).isEqualTo(tutor);
  assertThat(servicio.consultar(identidad(),periodo,1,5,"").asignado().id()).isEqualTo(tutor);
  assertThat(consultas.queryForObject("SELECT asignado_por FROM asignaciones_tutor WHERE estudiante_id=? AND periodo_id=? AND estado='activa'",Long.class,estudiante,periodo)).isEqualTo(usuario);
  assertThat(asignaciones.listarPagina(periodo,"activa","ELECCION-PRUEBA",5,0)).hasSize(1);
  assertThat(tutorados.consultar(String.valueOf(usuarioTutor))).extracting(fila->fila.id()).contains(estudiante);
 }
 @Test void impideSegundaAsignacionYRespetaLaDeCoordinacion(){
  asignaciones.crear(estudiante,tutor,periodo,usuario,"Asignación previa");
  assertThatThrownBy(()->servicio.elegir(identidad(),new SolicitudElegirTutor(tutor,periodo))).hasMessageContaining("Ya tienes un tutor");
  assertThat(consultas.queryForObject("SELECT COUNT(*) FROM asignaciones_tutor WHERE estudiante_id=? AND estado='activa'",Long.class,estudiante)).isEqualTo(1);
 }
 @Test void rechazaTutorInactivo(){
  consultas.update("UPDATE usuarios SET estado='inactivo' WHERE id=?",usuarioTutor);
  assertThat(servicio.consultar(identidad(),periodo,1,5,"eleccion-tutor").totalRegistros()).isZero();
  assertThatThrownBy(()->servicio.elegir(identidad(),new SolicitudElegirTutor(tutor,periodo))).hasMessageContaining("ya no está disponible");
 }
 @Test void rechazaPeriodoCerrado(){
  consultas.update("UPDATE periodos_academicos SET estado='cerrado' WHERE id=?",periodo);
  assertThatThrownBy(()->servicio.elegir(identidad(),new SolicitudElegirTutor(tutor,periodo))).hasMessageContaining("inscripción activa");
 }
 @Test void rechazaInscripcionDeBajaYSinExpediente(){
  consultas.update("UPDATE situaciones_academicas SET estado_inscripcion='baja' WHERE estudiante_id=?",estudiante);
  assertThatThrownBy(()->servicio.elegir(identidad(),new SolicitudElegirTutor(tutor,periodo))).hasMessageContaining("inscripción activa");
 }
 @Test void otroPeriodoNoPuedeUsarse(){
  long otro=insertar("INSERT INTO periodos_academicos(clave,nombre,fecha_inicio,fecha_fin,estado) VALUES (?,?,?,?,?)","ELECCION-OTRO","Otro periodo","2020-01-01","2099-12-31","activo");
  assertThatThrownBy(()->servicio.elegir(identidad(),new SolicitudElegirTutor(tutor,otro))).hasMessageContaining("inscripción activa");
 }
 @Test void paginaYBuscaDesdeServidor(){
  for(int indice=0;indice<6;indice++)insertar("INSERT INTO tutores(usuario_id,departamento_id) VALUES (?,?)",cuenta("Grupo elección "+indice,"grupo-eleccion-"+indice+"@example.invalid","tutor"),departamento);
  var primera=servicio.consultar(identidad(),periodo,1,5,"Grupo eleccion");
  var segunda=servicio.consultar(identidad(),periodo,2,5,"Grupo eleccion");
  assertThat(primera.totalRegistros()).isEqualTo(6);assertThat(primera.totalPaginas()).isEqualTo(2);
  assertThat(primera.contenido()).hasSize(5);assertThat(segunda.contenido()).hasSize(1);
  assertThat(primera.contenido()).extracting(fila->fila.id()).doesNotContain(segunda.contenido().getFirst().id());
  assertThat(servicio.consultar(identidad(),periodo,99,5,"Grupo eleccion").pagina()).isEqualTo(2);
  assertThat(servicio.consultar(identidad(),periodo,1,5,"%_' OR 1=1").totalRegistros()).isZero();
 }
 @Test void noPermiteCuentaInactiva(){
  var cuenta=usuarios.findById(usuario).orElseThrow();cuenta.desactivar();usuarios.flush();
  assertThatThrownBy(()->servicio.elegir(identidad(),new SolicitudElegirTutor(tutor,periodo))).hasMessageContaining("inactiva");
 }
 @Test void sinExpedienteNoOfrecePeriodoNiTutores(){
  consultas.update("DELETE FROM situaciones_academicas WHERE estudiante_id=?",estudiante);
  var respuesta=servicio.consultar(identidad(),null,1,5,"");
  assertThat(respuesta.periodos()).isEmpty();assertThat(respuesta.periodoId()).isNull();assertThat(respuesta.contenido()).isEmpty();
 }
 @Test void permisosYValidacionHttp() throws Exception {
  cliente.perform(get("/api/estudiante/eleccion-tutor")).andExpect(status().isUnauthorized());
  cliente.perform(get("/api/estudiante/eleccion-tutor").header("Authorization","Bearer "+token("tutor"))).andExpect(status().isForbidden());
  cliente.perform(get("/api/estudiante/eleccion-tutor").header("Authorization","Bearer "+token("estudiante")).param("pagina","0")).andExpect(status().isUnprocessableEntity());
  cliente.perform(post("/api/estudiante/eleccion-tutor").header("Authorization","Bearer "+token("estudiante")).contentType("application/json").content("{\"tutorId\":0,\"periodoId\":0}"))
   .andExpect(status().isUnprocessableEntity());
 }
}
