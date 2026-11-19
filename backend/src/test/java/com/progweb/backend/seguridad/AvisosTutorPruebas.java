package com.progweb.backend.seguridad;

import java.time.LocalDateTime;
import java.util.List;
import com.progweb.backend.entidades.Usuario;
import com.progweb.backend.repositorios.RepositorioRoles;
import com.progweb.backend.repositorios.RepositorioUsuarios;
import com.progweb.backend.respuestas.RespuestaUsuario;
import com.progweb.backend.servicios.ServicioTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Pruebas de los avisos que recibe el tutor. Los datos se revierten al terminar cada prueba.
@SpringBootTest
@Transactional
class AvisosTutorPruebas {
    @Autowired WebApplicationContext contexto;
    @Autowired FilterChainProxy filtro;
    @Autowired ServicioTokens tokens;
    @Autowired RepositorioUsuarios usuarios;
    @Autowired RepositorioRoles roles;
    @Autowired JdbcTemplate consultas;
    MockMvc cliente;
    long usuarioTutor, usuarioOtroTutor;
    long antiguo, importante, retirado, oculto, ajeno, medio;
    String autorizacionTutor, autorizacionCoordinador, autorizacionEstudiante;

    long insertar(String sql, Object... datos) {
        consultas.update(sql, datos);
        return consultas.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    long crearUsuario(String correo, String rol) {
        var usuario = new Usuario("Prueba", correo + "@example.invalid", null);
        usuario.asignarRol(roles.buscarPorCodigo(rol).orElseThrow());
        return usuarios.saveAndFlush(usuario).obtenerId();
    }

    String token(long usuario, String rol) {
        return "Bearer " + tokens.crearToken(new RespuestaUsuario(usuario, "Prueba", "prueba@example.invalid", null, List.of(rol))).token();
    }

    long aviso(long autor, String titulo, String prioridad, String publicado, String retiradoEn) {
        return insertar("INSERT INTO avisos(autor_id,titulo,descripcion,prioridad,publicado_en,retirado_en) VALUES (?,?,?,?,?,?)",
                autor, titulo, "Descripción de " + titulo, prioridad, publicado, retiradoEn);
    }

    void destinatario(long aviso, long usuario, String leido, String ocultado) {
        consultas.update("INSERT INTO aviso_destinatarios(aviso_id,usuario_id,leido_en,ocultado_en) VALUES (?,?,?,?)", aviso, usuario, leido, ocultado);
    }

    LocalDateTime lectura(long aviso, long usuario) {
        return consultas.queryForObject("SELECT leido_en FROM aviso_destinatarios WHERE aviso_id=? AND usuario_id=?",
                LocalDateTime.class, aviso, usuario);
    }

    long sinLeer(long usuario) {
        return consultas.queryForObject("SELECT COUNT(*) FROM aviso_destinatarios WHERE usuario_id=? AND leido_en IS NULL", Long.class, usuario);
    }

    ResultActions consultar() throws Exception {
        return cliente.perform(get("/api/tutor/avisos").header("Authorization", autorizacionTutor));
    }

    ResultActions leer(long id) throws Exception {
        return cliente.perform(patch("/api/tutor/avisos/" + id + "/leer").header("Authorization", autorizacionTutor));
    }

    @BeforeEach
    void preparar() {
        cliente = MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtro).build();
        usuarioTutor = crearUsuario("avisos-tutor", "tutor");
        usuarioOtroTutor = crearUsuario("avisos-otro-tutor", "tutor");
        long usuarioCoordinador = crearUsuario("avisos-coordinador", "coordinador");
        long usuarioEstudiante = crearUsuario("avisos-estudiante", "estudiante");
        autorizacionTutor = token(usuarioTutor, "tutor");
        autorizacionCoordinador = token(usuarioCoordinador, "coordinador");
        autorizacionEstudiante = token(usuarioEstudiante, "estudiante");
        antiguo = aviso(usuarioCoordinador, "Aviso antiguo", "normal", "2026-01-05 10:00:00", null);
        importante = aviso(usuarioCoordinador, "Aviso importante", "alta", "2026-01-06 10:00:00", null);
        retirado = aviso(usuarioCoordinador, "Aviso retirado", "alta", "2026-01-06 12:00:00", "2026-02-01 00:00:00");
        oculto = aviso(usuarioCoordinador, "Aviso oculto", "normal", "2026-01-06 13:00:00", null);
        ajeno = aviso(usuarioCoordinador, "Aviso de otro tutor", "normal", "2026-01-06 14:00:00", null);
        medio = aviso(usuarioCoordinador, "Aviso medio", "media", "2026-01-07 10:00:00", null);
        destinatario(antiguo, usuarioTutor, null, null);
        destinatario(antiguo, usuarioOtroTutor, null, null);
        destinatario(importante, usuarioTutor, null, null);
        destinatario(retirado, usuarioTutor, null, null);
        destinatario(oculto, usuarioTutor, null, "2026-01-07 09:00:00");
        destinatario(ajeno, usuarioOtroTutor, null, null);
        destinatario(medio, usuarioTutor, "2026-01-08 09:00:00", null);
    }

