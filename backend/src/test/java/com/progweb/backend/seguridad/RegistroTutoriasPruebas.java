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
class RegistroTutoriasPruebas {
 @Autowired ServicioRegistroSesiones servicio;
 @Autowired ServicioReportesTutor reportes;
 @Autowired ServicioEstudiantesTutor tutorados;
 @Autowired ServicioSesionesAgenda agenda;
 @Autowired ServicioReportes coordinacion;
 @Autowired RepositorioUsuarios usuarios;
 @Autowired RepositorioRoles roles;
 @Autowired ServicioTokens tokens;
 @Autowired JdbcTemplate consultas;
 @Autowired WebApplicationContext contexto;
 @Autowired FilterChainProxy filtro;
 @MockitoBean(name="relojInicio") Clock reloj;
 long alumno,estudiante,profesor,otroProfesor,tutor,otroTutor,periodo,carrera,departamento,motivo;MockMvc cliente;
 final LocalDateTime inicio=LocalDateTime.of(2030,6,10,16,0),fin=inicio.plusMinutes(90);
 long insertar(String sql,Object... datos){consultas.update(sql,datos);return consultas.queryForObject("SELECT LAST_INSERT_ID()",Long.class);}
 long cuenta(String correo,String rol){var cuenta=new Usuario("Registro prueba",correo,null);cuenta.asignarRol(roles.buscarPorCodigo(rol).orElseThrow());usuarios.saveAndFlush(cuenta);return cuenta.obtenerId();}
 @BeforeEach void preparar(){when(reloj.instant()).thenReturn(Instant.parse("2030-06-15T18:00:00Z"));when(reloj.getZone()).thenReturn(ZoneOffset.UTC);cliente=MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtro).build();
  alumno=cuenta("registro-alumno@example.invalid","estudiante");profesor=cuenta("registro-profesor@example.invalid","tutor");otroProfesor=cuenta("registro-otro@example.invalid","tutor");
  departamento=insertar("INSERT INTO departamentos(nombre) VALUES (?)","Departamento registro tutorías");
  carrera=insertar("INSERT INTO carreras(departamento_id,clave,nombre) VALUES (?,?,?)",departamento,"REG-TUTORIAS","Carrera registro tutorías");
  periodo=insertar("INSERT INTO periodos_academicos(clave,nombre,fecha_inicio,fecha_fin,estado) VALUES (?,?,?,?,?)","REG-TUTORIAS","Periodo registro tutorías","2030-01-01","2030-12-31","activo");
  estudiante=insertar("INSERT INTO estudiantes(usuario_id,matricula) VALUES (?,?)",alumno,"REG-TUTORIAS");
  insertar("INSERT INTO situaciones_academicas(estudiante_id,periodo_id,carrera_id,semestre) VALUES (?,?,?,3)",estudiante,periodo,carrera);
  tutor=insertar("INSERT INTO tutores(usuario_id,departamento_id) VALUES (?,?)",profesor,departamento);otroTutor=insertar("INSERT INTO tutores(usuario_id,departamento_id) VALUES (?,?)",otroProfesor,departamento);
  insertar("INSERT INTO asignaciones_tutor(estudiante_id,tutor_id,periodo_id,asignado_por,inicio_en,estado) VALUES (?,?,?,?,?,'activa')",estudiante,tutor,periodo,profesor,inicio.minusMonths(1));
  motivo=insertar("INSERT INTO motivos_reporte(codigo,nombre) VALUES (?,?)","registro-tutorias","Motivo registro tutorías");
 }
 String tutorId(){return String.valueOf(profesor);}
 SolicitudRegistrarSesion registro(){return new SolicitudRegistrarSesion(estudiante,periodo,"Tutoría de registro",inicio,fin,"Aula prueba","Observación de seguimiento");}
 FiltrosSesionesRealizadas filtros(String texto){return new FiltrosSesionesRealizadas(1,5,texto,null,null,null);}
 FiltrosReportes filtrosReporte(){return new FiltrosReportes(1,5,"",null,null,null,null,null);}
 SolicitudReporteTutor reporte(Long sesion,String estado){return new SolicitudReporteTutor(estudiante,periodo,sesion,motivo,LocalDate.of(2030,6,10),"Contenido de seguimiento",estado);}
 long programada(String estado,LocalDateTime comienzo){return insertar("INSERT INTO sesiones(tutor_id,estudiante_id,periodo_id,tema,inicio_en,fin_en,modalidad,ubicacion,estado,creado_por) VALUES (?,?,?,'Programada',?,?,'presencial','Aula',?,?)",tutor,estudiante,periodo,comienzo,comienzo.plusHours(1),estado,alumno);}
 @Test void registroGuardaDuracionObservacionesYActor(){var respuesta=servicio.registrar(tutorId(),registro());assertThat(respuesta.duracionMinutos()).isEqualTo(90);assertThat(respuesta.estado()).isEqualTo("completada");assertThat(respuesta.observaciones()).contains("seguimiento");assertThat(consultas.queryForObject("SELECT creado_por FROM sesiones WHERE id=?",Long.class,respuesta.id())).isEqualTo(profesor);assertThat(consultas.queryForObject("SELECT COUNT(*) FROM observaciones_estudiante WHERE sesion_id=? AND autor_id=?",Long.class,respuesta.id(),profesor)).isEqualTo(1);}
 @Test void registroAlimentaHistorialDelTutorYEstudiante(){var sesion=servicio.registrar(tutorId(),registro());assertThat(tutorados.consultar(tutorId()).getFirst().sesionesCompletadas()).isEqualTo(1);assertThat(tutorados.consultarDetalle(tutorId(),estudiante).sesiones().getFirst().duracionMinutos()).isEqualTo(90);var historial=agenda.listar(String.valueOf(alumno),false,"completada",null,null,1,5);assertThat(historial.contenido().getFirst().id()).isEqualTo(sesion.id());assertThat(historial.contenido().getFirst().observaciones()).contains("seguimiento");assertThat(historial.contenido().getFirst().duracionMinutos()).isEqualTo(90);}
 @Test void resumenSumaHorasRealesYNoCanceladas(){servicio.registrar(tutorId(),registro());programada("cancelada",inicio.plusDays(1));var pagina=servicio.listar(tutorId(),filtros(""));assertThat(pagina.resumen().horas()).isEqualByComparingTo("1.50");assertThat(pagina.resumen().canceladas()).isEqualTo(1);}
 @Test void impideDuplicarElRegistro(){servicio.registrar(tutorId(),registro());assertThatThrownBy(()->servicio.registrar(tutorId(),registro())).hasMessageContaining("Ya hay una sesión");}
 @Test void rechazaHorarioInvalidoYFuturo(){assertThatThrownBy(()->servicio.registrar(tutorId(),new SolicitudRegistrarSesion(estudiante,periodo,"Tema",fin,inicio,"Aula",null))).hasMessageContaining("horario válido");assertThatThrownBy(()->servicio.registrar(tutorId(),new SolicitudRegistrarSesion(estudiante,periodo,"Tema",inicio.plusYears(1),fin.plusYears(1),"Aula",null))).hasMessageContaining("todavía no ha terminado");}
 @Test void noRegistraUnEstudianteAjeno(){assertThatThrownBy(()->servicio.registrar(String.valueOf(otroProfesor),registro())).hasMessageContaining("asignación activa");}
 @Test void periodoCerradoImpideRegistroDirecto(){consultas.update("UPDATE periodos_academicos SET estado='cerrado' WHERE id=?",periodo);assertThatThrownBy(()->servicio.registrar(tutorId(),registro())).hasMessageContaining("asignación activa");}
 @Test void cierraConfirmadaConHorarioRealYUnaSolaObservacion(){long id=programada("confirmada",inicio);var cierre=new SolicitudCerrarSesion("completada",inicio.plusMinutes(5),inicio.plusMinutes(50),"Nota de cierre");assertThat(servicio.cerrar(tutorId(),id,cierre).duracionMinutos()).isEqualTo(45);assertThatThrownBy(()->servicio.cerrar(tutorId(),id,cierre)).hasMessageContaining("Solo se puede cerrar");assertThat(consultas.queryForObject("SELECT COUNT(*) FROM observaciones_estudiante WHERE sesion_id=?",Long.class,id)).isEqualTo(1);}
 @Test void noAsistioNoSumaHoras(){long id=programada("confirmada",inicio);assertThat(servicio.cerrar(tutorId(),id,new SolicitudCerrarSesion("no_asistio",null,null,"Sin asistencia")).duracionMinutos()).isZero();assertThat(servicio.listar(tutorId(),filtros("")).resumen().horas()).isZero();}
 @Test void noCierraSolicitadasNiFuturas(){long id=programada("solicitada",inicio);assertThatThrownBy(()->servicio.cerrar(tutorId(),id,new SolicitudCerrarSesion("completada",inicio,fin,null))).hasMessageContaining("confirmada");long futuro=programada("confirmada",inicio.plusYears(1));assertThatThrownBy(()->servicio.cerrar(tutorId(),futuro,new SolicitudCerrarSesion("no_asistio",null,null,null))).hasMessageContaining("todavía no ha comenzado");}
 @Test void soloElTutorPropietarioCierra(){long id=programada("confirmada",inicio);assertThatThrownBy(()->servicio.cerrar(String.valueOf(otroProfesor),id,new SolicitudCerrarSesion("no_asistio",null,null,null))).hasMessageContaining("No se encontró");}
 @Test void cierreEsPosibleTrasTerminarAsignacion(){long id=programada("confirmada",inicio);consultas.update("UPDATE asignaciones_tutor SET estado='finalizada',fin_en=? WHERE estudiante_id=?",fin,estudiante);assertThat(servicio.cerrar(tutorId(),id,new SolicitudCerrarSesion("completada",inicio,fin,null)).estado()).isEqualTo("completada");}
 @Test void filtrosSonDelServidorYNoAlteranResumen(){servicio.registrar(tutorId(),registro());assertThat(servicio.listar(tutorId(),filtros("inexistente")).contenido()).isEmpty();assertThat(servicio.listar(tutorId(),filtros("Tutoría")).totalRegistros()).isEqualTo(1);assertThat(servicio.listar(tutorId(),filtros("inexistente")).resumen().completadas()).isEqualTo(1);assertThat(servicio.listar(String.valueOf(otroProfesor),filtros("")).contenido()).isEmpty();}
 @Test void urlVirtualSeGuardaSinInventarUbicacion(){var sesion=servicio.registrar(tutorId(),new SolicitudRegistrarSesion(estudiante,periodo,"Virtual",inicio,fin,"https://example.invalid/reunion",null));assertThat(sesion.ubicacion()).isEqualTo("https://example.invalid/reunion");}
 @Test void reporteCreadoApareceEnCoordinacion(){var sesion=servicio.registrar(tutorId(),registro());var resultado=reportes.crear(tutorId(),reporte(sesion.id(),"completado"));assertThat(resultado.estado()).isEqualTo("completado");assertThat(resultado.completadoPor()).isEqualTo("Registro prueba");assertThat(coordinacion.listar(filtrosReporte()).contenido()).extracting(fila->fila.id()).contains(resultado.id());}
 @Test void pendienteSeCompletaSinDuplicar(){var resultado=reportes.crear(tutorId(),reporte(null,"pendiente"));assertThat(resultado.completadoEn()).isNull();var completado=reportes.completar(tutorId(),resultado.id(),new SolicitudCompletarReporte("Contenido final"));assertThat(completado.contenido()).isEqualTo("Contenido final");assertThat(completado.completadoEn()).isNotNull();assertThatThrownBy(()->reportes.completar(tutorId(),resultado.id(),new SolicitudCompletarReporte("Otro"))).hasMessageContaining("ya está completado");}
 @Test void otroTutorNoConsultaCompletaNiDescargaReporte(){var resultado=reportes.crear(tutorId(),reporte(null,"pendiente"));assertThat(reportes.listar(String.valueOf(otroProfesor),filtrosReporte()).contenido()).isEmpty();assertThatThrownBy(()->reportes.completar(String.valueOf(otroProfesor),resultado.id(),new SolicitudCompletarReporte("Otro"))).hasMessageContaining("No se encontró");assertThatThrownBy(()->reportes.individual(String.valueOf(otroProfesor),resultado.id())).hasMessageContaining("No se encontró");}
 @Test void motivoInactivoYSesionAjenaSeRechazan(){consultas.update("UPDATE motivos_reporte SET activo=false WHERE id=?",motivo);assertThatThrownBy(()->reportes.crear(tutorId(),reporte(null,"completado"))).hasMessageContaining("motivo disponible");}
 @Test void reporteNoAceptaSesionSinCompletar(){long id=programada("confirmada",inicio);assertThatThrownBy(()->reportes.crear(tutorId(),reporte(id,"completado"))).hasMessageContaining("sesión completada");}
 @Test void pdfIndividualYResumenSonReales(){var sesion=servicio.registrar(tutorId(),registro());var resultado=reportes.crear(tutorId(),reporte(sesion.id(),"completado"));assertThat(new String(reportes.individual(tutorId(),resultado.id()),java.nio.charset.StandardCharsets.ISO_8859_1)).startsWith("%PDF-");var analisis=reportes.analisis(tutorId());assertThat(analisis.total()).isEqualTo(1);assertThat(analisis.estudiantes()).isEqualTo(1);assertThat(analisis.horas()).isEqualByComparingTo("1.50");assertThat(analisis.promedio()).isNull();assertThat(analisis.rendimiento()).hasSize(6);}
 @Test void graficasUsanMedicionesRealesInclusoPromedioCero(){long situacion=consultas.queryForObject("SELECT id FROM situaciones_academicas WHERE estudiante_id=?",Long.class,estudiante);insertar("INSERT INTO mediciones_academicas(situacion_id,medido_en,promedio,registrado_por) VALUES (?,?,?,?)",situacion,inicio.minusMonths(1),8,profesor);insertar("INSERT INTO mediciones_academicas(situacion_id,medido_en,promedio,registrado_por) VALUES (?,?,?,?)",situacion,inicio,0,profesor);var analisis=reportes.analisis(tutorId());assertThat(analisis.promedio()).isZero();assertThat(analisis.progreso().getFirst().inicial()).isEqualByComparingTo("8");assertThat(analisis.progreso().getFirst().actual()).isZero();assertThat(analisis.rendimiento().getLast().promedio()).isZero();}
 @Test void permisosYValidacionHttp() throws Exception {String token=tokens.crearToken(new RespuestaUsuario(alumno,"Alumno","registro-alumno@example.invalid",null,List.of("estudiante"))).token();cliente.perform(get("/api/tutor/sesiones-realizadas")).andExpect(status().isUnauthorized());cliente.perform(post("/api/tutor/sesiones-realizadas").header("Authorization","Bearer "+token).contentType("application/json").content("{}")).andExpect(status().isForbidden());String tutorToken=tokens.crearToken(new RespuestaUsuario(profesor,"Profesor","registro-profesor@example.invalid",null,List.of("tutor"))).token();cliente.perform(post("/api/tutor/reportes").header("Authorization","Bearer "+tutorToken).contentType("application/json").content("{}")).andExpect(status().isUnprocessableEntity());}
}
