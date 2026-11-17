package com.progweb.backend.seguridad;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import com.progweb.backend.entidades.Usuario;
import com.progweb.backend.repositorios.*;
import com.progweb.backend.respuestas.RespuestaUsuario;
import com.progweb.backend.servicios.ServicioTokens;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class ReportesPruebas {
    @Autowired WebApplicationContext contexto;
    @Autowired FilterChainProxy filtro;
    @Autowired ServicioTokens tokens;
    @Autowired RepositorioUsuarios usuarios;
    @Autowired RepositorioRoles roles;
    @Autowired JdbcTemplate consultas;
    @Autowired ObjectMapper conversor;
    @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager entidades;
    MockMvc cliente;
    long autor,periodo,otroPeriodo,motivo,otroMotivo;
    List<Long> ids=new ArrayList<>();
    String autorizacion;
    long insertar(String sql,Object... datos){consultas.update(sql,datos);return consultas.queryForObject("SELECT LAST_INSERT_ID()",Long.class);}
    long usuario(String nombre,String correo,String rol){var usuario=new Usuario(nombre,correo+"@example.invalid",null);usuario.asignarRol(roles.buscarPorCodigo(rol).orElseThrow());usuarios.saveAndFlush(usuario);return usuario.obtenerId();}
    String token(String rol){return "Bearer "+tokens.crearToken(new RespuestaUsuario(autor,"Prueba","prueba@example.invalid",null,List.of(rol))).token();}
    @BeforeEach void preparar(){
        cliente=MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtro).build();
        autor=usuario("Coordinación","reportes-autor","coordinador");autorizacion=token("coordinador");
        long usuarioEstudiante=usuario("José Pérez","reportes-estudiante","estudiante");long usuarioTutor=usuario("Tutora Ángela","reportes-tutor","tutor");
        long departamento=insertar("INSERT INTO departamentos(nombre) VALUES (?)","Departamento de reportes de prueba");
        long carrera=insertar("INSERT INTO carreras(departamento_id,clave,nombre) VALUES (?,?,?)",departamento,"REP-PRUEBA","Carrera académica de prueba");
        long estudiante=insertar("INSERT INTO estudiantes(usuario_id,matricula) VALUES (?,?)",usuarioEstudiante,"REP-PRUEBA");
        long tutor=insertar("INSERT INTO tutores(usuario_id,departamento_id) VALUES (?,?)",usuarioTutor,departamento);
        periodo=insertar("INSERT INTO periodos_academicos(clave,nombre,fecha_inicio,fecha_fin,estado) VALUES (?,?,?,?,?)","REP-PRUEBA","Periodo de reportes","2026-01-01","2026-12-31","activo");
        otroPeriodo=insertar("INSERT INTO periodos_academicos(clave,nombre,fecha_inicio,fecha_fin,estado) VALUES (?,?,?,?,?)","REP-OTRO","Otro periodo de reportes","2025-01-01","2025-12-31","cerrado");
        insertar("INSERT INTO situaciones_academicas(estudiante_id,periodo_id,carrera_id,semestre) VALUES (?,?,?,3)",estudiante,periodo,carrera);
        motivo=insertar("INSERT INTO motivos_reporte(codigo,nombre) VALUES (?,?)","rep_prueba_a","Motivo académico de prueba");
        otroMotivo=insertar("INSERT INTO motivos_reporte(codigo,nombre) VALUES (?,?)","rep_prueba_b","Otro motivo de prueba");
        for(int indice=0;indice<8;indice++){
            boolean completado=indice%2==0;
            String contenido=indice==0?("Observación académica de José: orientación y seguimiento. ".repeat(6)+"\n").repeat(35):"Contenido de reporte "+indice;
            long id=insertar("INSERT INTO reportes(estudiante_id,tutor_id,periodo_id,motivo_id,fecha,contenido,estado,completado_en,completado_por) VALUES (?,?,?,?,?,?,?,?,?)",
                estudiante,tutor,periodo,indice<5?motivo:otroMotivo,"2026-01-"+String.format("%02d",indice+1),contenido,
                completado?"completado":"pendiente",completado?"2026-02-01 10:00:00":null,completado?autor:null);
            ids.add(id);
        }
    }
    @Test void paginaSinDuplicarFilasYCalculaResumenCompleto()throws Exception{
        var vistos=new java.util.HashSet<Long>();
        for(int pagina=1;pagina<=3;pagina++){
            String respuesta=cliente.perform(get("/api/coordinacion/reportes").header("Authorization",autorizacion).param("periodoId",String.valueOf(periodo)).param("pagina",String.valueOf(pagina)).param("tamano","3"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalRegistros").value(8)).andExpect(jsonPath("$.totalPaginas").value(3))
                .andExpect(jsonPath("$.resumen.total").value(8)).andExpect(jsonPath("$.resumen.completados").value(4)).andExpect(jsonPath("$.resumen.pendientes").value(4))
                .andExpect(jsonPath("$.motivos[0].cantidad").value(5)).andExpect(jsonPath("$.motivos[0].porcentaje").value(62.5))
                .andReturn().getResponse().getContentAsString();
            var contenido=conversor.readTree(respuesta).get("contenido");assertThat(contenido.size()).isEqualTo(pagina==3?2:3);for(var fila:contenido)assertThat(vistos.add(fila.get("id").asLong())).isTrue();
        }
        assertThat(vistos).hasSize(8);
    }
    @Test void filtrosAfectanTablaResumenYGrafica()throws Exception{
        cliente.perform(get("/api/coordinacion/reportes").header("Authorization",autorizacion).param("periodoId",String.valueOf(periodo)).param("estado","completado").param("motivoId",String.valueOf(motivo)).param("busqueda","JOSE").param("desde","2026-01-03").param("hasta","2026-01-05"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalRegistros").value(2)).andExpect(jsonPath("$.resumen.completados").value(2)).andExpect(jsonPath("$.resumen.pendientes").value(0))
            .andExpect(jsonPath("$.motivos.length()").value(1)).andExpect(jsonPath("$.motivos[0].porcentaje").value(100.0));
    }
    @Test void noConfundeExpedientesDeOtroPeriodo()throws Exception{
        cliente.perform(get("/api/coordinacion/reportes").header("Authorization",autorizacion).param("periodoId",String.valueOf(otroPeriodo)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalRegistros").value(0)).andExpect(jsonPath("$.resumen.total").value(0)).andExpect(jsonPath("$.motivos").isEmpty());
    }
    @Test void validaParametrosConBeanValidationYFechasEnServicio()throws Exception{
        for(var parametro:Map.of("pagina","0","tamano","101","estado","otro","periodoId","-1","motivoId","0","busqueda","a".repeat(161)).entrySet())
            cliente.perform(get("/api/coordinacion/reportes").header("Authorization",autorizacion).param(parametro.getKey(),parametro.getValue())).andExpect(status().isUnprocessableEntity());
        cliente.perform(get("/api/coordinacion/reportes").header("Authorization",autorizacion).param("desde","2026-02-01").param("hasta","2026-01-01"))
            .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.erroresPorCampo.hasta").isArray());
    }
    @ParameterizedTest @ValueSource(strings={"%","_","' OR 1=1 --"})
    void busquedaUsaTextoLiteral(String busqueda)throws Exception{
        cliente.perform(get("/api/coordinacion/reportes").header("Authorization",autorizacion).param("periodoId",String.valueOf(periodo)).param("busqueda",busqueda))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalRegistros").value(0));
    }
    @Test void exportacionFiltradaIncluyeTodasLasPaginasSinCambiarEstado()throws Exception{
        byte[] archivo=cliente.perform(get("/api/coordinacion/reportes/exportar/pdf").header("Authorization",autorizacion).param("periodoId",String.valueOf(periodo)).param("estado","pendiente").param("tamano","1"))
            .andExpect(status().isOk()).andExpect(content().contentType("application/pdf")).andExpect(header().string("Cache-Control","no-store"))
            .andReturn().getResponse().getContentAsByteArray();
        try(var documento=Loader.loadPDF(archivo)){
            String texto=new PDFTextStripper().getText(documento);
            assertThat(texto).contains("Reportes incluidos: 4","José Pérez","Tutora Ángela");
            for(int indice=0;indice<ids.size();indice++){
                if(indice%2==1)assertThat(texto).contains("Reporte #"+ids.get(indice));else assertThat(texto).doesNotContain("Reporte #"+ids.get(indice)+" -");
            }
        }
        assertThat(consultas.queryForObject("SELECT COUNT(*) FROM reportes WHERE periodo_id=? AND estado='pendiente'",Long.class,periodo)).isEqualTo(4);
    }
    @Test void pdfIndividualEsLegibleYContinuaContenidoLargo()throws Exception{
        byte[] archivo=cliente.perform(get("/api/coordinacion/reportes/"+ids.getFirst()+"/pdf").header("Authorization",autorizacion))
            .andExpect(status().isOk()).andExpect(content().contentType("application/pdf")).andReturn().getResponse().getContentAsByteArray();
        try(var documento=Loader.loadPDF(archivo)){
            assertThat(documento.getNumberOfPages()).isGreaterThan(1);
            String texto=new PDFTextStripper().getText(documento);
            assertThat(texto).contains("Observación académica de José","Reportes incluidos: 1","Página 1 de","(continuación)");
        }
    }
    @ParameterizedTest @ValueSource(strings={"estudiante","tutor"})
    void otrosRolesNoPuedenConsultarNiDescargar(String rol)throws Exception{
        for(String ruta:List.of("","/motivos","/exportar/pdf","/"+ids.getFirst()+"/pdf"))
            cliente.perform(get("/api/coordinacion/reportes"+ruta).header("Authorization",token(rol))).andExpect(status().isForbidden());
    }
    @Test void erroresDeDescargaRespetanSesionYRecursos()throws Exception{
        cliente.perform(get("/api/coordinacion/reportes/exportar/pdf")).andExpect(status().isUnauthorized());
        cliente.perform(get("/api/coordinacion/reportes/999999999/pdf").header("Authorization",autorizacion)).andExpect(status().isNotFound());
        consultas.update("DELETE FROM usuario_roles WHERE usuario_id=?",autor);entidades.clear();
        cliente.perform(get("/api/coordinacion/reportes/exportar/pdf").header("Authorization",autorizacion)).andExpect(status().isForbidden());
    }
    @Test void exportacionSinResultadosNoFalla()throws Exception{
        byte[] archivo=cliente.perform(get("/api/coordinacion/reportes/exportar/pdf").header("Authorization",autorizacion).param("periodoId",String.valueOf(otroPeriodo)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try(var documento=Loader.loadPDF(archivo)){assertThat(new PDFTextStripper().getText(documento)).contains("No hay reportes que coincidan");}
    }
}
