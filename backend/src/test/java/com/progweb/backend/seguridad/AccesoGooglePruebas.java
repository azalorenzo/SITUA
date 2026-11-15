package com.progweb.backend.seguridad;

import java.util.Map;
import com.progweb.backend.entidades.Usuario;
import com.progweb.backend.repositorios.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class AccesoGooglePruebas {
    @MockitoBean VerificadorGoogle verificador;
    @Autowired WebApplicationContext contexto;
    @Autowired FilterChainProxy filtro;
    @Autowired ObjectMapper conversor;
    @Autowired RepositorioUsuarios usuarios;
    @Autowired RepositorioRoles roles;
    @Autowired RepositorioIdentidades identidades;
    @Autowired RepositorioEstudiantes estudiantes;
    @Autowired PasswordEncoder codificador;
    MockMvc cliente;
    @BeforeEach void preparar() {
        cliente = MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtro).build();
        when(verificador.verificar("credencial-prueba", "comprobacion-prueba"))
            .thenReturn(new DatosGoogle("google-prueba", "google-prueba@example.invalid", "Estudiante Google", null));
    }
    org.springframework.test.web.servlet.ResultActions ingresar(String campo, String valor) throws Exception {
        var datos = new java.util.HashMap<String, String>(Map.of("credencial", "credencial-prueba", "comprobacion", "comprobacion-prueba"));
        if (campo != null) datos.put(campo, valor);
        return cliente.perform(post("/api/autenticacion/google").contentType(MediaType.APPLICATION_JSON)
            .content(conversor.writeValueAsString(datos)));
    }
    Usuario cuentaLocal() {
        var usuario = new Usuario("Cuenta local", "google-prueba@example.invalid", codificador.encode("ClaveLocal!123"));
        usuario.asignarRol(roles.buscarPorCodigo("tutor").orElseThrow());
        return usuarios.saveAndFlush(usuario);
    }
    @Test void pideMatriculaAntesDeCrear() throws Exception {
        ingresar(null, null).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.erroresPorCampo.matricula").isArray());
        assertThat(usuarios.buscarPorCorreo("google-prueba@example.invalid")).isEmpty();
    }
    @Test void creaEstudianteSinClaveLocalYReutilizaIdentidad() throws Exception {
        ingresar("matricula", "GOOGLE-PRUEBA").andExpect(status().isOk()).andExpect(jsonPath("$.usuario.roles[0]").value("estudiante"));
        assertThat(usuarios.buscarPorCorreo("google-prueba@example.invalid").orElseThrow().obtenerContrasenaHash()).isNull();
        assertThat(estudiantes.buscarPorMatricula("GOOGLE-PRUEBA")).isPresent();
        long cantidad = usuarios.count();
        ingresar(null, null).andExpect(status().isOk());
        assertThat(usuarios.count()).isEqualTo(cantidad);
        cliente.perform(post("/api/autenticacion/iniciar-sesion").contentType(MediaType.APPLICATION_JSON)
            .content(conversor.writeValueAsString(Map.of("correo", "google-prueba@example.invalid", "contrasena", "ClaveLocal!123"))))
            .andExpect(status().isUnauthorized());
    }
    @Test void cuentaExistenteExigeClaveYConservaRol() throws Exception {
        var usuario = cuentaLocal();
        ingresar(null, null).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.erroresPorCampo.contrasenaVinculacion").isArray());
        ingresar("contrasenaVinculacion", "Incorrecta!123").andExpect(status().isUnauthorized());
        assertThat(identidades.buscarGoogle("google-prueba")).isEmpty();
        ingresar("contrasenaVinculacion", "ClaveLocal!123").andExpect(status().isOk()).andExpect(jsonPath("$.usuario.roles[0]").value("tutor"));
        assertThat(identidades.buscarGoogle("google-prueba").orElseThrow().obtenerUsuario().obtenerId()).isEqualTo(usuario.obtenerId());
        ingresar(null, null).andExpect(status().isOk());
    }
    @Test void noPermiteVincularCuentaInactiva() throws Exception {
        var usuario = cuentaLocal(); usuario.desactivar(); usuarios.flush();
        ingresar("contrasenaVinculacion", "ClaveLocal!123").andExpect(status().isForbidden());
        assertThat(identidades.buscarGoogle("google-prueba")).isEmpty();
    }
}
