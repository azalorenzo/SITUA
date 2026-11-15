package com.progweb.backend.seguridad;

import java.util.HashMap;
import java.util.Map;
import com.progweb.backend.repositorios.RepositorioUsuarios;
import com.progweb.backend.repositorios.RepositorioEstudiantes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class RegistroPruebas {
    @Autowired WebApplicationContext contexto;
    @Autowired FilterChainProxy filtro;
    @Autowired ObjectMapper conversor;
    @Autowired RepositorioUsuarios usuarios;
    @Autowired RepositorioEstudiantes estudiantes;
    @Autowired PasswordEncoder codificador;
    MockMvc cliente;
    @BeforeEach void preparar() { cliente = MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtro).build(); }
    Map<String, Object> datos() {
        return new HashMap<>(Map.of("nombre", "Estudiante nuevo", "correo", "registro-prueba@example.invalid",
            "matricula", "REG-PRUEBA", "contrasena", "PruebaRegistro!123", "confirmarContrasena", "PruebaRegistro!123"));
    }
    org.springframework.test.web.servlet.ResultActions registrar(Map<String, Object> datos) throws Exception {
        return cliente.perform(post("/api/autenticacion/registrar").contentType(MediaType.APPLICATION_JSON)
            .content(conversor.writeValueAsString(datos)));
    }
    @Test void creaPerfilConClaveProtegidaYPermiteIngresar() throws Exception {
        registrar(datos()).andExpect(status().isCreated()).andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.usuario.roles[0]").value("estudiante"));
        var usuario = usuarios.buscarPorCorreo("registro-prueba@example.invalid").orElseThrow();
        assertThat(codificador.matches("PruebaRegistro!123", usuario.obtenerContrasenaHash())).isTrue();
        assertThat(estudiantes.buscarPorMatricula("REG-PRUEBA")).isPresent();
        cliente.perform(post("/api/autenticacion/iniciar-sesion").contentType(MediaType.APPLICATION_JSON)
            .content(conversor.writeValueAsString(Map.of("correo", usuario.obtenerCorreo(), "contrasena", "PruebaRegistro!123"))))
            .andExpect(status().isOk());
    }
    @Test void ignoraRolesEnviadosPorElNavegador() throws Exception {
        var solicitud = datos(); solicitud.put("roles", new String[]{"coordinador"});
        registrar(solicitud).andExpect(status().isCreated()).andExpect(jsonPath("$.usuario.roles.length()").value(1))
            .andExpect(jsonPath("$.usuario.roles[0]").value("estudiante"));
    }
    @Test void rechazaCorreoRepetido() throws Exception {
        registrar(datos()).andExpect(status().isCreated());
        registrar(datos()).andExpect(status().isConflict()).andExpect(jsonPath("$.erroresPorCampo.correo").isArray());
    }
    @Test void matriculaRepetidaNoDejaUsuarioHuerfano() throws Exception {
        registrar(datos()).andExpect(status().isCreated());
        var solicitud = datos(); solicitud.put("correo", "otro-registro@example.invalid");
        registrar(solicitud).andExpect(status().isConflict()).andExpect(jsonPath("$.erroresPorCampo.matricula").isArray());
        assertThat(usuarios.buscarPorCorreo("otro-registro@example.invalid")).isEmpty();
    }
    @Test void rechazaConfirmacionDiferente() throws Exception {
        var solicitud = datos(); solicitud.put("confirmarContrasena", "Diferente!123");
        registrar(solicitud).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.erroresPorCampo.confirmarContrasena").isArray());
    }
    @Test void rechazaClaveDebil() throws Exception {
        var solicitud = datos(); solicitud.put("contrasena", "debil");
        registrar(solicitud).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.erroresPorCampo.contrasena").isArray());
    }
    @Test void rechazaClaveQueExcedeBytesDeBcrypt() throws Exception {
        var solicitud = datos(); String clave = "Aa1!" + "é".repeat(35);
        solicitud.put("contrasena", clave); solicitud.put("confirmarContrasena", clave);
        registrar(solicitud).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.erroresPorCampo.contrasena").isArray());
    }
}
