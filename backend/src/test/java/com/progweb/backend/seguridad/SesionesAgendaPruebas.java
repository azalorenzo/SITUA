package com.progweb.backend.seguridad;

import java.time.*;
import java.util.List;
import com.progweb.backend.entidades.Usuario;
import com.progweb.backend.repositorios.*;
import com.progweb.backend.respuestas.RespuestaUsuario;
import com.progweb.backend.servicios.*;
import com.progweb.backend.solicitudes.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @Transactional
class SesionesAgendaPruebas {
 @Autowired ServicioSesionesAgenda servicio;
 @Autowired ServicioVencimientoSesiones vencimiento;
 @Autowired ServicioHorarios horarios;
 @Autowired RepositorioUsuarios usuarios;
 @Autowired RepositorioRoles roles;
 @Autowired RepositorioAsignaciones asignaciones;
 @Autowired ServicioTokens tokens;
 @Autowired JdbcTemplate consultas;
 @Autowired WebApplicationContext contexto;
 @Autowired FilterChainProxy filtro;
 @MockitoBean(name="relojInicio") Clock reloj;
 long alumno,otroAlumno,estudiante,otroEstudiante,profesor,otroProfesor,tutor,otroTutor,periodo,horario,carrera,departamento;
 final LocalDateTime inicio=LocalDateTime.of(2030,1,10,16,0);
 MockMvc cliente;
 long insertar(String sql,Object... datos){consultas.update(sql,datos);return consultas.queryForObject("SELECT LAST_INSERT_ID()",Long.class);}
 long cuenta(String correo,String rol){var cuenta=new Usuario("Sesiones prueba",correo,null);cuenta.asignarRol(roles.buscarPorCodigo(rol).orElseThrow());usuarios.saveAndFlush(cuenta);return cuenta.obtenerId();}
 @BeforeEach void preparar(){
  when(reloj.instant()).thenReturn(Instant.parse("2030-01-01T12:00:00Z"));when(reloj.getZone()).thenReturn(ZoneOffset.UTC);
  cliente=MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtro).build();
  alumno=cuenta("sesiones-alumno@example.invalid","estudiante");otroAlumno=cuenta("sesiones-otro-alumno@example.invalid","estudiante");
  estudiante=insertar("INSERT INTO estudiantes(usuario_id,matricula) VALUES (?,?)",alumno,"SESIONES-ALUMNO");
  otroEstudiante=insertar("INSERT INTO estudiantes(usuario_id,matricula) VALUES (?,?)",otroAlumno,"SESIONES-OTRO");
  departamento=insertar("INSERT INTO departamentos(nombre) VALUES (?)","Departamento sesiones prueba");
  carrera=insertar("INSERT INTO carreras(departamento_id,clave,nombre) VALUES (?,?,?)",departamento,"SESIONES-PRUEBA","Carrera sesiones prueba");
  periodo=insertar("INSERT INTO periodos_academicos(clave,nombre,fecha_inicio,fecha_fin,estado) VALUES (?,?,?,?,?)","SESIONES-PRUEBA","Periodo sesiones prueba","2030-01-01","2030-12-31","activo");
  for(long id:List.of(estudiante,otroEstudiante))insertar("INSERT INTO situaciones_academicas(estudiante_id,periodo_id,carrera_id,semestre) VALUES (?,?,?,?)",id,periodo,carrera,3);
  profesor=cuenta("sesiones-profesor@example.invalid","tutor");otroProfesor=cuenta("sesiones-otro-profesor@example.invalid","tutor");
  tutor=insertar("INSERT INTO tutores(usuario_id,departamento_id) VALUES (?,?)",profesor,departamento);
  otroTutor=insertar("INSERT INTO tutores(usuario_id,departamento_id) VALUES (?,?)",otroProfesor,departamento);
  asignaciones.crear(estudiante,tutor,periodo,alumno,null);asignaciones.crear(otroEstudiante,tutor,periodo,otroAlumno,null);
  horario=crearHorario(tutor,inicio,inicio.plusHours(1));
 }
 long crearHorario(long tutor,LocalDateTime desde,LocalDateTime hasta){return insertar("INSERT INTO disponibilidades_tutor(tutor_id,inicio_en,fin_en,modalidad,ubicacion) VALUES (?,?,?,'presencial','Aula prueba')",tutor,desde,hasta);}
 String id(long usuario){return String.valueOf(usuario);}
 SolicitudReservarSesion solicitud(long disponibilidad){return new SolicitudReservarSesion(disponibilidad,periodo,"Tema prueba","Mensaje prueba");}
 String token(long usuario,String rol){return tokens.crearToken(new RespuestaUsuario(usuario,"Prueba","prueba@example.invalid",null,List.of(rol))).token();}
 @Test void reservaConfirmaCancelaYVuelveADisponer(){
  assertThat(servicio.disponibles(id(alumno),periodo,1,5).contenido()).extracting(fila->fila.id()).contains(horario);
  var sesion=servicio.solicitar(id(alumno),solicitud(horario));assertThat(sesion.estado()).isEqualTo("solicitada");
  assertThat(servicio.disponibles(id(otroAlumno),periodo,1,5).totalRegistros()).isZero();
  assertThat(servicio.listar(id(profesor),true,"solicitada",null,null,1,5).contenido()).extracting(fila->fila.id()).contains(sesion.id());
  assertThat(servicio.cambiar(id(profesor),true,sesion.id(),"confirmar").estado()).isEqualTo("confirmada");
  assertThat(servicio.cambiar(id(alumno),false,sesion.id(),"cancelar").estado()).isEqualTo("cancelada");
  assertThat(servicio.disponibles(id(otroAlumno),periodo,1,5).totalRegistros()).isEqualTo(1);
  assertThat(consultas.queryForObject("SELECT COUNT(*) FROM sesion_cambios WHERE sesion_id=?",Long.class,sesion.id())).isEqualTo(3);
  assertThat(servicio.resumen(id(alumno)).proximas()).isEmpty();
  assertThat(servicio.resumen(id(alumno)).actividades()).hasSize(3);
 }
 @Test void rechazoLiberaHorarioSinBorrarHistorial(){
  var primera=servicio.solicitar(id(alumno),solicitud(horario));
  servicio.cambiar(id(profesor),true,primera.id(),"rechazar");
  var segunda=servicio.solicitar(id(otroAlumno),solicitud(horario));
  assertThat(segunda.id()).isNotEqualTo(primera.id());assertThat(segunda.estado()).isEqualTo("solicitada");
  assertThat(consultas.queryForObject("SELECT estado FROM sesiones WHERE id=?",String.class,primera.id())).isEqualTo("rechazada");
 }
 @Test void tutorTambienPuedeCancelarUnaConfirmada(){
  var sesion=servicio.solicitar(id(alumno),solicitud(horario));servicio.cambiar(id(profesor),true,sesion.id(),"confirmar");
  assertThat(servicio.cambiar(id(profesor),true,sesion.id(),"cancelar").estado()).isEqualTo("cancelada");
 }
 @Test void noPermiteReservarDosVecesElMismoHorario(){
  servicio.solicitar(id(alumno),solicitud(horario));
  assertThatThrownBy(()->servicio.solicitar(id(otroAlumno),solicitud(horario))).hasMessageContaining("se cruza");
  assertThat(consultas.queryForObject("SELECT COUNT(*) FROM sesiones WHERE disponibilidad_id=?",Long.class,horario)).isEqualTo(1);
 }
 @Test void impideCrucesDelMismoEstudianteAunqueCambieTutorYPeriodo(){
  servicio.solicitar(id(alumno),solicitud(horario));
  consultas.update("UPDATE asignaciones_tutor SET estado='finalizada',fin_en=UTC_TIMESTAMP(6) WHERE estudiante_id=?",estudiante);
  asignaciones.crear(estudiante,otroTutor,periodo,alumno,null);
  long cruzado=crearHorario(otroTutor,inicio.plusMinutes(30),inicio.plusHours(2));
  assertThatThrownBy(()->servicio.solicitar(id(alumno),solicitud(cruzado))).hasMessageContaining("se cruza");
 }
 @Test void horariosContiguosSiSePermiten(){
  servicio.solicitar(id(alumno),solicitud(horario));long contiguo=crearHorario(tutor,inicio.plusHours(1),inicio.plusHours(2));
  assertThat(servicio.solicitar(id(alumno),solicitud(contiguo)).estado()).isEqualTo("solicitada");
 }
 @Test void noExponeNiCambiaSesionesDeOtraCuenta(){
  var sesion=servicio.solicitar(id(alumno),solicitud(horario));
  assertThat(servicio.listar(id(otroAlumno),false,null,null,null,1,5).contenido()).isEmpty();
  assertThatThrownBy(()->servicio.cambiar(id(otroAlumno),false,sesion.id(),"cancelar")).hasMessageContaining("No se encontró");
  assertThatThrownBy(()->servicio.cambiar(id(otroProfesor),true,sesion.id(),"confirmar")).hasMessageContaining("No se encontró");
 }
 @Test void exigeAsignacionActivaInscripcionYPeriodo(){
  consultas.update("UPDATE asignaciones_tutor SET estado='finalizada',fin_en=UTC_TIMESTAMP(6) WHERE estudiante_id=?",estudiante);
  assertThat(servicio.opciones(id(alumno))).isEmpty();
  assertThatThrownBy(()->servicio.solicitar(id(alumno),solicitud(horario))).hasMessageContaining("asignación activa");
 }
 @Test void rechazaPeriodoCerrado(){
  consultas.update("UPDATE periodos_academicos SET estado='cerrado' WHERE id=?",periodo);
  assertThatThrownBy(()->servicio.solicitar(id(alumno),solicitud(horario))).hasMessageContaining("asignación activa");
 }
 @Test void rechazaInscripcionDeBaja(){
  consultas.update("UPDATE situaciones_academicas SET estado_inscripcion='baja' WHERE estudiante_id=?",estudiante);
  assertThatThrownBy(()->servicio.solicitar(id(alumno),solicitud(horario))).hasMessageContaining("asignación activa");
 }
 @Test void rechazaTutorInactivoYHorarioBloqueado(){
  consultas.update("UPDATE disponibilidades_tutor SET estado='bloqueada' WHERE id=?",horario);
  assertThatThrownBy(()->servicio.solicitar(id(alumno),solicitud(horario))).hasMessageContaining("ya no está disponible");
 }
 @Test void unaSolicitudNoSeConfirmaTrasCambiarLaAsignacion(){
  var sesion=servicio.solicitar(id(alumno),solicitud(horario));
  consultas.update("UPDATE asignaciones_tutor SET estado='finalizada',fin_en=UTC_TIMESTAMP(6) WHERE estudiante_id=?",estudiante);
  assertThatThrownBy(()->servicio.cambiar(id(profesor),true,sesion.id(),"confirmar")).hasMessageContaining("asignación activa");
 }
 @Test void unAlumnoInactivoNoPuedeSolicitar(){
  var cuenta=usuarios.findById(alumno).orElseThrow();cuenta.desactivar();usuarios.flush();
  assertThatThrownBy(()->servicio.solicitar(id(alumno),solicitud(horario))).hasMessageContaining("inactiva");
 }
 @Test void noPermiteReservarEnElPasadoNiCancelarUnaSesionIniciada(){
  var sesion=servicio.solicitar(id(alumno),solicitud(horario));when(reloj.instant()).thenReturn(inicio.toInstant(ZoneOffset.UTC));
  assertThatThrownBy(()->servicio.cambiar(id(alumno),false,sesion.id(),"cancelar")).hasMessageContaining("ya no permite");
  assertThatThrownBy(()->servicio.solicitar(id(otroAlumno),solicitud(horario))).hasMessageContaining("ya no está disponible");
 }
 @Test void noRepiteTransicionesNiReabreEstadosFinales(){
  var sesion=servicio.solicitar(id(alumno),solicitud(horario));servicio.cambiar(id(profesor),true,sesion.id(),"rechazar");
  assertThatThrownBy(()->servicio.cambiar(id(profesor),true,sesion.id(),"confirmar")).hasMessageContaining("ya no permite");
 }
 @Test void conservaHorarioAsociadoAunqueSeCanceleSesion(){
  var sesion=servicio.solicitar(id(alumno),solicitud(horario));servicio.cambiar(id(alumno),false,sesion.id(),"cancelar");
  assertThatThrownBy(()->horarios.eliminar(horario,id(profesor))).hasMessageContaining("sesiones registradas");
 }
 @Test void noEditaUnHorarioReservado(){
  servicio.solicitar(id(alumno),solicitud(horario));
  assertThatThrownBy(()->horarios.editar(horario,new SolicitudHorario(inicio.plusHours(1),inicio.plusHours(2),"Aula"),id(profesor))).hasMessageContaining("sesiones registradas");
 }
 @Test void agendaYFiltrosSeAplicanEnElServidor(){
  var sesion=servicio.solicitar(id(alumno),solicitud(horario));
  assertThat(servicio.agenda(id(alumno),inicio.toLocalDate().atStartOfDay(),inicio.toLocalDate().plusDays(1).atStartOfDay())).hasSize(1);
  assertThat(servicio.agenda(id(alumno),inicio.plusDays(1),inicio.plusDays(2))).isEmpty();
  assertThat(servicio.listar(id(alumno),false,"confirmada",null,null,1,5).totalRegistros()).isZero();
  assertThat(servicio.listar(id(alumno),false,"solicitada",null,null,1,5).contenido().getFirst().id()).isEqualTo(sesion.id());
  assertThatThrownBy(()->servicio.agenda(id(alumno),inicio,inicio.plusMonths(4))).hasMessageContaining("intervalo válido");
 }
 @Test void permisosYValidacionHttp() throws Exception {
  cliente.perform(get("/api/estudiante/sesiones")).andExpect(status().isUnauthorized());
  cliente.perform(get("/api/tutor/sesiones").header("Authorization","Bearer "+token(alumno,"estudiante"))).andExpect(status().isForbidden());
  cliente.perform(post("/api/estudiante/sesiones").header("Authorization","Bearer "+token(alumno,"estudiante")).contentType("application/json").content("{}"))
   .andExpect(status().isUnprocessableEntity());
  cliente.perform(get("/api/estudiante/sesiones").header("Authorization","Bearer "+token(alumno,"estudiante")).param("estado","otro"))
   .andExpect(status().isUnprocessableEntity());
 }
 @Test void venceAlLlegarLaHoraPeroNoAntes(){
  var sesion=servicio.solicitar(id(alumno),solicitud(horario));
  assertThat(vencimiento.vencerPropias(estudiante,false,inicio.minusNanos(1000))).isZero();
  assertThat(vencimiento.vencerPropias(estudiante,false,inicio)).isEqualTo(1);
  assertThat(consultas.queryForObject("SELECT estado FROM sesiones WHERE id=?",String.class,sesion.id())).isEqualTo("vencida");
  assertThat(consultas.queryForObject("SELECT reserva_vigente FROM sesiones WHERE id=?",Long.class,sesion.id())).isNull();
 }
 @Test void consultarVencidasGuardaUnSoloCambioAutomatico() throws Exception {
  var sesion=servicio.solicitar(id(alumno),solicitud(horario));
  when(reloj.instant()).thenReturn(inicio.toInstant(ZoneOffset.UTC));
  for(int intento=0;intento<2;intento++)cliente.perform(get("/api/estudiante/sesiones").param("estado","vencida").header("Authorization","Bearer "+token(alumno,"estudiante")))
    .andExpect(status().isOk()).andExpect(jsonPath("$.totalRegistros").value(1)).andExpect(jsonPath("$.contenido[0].estado").value("vencida"))
    .andExpect(jsonPath("$.contenido[0].puedeCancelar").value(false)).andExpect(jsonPath("$.contenido[0].puedeResponder").value(false))
    .andExpect(jsonPath("$.contenido[0].puedeRegistrar").value(false));
  assertThat(consultas.queryForObject("SELECT COUNT(*) FROM sesion_cambios WHERE sesion_id=? AND tipo='vencimiento' AND actor_id IS NULL AND estado_nuevo='vencida'",Long.class,sesion.id())).isEqualTo(1);
  assertThat(consultas.queryForObject("SELECT ocurrido_en FROM sesion_cambios WHERE sesion_id=? AND tipo='vencimiento'",LocalDateTime.class,sesion.id())).isEqualTo(inicio);
 }
 @Test void lasConfirmadasNoVencen(){
  var sesion=servicio.solicitar(id(alumno),solicitud(horario));
  servicio.cambiar(id(profesor),true,sesion.id(),"confirmar");
  assertThat(vencimiento.vencerPropias(tutor,true,inicio.plusHours(2))).isZero();
  when(reloj.instant()).thenReturn(inicio.plusHours(2).toInstant(ZoneOffset.UTC));
  var confirmadas=servicio.listar(id(profesor),true,"confirmada",null,null,1,5);
  assertThat(confirmadas.contenido()).hasSize(1);
  assertThat(confirmadas.contenido().getFirst().puedeRegistrar()).isTrue();
 }
 @Test void laConsultaSoloActualizaLasSolicitudesPropias(){
  servicio.solicitar(id(alumno),solicitud(horario));
  long otroHorario=crearHorario(tutor,inicio.plusHours(2),inicio.plusHours(3));
  var otra=servicio.solicitar(id(otroAlumno),solicitud(otroHorario));
  when(reloj.instant()).thenReturn(inicio.plusHours(4).toInstant(ZoneOffset.UTC));
  assertThat(servicio.listar(id(alumno),false,"vencida",null,null,1,5).totalRegistros()).isEqualTo(1);
  assertThat(consultas.queryForObject("SELECT estado FROM sesiones WHERE id=?",String.class,otra.id())).isEqualTo("solicitada");
  assertThat(servicio.listar(id(profesor),true,"vencida",null,null,1,5).totalRegistros()).isEqualTo(2);
  assertThat(servicio.listar(id(profesor),true,"solicitada",null,null,1,5).totalRegistros()).isZero();
 }
 @Test void agendaYResumenMuestranElVencimiento(){
  var sesion=servicio.solicitar(id(alumno),solicitud(horario));
  when(reloj.instant()).thenReturn(inicio.plusMinutes(1).toInstant(ZoneOffset.UTC));
  assertThat(servicio.agenda(id(alumno),inicio.toLocalDate().atStartOfDay(),inicio.toLocalDate().plusDays(1).atStartOfDay()).getFirst().estado()).isEqualTo("vencida");
  var resumen=servicio.resumen(id(alumno));
  assertThat(resumen.proximas()).isEmpty();
  assertThat(resumen.actividades().getFirst().estado()).isEqualTo("vencida");
  assertThatThrownBy(()->servicio.cambiar(id(profesor),true,sesion.id(),"confirmar")).hasMessageContaining("ya no permite");
 }
 @Test void elProcesoAutomaticoNoRepiteVencimientos(){
  var sesion=servicio.solicitar(id(alumno),solicitud(horario));
  vencimiento.vencerPendientes(inicio);
  vencimiento.vencerPendientes(inicio.plusHours(1));
  assertThat(consultas.queryForObject("SELECT estado FROM sesiones WHERE id=?",String.class,sesion.id())).isEqualTo("vencida");
  assertThat(consultas.queryForObject("SELECT COUNT(*) FROM sesion_cambios WHERE sesion_id=? AND tipo='vencimiento'",Long.class,sesion.id())).isEqualTo(1);
 }

}
