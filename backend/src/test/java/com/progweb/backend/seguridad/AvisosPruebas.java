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
class AvisosPruebas {
    @Autowired WebApplicationContext contexto;
    @Autowired FilterChainProxy filtro;
    @Autowired ServicioTokens tokens;
    @Autowired RepositorioUsuarios usuarios;
    @Autowired RepositorioRoles roles;
    @Autowired RepositorioAvisos avisos;
    @Autowired JdbcTemplate consultas;
    @Autowired ObjectMapper conversor;
    @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager entidades;
    MockMvc cliente;
    long autor,estudiante,tutor,ambos,inactivo;
    String autorizacion;
    long usuario(String correo,String... codigos){
        var usuario=new Usuario(correo,correo+"@example.invalid",null);
        for(String codigo:codigos)usuario.asignarRol(roles.buscarPorCodigo(codigo).orElseThrow());
        usuarios.saveAndFlush(usuario);return usuario.obtenerId();
    }
    String token(String rol){return "Bearer "+tokens.crearToken(new RespuestaUsuario(autor,"Prueba","prueba@example.invalid",null,List.of(rol))).token();}
    @BeforeEach void preparar(){
        cliente=MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtro).build();
        autor=usuario("avisos-coordinador","coordinador");estudiante=usuario("avisos-estudiante","estudiante");
        tutor=usuario("avisos-tutor","tutor");ambos=usuario("avisos-ambos","estudiante","tutor");
        inactivo=usuario("avisos-inactivo","estudiante");consultas.update("UPDATE usuarios SET estado='inactivo' WHERE id=?",inactivo);
        autorizacion=token("coordinador");
    }
    Map<String,Object> datos(){return new HashMap<>(Map.of("titulo","Aviso de prueba","descripcion","Descripción de prueba","categoria","qa-avisos","prioridad","alta","destinatarios","seleccion","usuarioIds",List.of(estudiante,tutor)));}
    org.springframework.test.web.servlet.ResultActions crear(Map<String,Object> solicitud)throws Exception{
        return cliente.perform(post("/api/coordinacion/avisos").header("Authorization",autorizacion).contentType(MediaType.APPLICATION_JSON).content(conversor.writeValueAsString(solicitud)));
    }
    long crear()throws Exception{
        String respuesta=crear(datos()).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return conversor.readTree(respuesta).get("aviso").get("id").asLong();
    }
    org.springframework.test.web.servlet.ResultActions editar(long id,Map<String,Object> solicitud)throws Exception{
        return cliente.perform(put("/api/coordinacion/avisos/"+id).header("Authorization",autorizacion).contentType(MediaType.APPLICATION_JSON).content(conversor.writeValueAsString(solicitud)));
    }
    @Test void creaAvisoYDestinatariosSinGenerarEnviosExternos()throws Exception{
        long envios=consultas.queryForObject("SELECT COUNT(*) FROM envios_comunicacion",Long.class);
        var solicitud=datos();solicitud.put("autorId",estudiante);solicitud.put("usuarioIds",List.of(estudiante,tutor,estudiante));
        String respuesta=crear(solicitud).andExpect(status().isCreated()).andExpect(jsonPath("$.aviso.autorId").value(autor))
            .andExpect(jsonPath("$.aviso.destinatarios").value(2)).andExpect(jsonPath("$.aviso.lecturas").value(0)).andReturn().getResponse().getContentAsString();
        long id=conversor.readTree(respuesta).get("aviso").get("id").asLong();
        assertThat(avisos.destinatarios(id)).hasSize(2);assertThat(consultas.queryForObject("SELECT COUNT(*) FROM envios_comunicacion",Long.class)).isEqualTo(envios);
        assertThat(consultas.queryForObject("SELECT COUNT(*) FROM v_avisos_visibles WHERE aviso_id=?",Long.class,id)).isEqualTo(2);
    }
    @ParameterizedTest @ValueSource(strings={"todos","estudiantes","tutores"})
    void gruposSeResuelvenSinDuplicarUsuarios(String grupo)throws Exception{
        var solicitud=datos();solicitud.put("destinatarios",grupo);solicitud.remove("usuarioIds");
        String respuesta=crear(solicitud).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id=conversor.readTree(respuesta).get("aviso").get("id").asLong();
        var ids=avisos.destinatarios(id).stream().map(destinatario->destinatario.usuarioId()).toList();
        assertThat(ids).contains(ambos).doesNotHaveDuplicates().doesNotContain(inactivo,autor);
        if(!grupo.equals("tutores"))assertThat(ids).contains(estudiante);
        if(!grupo.equals("estudiantes"))assertThat(ids).contains(tutor);
    }
    @Test void seleccionInvalidaNoDejaAvisoParcial()throws Exception{
        long inicial=consultas.queryForObject("SELECT COUNT(*) FROM avisos",Long.class);
        for(long invalido:List.of(inactivo,autor,999999999L)){
            var solicitud=datos();solicitud.put("usuarioIds",List.of(estudiante,invalido));crear(solicitud).andExpect(status().isConflict());
        }
        assertThat(consultas.queryForObject("SELECT COUNT(*) FROM avisos",Long.class)).isEqualTo(inicial);
    }
    @Test void exigeCamposYAlMenosUnDestinatario()throws Exception{
        crear(Map.of()).andExpect(status().isUnprocessableEntity());
        var solicitud=datos();solicitud.put("usuarioIds",List.of());crear(solicitud).andExpect(status().isUnprocessableEntity());
        solicitud.put("destinatarios","conservar");crear(solicitud).andExpect(status().isUnprocessableEntity());
        solicitud=datos();solicitud.put("prioridad","urgente");crear(solicitud).andExpect(status().isUnprocessableEntity());
        solicitud=datos();solicitud.put("descripcion","a".repeat(10001));crear(solicitud).andExpect(status().isUnprocessableEntity());
    }
    @Test void editarConservaAutorFechaLecturasYOcultaciones()throws Exception{
        long id=crear();var original=avisos.buscar(id).orElseThrow();
        consultas.update("UPDATE aviso_destinatarios SET leido_en=UTC_TIMESTAMP(6),ocultado_en=UTC_TIMESTAMP(6) WHERE aviso_id=? AND usuario_id=?",id,estudiante);
        consultas.update("UPDATE usuarios SET estado='inactivo' WHERE id=?",estudiante);
        var solicitud=datos();solicitud.put("titulo","Título actualizado");solicitud.put("destinatarios","conservar");solicitud.remove("usuarioIds");
        editar(id,solicitud).andExpect(status().isOk()).andExpect(jsonPath("$.aviso.lecturas").value(1));
        var actual=avisos.buscar(id).orElseThrow();assertThat(actual.autorId()).isEqualTo(original.autorId());assertThat(actual.publicadoEn()).isEqualTo(original.publicadoEn());
        var destinatario=avisos.destinatarios(id).stream().filter(d->d.usuarioId()==estudiante).findFirst().orElseThrow();
        assertThat(destinatario.leidoEn()).isNotNull();assertThat(destinatario.ocultadoEn()).isNotNull();
    }
    @Test void cambiarDestinatariosNoReiniciaLasLecturasConservadas()throws Exception{
        long id=crear();consultas.update("UPDATE aviso_destinatarios SET leido_en=UTC_TIMESTAMP(6) WHERE aviso_id=? AND usuario_id=?",id,estudiante);
        var solicitud=datos();solicitud.put("usuarioIds",List.of(estudiante,ambos));editar(id,solicitud).andExpect(status().isOk());
        var destinatarios=avisos.destinatarios(id);assertThat(destinatarios).extracting(d->d.usuarioId()).containsExactlyInAnyOrder(estudiante,ambos);
        assertThat(destinatarios.stream().filter(d->d.usuarioId()==estudiante).findFirst().orElseThrow().leidoEn()).isNotNull();
        assertThat(destinatarios.stream().filter(d->d.usuarioId()==ambos).findFirst().orElseThrow().leidoEn()).isNull();
    }
    @Test void edicionInvalidaConservaContenidoYDestinatarios()throws Exception{
        long id=crear();var solicitud=datos();solicitud.put("titulo","No guardar");solicitud.put("usuarioIds",List.of(inactivo));
        editar(id,solicitud).andExpect(status().isConflict());assertThat(avisos.buscar(id).orElseThrow().titulo()).isEqualTo("Aviso de prueba");assertThat(avisos.destinatarios(id)).hasSize(2);
    }
    @Test void retirarConservaHistorialYExcluyeVistaVisible()throws Exception{
        long id=crear();
        cliente.perform(patch("/api/coordinacion/avisos/"+id+"/retirar").header("Authorization",autorizacion)).andExpect(status().isOk()).andExpect(jsonPath("$.retiradoEn").isNotEmpty());
        assertThat(avisos.destinatarios(id)).hasSize(2);assertThat(consultas.queryForObject("SELECT COUNT(*) FROM v_avisos_visibles WHERE aviso_id=?",Long.class,id)).isZero();
        editar(id,datos()).andExpect(status().isConflict());
        cliente.perform(patch("/api/coordinacion/avisos/"+id+"/retirar").header("Authorization",autorizacion)).andExpect(status().isConflict());
    }
    @ParameterizedTest @ValueSource(strings={"estudiante","tutor"})
    void otrosRolesNoAccedenALaAdministracion(String rol)throws Exception{
        cliente.perform(get("/api/coordinacion/avisos").header("Authorization",token(rol))).andExpect(status().isForbidden());
        cliente.perform(get("/api/coordinacion/avisos/destinatarios").header("Authorization",token(rol))).andExpect(status().isForbidden());
        cliente.perform(post("/api/coordinacion/avisos").header("Authorization",token(rol)).contentType(MediaType.APPLICATION_JSON).content(conversor.writeValueAsString(datos()))).andExpect(status().isForbidden());
    }
    @Test void exigeSesionAutorActualYRecursosExistentes()throws Exception{
        cliente.perform(get("/api/coordinacion/avisos")).andExpect(status().isUnauthorized());
        cliente.perform(get("/api/coordinacion/avisos/999999999").header("Authorization",autorizacion)).andExpect(status().isNotFound());
        consultas.update("DELETE FROM usuario_roles WHERE usuario_id=?",autor);entidades.clear();crear(datos()).andExpect(status().isForbidden());
    }
    @Test void paginasYFiltrosIncluyenTotalesReales()throws Exception{
        for(int indice=0;indice<7;indice++){
            var solicitud=datos();solicitud.put("titulo","Aviso José "+indice);solicitud.put("prioridad",indice<3?"alta":"normal");crear(solicitud).andExpect(status().isCreated());
        }
        var ids=new java.util.HashSet<Long>();
        for(int pagina=1;pagina<=3;pagina++){
            String respuesta=cliente.perform(get("/api/coordinacion/avisos").header("Authorization",autorizacion).param("categoria","qa-avisos").param("pagina",String.valueOf(pagina)).param("tamano","3").param("busqueda","JOSE"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalRegistros").value(7)).andExpect(jsonPath("$.totalPaginas").value(3)).andReturn().getResponse().getContentAsString();
            var contenido=conversor.readTree(respuesta).get("contenido");assertThat(contenido.size()).isEqualTo(pagina==3?1:3);for(var fila:contenido)assertThat(ids.add(fila.get("id").asLong())).isTrue();
        }
        assertThat(ids).hasSize(7);
        cliente.perform(get("/api/coordinacion/avisos").header("Authorization",autorizacion).param("categoria","qa-avisos").param("prioridad","alta").param("estado","vigente"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalRegistros").value(3));
    }
    @Test void selectorEsPaginadoYNoDuplicaUsuariosConDosRoles()throws Exception{
        String respuesta=cliente.perform(get("/api/coordinacion/avisos/destinatarios").header("Authorization",autorizacion).param("busqueda","avisos-").param("tamano","2"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalRegistros").value(3)).andExpect(jsonPath("$.totalPaginas").value(2)).andReturn().getResponse().getContentAsString();
        assertThat(conversor.readTree(respuesta).get("contenido").size()).isEqualTo(2);
        cliente.perform(get("/api/coordinacion/avisos/destinatarios").header("Authorization",autorizacion).param("busqueda","avisos-").param("rol","tutor"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalRegistros").value(2));
    }
    @Test void filtrosInvalidosYBusquedaLiteral()throws Exception{
        crear();
        for(var parametro:Map.of("pagina","0","tamano","101","prioridad","urgente","estado","otro","busqueda","a".repeat(161)).entrySet())
            cliente.perform(get("/api/coordinacion/avisos").header("Authorization",autorizacion).param(parametro.getKey(),parametro.getValue())).andExpect(status().isUnprocessableEntity());
        for(String texto:List.of("%","_","' OR 1=1 --"))
            cliente.perform(get("/api/coordinacion/avisos").header("Authorization",autorizacion).param("categoria","qa-avisos").param("busqueda",texto))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalRegistros").value(0));
    }
}
