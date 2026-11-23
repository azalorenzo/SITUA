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
class IniciosYActualizacionesPruebas {
 @Autowired ServicioInicioEstudiante inicioEstudiante;
 @Autowired ServicioInicioTutor inicioTutor;
 @Autowired ServicioAvisosEstudiante avisos;
 @Autowired RepositorioUsuarios usuarios;
 @Autowired RepositorioRoles roles;
 @Autowired ServicioTokens tokens;
 @Autowired JdbcTemplate consultas;
 @Autowired WebApplicationContext contexto;
 @Autowired FilterChainProxy filtro;
 @MockitoBean(name="relojInicio") Clock reloj;
 MockMvc cliente;
 long alumno,otroAlumno,estudiante,otroEstudiante,profesor,otroProfesor,tutor,otroTutor,coordinador,periodo,carrera,departamento;
 final LocalDateTime momento=LocalDateTime.of(2030,6,15,18,0);
 long insertar(String sql,Object... datos){consultas.update(sql,datos);return consultas.queryForObject("SELECT LAST_INSERT_ID()",Long.class);}
 long cuenta(String correo,String rol){var cuenta=new Usuario("Inicio prueba",correo,null);cuenta.asignarRol(roles.buscarPorCodigo(rol).orElseThrow());usuarios.saveAndFlush(cuenta);return cuenta.obtenerId();}
 @BeforeEach void preparar(){when(reloj.instant()).thenReturn(momento.toInstant(ZoneOffset.UTC));when(reloj.getZone()).thenReturn(ZoneOffset.UTC);cliente=MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtro).build();
  alumno=cuenta("inicio-real-alumno@example.invalid","estudiante");otroAlumno=cuenta("inicio-real-otro@example.invalid","estudiante");profesor=cuenta("inicio-real-tutor@example.invalid","tutor");otroProfesor=cuenta("inicio-real-otro-tutor@example.invalid","tutor");coordinador=cuenta("inicio-real-coord@example.invalid","coordinador");
  departamento=insertar("INSERT INTO departamentos(nombre) VALUES (?)","Departamento inicio real");carrera=insertar("INSERT INTO carreras(departamento_id,clave,nombre) VALUES (?,?,?)",departamento,"INICIO-REAL","Carrera inicio real");
  periodo=insertar("INSERT INTO periodos_academicos(clave,nombre,fecha_inicio,fecha_fin,estado) VALUES (?,?,?,?,?)","INICIO-REAL","Periodo inicio real","2030-01-01","2030-12-31","activo");
  estudiante=insertar("INSERT INTO estudiantes(usuario_id,matricula) VALUES (?,?)",alumno,"INICIO-REAL-A");otroEstudiante=insertar("INSERT INTO estudiantes(usuario_id,matricula) VALUES (?,?)",otroAlumno,"INICIO-REAL-B");
  for(long id:List.of(estudiante,otroEstudiante))insertar("INSERT INTO situaciones_academicas(estudiante_id,periodo_id,carrera_id,semestre) VALUES (?,?,?,3)",id,periodo,carrera);
  tutor=insertar("INSERT INTO tutores(usuario_id,departamento_id) VALUES (?,?)",profesor,departamento);otroTutor=insertar("INSERT INTO tutores(usuario_id,departamento_id) VALUES (?,?)",otroProfesor,departamento);
  insertar("INSERT INTO asignaciones_tutor(estudiante_id,tutor_id,periodo_id,asignado_por,inicio_en,estado) VALUES (?,?,?,?,?,'activa')",estudiante,tutor,periodo,coordinador,momento.minusMonths(1));
 }
 String id(long usuario){return String.valueOf(usuario);}
 long sesion(long estudiante,long tutor,String estado,LocalDateTime inicio,boolean virtual){return insertar("INSERT INTO sesiones(tutor_id,estudiante_id,periodo_id,tema,inicio_en,fin_en,modalidad,ubicacion,enlace_virtual,estado,creado_por) VALUES (?,?,?,'Sesión real',?,?,?,?,?,?,?)",tutor,estudiante,periodo,inicio,inicio.plusHours(1),virtual?"virtual":"presencial",virtual?null:"Aula",virtual?"https://example.invalid/reunion":null,estado,alumno);}
 long aviso(long usuario,String titulo){long id=insertar("INSERT INTO avisos(autor_id,titulo,descripcion,publicado_en) VALUES (?,?,?,?)",coordinador,titulo,"Contenido real",momento.minusDays(1));insertar("INSERT INTO aviso_destinatarios(aviso_id,usuario_id) VALUES (?,?)",id,usuario);return id;}
 SolicitudActividadPersonal actividad(String titulo,String tipo){return new SolicitudActividadPersonal(titulo,"Descripción",tipo,tipo.equals("taller")?momento.toLocalDate().plusDays(1):null,null,null,"Responsable","Biblioteca");}
 @Test void estudianteUsaProximaSesionYVirtualesReales(){long proxima=sesion(estudiante,tutor,"confirmada",momento.plusHours(1),false);long virtual=sesion(estudiante,tutor,"solicitada",momento.plusHours(2),true);sesion(estudiante,tutor,"cancelada",momento.plusMinutes(30),false);sesion(estudiante,tutor,"completada",momento.minusDays(1),false);sesion(otroEstudiante,otroTutor,"confirmada",momento.plusMinutes(10),true);var datos=inicioEstudiante.consultar(id(alumno));assertThat(datos.proxima().id()).isEqualTo(proxima);assertThat(datos.virtuales()).extracting(fila->fila.id()).containsExactly(virtual);assertThat(inicioEstudiante.consultar(id(otroAlumno)).proxima().id()).isNotEqualTo(proxima);}
 @Test void inicioVacioNoInventaDatos(){var datos=inicioEstudiante.consultar(id(alumno));assertThat(datos.proxima()).isNull();assertThat(datos.virtuales()).isEmpty();assertThat(datos.talleres()).isEmpty();assertThat(datos.recordatorios()).isEmpty();}
 @Test void recordatorioSeGuardaEditaYElimina(){var creado=inicioEstudiante.crear(id(alumno),actividad("Pendiente","recordatorio"));assertThat(inicioEstudiante.consultar(id(alumno)).recordatorios()).extracting(fila->fila.id()).containsExactly(creado.id());inicioEstudiante.editar(id(alumno),creado.id(),actividad("Editado","recordatorio"));assertThat(inicioEstudiante.consultar(id(alumno)).recordatorios().getFirst().titulo()).isEqualTo("Editado");inicioEstudiante.eliminar(id(alumno),creado.id());assertThat(inicioEstudiante.consultar(id(alumno)).recordatorios()).isEmpty();}
 @Test void noLeeNiModificaActividadesAjenas(){var creado=inicioEstudiante.crear(id(alumno),actividad("Privado","taller"));assertThat(inicioEstudiante.consultar(id(otroAlumno)).talleres()).isEmpty();assertThatThrownBy(()->inicioEstudiante.editar(id(otroAlumno),creado.id(),actividad("Ajeno","taller"))).hasMessageContaining("No se encontró");assertThatThrownBy(()->inicioEstudiante.eliminar(id(otroAlumno),creado.id())).hasMessageContaining("No se encontró");}
 @Test void fechaYHorarioSeValidan(){assertThatThrownBy(()->inicioEstudiante.crear(id(alumno),new SolicitudActividadPersonal("Taller",null,"taller",null,null,null,null,null))).hasMessageContaining("fecha del taller");assertThatThrownBy(()->inicioEstudiante.crear(id(alumno),new SolicitudActividadPersonal("Taller",null,"taller",null,momento,momento.minusHours(1),null,null))).hasMessageContaining("fecha y las horas");assertThatThrownBy(()->inicioEstudiante.crear(id(alumno),new SolicitudActividadPersonal("Taller",null,"taller",momento.toLocalDate(),momento,momento.plusHours(1),null,null))).hasMessageContaining("fecha y las horas");}
 @Test void tallerConHorarioSeConservaEnUtc(){var creado=inicioEstudiante.crear(id(alumno),new SolicitudActividadPersonal("Horario",null,"taller",null,momento,momento.plusHours(1),"Tutor invitado","Aula"));assertThat(creado.fecha()).isNull();assertThat(inicioEstudiante.consultar(id(alumno)).talleres().getFirst().inicioEn()).isEqualTo(momento);}
 @Test void tutorCalculaIndicadoresSinCruzarCuentas(){sesion(estudiante,tutor,"completada",momento.minusDays(1),false);sesion(estudiante,tutor,"confirmada",momento.plusHours(1),false);sesion(estudiante,tutor,"cancelada",momento.plusHours(2),false);sesion(otroEstudiante,otroTutor,"confirmada",momento.plusHours(3),true);long motivo=consultas.queryForObject("SELECT MIN(id) FROM motivos_reporte",Long.class);insertar("INSERT INTO reportes(estudiante_id,tutor_id,periodo_id,motivo_id,fecha) VALUES (?,?,?,?,?)",estudiante,tutor,periodo,motivo,momento.toLocalDate());var datos=inicioTutor.consultar(id(profesor));assertThat(datos.indicadores().estudiantes()).isEqualTo(1);assertThat(datos.indicadores().realizadas()).isEqualTo(1);assertThat(datos.indicadores().reportesPendientes()).isEqualTo(1);assertThat(datos.indicadores().programadas()).isEqualTo(1);assertThat(datos.proximas()).hasSize(1);assertThat(inicioTutor.consultar(id(otroProfesor)).indicadores().reportesPendientes()).isZero();}
 @Test void tutorNoDuplicaEstudianteConDosPeriodosYUsaMedicionReciente(){long otro=insertar("INSERT INTO periodos_academicos(clave,nombre,fecha_inicio,fecha_fin,estado) VALUES (?,?,?,?,?)","INICIO-REAL-OTRO","Otro periodo","2030-07-01","2030-12-31","activo");long situacion=insertar("INSERT INTO situaciones_academicas(estudiante_id,periodo_id,carrera_id,semestre) VALUES (?,?,?,4)",estudiante,otro,carrera);insertar("INSERT INTO asignaciones_tutor(estudiante_id,tutor_id,periodo_id,asignado_por,inicio_en,estado) VALUES (?,?,?,?,?,'activa')",estudiante,tutor,otro,coordinador,momento);insertar("INSERT INTO mediciones_academicas(situacion_id,medido_en,nivel_riesgo,registrado_por) VALUES (?,?,'alto',?)",situacion,momento,profesor);var datos=inicioTutor.consultar(id(profesor));assertThat(datos.indicadores().estudiantes()).isEqualTo(1);assertThat(datos.estudiantes()).hasSize(1);assertThat(datos.estudiantes().getFirst().nivelRiesgo()).isEqualTo("alto");}
 @Test void tutorExcluyeInscripcionesDeBaja(){consultas.update("UPDATE situaciones_academicas SET estado_inscripcion='baja' WHERE estudiante_id=?",estudiante);assertThat(inicioTutor.consultar(id(profesor)).indicadores().estudiantes()).isZero();assertThat(inicioTutor.consultar(id(profesor)).estudiantes()).isEmpty();}
 @Test void avisoSoloApareceASuDestinatario(){long propio=aviso(alumno,"Propio");aviso(otroAlumno,"Ajeno");var pagina=avisos.consultar(id(alumno),1,5);assertThat(pagina.totalRegistros()).isEqualTo(1);assertThat(pagina.noLeidos()).isEqualTo(1);assertThat(pagina.contenido()).extracting(fila->fila.id()).containsExactly(propio);}
 @Test void leerAvisoConservaPrimeraFechaYNoTocaOtros(){long propio=aviso(alumno,"Compartido");insertar("INSERT INTO aviso_destinatarios(aviso_id,usuario_id) VALUES (?,?)",propio,otroAlumno);var primera=avisos.marcar(id(alumno),propio);when(reloj.instant()).thenReturn(momento.plusHours(1).toInstant(ZoneOffset.UTC));assertThat(avisos.marcar(id(alumno),propio).leidoEn()).isEqualTo(primera.leidoEn());assertThat(avisos.consultar(id(alumno),1,5).noLeidos()).isZero();assertThat(avisos.consultar(id(otroAlumno),1,5).noLeidos()).isEqualTo(1);}
 @Test void ocultosYRetiradosNoSeVenNiSeMarcan(){long retirado=aviso(alumno,"Retirado"),oculto=aviso(alumno,"Oculto");consultas.update("UPDATE avisos SET retirado_en=? WHERE id=?",momento,retirado);consultas.update("UPDATE aviso_destinatarios SET ocultado_en=? WHERE aviso_id=?",momento,oculto);assertThat(avisos.consultar(id(alumno),1,5).contenido()).isEmpty();assertThatThrownBy(()->avisos.marcar(id(alumno),retirado)).hasMessageContaining("No se encontró");assertThat(avisos.marcarTodos(id(alumno)).marcados()).isZero();}
 @Test void noMarcaAvisoAjeno(){long ajeno=aviso(otroAlumno,"Ajeno");assertThatThrownBy(()->avisos.marcar(id(alumno),ajeno)).hasMessageContaining("No se encontró");}
 @Test void paginasYNoLeidosSeCalculanEnServidor(){for(int indice=0;indice<7;indice++)aviso(alumno,"Aviso "+indice);var primera=avisos.consultar(id(alumno),1,5);var segunda=avisos.consultar(id(alumno),2,5);assertThat(primera.totalPaginas()).isEqualTo(2);assertThat(primera.contenido()).hasSize(5);assertThat(segunda.contenido()).hasSize(2);assertThat(segunda.noLeidos()).isEqualTo(7);assertThat(avisos.consultar(id(alumno),99,5).pagina()).isEqualTo(2);assertThat(avisos.marcarTodos(id(alumno)).marcados()).isEqualTo(7);assertThat(avisos.consultar(id(alumno),1,5).noLeidos()).isZero();}
 @Test void cuentaInactivaNoPuedeConsultarNiGuardar(){var usuario=usuarios.findById(alumno).orElseThrow();usuario.desactivar();usuarios.flush();assertThatThrownBy(()->inicioEstudiante.consultar(id(alumno))).hasMessageContaining("inactiva");assertThatThrownBy(()->avisos.consultar(id(alumno),1,5)).hasMessageContaining("inactiva");}
 @Test void permisosYValidacionHttp() throws Exception {String token=tokens.crearToken(new RespuestaUsuario(alumno,"Alumno","inicio-real-alumno@example.invalid",null,List.of("estudiante"))).token();cliente.perform(get("/api/estudiante/inicio")).andExpect(status().isUnauthorized());cliente.perform(get("/api/tutor/inicio").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());cliente.perform(get("/api/estudiante/avisos").param("pagina","0").header("Authorization","Bearer "+token)).andExpect(status().isUnprocessableEntity());cliente.perform(post("/api/estudiante/actividades").contentType("application/json").content("{}").header("Authorization","Bearer "+token)).andExpect(status().isUnprocessableEntity());}
}
