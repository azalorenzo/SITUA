package com.progweb.backend.seguridad;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.progweb.backend.entidades.Usuario;
import com.progweb.backend.repositorios.*;
import com.progweb.backend.respuestas.RespuestaUsuario;
import com.progweb.backend.servicios.ServicioTokens;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
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
class TutoresPruebas {
    @Autowired WebApplicationContext contexto;
    @Autowired FilterChainProxy filtro;
    @Autowired ServicioTokens tokens;
    @Autowired RepositorioUsuarios usuarios;
    @Autowired RepositorioRoles roles;
    @Autowired RepositorioTutores tutores;
    @Autowired JdbcTemplate consultas;
    @Autowired ObjectMapper conversor;
    @Autowired PasswordEncoder codificador;
    @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager entidades;
    MockMvc cliente;
    long autor,departamento,otroDepartamento,departamentoInactivo;
    String autorizacion;
    long insertar(String sql,Object... datos) {
        consultas.update(sql,datos);return consultas.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
    }
    Usuario usuario(String correo,String rol) {
        var usuario=new Usuario("Prueba",correo,null);usuario.asignarRol(roles.buscarPorCodigo(rol).orElseThrow());return usuarios.saveAndFlush(usuario);
    }
    String token(String rol) {
        return "Bearer "+tokens.crearToken(new RespuestaUsuario(autor,"Prueba","prueba@example.invalid",null,List.of(rol))).token();
    }
    @BeforeEach void preparar() {
        cliente=MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtro).build();
        autor=usuario("tutores-autor@example.invalid","coordinador").obtenerId();autorizacion=token("coordinador");
        departamento=insertar("INSERT INTO departamentos(nombre) VALUES (?)","Departamento de pruebas de tutores");
        otroDepartamento=insertar("INSERT INTO departamentos(nombre) VALUES (?)","Otro departamento de pruebas de tutores");
        departamentoInactivo=insertar("INSERT INTO departamentos(nombre,activo) VALUES (?,0)","Departamento inactivo de pruebas de tutores");
    }
    Map<String,Object> datos() {
        return new HashMap<>(Map.of("nombre","Nuevo tutor","correo","nuevo-tutor-prueba@example.invalid","contrasena","NuevoTutor!123","departamentoId",departamento));
    }
    org.springframework.test.web.servlet.ResultActions crear(Map<String,Object> solicitud) throws Exception {
        return cliente.perform(post("/api/coordinacion/tutores").header("Authorization",autorizacion).contentType(MediaType.APPLICATION_JSON).content(conversor.writeValueAsString(solicitud)));
    }
    long crear() throws Exception {
        String respuesta=crear(datos()).andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("activo"))
            .andExpect(jsonPath("$.contrasenaHash").doesNotExist()).andReturn().getResponse().getContentAsString();
        return conversor.readTree(respuesta).get("id").asLong();
    }
    @Test void creaCuentaConBcryptYUnicamenteRolTutor() throws Exception {
        var solicitud=datos();solicitud.put("roles",List.of("coordinador"));crear(solicitud).andExpect(status().isCreated());
        entidades.clear();var usuario=usuarios.buscarPorCorreoConRoles("nuevo-tutor-prueba@example.invalid").orElseThrow();
        assertThat(codificador.matches("NuevoTutor!123",usuario.obtenerContrasenaHash())).isTrue();
        assertThat(usuario.obtenerRoles()).extracting(rol->rol.obtenerCodigo()).containsExactly("tutor");
        cliente.perform(post("/api/autenticacion/iniciar-sesion").contentType(MediaType.APPLICATION_JSON).content(conversor.writeValueAsString(Map.of("correo",usuario.obtenerCorreo(),"contrasena","NuevoTutor!123"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.usuario.roles[0]").value("tutor"));
    }
    @Test void correoDuplicadoNoCreaOtroPerfil() throws Exception {
        crear();crear(datos()).andExpect(status().isConflict()).andExpect(jsonPath("$.erroresPorCampo.correo").isArray());
        assertThat(consultas.queryForObject("SELECT COUNT(*) FROM tutores WHERE departamento_id=?",Long.class,departamento)).isEqualTo(1);
    }
    @Test void correoDeOtraCuentaNoSeConvierteEnTutor() throws Exception {
        usuario("nuevo-tutor-prueba@example.invalid","estudiante");crear(datos()).andExpect(status().isConflict());
        assertThat(consultas.queryForObject("SELECT COUNT(*) FROM tutores WHERE departamento_id=?",Long.class,departamento)).isZero();
    }
    @Test void departamentoInactivoNoDejaCuentaHuerfana() throws Exception {
        var solicitud=datos();solicitud.put("departamentoId",departamentoInactivo);crear(solicitud).andExpect(status().isConflict());
        assertThat(usuarios.buscarPorCorreo("nuevo-tutor-prueba@example.invalid")).isEmpty();
    }
    @Test void validaCamposYLimiteEnBytesDeLaContrasena() throws Exception {
        crear(Map.of()).andExpect(status().isUnprocessableEntity());
        var solicitud=datos();solicitud.put("contrasena","debil");crear(solicitud).andExpect(status().isUnprocessableEntity());
        solicitud.put("contrasena","Aa1!"+"é".repeat(35));crear(solicitud).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.erroresPorCampo.contrasena").isArray());
    }
    @Test void editarConservaContrasenaPermisosYContacto() throws Exception {
        long id=crear();var previo=usuarios.buscarPorCorreoConRoles("nuevo-tutor-prueba@example.invalid").orElseThrow();
        previo.cambiarTelefono("5551234567");usuarios.flush();String hash=previo.obtenerContrasenaHash();
        cliente.perform(put("/api/coordinacion/tutores/"+id).header("Authorization",autorizacion).contentType(MediaType.APPLICATION_JSON)
            .content(conversor.writeValueAsString(Map.of("nombre","Tutor actualizado","correo","actualizado-tutor@example.invalid","departamentoId",otroDepartamento,"contrasena","NoCambiar!123","roles",List.of("coordinador")))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.departamentoId").value(otroDepartamento));
        entidades.clear();var actualizado=usuarios.buscarPorCorreoConRoles("actualizado-tutor@example.invalid").orElseThrow();
        assertThat(actualizado.obtenerContrasenaHash()).isEqualTo(hash);assertThat(actualizado.obtenerTelefono()).isEqualTo("5551234567");
        assertThat(actualizado.obtenerRoles()).extracting(rol->rol.obtenerCodigo()).containsExactly("tutor");
    }
    @Test void editarNoPermiteCorreoDuplicadoNiNuevoDepartamentoInactivo() throws Exception {
        long id=crear();usuario("ocupado-tutor@example.invalid","estudiante");
        cliente.perform(put("/api/coordinacion/tutores/"+id).header("Authorization",autorizacion).contentType(MediaType.APPLICATION_JSON)
            .content(conversor.writeValueAsString(Map.of("nombre","Prueba","correo","ocupado-tutor@example.invalid","departamentoId",departamento))))
            .andExpect(status().isConflict());
        cliente.perform(put("/api/coordinacion/tutores/"+id).header("Authorization",autorizacion).contentType(MediaType.APPLICATION_JSON)
            .content(conversor.writeValueAsString(Map.of("nombre","Prueba","correo","nuevo-tutor-prueba@example.invalid","departamentoId",departamentoInactivo))))
            .andExpect(status().isConflict());
        assertThat(tutores.buscar(id).orElseThrow().departamentoId()).isEqualTo(departamento);
    }
    @Test void desactivarBloqueaLoginYReactivarLoHabilita() throws Exception {
        long id=crear();
        cliente.perform(patch("/api/coordinacion/tutores/"+id+"/desactivar").header("Authorization",autorizacion)).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("inactivo"));
        String cuerpo=conversor.writeValueAsString(Map.of("correo","nuevo-tutor-prueba@example.invalid","contrasena","NuevoTutor!123"));
        cliente.perform(post("/api/autenticacion/iniciar-sesion").contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isForbidden());
        assertThat(tutores.opciones().stream().noneMatch(tutor->tutor.id()==id)).isTrue();
        cliente.perform(patch("/api/coordinacion/tutores/"+id+"/activar").header("Authorization",autorizacion)).andExpect(status().isOk());
        cliente.perform(post("/api/autenticacion/iniciar-sesion").contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isOk());
    }
    @Test void desactivarConservaAsignaciones() throws Exception {
        long tutor=crear();long usuario=usuario("tutor-historial-estudiante@example.invalid","estudiante").obtenerId();
        long estudiante=insertar("INSERT INTO estudiantes(usuario_id,matricula) VALUES (?,?)",usuario,"TUTOR-HISTORIAL");
        long carrera=insertar("INSERT INTO carreras(departamento_id,clave,nombre) VALUES (?,?,?)",departamento,"TUTOR-HIST","Carrera de prueba");
        long periodo=insertar("INSERT INTO periodos_academicos(clave,nombre,fecha_inicio,fecha_fin,estado) VALUES (?,?,?,?,?)","TUTOR-HIST","Periodo de prueba","2020-01-01","2099-01-01","activo");
        insertar("INSERT INTO situaciones_academicas(estudiante_id,periodo_id,carrera_id,semestre) VALUES (?,?,?,1)",estudiante,periodo,carrera);
        long asignacion=insertar("INSERT INTO asignaciones_tutor(estudiante_id,tutor_id,periodo_id,asignado_por,inicio_en,estado) VALUES (?,?,?,?,UTC_TIMESTAMP(6),'activa')",estudiante,tutor,periodo,autor);
        cliente.perform(patch("/api/coordinacion/tutores/"+tutor+"/desactivar").header("Authorization",autorizacion)).andExpect(status().isOk());
        assertThat(consultas.queryForObject("SELECT estado FROM asignaciones_tutor WHERE id=?",String.class,asignacion)).isEqualTo("activa");
    }
    @Test void noPermiteDesactivarCoordinacionDesdeTutores() throws Exception {
        long id=insertar("INSERT INTO tutores(usuario_id,departamento_id) VALUES (?,?)",autor,departamento);
        cliente.perform(patch("/api/coordinacion/tutores/"+id+"/desactivar").header("Authorization",autorizacion)).andExpect(status().isConflict());
    }
    @ParameterizedTest @ValueSource(strings={"tutor","estudiante"})
    void soloCoordinacionPuedeAdministrar(String rol) throws Exception {
        for(String ruta:List.of("/tutores","/departamentos","/tutores/opciones"))
            cliente.perform(get("/api/coordinacion"+ruta).header("Authorization",token(rol))).andExpect(status().isForbidden());
        cliente.perform(post("/api/coordinacion/tutores").header("Authorization",token(rol)).contentType(MediaType.APPLICATION_JSON).content(conversor.writeValueAsString(datos()))).andExpect(status().isForbidden());
    }
    @Test void validaAutorActualYRecursosInexistentes() throws Exception {
        cliente.perform(get("/api/coordinacion/tutores")).andExpect(status().isUnauthorized());
        cliente.perform(patch("/api/coordinacion/tutores/999999999/activar").header("Authorization",autorizacion)).andExpect(status().isNotFound());
        consultas.update("DELETE FROM usuario_roles WHERE usuario_id=?",autor);entidades.clear();crear(datos()).andExpect(status().isForbidden());
    }
    @Test void paginacionYFiltrosSeAplicanAntesDeContar() throws Exception {
        for(int indice=0;indice<7;indice++) {
            var solicitud=datos();solicitud.put("nombre","José Tutor "+indice);solicitud.put("correo","paginado-tutor-"+indice+"@example.invalid");crear(solicitud).andExpect(status().isCreated());
        }
        var ids=new java.util.HashSet<Long>();
        for(int pagina=1;pagina<=3;pagina++) {
            String respuesta=cliente.perform(get("/api/coordinacion/tutores").header("Authorization",autorizacion)
                .param("pagina",String.valueOf(pagina)).param("tamano","3").param("departamentoId",String.valueOf(departamento)).param("busqueda","JOSE").param("estado","activo"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalRegistros").value(7)).andExpect(jsonPath("$.totalPaginas").value(3))
                .andReturn().getResponse().getContentAsString();
            var contenido=conversor.readTree(respuesta).get("contenido");assertThat(contenido.size()).isEqualTo(pagina==3?1:3);
            for(var fila:contenido)assertThat(ids.add(fila.get("id").asLong())).isTrue();
        }
        assertThat(ids).hasSize(7);
        cliente.perform(get("/api/coordinacion/tutores").header("Authorization",autorizacion).param("departamentoId",String.valueOf(otroDepartamento)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalRegistros").value(0)).andExpect(jsonPath("$.totalPaginas").value(0));
    }
    @Test void validaParametrosYBusquedaLiteral() throws Exception {
        crear();
        for(var parametro:Map.of("pagina","0","tamano","101","estado","otro","departamentoId","-1","busqueda","a".repeat(161)).entrySet())
            cliente.perform(get("/api/coordinacion/tutores").header("Authorization",autorizacion).param(parametro.getKey(),parametro.getValue())).andExpect(status().isUnprocessableEntity());
        for(String texto:List.of("%","_","' OR 1=1 --"))
            cliente.perform(get("/api/coordinacion/tutores").header("Authorization",autorizacion).param("departamentoId",String.valueOf(departamento)).param("busqueda",texto))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalRegistros").value(0));
    }
}
