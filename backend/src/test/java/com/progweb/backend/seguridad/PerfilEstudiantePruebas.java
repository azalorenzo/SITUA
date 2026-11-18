package com.progweb.backend.seguridad;
import java.util.List;
import com.progweb.backend.entidades.Usuario;
import com.progweb.backend.repositorios.*;
import com.progweb.backend.respuestas.RespuestaUsuario;
import com.progweb.backend.servicios.*;
import com.progweb.backend.solicitudes.SolicitudCompletarPerfil;
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
class PerfilEstudiantePruebas {
 @Autowired ServicioPerfilEstudiante servicio;
 @Autowired RepositorioUsuarios usuarios;
 @Autowired RepositorioRoles roles;
 @Autowired ServicioTokens tokens;
 @Autowired JdbcTemplate consultas;
 @Autowired WebApplicationContext contexto;
 @Autowired FilterChainProxy filtro;
 long usuario,estudiante,carrera,periodo;MockMvc cliente;
 long insertar(String sql,Object... datos){consultas.update(sql,datos);return consultas.queryForObject("SELECT LAST_INSERT_ID()",Long.class);}
 @BeforeEach void preparar(){
  cliente=MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtro).build();
  var cuenta=new Usuario("Perfil prueba","perfil-prueba@example.invalid",null);cuenta.asignarRol(roles.buscarPorCodigo("estudiante").orElseThrow());usuarios.saveAndFlush(cuenta);usuario=cuenta.obtenerId();
  estudiante=insertar("INSERT INTO estudiantes(usuario_id,matricula) VALUES (?,?)",usuario,"PERFIL-PRUEBA");
  long departamento=insertar("INSERT INTO departamentos(nombre) VALUES (?)","Perfil departamento prueba");
  carrera=insertar("INSERT INTO carreras(departamento_id,clave,nombre) VALUES (?,?,?)",departamento,"PERFIL-PRUEBA","Perfil carrera prueba");
  periodo=insertar("INSERT INTO periodos_academicos(clave,nombre,fecha_inicio,fecha_fin,estado) VALUES (?,?,?,?,?)","PERFIL-PRUEBA","Perfil periodo prueba","2020-01-01","2099-12-31","activo");
 }
 String token(String rol){return tokens.crearToken(new RespuestaUsuario(usuario,"Perfil prueba","perfil-prueba@example.invalid",null,List.of(rol))).token();}
 @Test void guardaSoloElPerfilPropioUnaVez(){
  var antes=servicio.consultar(String.valueOf(usuario));assertThat(antes.completo()).isFalse();assertThat(antes.carreras()).extracting(fila->fila.id()).contains(carrera);
  assertThat(servicio.completar(String.valueOf(usuario),new SolicitudCompletarPerfil(carrera,periodo,3)).completo()).isTrue();
  servicio.completar(String.valueOf(usuario),new SolicitudCompletarPerfil(carrera,periodo,5));
  assertThat(consultas.queryForObject("SELECT semestre FROM situaciones_academicas WHERE estudiante_id=?",Integer.class,estudiante)).isEqualTo(3);
  assertThat(servicio.consultar(String.valueOf(usuario)).completo()).isTrue();
 }
 @Test void rechazaCatalogosInactivos(){
  consultas.update("UPDATE carreras SET activa=false WHERE id=?",carrera);
  assertThatThrownBy(()->servicio.completar(String.valueOf(usuario),new SolicitudCompletarPerfil(carrera,periodo,3))).hasMessageContaining("carrera disponible");
 }
 @Test void rechazaPeriodoCerrado(){
  consultas.update("UPDATE periodos_academicos SET estado='cerrado' WHERE id=?",periodo);
  assertThatThrownBy(()->servicio.completar(String.valueOf(usuario),new SolicitudCompletarPerfil(carrera,periodo,3))).hasMessageContaining("periodo disponible");
 }
 @Test void requiereSesionYRolEstudiante() throws Exception {
  cliente.perform(get("/api/estudiante/perfil")).andExpect(status().isUnauthorized());
  cliente.perform(get("/api/estudiante/perfil").header("Authorization","Bearer "+token("tutor"))).andExpect(status().isForbidden());
  cliente.perform(get("/api/estudiante/perfil").header("Authorization","Bearer "+token("estudiante"))).andExpect(status().isOk()).andExpect(jsonPath("$.completo").value(false));
 }
 @Test void validaSemestreAntesDeGuardar() throws Exception {
  cliente.perform(put("/api/estudiante/perfil").header("Authorization","Bearer "+token("estudiante")).contentType("application/json")
   .content("{\"carreraId\":"+carrera+",\"periodoId\":"+periodo+",\"semestre\":0}"))
   .andExpect(status().isUnprocessableEntity());
 }
}