    @Test
    void listaSoloLosAvisosVisiblesDelTutorDelMasNuevoAlMasAntiguo() throws Exception {
        consultar().andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].titulo").value("Aviso medio")).andExpect(jsonPath("$[0].prioridad").value("media"))
                .andExpect(jsonPath("$[0].leidoEn", startsWith("2026-01-08T09:00")))
                .andExpect(jsonPath("$[1].titulo").value("Aviso importante")).andExpect(jsonPath("$[1].prioridad").value("alta"))
                .andExpect(jsonPath("$[1].publicadoEn", startsWith("2026-01-06T10:00"))).andExpect(jsonPath("$[1].leidoEn").value(nullValue()))
                .andExpect(jsonPath("$[1].sesionId").value(nullValue()))
                .andExpect(jsonPath("$[2].titulo").value("Aviso antiguo"));
    }

    @Test
    void ignoraUnUsuarioEnviadoDesdeElCliente() throws Exception {
        cliente.perform(get("/api/tutor/avisos?usuarioId=" + usuarioOtroTutor).header("Authorization", autorizacionTutor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void marcaUnAvisoComoLeidoSoloParaElTutorConectado() throws Exception {
        leer(importante).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(importante))
                .andExpect(jsonPath("$.leidoEn").isNotEmpty());
        assertThat(lectura(importante, usuarioTutor)).isNotNull();
        leer(antiguo).andExpect(status().isOk());
        assertThat(lectura(antiguo, usuarioTutor)).isNotNull();
        // El otro destinatario del mismo aviso sigue sin leerlo.
        assertThat(lectura(antiguo, usuarioOtroTutor)).isNull();
    }

    @Test
    void marcarDeNuevoNoCambiaLaFechaDeLectura() throws Exception {
        leer(medio).andExpect(status().isOk()).andExpect(jsonPath("$.leidoEn", startsWith("2026-01-08T09:00")));
        assertThat(lectura(medio, usuarioTutor)).isEqualTo(LocalDateTime.of(2026, 1, 8, 9, 0));
    }

    @Test
    void noMarcaAvisosAjenosRetiradosOcultosNiInexistentes() throws Exception {
        leer(ajeno).andExpect(status().isNotFound());
        leer(retirado).andExpect(status().isNotFound());
        leer(oculto).andExpect(status().isNotFound());
        leer(999999999L).andExpect(status().isNotFound());
        assertThat(lectura(ajeno, usuarioOtroTutor)).isNull();
        assertThat(lectura(retirado, usuarioTutor)).isNull();
        assertThat(lectura(oculto, usuarioTutor)).isNull();
    }

    @Test
    void marcaTodosLosAvisosVisiblesSinTocarLosDemas() throws Exception {
        cliente.perform(patch("/api/tutor/avisos/leer-todos").header("Authorization", autorizacionTutor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.marcados").value(2));
        consultar().andExpect(jsonPath("$[0].leidoEn").isNotEmpty()).andExpect(jsonPath("$[1].leidoEn").isNotEmpty())
                .andExpect(jsonPath("$[2].leidoEn").isNotEmpty());
        // Quedan sin leer el retirado y el oculto del tutor, y los dos avisos del otro tutor.
        assertThat(sinLeer(usuarioTutor)).isEqualTo(2);
        assertThat(sinLeer(usuarioOtroTutor)).isEqualTo(2);
        cliente.perform(patch("/api/tutor/avisos/leer-todos").header("Authorization", autorizacionTutor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.marcados").value(0));
    }

    @Test
    void exigeSesionYRolDeTutor() throws Exception {
        cliente.perform(get("/api/tutor/avisos")).andExpect(status().isUnauthorized());
        cliente.perform(get("/api/tutor/avisos").header("Authorization", autorizacionEstudiante)).andExpect(status().isForbidden());
        // Coordinación puede entrar a la ruta, pero estos avisos son del rol de tutor.
        cliente.perform(get("/api/tutor/avisos").header("Authorization", autorizacionCoordinador)).andExpect(status().isForbidden());
        cliente.perform(patch("/api/tutor/avisos/leer-todos").header("Authorization", autorizacionCoordinador)).andExpect(status().isForbidden());
    }
}
