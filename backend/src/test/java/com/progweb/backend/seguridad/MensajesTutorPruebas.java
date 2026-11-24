package com.progweb.backend.seguridad;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.progweb.backend.entidades.Usuario;
import com.progweb.backend.repositorios.RepositorioRoles;
import com.progweb.backend.repositorios.RepositorioUsuarios;
import com.progweb.backend.respuestas.RespuestaUsuario;
import com.progweb.backend.servicios.ServicioTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Pruebas de los mensajes del tutor. Los datos se revierten al terminar cada prueba.
@SpringBootTest
@Transactional
class MensajesTutorPruebas {
    @Autowired WebApplicationContext contexto;
    @Autowired FilterChainProxy filtro;
    @Autowired ServicioTokens tokens;
    @Autowired RepositorioUsuarios usuarios;
    @Autowired RepositorioRoles roles;
    @Autowired JdbcTemplate consultas;
    @Autowired ObjectMapper conversor;
    MockMvc cliente;
    long usuarioTutor, usuarioAna, usuarioBeto, usuarioCarla, usuarioDario, usuarioOtroTutor;
    long conversacionAna, conversacionBeto, conversacionAjena, conversacionVacia;
    String autorizacionTutor, autorizacionCoordinador, autorizacionEstudiante;

    long insertar(String sql, Object... datos) {
        consultas.update(sql, datos);
        return consultas.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    long crearUsuario(String nombre, String correo, String rol) {
        var usuario = new Usuario(nombre, correo + "@example.invalid", null);
        usuario.asignarRol(roles.buscarPorCodigo(rol).orElseThrow());
        return usuarios.saveAndFlush(usuario).obtenerId();
    }

    String token(long usuario, String rol) {
        return "Bearer " + tokens.crearToken(new RespuestaUsuario(usuario, "Prueba", "prueba@example.invalid", null, List.of(rol))).token();
    }

    long conversacion(long creador, long... participantes) {
        long id = insertar("INSERT INTO conversaciones(tipo,creada_por) VALUES ('directa',?)", creador);
        for (long participante : participantes) {
            consultas.update("INSERT INTO conversacion_participantes(conversacion_id,usuario_id) VALUES (?,?)", id, participante);
        }
        return id;
    }

    void mensaje(long conversacion, long emisor, String texto, String fecha) {
        consultas.update("INSERT INTO mensajes(conversacion_id,emisor_id,contenido,enviado_en) VALUES (?,?,?,?)", conversacion, emisor, texto, fecha);
    }

    ResultActions conversaciones(String autorizacion) throws Exception {
        return cliente.perform(get("/api/tutor/mensajes/conversaciones").header("Authorization", autorizacion));
    }

    ResultActions mensajes(String autorizacion, long conversacion) throws Exception {
        return cliente.perform(get("/api/tutor/mensajes/conversaciones/" + conversacion + "/mensajes").header("Authorization", autorizacion));
    }

    ResultActions enviar(String autorizacion, long conversacion, Map<String, Object> cuerpo) throws Exception {
        return cliente.perform(post("/api/tutor/mensajes/conversaciones/" + conversacion + "/mensajes").header("Authorization", autorizacion)
                .contentType(MediaType.APPLICATION_JSON).content(conversor.writeValueAsString(cuerpo)));
    }

    ResultActions leer(String autorizacion, long conversacion) throws Exception {
        return cliente.perform(patch("/api/tutor/mensajes/conversaciones/" + conversacion + "/leer").header("Authorization", autorizacion));
    }

    @BeforeEach
    void preparar() {
        cliente = MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtro).build();
        usuarioTutor = crearUsuario("Tutora de prueba", "msj-tutor", "tutor");
        usuarioOtroTutor = crearUsuario("Otro tutor de prueba", "msj-otro-tutor", "tutor");
        long usuarioCoordinador = crearUsuario("Coordinación de prueba", "msj-coord", "coordinador");
        usuarioAna = crearUsuario("Alumna Ana", "msj-ana", "estudiante");
        usuarioBeto = crearUsuario("Alumno Beto", "msj-beto", "estudiante");
        usuarioCarla = crearUsuario("Alumna Carla", "msj-carla", "estudiante");
        usuarioDario = crearUsuario("Alumno Darío", "msj-dario", "estudiante");
        consultas.update("UPDATE usuarios SET avatar_url='https://example.invalid/ana.png' WHERE id=?", usuarioAna);
        autorizacionTutor = token(usuarioTutor, "tutor");
        autorizacionCoordinador = token(usuarioCoordinador, "coordinador");
        autorizacionEstudiante = token(usuarioAna, "estudiante");

        conversacionAna = conversacion(usuarioAna, usuarioAna, usuarioTutor);
        mensaje(conversacionAna, usuarioAna, "Hola, tengo una duda", "2026-10-01 10:00:00");
        mensaje(conversacionAna, usuarioTutor, "Dime, te escucho", "2026-10-01 10:05:00");
        mensaje(conversacionAna, usuarioAna, "Gracias por la explicación", "2026-10-01 10:10:00");
        conversacionBeto = conversacion(usuarioTutor, usuarioTutor, usuarioBeto);
        mensaje(conversacionBeto, usuarioTutor, "Recuerda la sesión de mañana", "2026-09-30 09:00:00");
        conversacionAjena = conversacion(usuarioCarla, usuarioCarla, usuarioOtroTutor);
        mensaje(conversacionAjena, usuarioCarla, "Mensaje privado de otra conversación", "2026-10-02 08:00:00");
        conversacionVacia = conversacion(usuarioTutor, usuarioTutor, usuarioDario);
    }

