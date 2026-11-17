package com.progweb.backend.seguridad;

import java.util.List;
import com.progweb.backend.solicitudes.FiltrosEstudiantes;
import com.progweb.backend.entidades.Usuario;
import com.progweb.backend.repositorios.RepositorioUsuarios;
import com.progweb.backend.repositorios.RepositorioConsultasCoordinacion;
import com.progweb.backend.respuestas.RespuestaUsuario;
import com.progweb.backend.servicios.ServicioTokens;
import com.progweb.backend.servicios.ServicioConsultasCoordinacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class ConsultasCoordinacionPruebas {
    @Autowired WebApplicationContext contexto;
    @Autowired FilterChainProxy filtro;
    @Autowired ServicioTokens tokens;
    @Autowired RepositorioUsuarios usuarios;
    @Autowired RepositorioConsultasCoordinacion repositorio;
    @Autowired JdbcTemplate consultas;
    @Autowired ServicioConsultasCoordinacion servicio;
    MockMvc cliente;

    @BeforeEach void preparar() {
        cliente = MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtro).build();
    }

    String token(String rol) {
        return tokens.crearToken(new RespuestaUsuario(1L, "Prueba", "prueba@example.invalid", null, List.of(rol))).token();
    }

    long insertar(String sentencia, Object... datos) {
        consultas.update(sentencia, datos);
        return consultas.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    Usuario crearUsuario(String correo) {
        return usuarios.saveAndFlush(new Usuario("Consulta de coordinación", correo,
            "$2a$12$uYol2hv3vLuvp3WUXR/FU.n9pyxP9NFY8S8x0qnFsFVaycTxewX5K"));
    }

    @ParameterizedTest @ValueSource(strings = {"tutores", "estudiantes"})
    void requiereSesion(String recurso) throws Exception {
        cliente.perform(get("/api/coordinacion/" + recurso)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest @ValueSource(strings = {"tutor", "estudiante"})
    void otrosRolesNoPuedenConsultarLasListas(String rol) throws Exception {
        for (String recurso : List.of("tutores", "estudiantes")) {
            cliente.perform(get("/api/coordinacion/" + recurso).header("Authorization", "Bearer " + token(rol)))
                .andExpect(status().isForbidden());
        }
    }

    @ParameterizedTest @ValueSource(strings = {"tutores", "estudiantes"})
    void coordinacionPuedeConsultarSinExponerClaves(String recurso) throws Exception {
        String respuesta = cliente.perform(get("/api/coordinacion/" + recurso)
                .header("Authorization", "Bearer " + token("coordinador")))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.contenido").isArray()).andReturn().getResponse().getContentAsString();
        assertThat(respuesta).doesNotContain("password_hash", "contrasenaHash", "passwordHash", "$2a$", "$2y$");
    }

    @Test void tutorIncluyeDepartamentoYEstadoReal() {
        var usuario = crearUsuario("tutor-consulta@example.invalid"); usuario.desactivar(); usuarios.flush();
        long departamento = insertar("INSERT INTO departamentos(nombre) VALUES (?)", "Departamento de prueba de consultas");
        long tutor = insertar("INSERT INTO tutores(usuario_id, departamento_id) VALUES (?, ?)", usuario.obtenerId(), departamento);
        var respuesta = repositorio.consultarTutores().stream().filter(fila -> fila.id() == tutor).findFirst().orElseThrow();
        assertThat(respuesta.usuarioId()).isEqualTo(usuario.obtenerId());
        assertThat(respuesta.departamento()).isEqualTo("Departamento de prueba de consultas");
        assertThat(respuesta.estado()).isEqualTo("inactivo");
    }

    @Test void estudianteSinExpedienteApareceConCamposAcademicosNulos() {
        var usuario = crearUsuario("estudiante-consulta@example.invalid");
        long estudiante = insertar("INSERT INTO estudiantes(usuario_id, matricula) VALUES (?, ?)", usuario.obtenerId(), "CONSULTA-NUEVO");
        var respuesta = repositorio.consultarEstudiantes(new FiltrosEstudiantes(1,10,"estudiante-consulta@example.invalid",null),1).stream().filter(fila -> fila.id() == estudiante).findFirst().orElseThrow();
        assertThat(respuesta.matricula()).isEqualTo("CONSULTA-NUEVO");
        assertThat(respuesta.periodoId()).isNull(); assertThat(respuesta.carrera()).isNull();
        assertThat(respuesta.semestre()).isNull(); assertThat(respuesta.promedio()).isNull();
        assertThat(respuesta.nivelRiesgo()).isNull();
    }

    @Test void estudianteUsaPeriodoMasRecienteYUltimaMedicionSinDuplicarse() {
        var usuario = crearUsuario("expediente-consulta@example.invalid");
        long estudiante = insertar("INSERT INTO estudiantes(usuario_id, matricula) VALUES (?, ?)", usuario.obtenerId(), "CONSULTA-EXPEDIENTE");
        long departamento = insertar("INSERT INTO departamentos(nombre) VALUES (?)", "Departamento de expediente de consultas");
        long carrera = insertar("INSERT INTO carreras(departamento_id, clave, nombre) VALUES (?, ?, ?)", departamento, "CONS-PRUEBA", "Carrera de prueba");
        long anterior = insertar("INSERT INTO periodos_academicos(clave, nombre, fecha_inicio, fecha_fin, estado) VALUES (?, ?, ?, ?, ?)",
            "CONS-ANT", "Periodo anterior", "2020-01-01", "2020-06-01", "cerrado");
        long reciente = insertar("INSERT INTO periodos_academicos(clave, nombre, fecha_inicio, fecha_fin, estado) VALUES (?, ?, ?, ?, ?)",
            "CONS-REC", "Periodo reciente", "2021-01-01", "2021-06-01", "cerrado");
        // Insertamos el periodo anterior al final para comprobar que no se ordene por el id del expediente.
        long situacion = insertar("INSERT INTO situaciones_academicas(estudiante_id, periodo_id, carrera_id, semestre) VALUES (?, ?, ?, ?)", estudiante, reciente, carrera, 4);
        insertar("INSERT INTO situaciones_academicas(estudiante_id, periodo_id, carrera_id, semestre) VALUES (?, ?, ?, ?)", estudiante, anterior, carrera, 2);
        insertar("INSERT INTO mediciones_academicas(situacion_id, medido_en, promedio, nivel_riesgo, registrado_por) VALUES (?, ?, ?, ?, ?)", situacion, "2021-02-01 10:00:00", 7, "medio", usuario.obtenerId());
        insertar("INSERT INTO mediciones_academicas(situacion_id, medido_en, promedio, nivel_riesgo, registrado_por) VALUES (?, ?, ?, ?, ?)", situacion, "2021-02-01 10:00:00", 9, "bajo", usuario.obtenerId());
        var respuestas = repositorio.consultarEstudiantes(new FiltrosEstudiantes(1,10,"expediente-consulta@example.invalid",null),1).stream().filter(fila -> fila.id() == estudiante).toList();
        assertThat(respuestas).hasSize(1);
        var respuesta = respuestas.getFirst();
        assertThat(respuesta.periodoId()).isEqualTo(reciente);
        assertThat(respuesta.semestre()).isEqualTo(4);
        assertThat(respuesta.promedio()).isEqualByComparingTo("9.00");
        assertThat(respuesta.nivelRiesgo()).isEqualTo("bajo");
    }

    @Test void paginaSeAplicaDespuesDeBuscarYOrdenaSinRepetirEstudiantes() throws Exception {
        for(int indice=0;indice<3;indice++) {
            var usuario=crearUsuario("pagina-estudiante-"+indice+"@example.invalid");
            insertar("INSERT INTO estudiantes(usuario_id,matricula) VALUES (?,?)",usuario.obtenerId(),"PAG-EST-"+indice);
        }
        var filtros=new FiltrosEstudiantes(1,2,"pagina-estudiante-",null);
        var primera=servicio.consultarEstudiantes(filtros);
        var segunda=servicio.consultarEstudiantes(new FiltrosEstudiantes(2,2,"pagina-estudiante-",null));
        assertThat(primera.totalRegistros()).isEqualTo(3);assertThat(primera.totalPaginas()).isEqualTo(2);
        assertThat(primera.contenido()).hasSize(2);assertThat(segunda.contenido()).hasSize(1);
        assertThat(primera.contenido()).extracting(fila->fila.id()).doesNotContain(segunda.contenido().getFirst().id());
        assertThat(servicio.consultarEstudiantes(new FiltrosEstudiantes(Integer.MAX_VALUE,2,"pagina-estudiante-",null)).pagina()).isEqualTo(2);
        cliente.perform(get("/api/coordinacion/estudiantes").param("busqueda","pagina-estudiante-").param("tamano","2").param("pagina","2")
            .header("Authorization","Bearer "+token("coordinador"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.contenido.length()").value(1)).andExpect(jsonPath("$.totalRegistros").value(3));
    }

    @Test void estadoYBusquedaSeCombinanYElResumenNoDependeDeLaPagina() {
        var usuario=crearUsuario("filtro-estudiante@example.invalid");usuario.desactivar();usuarios.flush();
        insertar("INSERT INTO estudiantes(usuario_id,matricula) VALUES (?,?)",usuario.obtenerId(),"FILTRO-EST-UNICO");
        var encontrado=servicio.consultarEstudiantes(new FiltrosEstudiantes(1,1,"FILTRO-EST-UNICO","inactivo"));
        var vacio=servicio.consultarEstudiantes(new FiltrosEstudiantes(1,1,"FILTRO-EST-UNICO","activo"));
        assertThat(encontrado.totalRegistros()).isEqualTo(1);assertThat(vacio.totalRegistros()).isZero();
        assertThat(vacio.pagina()).isEqualTo(1);assertThat(vacio.totalPaginas()).isZero();
        assertThat(vacio.contenido()).isEmpty();assertThat(encontrado.resumen()).isEqualTo(vacio.resumen());
        assertThat(encontrado.distribucionRiesgo()).isEqualTo(vacio.distribucionRiesgo());
        assertThat(encontrado.distribucionSemestres()).isEqualTo(vacio.distribucionSemestres());
        assertThat(encontrado.enRiesgo()).isEqualTo(vacio.enRiesgo());
    }

    @Test void busquedaRespetaAcentosMayusculasYComodinesLiterales() {
        var usuario=usuarios.saveAndFlush(new Usuario("José Búsqueda 100%_!","comodines-estudiante@example.invalid",null));
        insertar("INSERT INTO estudiantes(usuario_id,matricula) VALUES (?,?)",usuario.obtenerId(),"BUSQUEDA-EST-UNICA");
        for(String texto:List.of("JOSE BUSQUEDA","100%_!","comodines-estudiante@example.invalid","BUSQUEDA-EST-UNICA")) {
            var pagina=servicio.consultarEstudiantes(new FiltrosEstudiantes(1,10,texto,null));
            assertThat(pagina.contenido()).extracting(fila->fila.usuarioId()).containsExactly(usuario.obtenerId());
        }
        assertThat(servicio.consultarEstudiantes(new FiltrosEstudiantes(1,10,"' OR 1=1 --",null)).totalRegistros()).isZero();
    }

    @Test void resumenIncluyePromedioCeroYNulosSinDuplicarMediciones() {
        var antes=repositorio.resumenEstudiantes();
        var usuario=crearUsuario("resumen-estudiante@example.invalid");
        long estudiante=insertar("INSERT INTO estudiantes(usuario_id,matricula) VALUES (?,?)",usuario.obtenerId(),"RES-EST-UNICA");
        long departamento=insertar("INSERT INTO departamentos(nombre) VALUES (?)","Resumen estudiantes prueba");
        long carrera=insertar("INSERT INTO carreras(departamento_id,clave,nombre) VALUES (?,?,?)",departamento,"RES-EST","Carrera resumen estudiantes");
        long periodo=insertar("INSERT INTO periodos_academicos(clave,nombre,fecha_inicio,fecha_fin) VALUES (?,?,?,?)","RES-EST","Resumen estudiantes","2020-01-01","2020-12-31");
        long situacion=insertar("INSERT INTO situaciones_academicas(estudiante_id,periodo_id,carrera_id,semestre) VALUES (?,?,?,?)",estudiante,periodo,carrera,3);
        insertar("INSERT INTO mediciones_academicas(situacion_id,medido_en,promedio,nivel_riesgo,registrado_por) VALUES (?,?,?,?,?)",situacion,"2020-06-01 10:00:00",9,"bajo",usuario.obtenerId());
        insertar("INSERT INTO mediciones_academicas(situacion_id,medido_en,promedio,nivel_riesgo,registrado_por) VALUES (?,?,?,?,?)",situacion,"2020-06-01 10:00:00",0,"alto",usuario.obtenerId());
        var despues=servicio.consultarEstudiantes(new FiltrosEstudiantes(1,1,"Carrera resumen estudiantes",null));
        assertThat(despues.totalRegistros()).isEqualTo(1);assertThat(despues.contenido().getFirst().promedio()).isZero();
        assertThat(despues.resumen().total()).isEqualTo(antes.total()+1);
        assertThat(despues.resumen().conPromedio()).isEqualTo(antes.conPromedio()+1);
        assertThat(despues.resumen().enRiesgo()).isEqualTo(antes.enRiesgo()+1);
        assertThat(despues.resumen().sinEvaluacion()).isEqualTo(antes.sinEvaluacion());
        assertThat(despues.enRiesgo()).filteredOn(fila->fila.id()==estudiante).hasSize(1);
        assertThat(despues.distribucionRiesgo().stream().mapToLong(fila->fila.cantidad()).sum()).isEqualTo(despues.resumen().total());
        assertThat(despues.distribucionSemestres().stream().mapToLong(fila->fila.cantidad()).sum()+despues.resumen().sinSemestre()).isEqualTo(despues.resumen().total());
    }

    @ParameterizedTest @ValueSource(strings={"pagina=0","pagina=-1","tamano=0","tamano=101","estado=incorrecto","pagina=abc"})
    void rechazaFiltrosInvalidos(String parametro) throws Exception {
        var partes=parametro.split("=");
        cliente.perform(get("/api/coordinacion/estudiantes").param(partes[0],partes[1])
            .header("Authorization","Bearer "+token("coordinador"))).andExpect(status().isUnprocessableEntity());
    }

    @Test void rechazaBusquedaDemasiadoLarga() throws Exception {
        cliente.perform(get("/api/coordinacion/estudiantes").param("busqueda","x".repeat(161))
            .header("Authorization","Bearer "+token("coordinador"))).andExpect(status().isUnprocessableEntity());
    }
}
