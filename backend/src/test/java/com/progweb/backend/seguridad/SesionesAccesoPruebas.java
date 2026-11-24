package com.progweb.backend.seguridad;

import java.util.*;
import com.progweb.backend.entidades.Usuario;
import com.progweb.backend.repositorios.*;
import com.progweb.backend.respuestas.RespuestaUsuario;
import com.progweb.backend.servicios.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @Transactional
class SesionesAccesoPruebas {
    @Autowired WebApplicationContext contexto;
    @Autowired FilterChainProxy filtro;
    @Autowired RepositorioUsuarios usuarios;
    @Autowired RepositorioRoles roles;
    @Autowired ServicioTokens tokens;
    @Autowired PasswordEncoder codificador;
    @Autowired JdbcTemplate consultas;
    @Autowired ObjectMapper conversor;
    @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager entidades;
    @MockitoBean EnvioCodigoRecuperacion envio;
    MockMvc cliente;long usuario;String primero,segundo,codigo;
    final String correo="sesiones-acceso-prueba@example.invalid";
    @BeforeEach void preparar(){
        cliente=MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtro).build();
        var cuenta=new Usuario("Prueba de sesiones",correo,codificador.encode("Anterior!123"));cuenta.asignarRol(roles.buscarPorCodigo("estudiante").orElseThrow());
        usuario=usuarios.saveAndFlush(cuenta).obtenerId();primero=token();segundo=token();
        when(envio.habilitado()).thenReturn(true);
        doAnswer(llamada->{codigo=llamada.getArgument(1);return null;}).when(envio).enviar(eq(correo),anyString());
    }
    String token(){return tokens.crearToken(new RespuestaUsuario(usuario,"Prueba de sesiones",correo,null,List.of("estudiante"))).token();}
    void solicitar() throws Exception {cliente.perform(post("/api/autenticacion/recuperacion/solicitar").contentType("application/json").content(conversor.writeValueAsString(Map.of("correo",correo)))).andExpect(status().isOk());}
    org.springframework.test.web.servlet.ResultActions cambiar(String clave,String valor) throws Exception {
        return cliente.perform(post("/api/autenticacion/recuperacion/cambiar-contrasena").contentType("application/json")
                .content(conversor.writeValueAsString(Map.of("correo",correo,"codigo",valor,"contrasena",clave,"confirmacion",clave))));
    }
    org.springframework.test.web.servlet.ResultActions sesion(String token) throws Exception {return cliente.perform(get("/api/autenticacion/sesion").header("Authorization","Bearer "+token));}
    @Test void cerrarInvalidaSoloElTokenPresentado() throws Exception {
        cliente.perform(post("/api/autenticacion/cerrar-sesion").header("Authorization","Bearer "+primero)).andExpect(status().isOk());
        sesion(primero).andExpect(status().isUnauthorized());sesion(segundo).andExpect(status().isOk());
        cliente.perform(get("/api/estudiante/inicio").header("Authorization","Bearer "+primero)).andExpect(status().isUnauthorized());
        assertThat(consultas.queryForObject("SELECT COUNT(*) FROM tokens_revocados WHERE usuario_id=?",Long.class,usuario)).isEqualTo(1);
        cliente.perform(post("/api/autenticacion/cerrar-sesion").header("Authorization","Bearer "+primero)).andExpect(status().isUnauthorized());
    }
    @Test void recuperarInvalidaTodasLasSesionesAnterioresYPermiteNuevoLogin() throws Exception {
        solicitar();cambiar("NuevaClave!123",codigo).andExpect(status().isOk());
        sesion(primero).andExpect(status().isUnauthorized());sesion(segundo).andExpect(status().isUnauthorized());
        entidades.clear();
        String cuerpo=cliente.perform(post("/api/autenticacion/iniciar-sesion").contentType("application/json").content(conversor.writeValueAsString(Map.of("correo",correo,"contrasena","NuevaClave!123"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        sesion(conversor.readTree(cuerpo).get("token").asString()).andExpect(status().isOk());
        assertThat(consultas.queryForObject("SELECT version_sesion FROM usuarios WHERE id=?",Long.class,usuario)).isEqualTo(1);
    }
    @Test void rechazaClavesDebilesSinConsumirElCodigo() throws Exception {
        solicitar();for(String clave:List.of("abcdefgh","Mayusculas!","sinmayuscula!123","SinEspecial123"))cambiar(clave,codigo).andExpect(status().isUnprocessableContent());
        assertThat(consultas.queryForObject("SELECT utilizado FROM recuperaciones_contrasena WHERE usuario_id=?",Boolean.class,usuario)).isFalse();
        sesion(primero).andExpect(status().isOk());cambiar("NuevaClave!123",codigo).andExpect(status().isOk());
    }
    @Test void unCodigoIncorrectoNoInvalidaLasSesiones() throws Exception {
        solicitar();cambiar("NuevaClave!123",codigo.equals("000000")?"111111":"000000").andExpect(status().isUnprocessableContent());
        sesion(primero).andExpect(status().isOk());sesion(segundo).andExpect(status().isOk());
        assertThat(consultas.queryForObject("SELECT version_sesion FROM usuarios WHERE id=?",Long.class,usuario)).isZero();
    }
    @Test void cerrarExigeUnTokenValido() throws Exception {
        cliente.perform(post("/api/autenticacion/cerrar-sesion")).andExpect(status().isUnauthorized());
        cliente.perform(post("/api/autenticacion/cerrar-sesion").header("Authorization","Bearer alterado")).andExpect(status().isUnauthorized());
    }
}
