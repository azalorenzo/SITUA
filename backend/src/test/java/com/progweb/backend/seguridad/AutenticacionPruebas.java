package com.progweb.backend.seguridad;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.progweb.backend.entidades.Usuario;
import com.progweb.backend.repositorios.RepositorioRoles;
import com.progweb.backend.repositorios.RepositorioUsuarios;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "seguridad.jwt.minutos-vigencia=15")
@Transactional
class AutenticacionPruebas {
    @Autowired private WebApplicationContext contexto;
    @Autowired private FilterChainProxy filtroSeguridad;
    @Autowired private RepositorioUsuarios repositorioUsuarios;
    @Autowired private RepositorioRoles repositorioRoles;
    @Autowired private PasswordEncoder codificador;
    @Autowired private JwtEncoder generador;
    @Autowired private JwtDecoder lector;
    @Autowired private ObjectMapper conversor;
    @PersistenceContext private EntityManager administradorEntidades;
    private MockMvc cliente;
    private Usuario usuario;

    @BeforeEach
    void prepararAcceso() {
        cliente = MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtroSeguridad).build();
        usuario = new Usuario("Prueba de acceso", "acceso-prueba@example.invalid",
                codificador.encode("PruebaAcceso!2026"));
        usuario.asignarRol(repositorioRoles.buscarPorCodigo("estudiante").orElseThrow());
        repositorioUsuarios.saveAndFlush(usuario);
    }

    @ParameterizedTest
    @ValueSource(strings = {"coordinador", "tutor", "estudiante"})
    void iniciaSesionConCadaRolSinExponerElHash(String codigo) throws Exception {
        usuario.quitarRol(repositorioRoles.buscarPorCodigo("estudiante").orElseThrow());
        usuario.asignarRol(repositorioRoles.buscarPorCodigo(codigo).orElseThrow());
        repositorioUsuarios.flush();
        String respuesta = cliente.perform(post("/api/autenticacion/iniciar-sesion")
                        .contentType(MediaType.APPLICATION_JSON).content(solicitud("PruebaAcceso!2026")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.tipoToken").value("Bearer"))
                .andExpect(jsonPath("$.usuario.correo").value("acceso-prueba@example.invalid"))
                .andExpect(jsonPath("$.usuario.roles[0]").value(codigo))
                .andExpect(jsonPath("$.usuario.contrasenaHash").doesNotExist())
                .andExpect(jsonPath("$.usuario.password_hash").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        Jwt token = lector.decode(conversor.readTree(respuesta).get("token").asString());
        assertThat(token.getSubject()).isEqualTo(usuario.obtenerId().toString());
        assertThat(token.getId()).isNotBlank();
        assertThat(token.getClaimAsStringList("roles")).containsExactly(codigo);
        assertThat(token.getExpiresAt()).isEqualTo(token.getIssuedAt().plusSeconds(900));
    }

    @Test
    void devuelveElMismoMensajeParaCorreoAusenteYClaveIncorrecta() throws Exception {
        String equivocado = cliente.perform(post("/api/autenticacion/iniciar-sesion")
                        .contentType(MediaType.APPLICATION_JSON).content(solicitud("ClaveIncorrecta!2026")))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String ausente = cliente.perform(post("/api/autenticacion/iniciar-sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(conversor.writeValueAsString(Map.of("correo", "nadie-acceso@example.invalid",
                                "contrasena", "PruebaAcceso!2026"))))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertThat(conversor.readTree(equivocado)).isEqualTo(conversor.readTree(ausente));
    }

    @Test
    void rechazaLaCuentaInactivaAunqueLaClaveSeaCorrecta() throws Exception {
        usuario.desactivar(); repositorioUsuarios.flush();
        cliente.perform(post("/api/autenticacion/iniciar-sesion").contentType(MediaType.APPLICATION_JSON)
                        .content(solicitud("PruebaAcceso!2026")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.estado").value(403));
    }

    @Test
    void rechazaLaCuentaSinRoles() throws Exception {
        usuario.quitarRol(repositorioRoles.buscarPorCodigo("estudiante").orElseThrow());
        repositorioUsuarios.flush();
        cliente.perform(post("/api/autenticacion/iniciar-sesion").contentType(MediaType.APPLICATION_JSON)
                        .content(solicitud("PruebaAcceso!2026"))).andExpect(status().isForbidden());
    }

    @Test
    void validaLosCamposConBeanValidation() throws Exception {
        cliente.perform(post("/api/autenticacion/iniciar-sesion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"invalido\",\"contrasena\":\"corta\"}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.erroresPorCampo.correo").isArray())
                .andExpect(jsonPath("$.erroresPorCampo.contrasena").isArray());
    }

    @Test
    void rechazaJsonMalEscrito() throws Exception {
        cliente.perform(post("/api/autenticacion/iniciar-sesion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.estado").value(400));
    }

    @Test
    void consultaLaSesionUsandoElTokenEnAuthorization() throws Exception {
        cliente.perform(get("/api/autenticacion/sesion").header("Authorization", "Bearer " + iniciarSesion()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(usuario.obtenerId()))
                .andExpect(jsonPath("$.correo").value(usuario.obtenerCorreo()))
                .andExpect(jsonPath("$.contrasenaHash").doesNotExist());
    }

    @Test
    void rechazaLaSesionSinToken() throws Exception {
        cliente.perform(get("/api/autenticacion/sesion")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401)).andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void rechazaUnTokenAlterado() throws Exception {
        String token = iniciarSesion();
        int inicioFirma = token.lastIndexOf('.') + 1;
        char reemplazo = token.charAt(inicioFirma) == 'a' ? 'b' : 'a';
        String alterado = token.substring(0, inicioFirma) + reemplazo + token.substring(inicioFirma + 1);
        cliente.perform(get("/api/autenticacion/sesion").header("Authorization", "Bearer " + alterado))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.estado").value(401));
    }

    @Test
    void rechazaTokensVencidos() throws Exception {
        comprobarTokenRechazado(crearTokenDePrueba("tutoria-api", "tutoria-web", Instant.now().minusSeconds(120)));
    }

    @Test
    void rechazaTokensDeOtroEmisor() throws Exception {
        comprobarTokenRechazado(crearTokenDePrueba("otro-emisor", "tutoria-web", Instant.now().plusSeconds(300)));
    }

    @Test
    void rechazaTokensDirigidosAOtraAplicacion() throws Exception {
        comprobarTokenRechazado(crearTokenDePrueba("tutoria-api", "otra-aplicacion", Instant.now().plusSeconds(300)));
    }

    @Test
    void unEstudianteNoPuedeEntrarEnLasRutasDeCoordinacion() throws Exception {
        cliente.perform(get("/api/coordinacion/comprobar").header("Authorization", "Bearer " + iniciarSesion()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.estado").value(403));
    }

    @Test
    void revisaSiLaCuentaFueDesactivadaAlRecuperarLaSesion() throws Exception {
        String token = iniciarSesion(); usuario.desactivar(); repositorioUsuarios.flush();
        cliente.perform(get("/api/autenticacion/sesion").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void permiteElOrigenLocalDelFrontend() throws Exception {
        cliente.perform(options("/api/autenticacion/iniciar-sesion")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    private String solicitud(String contrasena) throws Exception {
        return conversor.writeValueAsString(Map.of("correo", usuario.obtenerCorreo(), "contrasena", contrasena));
    }

    private String iniciarSesion() throws Exception {
        String respuesta = cliente.perform(post("/api/autenticacion/iniciar-sesion")
                        .contentType(MediaType.APPLICATION_JSON).content(solicitud("PruebaAcceso!2026")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return conversor.readTree(respuesta).get("token").asString();
    }

    private String crearTokenDePrueba(String emisor, String audiencia, Instant vencimiento) {
        JwtClaimsSet datos = JwtClaimsSet.builder().issuer(emisor).audience(List.of(audiencia))
                .subject(usuario.obtenerId().toString()).issuedAt(Instant.now().minusSeconds(600))
                .expiresAt(vencimiento).id(UUID.randomUUID().toString())
                .claim("roles", List.of("estudiante")).build();
        return generador.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), datos))
                .getTokenValue();
    }

    private void comprobarTokenRechazado(String token) throws Exception {
        cliente.perform(get("/api/autenticacion/sesion").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.estado").value(401));
    }
}
