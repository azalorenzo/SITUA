package com.progweb.backend.seguridad;

import java.util.List;
import java.util.Map;
import com.progweb.backend.entidades.Usuario;
import com.progweb.backend.repositorios.*;
import com.progweb.backend.respuestas.RespuestaUsuario;
import com.progweb.backend.servicios.ServicioTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class AsignacionesPruebas {
    @Autowired WebApplicationContext contexto;
    @Autowired FilterChainProxy filtro;
    @Autowired ServicioTokens tokens;
    @Autowired RepositorioUsuarios usuarios;
    @Autowired RepositorioRoles roles;
    @Autowired RepositorioAsignaciones asignaciones;
    @Autowired JdbcTemplate consultas;
    @Autowired ObjectMapper conversor;
    @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager entidades;
    MockMvc cliente;
    long estudiante, tutor, otroTutor, periodo, autor, usuarioEstudiante, usuarioTutor;
    String autorizacion;

    long insertar(String sentencia, Object... datos) {
        consultas.update(sentencia, datos);
        return consultas.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
    Usuario usuario(String nombre, String rol) {
        var usuario = new Usuario(nombre, nombre + "@example.invalid", null);
        usuario.asignarRol(roles.buscarPorCodigo(rol).orElseThrow());
        return usuarios.saveAndFlush(usuario);
    }
    String token(String rol) {
        return "Bearer " + tokens.crearToken(new RespuestaUsuario(autor, "Prueba", "prueba@example.invalid", null, List.of(rol))).token();
    }
    @BeforeEach void preparar() {
        cliente = MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtro).build();
        autor = usuario("asignacion-coordinador", "coordinador").obtenerId();
        usuarioEstudiante = usuario("asignacion-estudiante", "estudiante").obtenerId();
        usuarioTutor = usuario("asignacion-tutor", "tutor").obtenerId();
        long otroUsuario = usuario("asignacion-otro-tutor", "tutor").obtenerId();
        long departamento = insertar("INSERT INTO departamentos(nombre) VALUES (?)", "Departamento de asignaciones de prueba");
        long carrera = insertar("INSERT INTO carreras(departamento_id, clave, nombre) VALUES (?, ?, ?)", departamento, "ASIG-PRUEBA", "Carrera de prueba");
        tutor = insertar("INSERT INTO tutores(usuario_id, departamento_id) VALUES (?, ?)", usuarioTutor, departamento);
        otroTutor = insertar("INSERT INTO tutores(usuario_id, departamento_id) VALUES (?, ?)", otroUsuario, departamento);
        estudiante = insertar("INSERT INTO estudiantes(usuario_id, matricula) VALUES (?, ?)", usuarioEstudiante, "ASIG-EST-PRUEBA");
        periodo = insertar("INSERT INTO periodos_academicos(clave, nombre, fecha_inicio, fecha_fin, estado) VALUES (?, ?, ?, ?, ?)", "ASIG-PERIODO", "Periodo de prueba", "2020-01-01", "2099-12-31", "activo");
        insertar("INSERT INTO situaciones_academicas(estudiante_id, periodo_id, carrera_id, semestre) VALUES (?, ?, ?, ?)", estudiante, periodo, carrera, 1);
        autorizacion = token("coordinador");
    }
    org.springframework.test.web.servlet.ResultActions crear(long tutorElegido) throws Exception {
        return cliente.perform(post("/api/coordinacion/asignaciones").header("Authorization", autorizacion)
            .contentType(MediaType.APPLICATION_JSON).content(conversor.writeValueAsString(Map.of(
                "estudianteId", estudiante, "tutorId", tutorElegido, "periodoId", periodo, "motivo", "Prueba de asignación"))));
    }
    long crear() throws Exception {
        var respuesta = crear(tutor).andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("activa"))
            .andExpect(jsonPath("$.asignadoPor").value(autor)).andReturn().getResponse().getContentAsString();
        return conversor.readTree(respuesta).get("id").asLong();
    }
    @Test void creaConsultaYCuentaEstudianteAsignado() throws Exception {
        long id = crear();
        assertThat(asignaciones.buscar(id)).isPresent();
        assertThat(asignaciones.estudiantes(periodo).stream().filter(e -> e.id() == estudiante).findFirst().orElseThrow().asignacionActivaId()).isEqualTo(id);
        cliente.perform(get("/api/coordinacion/asignaciones").header("Authorization", autorizacion))
            .andExpect(status().isOk()).andExpect(jsonPath("$.contenido").isArray());
    }
    @Test void noPermiteDosAsignacionesActivas() throws Exception {
        crear(); crear(otroTutor).andExpect(status().isConflict());
        assertThat(consultas.queryForObject("SELECT COUNT(*) FROM asignaciones_tutor WHERE estudiante_id=?", Long.class, estudiante)).isEqualTo(1);
    }
    @ParameterizedTest @ValueSource(strings={"finalizar", "cancelar"})
    void cierraSinBorrarYPermiteOtraAsignacion(String accion) throws Exception {
        long id = crear();
        cliente.perform(patch("/api/coordinacion/asignaciones/"+id+"/"+accion).header("Authorization", autorizacion)).andExpect(status().isOk());
        var anterior = asignaciones.buscar(id).orElseThrow();
        assertThat(anterior.finEn()).isNotNull();
        assertThat(anterior.estado()).isEqualTo(accion.equals("finalizar") ? "finalizada" : "cancelada");
        crear(otroTutor).andExpect(status().isCreated());
        cliente.perform(patch("/api/coordinacion/asignaciones/"+id+"/"+accion).header("Authorization", autorizacion)).andExpect(status().isConflict());
    }
    @Test void cambioDeTutorConservaHistorial() throws Exception {
        long id = crear();
        cliente.perform(patch("/api/coordinacion/asignaciones/"+id+"/cambiar-tutor").header("Authorization", autorizacion)
            .contentType(MediaType.APPLICATION_JSON).content(conversor.writeValueAsString(Map.of("tutorId", otroTutor, "motivo", "Cambio solicitado"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.tutorId").value(otroTutor)).andExpect(jsonPath("$.estado").value("activa"));
        assertThat(asignaciones.buscar(id).orElseThrow().estado()).isEqualTo("finalizada");
        assertThat(consultas.queryForObject("SELECT COUNT(*) FROM asignaciones_tutor WHERE estudiante_id=?", Long.class, estudiante)).isEqualTo(2);
    }
    @Test void cambioInvalidoConservaAsignacionOriginal() throws Exception {
        long id = crear();
        consultas.update("UPDATE usuarios SET estado='inactivo' WHERE id=(SELECT usuario_id FROM tutores WHERE id=?)", otroTutor);
        cliente.perform(patch("/api/coordinacion/asignaciones/"+id+"/cambiar-tutor").header("Authorization", autorizacion)
            .contentType(MediaType.APPLICATION_JSON).content(conversor.writeValueAsString(Map.of("tutorId", otroTutor))))
            .andExpect(status().isConflict());
        assertThat(asignaciones.buscar(id).orElseThrow().estado()).isEqualTo("activa");
    }
    @Test void exigeExpedienteActivo() throws Exception {
        consultas.update("DELETE FROM situaciones_academicas WHERE estudiante_id=?", estudiante);
        crear(tutor).andExpect(status().isConflict()).andExpect(jsonPath("$.erroresPorCampo.estudianteId").isArray());
    }
    @Test void exigePeriodoActivo() throws Exception {
        consultas.update("UPDATE periodos_academicos SET estado='cerrado' WHERE id=?", periodo);
        crear(tutor).andExpect(status().isConflict()).andExpect(jsonPath("$.erroresPorCampo.periodoId").isArray());
    }
    @ParameterizedTest @ValueSource(strings={"estudiante", "tutor"})
    void rechazaCuentasInactivas(String tipo) throws Exception {
        consultas.update("UPDATE usuarios SET estado='inactivo' WHERE id=?", tipo.equals("estudiante") ? usuarioEstudiante : usuarioTutor);
        crear(tutor).andExpect(status().isConflict());
    }
    @ParameterizedTest @ValueSource(strings={"estudiante", "tutor"})
    void otrosRolesNoPuedenLeerNiEscribir(String rol) throws Exception {
        cliente.perform(get("/api/coordinacion/asignaciones").header("Authorization", token(rol))).andExpect(status().isForbidden());
        cliente.perform(post("/api/coordinacion/asignaciones").header("Authorization", token(rol)).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
    }
    @Test void exigeAutenticacionYDatosValidos() throws Exception {
        cliente.perform(get("/api/coordinacion/asignaciones")).andExpect(status().isUnauthorized());
        cliente.perform(post("/api/coordinacion/asignaciones").header("Authorization", autorizacion).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnprocessableEntity());
    }
    @Test void compruebaRolActualAunqueElTokenAunTengaCoordinador() throws Exception {
        consultas.update("DELETE FROM usuario_roles WHERE usuario_id=?", autor);
        entidades.clear();
        crear(tutor).andExpect(status().isForbidden());
    }
    @Test void cambiarAlMismoTutorNoModificaElHistorial() throws Exception {
        long id = crear();
        cliente.perform(patch("/api/coordinacion/asignaciones/"+id+"/cambiar-tutor").header("Authorization", autorizacion)
            .contentType(MediaType.APPLICATION_JSON).content(conversor.writeValueAsString(Map.of("tutorId", tutor))))
            .andExpect(status().isConflict());
        assertThat(asignaciones.buscar(id).orElseThrow().estado()).isEqualTo("activa");
    }

    @Test void cuentaSesionesSoloDentroDeCadaAsignacion() {
        long anterior = insertar("INSERT INTO asignaciones_tutor(estudiante_id,tutor_id,periodo_id,asignado_por,inicio_en,fin_en,estado) VALUES (?,?,?,?,?,?,?)",
            estudiante, tutor, periodo, autor, "2020-01-01 00:00:00", "2020-02-01 00:00:00", "finalizada");
        long actual = insertar("INSERT INTO asignaciones_tutor(estudiante_id,tutor_id,periodo_id,asignado_por,inicio_en,estado) VALUES (?,?,?,?,?,?)",
            estudiante, tutor, periodo, autor, "2020-02-01 00:00:00", "activa");
        for (String inicio : List.of("2020-01-15 00:00:00", "2020-02-01 00:00:00", "2020-02-15 00:00:00")) {
            consultas.update("INSERT INTO sesiones(tutor_id,estudiante_id,periodo_id,tema,inicio_en,fin_en,modalidad,ubicacion,estado,creado_por) VALUES (?,?,?,?,?,DATE_ADD(?, INTERVAL 1 HOUR),'presencial','Aula de prueba','completada',?)",
                tutor, estudiante, periodo, "Sesión de prueba", inicio, inicio, autor);
        }
        assertThat(asignaciones.buscar(anterior).orElseThrow().sesionesCompletadas()).isEqualTo(1);
        assertThat(asignaciones.buscar(actual).orElseThrow().sesionesCompletadas()).isEqualTo(2);
        assertThat(asignaciones.buscar(anterior).orElseThrow().ultimaSesion()).isEqualTo(java.time.LocalDateTime.parse("2020-01-15T00:00:00"));
    }

    void prepararHistorialPaginado() throws Exception {
        for (int indice=0; indice<7; indice++) {
            insertar("INSERT INTO asignaciones_tutor(estudiante_id,tutor_id,periodo_id,asignado_por,inicio_en,fin_en,estado) VALUES (?,?,?,?,?,?,?)",
                estudiante, tutor, periodo, autor, "2020-01-01 00:00:00", "2020-02-01 00:00:00", "finalizada");
        }
        crear();
    }

    @Test void paginaEnMysqlSinRepetirRegistrosYConTotales() throws Exception {
        prepararHistorialPaginado();
        var ids = new java.util.HashSet<Long>();
        for (int pagina=1; pagina<=3; pagina++) {
            String respuesta = cliente.perform(get("/api/coordinacion/asignaciones").header("Authorization", autorizacion)
                .param("periodoId", String.valueOf(periodo)).param("pagina", String.valueOf(pagina)).param("tamano", "3"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalRegistros").value(8)).andExpect(jsonPath("$.totalPaginas").value(3))
                .andExpect(jsonPath("$.pagina").value(pagina)).andExpect(jsonPath("$.resumen.total").value(8))
                .andExpect(jsonPath("$.resumen.activas").value(1)).andReturn().getResponse().getContentAsString();
            var contenido = conversor.readTree(respuesta).get("contenido");
            assertThat(contenido.size()).isEqualTo(pagina == 3 ? 2 : 3);
            for (var fila : contenido) assertThat(ids.add(fila.get("id").asLong())).isTrue();
        }
        assertThat(ids).hasSize(8);
    }

    @Test void filtrosAfectanConteoYExportacionPeroNoResumenDelPeriodo() throws Exception {
        prepararHistorialPaginado();
        consultas.update("UPDATE usuarios SET nombre=? WHERE id=?", "José Pérez", usuarioEstudiante);
        cliente.perform(get("/api/coordinacion/asignaciones").header("Authorization", autorizacion)
            .param("periodoId", String.valueOf(periodo)).param("estado", "finalizada").param("busqueda", "JOSE").param("tamano", "2"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.contenido.length()").value(2))
            .andExpect(jsonPath("$.totalRegistros").value(7)).andExpect(jsonPath("$.totalPaginas").value(4))
            .andExpect(jsonPath("$.resumen.total").value(8));
        cliente.perform(get("/api/coordinacion/asignaciones/exportar").header("Authorization", autorizacion)
            .param("periodoId", String.valueOf(periodo)).param("estado", "finalizada").param("busqueda", "JOSE"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(7));
    }

    @ParameterizedTest @ValueSource(strings={"%", "_", "' OR 1=1 --"})
    void busquedaTrataLosCaracteresComoTexto(String texto) throws Exception {
        prepararHistorialPaginado();
        cliente.perform(get("/api/coordinacion/asignaciones").header("Authorization", autorizacion)
            .param("periodoId", String.valueOf(periodo)).param("busqueda", texto))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalRegistros").value(0))
            .andExpect(jsonPath("$.totalPaginas").value(0)).andExpect(jsonPath("$.contenido").isEmpty());
    }

    @Test void paginaFueraDeRangoRegresaALaUltimaDisponible() throws Exception {
        prepararHistorialPaginado();
        cliente.perform(get("/api/coordinacion/asignaciones").header("Authorization", autorizacion)
            .param("periodoId", String.valueOf(periodo)).param("pagina", "999").param("tamano", "3"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.pagina").value(3)).andExpect(jsonPath("$.contenido.length()").value(2));
    }

    @Test void rechazaParametrosDePaginacionInvalidos() throws Exception {
        for (var parametro : java.util.Map.of("pagina", "0", "tamano", "101", "estado", "inventado", "periodoId", "-1", "busqueda", "a".repeat(161)).entrySet()) {
            cliente.perform(get("/api/coordinacion/asignaciones").header("Authorization", autorizacion).param(parametro.getKey(), parametro.getValue()))
                .andExpect(status().isUnprocessableEntity());
        }
    }

}
