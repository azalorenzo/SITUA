package com.progweb.backend.seguridad;

import java.time.*;
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
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @Transactional
class LlamadasPruebas {
    @Autowired ServicioLlamadas servicio;
    @Autowired RepositorioMensajesTutor mensajes;
    @Autowired RepositorioUsuarios usuarios;
    @Autowired RepositorioRoles roles;
    @Autowired ServicioTokens tokens;
    @Autowired JdbcTemplate consultas;
    @Autowired WebApplicationContext contexto;
    @Autowired FilterChainProxy filtro;
    @MockitoBean ProveedorLlamadas proveedor;
    long estudiante,tutor,ajeno,coordinador,conversacion,asignacion;
    MockMvc cliente;
    long insertar(String sql,Object... datos){consultas.update(sql,datos);return consultas.queryForObject("SELECT LAST_INSERT_ID()",Long.class);}
    long cuenta(String nombre,String rol){var cuenta=new Usuario(nombre,"llamada-"+nombre+"@example.invalid",null);cuenta.asignarRol(roles.buscarPorCodigo(rol).orElseThrow());return usuarios.saveAndFlush(cuenta).obtenerId();}
    String sujeto(long usuario){return Long.toString(usuario);}
    String token(long usuario,String rol){return "Bearer "+tokens.crearToken(new RespuestaUsuario(usuario,"Prueba","prueba@example.invalid",null,List.of(rol))).token();}
    ServicioLlamadas.Llamada iniciar(){return servicio.iniciar(sujeto(estudiante),conversacion,"audio");}
    @BeforeEach void preparar(){
        cliente=MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtro).build();
        estudiante=cuenta("estudiante","estudiante");tutor=cuenta("tutor","tutor");ajeno=cuenta("ajeno","estudiante");coordinador=cuenta("coordinador","coordinador");
        long perfilEstudiante=insertar("INSERT INTO estudiantes(usuario_id,matricula) VALUES (?,?)",estudiante,"LLAMADA-PRUEBA");
        long departamento=insertar("INSERT INTO departamentos(nombre) VALUES (?)","Departamento llamada");
        long carrera=insertar("INSERT INTO carreras(departamento_id,clave,nombre) VALUES (?,?,?)",departamento,"LLAMADA-PRUEBA","Carrera llamada");
        long periodo=insertar("INSERT INTO periodos_academicos(clave,nombre,fecha_inicio,fecha_fin,estado) VALUES (?,?,?,?,?)","LLAMADA-PRUEBA","Periodo llamada","2020-01-01","2099-12-31","activo");
        insertar("INSERT INTO situaciones_academicas(estudiante_id,periodo_id,carrera_id,semestre) VALUES (?,?,?,?)",perfilEstudiante,periodo,carrera,3);
        long perfilTutor=insertar("INSERT INTO tutores(usuario_id,departamento_id) VALUES (?,?)",tutor,departamento);
        asignacion=insertar("INSERT INTO asignaciones_tutor(estudiante_id,tutor_id,periodo_id,asignado_por,inicio_en,estado) VALUES (?,?,?,?,'2020-01-01','activa')",perfilEstudiante,perfilTutor,periodo,coordinador);
        mensajes.asegurarConversacion(estudiante,tutor);
        conversacion=consultas.queryForObject("SELECT conversacion_id FROM conversacion_participantes WHERE usuario_id=?",Long.class,estudiante);
        when(proveedor.habilitado()).thenReturn(true);
        when(proveedor.crearSala(anyString(),anyString(),any())).thenAnswer(invocacion->new ProveedorLlamadas.Sala(invocacion.getArgument(0),"https://prueba.daily.co/sala"));
        when(proveedor.crearAcceso(anyString(),anyString(),anyLong(),anyString(),any())).thenReturn("token-privado");
    }
    @Test void invitacionNoCreaSalaNiEntregaAcceso(){
        var llamada=iniciar();assertThat(llamada.estado()).isEqualTo("pendiente");assertThat(llamada.soyIniciador()).isTrue();
        assertThat(Duration.between(llamada.creadaEn(),llamada.venceEn()).toSeconds()).isEqualTo(45);
        var entrante=servicio.actual(sujeto(tutor)).llamada();assertThat(entrante.soyIniciador()).isFalse();assertThat(entrante.nombre()).isEqualTo("estudiante");
        assertThatThrownBy(()->servicio.acceso(sujeto(estudiante),llamada.id())).hasMessageContaining("no está activa");
        verify(proveedor,never()).crearSala(anyString(),anyString(),any());
    }
    @Test void aceptaSoloReceptorYPermiteAccesoATodasLasPartes(){
        var llamada=iniciar();assertThatThrownBy(()->servicio.aceptar(sujeto(estudiante),llamada.id())).hasMessageContaining("otra persona");
        var activa=servicio.aceptar(sujeto(tutor),llamada.id());assertThat(activa.estado()).isEqualTo("activa");
        assertThat(Duration.between(activa.inicioEn(),activa.venceEn()).toSeconds()).isEqualTo(300);
        assertThat(servicio.acceso(sujeto(tutor),llamada.id()).token()).isEqualTo("token-privado");
        assertThat(servicio.acceso(sujeto(estudiante),llamada.id()).token()).isEqualTo("token-privado");
        servicio.aceptar(sujeto(tutor),llamada.id());verify(proveedor,times(1)).crearSala(anyString(),eq("audio"),any());
    }
    @Test void impideVerAceptarColgarOEntrarEnLlamadasAjenas(){
        var llamada=iniciar();assertThat(servicio.actual(sujeto(ajeno)).llamada()).isNull();
        assertThatThrownBy(()->servicio.aceptar(sujeto(ajeno),llamada.id())).hasMessageContaining("No se encontró");
        assertThatThrownBy(()->servicio.acceso(sujeto(ajeno),llamada.id())).hasMessageContaining("No se encontró");
        assertThatThrownBy(()->servicio.terminar(sujeto(ajeno),llamada.id(),false)).hasMessageContaining("No se encontró");
    }
    @Test void rechazarNoConsumeSalaYPuedeVolverALlamar(){
        var llamada=iniciar();servicio.terminar(sujeto(tutor),llamada.id(),true);
        assertThat(servicio.actual(sujeto(estudiante)).llamada()).isNull();
        assertThat(consultas.queryForObject("SELECT estado FROM llamadas WHERE id=?",String.class,llamada.id())).isEqualTo("rechazada");
        assertThat(iniciar().id()).isNotEqualTo(llamada.id());verify(proveedor,never()).crearSala(anyString(),anyString(),any());
    }
    @Test void noPermiteInvitacionesSimultaneasEnAmbasDirecciones(){
        iniciar();assertThatThrownBy(this::iniciar).hasMessageContaining("ya tiene una llamada");
        assertThatThrownBy(()->servicio.iniciar(sujeto(tutor),conversacion,"video")).hasMessageContaining("ya tiene una llamada");
    }
    @Test void cancelarYColgarSonIdempotentesYRevocanAcceso(){
        var llamada=iniciar();servicio.aceptar(sujeto(tutor),llamada.id());
        servicio.terminar(sujeto(estudiante),llamada.id(),false);servicio.terminar(sujeto(estudiante),llamada.id(),false);
        assertThat(servicio.actual(sujeto(tutor)).llamada()).isNull();
        assertThatThrownBy(()->servicio.acceso(sujeto(tutor),llamada.id())).hasMessageContaining("no está activa");
        verify(proveedor,times(1)).cerrarSala(anyString(),eq(estudiante),eq(tutor));
    }
    @Test void llamadasVencidasNoPuedenAceptarseNiAccederse(){
        var llamada=iniciar();consultas.update("UPDATE llamadas SET vence_en='2000-01-01' WHERE id=?",llamada.id());
        assertThat(servicio.actual(sujeto(tutor)).llamada()).isNull();
        assertThatThrownBy(()->servicio.aceptar(sujeto(tutor),llamada.id())).hasMessageContaining("ya terminó");
        servicio.vencer();assertThat(consultas.queryForObject("SELECT estado FROM llamadas WHERE id=?",String.class,llamada.id())).isEqualTo("sin_respuesta");
    }
    @Test void rechazaAsignacionInactivaAlLlamarYAceptar(){
        var llamada=iniciar();consultas.update("UPDATE asignaciones_tutor SET estado='finalizada',fin_en='2099-01-01' WHERE id=?",asignacion);
        assertThatThrownBy(()->servicio.aceptar(sujeto(tutor),llamada.id())).hasMessageContaining("asignación activa");
        servicio.terminar(sujeto(estudiante),llamada.id(),false);
        assertThatThrownBy(this::iniciar).hasMessageContaining("asignación activa");
    }
    @Test void proveedorDesactivadoNoPermiteIniciar(){
        when(proveedor.habilitado()).thenReturn(false);assertThat(servicio.actual(sujeto(tutor)).habilitado()).isFalse();
        assertThatThrownBy(this::iniciar).hasMessageContaining("no están disponibles");
    }
    @Test void cerrarSesionFinalizaLlamadaYRevocaElToken()throws Exception{
        var llamada=iniciar();servicio.aceptar(sujeto(tutor),llamada.id());String acceso=token(estudiante,"estudiante");
        cliente.perform(post("/api/autenticacion/cerrar-sesion").header("Authorization",acceso)).andExpect(status().isOk());
        assertThat(servicio.actual(sujeto(tutor)).llamada()).isNull();
        cliente.perform(get("/api/llamadas/actual").header("Authorization",acceso)).andExpect(status().isUnauthorized());
        verify(proveedor).cerrarSala(anyString(),eq(estudiante),eq(tutor));
    }
    @Test void tampocoEntregaAccesoTrasElLimiteDeUnaLlamadaActiva(){
        var llamada=iniciar();servicio.aceptar(sujeto(tutor),llamada.id());
        consultas.update("UPDATE llamadas SET vence_en='2000-01-01' WHERE id=?",llamada.id());
        assertThatThrownBy(()->servicio.acceso(sujeto(estudiante),llamada.id())).hasMessageContaining("no está activa");
        servicio.vencer();assertThat(consultas.queryForObject("SELECT estado FROM llamadas WHERE id=?",String.class,llamada.id())).isEqualTo("finalizada");
    }
    @Test void validaPermisosYCamposHttp()throws Exception{
        cliente.perform(get("/api/llamadas/actual")).andExpect(status().isUnauthorized());
        cliente.perform(get("/api/llamadas/actual").header("Authorization",token(coordinador,"coordinador"))).andExpect(status().isForbidden());
        cliente.perform(post("/api/llamadas").header("Authorization",token(estudiante,"estudiante")).contentType("application/json").content("{\"conversacionId\":0,\"tipo\":\"cualquiera\"}"))
            .andExpect(status().isUnprocessableEntity());
        cliente.perform(post("/api/llamadas").header("Authorization",token(estudiante,"estudiante")).contentType("application/json").content("{\"conversacionId\":"+conversacion+",\"tipo\":\"video\"}"))
            .andExpect(status().isCreated()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.estado").value("pendiente")).andExpect(jsonPath("$.token").doesNotExist());
    }
}
