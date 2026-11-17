package com.progweb.backend.seguridad;

import java.time.*;
import java.util.List;
import com.progweb.backend.entidades.Usuario;
import com.progweb.backend.repositorios.*;
import com.progweb.backend.respuestas.*;
import com.progweb.backend.servicios.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class InicioCoordinacionPruebas {
    @MockitoBean(name="relojInicio") Clock reloj;
    @Autowired WebApplicationContext contexto;
    @Autowired FilterChainProxy filtro;
    @Autowired RepositorioUsuarios usuarios;
    @Autowired RepositorioRoles roles;
    @Autowired ServicioTokens tokens;
    @Autowired ServicioInicioCoordinacion servicio;
    @Autowired JdbcTemplate consultas;
    MockMvc cliente;
    RespuestaInicioCoordinacion inicial;
    long autor,estudiante,otroEstudiante,tutor,periodo,motivo;
    long insertar(String sql,Object... datos){consultas.update(sql,datos);return consultas.queryForObject("SELECT LAST_INSERT_ID()",Long.class);}
    long usuario(String nombre,String rol){var usuario=new Usuario(nombre,nombre+"@example.invalid",null);usuario.asignarRol(roles.buscarPorCodigo(rol).orElseThrow());usuarios.saveAndFlush(usuario);return usuario.obtenerId();}
    @BeforeEach void preparar(){
        when(reloj.instant()).thenReturn(Instant.parse("2027-02-15T12:00:00Z"));
        cliente=MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtro).build();
        inicial=servicio.consultar();
        autor=usuario("inicio-coordinador","coordinador");
        long departamento=insertar("INSERT INTO departamentos(nombre) VALUES (?)","Departamento de Inicio de prueba");
        long carrera=insertar("INSERT INTO carreras(departamento_id,clave,nombre) VALUES (?,?,?)",departamento,"INICIO-PRUEBA","Carrera de Inicio");
        tutor=insertar("INSERT INTO tutores(usuario_id,departamento_id) VALUES (?,?)",usuario("inicio-tutor","tutor"),departamento);
        long inactivo=usuario("inicio-tutor-inactivo","tutor");consultas.update("UPDATE usuarios SET estado='inactivo' WHERE id=?",inactivo);
        insertar("INSERT INTO tutores(usuario_id,departamento_id) VALUES (?,?)",inactivo,departamento);
        estudiante=insertar("INSERT INTO estudiantes(usuario_id,matricula) VALUES (?,?)",usuario("inicio-estudiante","estudiante"),"INICIO-EST-1");
        otroEstudiante=insertar("INSERT INTO estudiantes(usuario_id,matricula) VALUES (?,?)",usuario("inicio-estudiante-otro","estudiante"),"INICIO-EST-2");
        insertar("INSERT INTO estudiantes(usuario_id,matricula) VALUES (?,?)",usuario("inicio-sin-evaluacion","estudiante"),"INICIO-EST-3");
        periodo=insertar("INSERT INTO periodos_academicos(clave,nombre,fecha_inicio,fecha_fin,estado) VALUES (?,?,?,?,?)","INICIO-REC","Periodo reciente de Inicio","2026-01-01","2027-12-31","activo");
        long antiguo=insertar("INSERT INTO periodos_academicos(clave,nombre,fecha_inicio,fecha_fin,estado) VALUES (?,?,?,?,?)","INICIO-ANT","Periodo anterior de Inicio","2024-01-01","2024-12-31","cerrado");
        long situacion=insertar("INSERT INTO situaciones_academicas(estudiante_id,periodo_id,carrera_id,semestre) VALUES (?,?,?,3)",estudiante,periodo,carrera);
        long otraSituacion=insertar("INSERT INTO situaciones_academicas(estudiante_id,periodo_id,carrera_id,semestre) VALUES (?,?,?,2)",otroEstudiante,periodo,carrera);
        long vieja=insertar("INSERT INTO situaciones_academicas(estudiante_id,periodo_id,carrera_id,semestre) VALUES (?,?,?,1)",estudiante,antiguo,carrera);
        medicion(vieja,"2024-02-01 00:00:00","alto");
        medicion(situacion,"2026-03-01 00:00:00","bajo");
        medicion(situacion,"2026-02-01 00:00:00","alto");
        medicion(otraSituacion,"2026-03-01 00:00:00","bajo");
        medicion(otraSituacion,"2026-03-01 00:00:00","alto");
        motivo=insertar("INSERT INTO motivos_reporte(codigo,nombre) VALUES (?,?)","inicio_prueba","Motivo de Inicio de prueba");
        for(int indice=0;indice<2;indice++)insertar("INSERT INTO reportes(estudiante_id,tutor_id,periodo_id,motivo_id,fecha,estado) VALUES (?,?,?,?,?,'pendiente')",estudiante,tutor,periodo,motivo,"2026-03-01");
    }
    void medicion(long situacion,String fecha,String riesgo){insertar("INSERT INTO mediciones_academicas(situacion_id,medido_en,nivel_riesgo,registrado_por) VALUES (?,?,?,?)",situacion,fecha,riesgo,autor);}
    void sesion(String fecha,String estado){consultas.update("INSERT INTO sesiones(tutor_id,estudiante_id,periodo_id,tema,inicio_en,fin_en,modalidad,ubicacion,estado,creado_por) VALUES (?,?,?,?,?,DATE_ADD(?, INTERVAL 1 HOUR),'presencial','Aula de prueba',?,?)",tutor,estudiante,periodo,"Sesión de Inicio",fecha,fecha,estado,autor);}
    String token(String rol){return "Bearer "+tokens.crearToken(new RespuestaUsuario(autor,"Prueba","prueba@example.invalid",null,List.of(rol))).token();}
    @Test void indicadoresUsanPerfilesYUltimaEvaluacionDelPeriodoReciente(){
        var respuesta=servicio.consultar().indicadores();
        assertThat(respuesta.totalEstudiantes()).isEqualTo(inicial.indicadores().totalEstudiantes()+3);
        assertThat(respuesta.tutoresActivos()).isEqualTo(inicial.indicadores().tutoresActivos()+1);
        assertThat(respuesta.estudiantesEnRiesgo()).isEqualTo(inicial.indicadores().estudiantesEnRiesgo()+1);
        assertThat(respuesta.sinEvaluacion()).isEqualTo(inicial.indicadores().sinEvaluacion()+1);
        assertThat(respuesta.totalReportes()).isEqualTo(inicial.indicadores().totalReportes()+2);
    }
    @Test void motivosSonReportesRealesConPorcentajeDelTotal(){
        var respuesta=servicio.consultar();
        var encontrado=respuesta.motivos().stream().filter(fila->fila.id()==motivo).findFirst().orElseThrow();
        assertThat(encontrado.cantidad()).isEqualTo(2);
        var esperado=java.math.BigDecimal.valueOf(200).divide(java.math.BigDecimal.valueOf(respuesta.indicadores().totalReportes()),1,java.math.RoundingMode.HALF_UP);
        assertThat(encontrado.porcentaje()).isEqualByComparingTo(esperado);
    }
    @Test void serieMensualDistingueAnosIncluyeCerosYExcluyeFuturo(){
        sesion("2026-09-01 00:00:00","completada");sesion("2026-09-30 23:00:00","completada");
        sesion("2026-10-01 10:00:00","confirmada");sesion("2026-11-01 10:00:00","cancelada");
        sesion("2026-12-31 23:00:00","completada");sesion("2027-01-01 00:00:00","completada");
        sesion("2027-02-01 10:00:00","completada");sesion("2027-02-15 12:00:00","completada");
        sesion("2026-08-31 23:00:00","completada");sesion("2027-02-16 10:00:00","completada");sesion("2027-03-01 10:00:00","completada");
        var respuesta=servicio.consultar();
        assertThat(respuesta.mensual()).extracting(fila->fila.mes()).containsExactly("2026-09","2026-10","2026-11","2026-12","2027-01","2027-02");
        long[] incrementos={2,0,0,1,1,2};
        for(int indice=0;indice<6;indice++)assertThat(respuesta.mensual().get(indice).cantidad()).isEqualTo(inicial.mensual().get(indice).cantidad()+incrementos[indice]);
        assertThat(respuesta.desde()).isEqualTo(LocalDate.of(2026,9,1));assertThat(respuesta.hasta()).isEqualTo(LocalDate.of(2027,2,15));
        assertThat(respuesta.actualizadoEn()).isEqualTo(Instant.parse("2027-02-15T12:00:00Z"));
    }
    @Test void coordinacionConsultaResumenSinDatosSensibles()throws Exception{
        String respuesta=cliente.perform(get("/api/coordinacion/inicio").header("Authorization",token("coordinador")))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.mensual.length()").value(6)).andExpect(jsonPath("$.indicadores.totalEstudiantes").isNumber())
            .andReturn().getResponse().getContentAsString();
        assertThat(respuesta).doesNotContain("password_hash","contrasenaHash","@example.invalid");
    }
    @ParameterizedTest @ValueSource(strings={"estudiante","tutor"})
    void otrosRolesNoPuedenConsultarInicioDeCoordinacion(String rol)throws Exception{
        cliente.perform(get("/api/coordinacion/inicio").header("Authorization",token(rol))).andExpect(status().isForbidden());
    }
    @Test void requiereSesion()throws Exception{cliente.perform(get("/api/coordinacion/inicio")).andExpect(status().isUnauthorized());}
}