    @Test
    void listaSoloConversacionesDelTutorConLaOtraPersonaUltimoMensajeYNoLeidos() throws Exception {
        conversaciones(autorizacionTutor).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(conversacionAna)).andExpect(jsonPath("$[0].nombre").value("Alumna Ana"))
                .andExpect(jsonPath("$[0].avatarUrl").value("https://example.invalid/ana.png"))
                .andExpect(jsonPath("$[0].ultimoMensaje").value("Gracias por la explicación"))
                .andExpect(jsonPath("$[0].ultimoEnviadoEn", startsWith("2026-10-01T10:10"))).andExpect(jsonPath("$[0].noLeidos").value(2))
                .andExpect(jsonPath("$[1].id").value(conversacionBeto)).andExpect(jsonPath("$[1].nombre").value("Alumno Beto"))
                .andExpect(jsonPath("$[1].avatarUrl").value(nullValue())).andExpect(jsonPath("$[1].noLeidos").value(0))
                .andExpect(jsonPath("$[2].id").value(conversacionVacia)).andExpect(jsonPath("$[2].ultimoMensaje").value(nullValue()))
                .andExpect(jsonPath("$[2].ultimoEnviadoEn").value(nullValue()));
    }

    @Test
    void consultaLosMensajesEnOrdenCronologicoIndicandoCuálesEnvioElTutor() throws Exception {
        mensajes(autorizacionTutor, conversacionAna).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].contenido").value("Hola, tengo una duda")).andExpect(jsonPath("$[0].enviado").value(false))
                .andExpect(jsonPath("$[1].contenido").value("Dime, te escucho")).andExpect(jsonPath("$[1].enviado").value(true))
                .andExpect(jsonPath("$[1].enviadoEn", startsWith("2026-10-01T10:05")))
                .andExpect(jsonPath("$[2].contenido").value("Gracias por la explicación")).andExpect(jsonPath("$[2].enviado").value(false));
        mensajes(autorizacionTutor, conversacionVacia).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void noMuestraNiEscribeEnConversacionesAjenasOInexistentes() throws Exception {
        mensajes(autorizacionTutor, conversacionAjena).andExpect(status().isNotFound());
        mensajes(autorizacionTutor, 999999999L).andExpect(status().isNotFound());
        leer(autorizacionTutor, conversacionAjena).andExpect(status().isNotFound());
        enviar(autorizacionTutor, conversacionAjena, new HashMap<>(Map.of("contenido", "Intruso"))).andExpect(status().isNotFound());
        assertThat(consultas.queryForObject("SELECT COUNT(*) FROM mensajes WHERE conversacion_id=?", Long.class, conversacionAjena)).isEqualTo(1);
    }

    @Test
    void marcarComoLeidaSoloAvanzaLaMarcaDelTutor() throws Exception {
        leer(autorizacionTutor, conversacionAna).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(conversacionAna));
        assertThat(consultas.queryForObject("SELECT leido_hasta_en IS NOT NULL FROM conversacion_participantes WHERE conversacion_id=? AND usuario_id=?",
                Boolean.class, conversacionAna, usuarioTutor)).isTrue();
        assertThat(consultas.queryForObject("SELECT leido_hasta_en IS NULL FROM conversacion_participantes WHERE conversacion_id=? AND usuario_id=?",
                Boolean.class, conversacionAna, usuarioAna)).isTrue();
        conversaciones(autorizacionTutor).andExpect(jsonPath("$[0].noLeidos").value(0));
        // Marcar de nuevo, o marcar una conversación sin mensajes, no falla.
        leer(autorizacionTutor, conversacionAna).andExpect(status().isOk());
        leer(autorizacionTutor, conversacionVacia).andExpect(status().isOk());
    }

    @Test
    void enviaUnMensajeComoElTutorConectado() throws Exception {
        var cuerpo = new HashMap<String, Object>(Map.of("contenido", "  Nos vemos el viernes  "));
        cuerpo.put("emisorId", usuarioAna);
        enviar(autorizacionTutor, conversacionAna, cuerpo).andExpect(status().isCreated())
                .andExpect(jsonPath("$.contenido").value("Nos vemos el viernes")).andExpect(jsonPath("$.enviado").value(true));
        assertThat(consultas.queryForObject("SELECT COUNT(*) FROM mensajes WHERE conversacion_id=? AND emisor_id=? AND contenido='Nos vemos el viernes'",
                Long.class, conversacionAna, usuarioTutor)).isEqualTo(1);
        mensajes(autorizacionTutor, conversacionAna).andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[3].contenido").value("Nos vemos el viernes"));
        conversaciones(autorizacionTutor).andExpect(jsonPath("$[0].ultimoMensaje").value("Nos vemos el viernes"));
    }

    @Test
    void rechazaMensajesVaciosOMuyLargos() throws Exception {
        enviar(autorizacionTutor, conversacionAna, new HashMap<>(Map.of("contenido", "   "))).andExpect(status().isUnprocessableEntity());
        enviar(autorizacionTutor, conversacionAna, new HashMap<>()).andExpect(status().isUnprocessableEntity());
        enviar(autorizacionTutor, conversacionAna, new HashMap<>(Map.of("contenido", "M".repeat(4001)))).andExpect(status().isUnprocessableEntity());
        assertThat(consultas.queryForObject("SELECT COUNT(*) FROM mensajes WHERE conversacion_id=?", Long.class, conversacionAna)).isEqualTo(3);
    }

    @Test
    void exigeSesionYRolDeTutor() throws Exception {
        cliente.perform(get("/api/tutor/mensajes/conversaciones")).andExpect(status().isUnauthorized());
        conversaciones(autorizacionEstudiante).andExpect(status().isForbidden());
        // Coordinación puede entrar a la ruta, pero estas conversaciones son del rol de tutor.
        conversaciones(autorizacionCoordinador).andExpect(status().isForbidden());
        enviar(autorizacionCoordinador, conversacionAna, new HashMap<>(Map.of("contenido", "Hola"))).andExpect(status().isForbidden());
    }
    String rutaEstudiante() { return "/api/estudiante/mensajes/conversaciones"; }

    @Test
    void elEstudianteConsultaYSoloEnviaComoSuCuenta() throws Exception {
        cliente.perform(get(rutaEstudiante()).header("Authorization",autorizacionEstudiante))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Tutora de prueba"));
        cliente.perform(post(rutaEstudiante()+"/"+conversacionAna+"/mensajes").header("Authorization",autorizacionEstudiante)
                .contentType(MediaType.APPLICATION_JSON).content("{\"contenido\":\"Respuesta del estudiante\",\"emisorId\":"+usuarioTutor+"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.enviado").value(true));
        assertThat(consultas.queryForObject("SELECT emisor_id FROM mensajes WHERE conversacion_id=? AND contenido='Respuesta del estudiante'",Long.class,conversacionAna)).isEqualTo(usuarioAna);
        mensajes(autorizacionTutor,conversacionAna).andExpect(jsonPath("$[3].contenido").value("Respuesta del estudiante"))
                .andExpect(jsonPath("$[3].enviado").value(false));
    }

    @Test
    void elEstudianteNoConsultaEnviaNiLeeConversacionesAjenas() throws Exception {
        String ruta=rutaEstudiante()+"/"+conversacionBeto;
        cliente.perform(get(ruta+"/mensajes").header("Authorization",autorizacionEstudiante)).andExpect(status().isNotFound());
        cliente.perform(patch(ruta+"/leer").header("Authorization",autorizacionEstudiante)).andExpect(status().isNotFound());
        cliente.perform(post(ruta+"/mensajes").header("Authorization",autorizacionEstudiante).contentType(MediaType.APPLICATION_JSON)
                .content("{\"contenido\":\"Ajeno\"}")).andExpect(status().isNotFound());
    }

    @Test
    void losDosRolesRecibenLosMensajesDeLaOtraPersona() throws Exception {
        enviar(autorizacionTutor,conversacionAna,new HashMap<>(Map.of("contenido","Respuesta real del tutor"))).andExpect(status().isCreated());
        cliente.perform(get(rutaEstudiante()+"/"+conversacionAna+"/mensajes").header("Authorization",autorizacionEstudiante))
                .andExpect(status().isOk()).andExpect(jsonPath("$[3].contenido").value("Respuesta real del tutor"))
                .andExpect(jsonPath("$[3].enviado").value(false));
    }

    @Test
    void laLecturaDelEstudianteNoAfectaLaDelTutor() throws Exception {
        cliente.perform(patch(rutaEstudiante()+"/"+conversacionAna+"/leer").header("Authorization",autorizacionEstudiante)).andExpect(status().isOk());
        cliente.perform(get(rutaEstudiante()).header("Authorization",autorizacionEstudiante)).andExpect(jsonPath("$[0].noLeidos").value(0));
        conversaciones(autorizacionTutor).andExpect(jsonPath("$[0].noLeidos").value(2));
    }

    @Test
    void soloMarcaMensajesConsultadosAunqueCompartanLaHora() throws Exception {
        long primero=insertar("INSERT INTO mensajes(conversacion_id,emisor_id,contenido,enviado_en) VALUES (?,?,?,?)",conversacionAna,usuarioTutor,"Primero","2030-01-01 10:00:00");
        insertar("INSERT INTO mensajes(conversacion_id,emisor_id,contenido,enviado_en) VALUES (?,?,?,?)",conversacionAna,usuarioTutor,"Segundo","2030-01-01 10:00:00");
        cliente.perform(patch(rutaEstudiante()+"/"+conversacionAna+"/leer").param("ultimoMensajeId",String.valueOf(primero))
                .header("Authorization",autorizacionEstudiante)).andExpect(status().isOk());
        cliente.perform(get(rutaEstudiante()).header("Authorization",autorizacionEstudiante)).andExpect(jsonPath("$[0].noLeidos").value(1));
        cliente.perform(patch(rutaEstudiante()+"/"+conversacionAna+"/leer").param("ultimoMensajeId","999999999")
                .header("Authorization",autorizacionEstudiante)).andExpect(status().isNotFound());
    }

    @Test
    void laBusquedaSeAplicaEnElServidorSinComodinesNiConversacionesAjenas() throws Exception {
        cliente.perform(get(rutaEstudiante()).param("busqueda","Tutora").header("Authorization",autorizacionEstudiante)).andExpect(jsonPath("$.length()").value(1));
        cliente.perform(get(rutaEstudiante()).param("busqueda","Otro").header("Authorization",autorizacionEstudiante)).andExpect(jsonPath("$.length()").value(0));
        cliente.perform(get(rutaEstudiante()).param("busqueda","%").header("Authorization",autorizacionEstudiante)).andExpect(jsonPath("$.length()").value(0));
        cliente.perform(get(rutaEstudiante()).param("busqueda","a".repeat(201)).header("Authorization",autorizacionEstudiante)).andExpect(status().isUnprocessableContent());
    }

    @Test
    void exigeRolEstudianteYValidaElContenido() throws Exception {
        cliente.perform(get(rutaEstudiante())).andExpect(status().isUnauthorized());
        cliente.perform(get(rutaEstudiante()).header("Authorization",autorizacionTutor)).andExpect(status().isForbidden());
        cliente.perform(get(rutaEstudiante()).header("Authorization",autorizacionCoordinador)).andExpect(status().isForbidden());
        for(String contenido:List.of("   ","a".repeat(4001))) cliente.perform(post(rutaEstudiante()+"/"+conversacionAna+"/mensajes")
                .header("Authorization",autorizacionEstudiante).contentType(MediaType.APPLICATION_JSON)
                .content(conversor.writeValueAsString(Map.of("contenido",contenido)))).andExpect(status().isUnprocessableContent());
    }

    @Test
    void creaUnaSolaConversacionConElTutorAsignadoYConservaElHistorial() throws Exception {
        long departamento=insertar("INSERT INTO departamentos(nombre) VALUES (?)","Departamento mensajes");
        long tutor=insertar("INSERT INTO tutores(usuario_id,departamento_id) VALUES (?,?)",usuarioTutor,departamento);
        long carrera=insertar("INSERT INTO carreras(departamento_id,clave,nombre) VALUES (?,?,?)",departamento,"MENSAJES-PRUEBA","Carrera mensajes");
        long periodo=insertar("INSERT INTO periodos_academicos(clave,nombre,fecha_inicio,fecha_fin,estado) VALUES (?,?,?,?,?)","MENSAJES-PRUEBA","Periodo mensajes","2020-01-01","2031-12-31","activo");
        long estudiante=insertar("INSERT INTO estudiantes(usuario_id,matricula) VALUES (?,?)",usuarioCarla,"MENSAJES-CARLA");
        insertar("INSERT INTO situaciones_academicas(estudiante_id,periodo_id,carrera_id,semestre) VALUES (?,?,?,3)",estudiante,periodo,carrera);
        long asignacion=insertar("INSERT INTO asignaciones_tutor(estudiante_id,tutor_id,periodo_id,asignado_por,inicio_en,estado) VALUES (?,?,?,?,?,'activa')",estudiante,tutor,periodo,usuarioTutor,"2026-01-01 12:00:00");
        String acceso=token(usuarioCarla,"estudiante");
        for(int repeticion=0;repeticion<2;repeticion++){
            cliente.perform(get(rutaEstudiante()).header("Authorization",acceso)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
            conversaciones(autorizacionTutor).andExpect(status().isOk());
        }
        long menor=Math.min(usuarioCarla,usuarioTutor),mayor=Math.max(usuarioCarla,usuarioTutor);
        assertThat(consultas.queryForObject("SELECT COUNT(*) FROM conversaciones WHERE usuario_menor_id=? AND usuario_mayor_id=?",Long.class,menor,mayor)).isEqualTo(1);
        long conversacion=consultas.queryForObject("SELECT id FROM conversaciones WHERE usuario_menor_id=? AND usuario_mayor_id=?",Long.class,menor,mayor);
        assertThat(consultas.queryForObject("SELECT COUNT(*) FROM conversacion_participantes WHERE conversacion_id=?",Long.class,conversacion)).isEqualTo(2);
        consultas.update("UPDATE asignaciones_tutor SET estado='finalizada',fin_en='2026-10-01 12:00:00' WHERE id=?",asignacion);
        cliente.perform(get(rutaEstudiante()+"/"+conversacion+"/mensajes").header("Authorization",acceso)).andExpect(status().isOk());
    }

}
