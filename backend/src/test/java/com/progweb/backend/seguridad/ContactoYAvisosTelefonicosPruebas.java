package com.progweb.backend.seguridad;

import java.util.List;
import com.progweb.backend.entidades.Usuario;
import com.progweb.backend.repositorios.*;
import com.progweb.backend.respuestas.RespuestaUsuario;
import com.progweb.backend.servicios.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="mensajeria.espera-inicial-milisegundos=3600000")
@Transactional
class ContactoYAvisosTelefonicosPruebas {
    @Autowired JdbcTemplate consultas;
    @Autowired RepositorioUsuarios usuarios;
    @Autowired RepositorioRoles roles;
    @Autowired ServicioTokens tokens;
    @Autowired ServicioSesionesAgenda sesiones;
    @Autowired ServicioAvisosTelefonicos avisos;
    @Autowired WebApplicationContext contexto;
    @Autowired FilterChainProxy filtro;
    @MockitoBean EnvioAvisoTelefonico envio;
    MockMvc cliente;
    long alumno,profesor,sesion;
    String accesoAlumno,accesoTutor;
    long insertar(String sql,Object... valores) { consultas.update(sql,valores);return consultas.queryForObject("SELECT LAST_INSERT_ID()",Long.class); }
    long usuario(String rol) {
        var usuario=new Usuario("Prueba avisos "+rol,"telefono-"+rol+"@example.invalid",null);
        usuario.asignarRol(roles.buscarPorCodigo(rol).orElseThrow());return usuarios.saveAndFlush(usuario).obtenerId();
    }
    String token(long usuario,String rol) { return "Bearer "+tokens.crearToken(new RespuestaUsuario(usuario,"Prueba","prueba@example.invalid",null,List.of(rol))).token(); }
    @BeforeEach void preparar() {
        cliente=MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtro).build();alumno=usuario("estudiante");profesor=usuario("tutor");
        accesoAlumno=token(alumno,"estudiante");accesoTutor=token(profesor,"tutor");
        long departamento=insertar("INSERT INTO departamentos(nombre) VALUES ('Avisos telefónicos')");
        long carrera=insertar("INSERT INTO carreras(departamento_id,clave,nombre) VALUES (?,'TEL-PRUEBA','Prueba')",departamento);
        long periodo=insertar("INSERT INTO periodos_academicos(clave,nombre,fecha_inicio,fecha_fin,estado) VALUES ('TEL-PRUEBA','Prueba','2000-01-01','2099-12-31','activo')");
        long tutor=insertar("INSERT INTO tutores(usuario_id,departamento_id) VALUES (?,?)",profesor,departamento);
        long estudiante=insertar("INSERT INTO estudiantes(usuario_id,matricula) VALUES (?,'TEL-PRUEBA')",alumno);
        insertar("INSERT INTO situaciones_academicas(estudiante_id,periodo_id,carrera_id,semestre) VALUES (?,?,?,1)",estudiante,periodo,carrera);
        insertar("INSERT INTO asignaciones_tutor(estudiante_id,tutor_id,periodo_id,asignado_por,inicio_en,estado) VALUES (?,?,?,?,UTC_TIMESTAMP(),'activa')",estudiante,tutor,periodo,alumno);
        sesion=insertar("INSERT INTO sesiones(tutor_id,estudiante_id,periodo_id,tema,inicio_en,fin_en,modalidad,ubicacion,creado_por) VALUES (?,?,?,'Prueba',UTC_TIMESTAMP()+INTERVAL 2 DAY,UTC_TIMESTAMP()+INTERVAL 2 DAY+INTERVAL 1 HOUR,'presencial','Aula',?)",tutor,estudiante,periodo,alumno);
    }
    void contacto() { consultas.update("UPDATE usuarios SET telefono='+525512345678',acepta_sms=true WHERE id=?",alumno); }
    long cantidad() { return consultas.queryForObject("SELECT COUNT(*) FROM notificaciones_sesion WHERE sesion_id=?",Long.class,sesion); }
    @Test void guardaContactoPropioYPermiteRetirarAutorizacion() throws Exception {
        cliente.perform(put("/api/estudiante/contacto").header("Authorization",accesoAlumno).contentType("application/json").content("{\"telefono\":\"+525512345678\",\"aceptaSms\":true}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.aceptaSms").value(true));
        assertThat(consultas.queryForObject("SELECT telefono FROM usuarios WHERE id=?",String.class,profesor)).isNull();
        cliente.perform(put("/api/estudiante/contacto").header("Authorization",accesoAlumno).contentType("application/json").content("{\"telefono\":\"\",\"aceptaSms\":false}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.telefono").isEmpty());
    }
    @Test void validaTelefonoConsentimientoYPermisos() throws Exception {
        for(String telefono:List.of("5512345678",""))cliente.perform(put("/api/estudiante/contacto").header("Authorization",accesoAlumno).contentType("application/json")
            .content("{\"telefono\":\""+telefono+"\",\"aceptaSms\":true}")).andExpect(status().isUnprocessableEntity());
        cliente.perform(get("/api/estudiante/contacto")).andExpect(status().isUnauthorized());
        cliente.perform(get("/api/estudiante/contacto").header("Authorization",accesoTutor)).andExpect(status().isForbidden());
    }
    @Test void confirmarGuardaSmsUnaSolaVezYSinEnviarDuranteLaPeticion() {
        contacto();when(envio.habilitado(anyString())).thenReturn(true);
        sesiones.cambiar(String.valueOf(profesor),true,sesion,"confirmar");avisos.preparar(sesion);
        assertThat(cantidad()).isEqualTo(1);verify(envio,never()).enviar(anyString(),anyString(),any());
        assertThatThrownBy(()->sesiones.cambiar(String.valueOf(profesor),true,sesion,"confirmar")).hasMessageContaining("ya no permite");
        assertThat(cantidad()).isEqualTo(1);
    }
    @Test void sinAutorizacionOCanalesNoSeCreanEnvios() {
        when(envio.habilitado(anyString())).thenReturn(true);sesiones.cambiar(String.valueOf(profesor),true,sesion,"confirmar");assertThat(cantidad()).isZero();
        contacto();when(envio.habilitado(anyString())).thenReturn(false);avisos.preparar(sesion);assertThat(cantidad()).isZero();
    }
    @Test void rechazoNoPreparaEnvios() {
        contacto();when(envio.habilitado(anyString())).thenReturn(true);sesiones.cambiar(String.valueOf(profesor),true,sesion,"rechazar");assertThat(cantidad()).isZero();
    }
    @Test void trabajadorGuardaAceptacionSinVolverAEnviar() {
        contacto();when(envio.habilitado(anyString())).thenReturn(true);sesiones.cambiar(String.valueOf(profesor),true,sesion,"confirmar");
        when(envio.enviar(anyString(),anyString(),any())).thenReturn(new EnvioAvisoTelefonico.Resultado("aceptada","SM"+"a".repeat(32),null));
        avisos.procesar();avisos.procesar();avisos.procesar();verify(envio,times(1)).enviar(anyString(),eq("+525512345678"),any());
        assertThat(consultas.queryForObject("SELECT COUNT(*) FROM notificaciones_sesion WHERE sesion_id=? AND estado='aceptada'",Long.class,sesion)).isEqualTo(1);
    }
    @Test void cancelarORetirarPermisoAntesDeEnviarOmiteLosMensajes() {
        contacto();when(envio.habilitado(anyString())).thenReturn(true);sesiones.cambiar(String.valueOf(profesor),true,sesion,"confirmar");
        consultas.update("UPDATE usuarios SET acepta_sms=false WHERE id=?",alumno);
        avisos.procesar();avisos.procesar();verify(envio,never()).enviar(anyString(),anyString(),any());
        assertThat(consultas.queryForObject("SELECT COUNT(*) FROM notificaciones_sesion WHERE sesion_id=? AND estado='omitida'",Long.class,sesion)).isEqualTo(1);
    }
    @Test void envioInciertoNoSeRepiteAutomaticamente() {
        contacto();when(envio.habilitado(anyString())).thenReturn(true);sesiones.cambiar(String.valueOf(profesor),true,sesion,"confirmar");
        when(envio.enviar(anyString(),anyString(),any())).thenReturn(new EnvioAvisoTelefonico.Resultado("incierta",null,"sin_confirmacion"));
        avisos.procesar();avisos.procesar();avisos.procesar();verify(envio,times(1)).enviar(anyString(),anyString(),any());
    }
}
