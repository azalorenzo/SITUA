package com.progweb.backend.seguridad;

import java.util.*;
import com.progweb.backend.entidades.Usuario;
import com.progweb.backend.repositorios.*;
import com.progweb.backend.respuestas.RespuestaUsuario;
import com.progweb.backend.servicios.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"mensajeria.sms-habilitado=false"})
@Transactional
class PerfilUsuarioPruebas {
    @Autowired WebApplicationContext contexto;
    @Autowired FilterChainProxy filtro;
    @Autowired RepositorioUsuarios usuarios;
    @Autowired RepositorioRoles roles;
    @Autowired ServicioTokens tokens;
    @Autowired JdbcTemplate consultas;
    @Autowired ObjectMapper json;
    @Autowired ServicioPerfilUsuario servicio;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder codificador;
    @Autowired jakarta.persistence.EntityManager entidades;
    MockMvc cliente;
    long alumno,tutor,coordinador,ajeno,estudiante,otroEstudiante,departamento,otroDepartamento,carrera,otraCarrera,periodo,actual,historico;
    long insertar(String sql,Object... datos){consultas.update(sql,datos);return consultas.queryForObject("SELECT LAST_INSERT_ID()",Long.class);}
    long cuenta(String nombre,String rol){var cuenta=new Usuario(nombre,"perfil-"+nombre+"@example.invalid",null);cuenta.asignarRol(roles.buscarPorCodigo(rol).orElseThrow());return usuarios.saveAndFlush(cuenta).obtenerId();}
    String token(long usuario,String rol){return "Bearer "+tokens.crearToken(new RespuestaUsuario(usuario,"Prueba","prueba@example.invalid",null,List.of(rol))).token();}
    Map<String,Object> datos(String rol){
        var datos=new HashMap<String,Object>();datos.put("nombre","Nombre actualizado");datos.put("telefono","+525512345678");datos.put("aceptaSms",false);
        if(rol.equals("estudiante")){datos.put("matricula","PERFIL-NUEVA");datos.put("carreraId",otraCarrera);datos.put("periodoId",periodo);datos.put("semestre",5);}
        if(rol.equals("tutor"))datos.put("departamentoId",otroDepartamento);
        return datos;
    }
    ResultActions guardar(long usuario,String rol,Map<String,Object> datos)throws Exception {
        datos.putIfAbsent("correo",consultas.queryForObject("SELECT correo FROM usuarios WHERE id=?",String.class,usuario));
        return cliente.perform(put("/api/perfil").header("Authorization",token(usuario,rol)).contentType("application/json").content(json.writeValueAsString(datos)));
    }
    @BeforeEach void preparar(){
        cliente=MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtro).build();
        alumno=cuenta("alumno","estudiante");tutor=cuenta("tutor","tutor");coordinador=cuenta("coordinador","coordinador");ajeno=cuenta("ajeno","estudiante");
        departamento=insertar("INSERT INTO departamentos(nombre) VALUES ('Departamento perfil uno')");
        otroDepartamento=insertar("INSERT INTO departamentos(nombre) VALUES ('Departamento perfil dos')");
        carrera=insertar("INSERT INTO carreras(departamento_id,clave,nombre) VALUES (?,'PERFIL-UNO','Carrera perfil uno')",departamento);
        otraCarrera=insertar("INSERT INTO carreras(departamento_id,clave,nombre) VALUES (?,'PERFIL-DOS','Carrera perfil dos')",otroDepartamento);
        insertar("INSERT INTO tutores(usuario_id,departamento_id) VALUES (?,?)",tutor,departamento);
        estudiante=insertar("INSERT INTO estudiantes(usuario_id,matricula) VALUES (?,'PERFIL-ALUMNO')",alumno);
        otroEstudiante=insertar("INSERT INTO estudiantes(usuario_id,matricula) VALUES (?,'PERFIL-AJENO')",ajeno);
        long anterior=insertar("INSERT INTO periodos_academicos(clave,nombre,fecha_inicio,fecha_fin,estado) VALUES ('PERFIL-ANTERIOR','Periodo anterior perfil','2020-01-01','2020-12-31','cerrado')");
        periodo=insertar("INSERT INTO periodos_academicos(clave,nombre,fecha_inicio,fecha_fin,estado) VALUES ('PERFIL-ACTUAL','Periodo actual perfil','2030-01-01','2030-12-31','activo')");
        historico=insertar("INSERT INTO situaciones_academicas(estudiante_id,periodo_id,carrera_id,semestre) VALUES (?,?,?,2)",estudiante,anterior,carrera);
        actual=insertar("INSERT INTO situaciones_academicas(estudiante_id,periodo_id,carrera_id,semestre) VALUES (?,?,?,4)",estudiante,periodo,carrera);
    }
    @Test void consultaSoloElPerfilPropioSinSecretos()throws Exception{
        cliente.perform(get("/api/perfil").header("Authorization",token(alumno,"estudiante")))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.usuario.id").value(alumno)).andExpect(jsonPath("$.rol").value("estudiante"))
            .andExpect(jsonPath("$.matricula").value("PERFIL-ALUMNO")).andExpect(jsonPath("$.semestre").value(4))
            .andExpect(jsonPath("$.puedeEditarAcademico").value(true)).andExpect(jsonPath("$.usuario.contrasena").doesNotExist()).andExpect(jsonPath("$.departamentos.length()").value(0));
    }
    @Test void estudianteActualizaSusDatosYSoloElExpedienteVigente()throws Exception{
        var datos=datos("estudiante");datos.put("aceptaSms",true);
        guardar(alumno,"estudiante",datos).andExpect(status().isOk()).andExpect(jsonPath("$.usuario.nombre").value("Nombre actualizado"))
            .andExpect(jsonPath("$.matricula").value("PERFIL-NUEVA")).andExpect(jsonPath("$.semestre").value(5)).andExpect(jsonPath("$.aceptaSms").value(true));
        assertThat(consultas.queryForObject("SELECT carrera_id FROM situaciones_academicas WHERE id=?",Long.class,actual)).isEqualTo(otraCarrera);
        assertThat(consultas.queryForObject("SELECT semestre FROM situaciones_academicas WHERE id=?",Integer.class,historico)).isEqualTo(2);
        assertThat(consultas.queryForObject("SELECT nombre FROM usuarios WHERE id=?",String.class,ajeno)).isEqualTo("ajeno");
        assertThat(consultas.queryForObject("SELECT preferencias_contacto_en IS NOT NULL FROM usuarios WHERE id=?",Boolean.class,alumno)).isTrue();
    }
    @Test void tutorEditaDepartamentoYDatosPersonales()throws Exception{
        guardar(tutor,"tutor",datos("tutor")).andExpect(status().isOk()).andExpect(jsonPath("$.departamentoId").value(otroDepartamento));
        assertThat(consultas.queryForObject("SELECT departamento_id FROM tutores WHERE usuario_id=?",Long.class,tutor)).isEqualTo(otroDepartamento);
    }
    @Test void coordinacionEditaNombreYTelefonoSinCambiarSuRol()throws Exception{
        guardar(coordinador,"coordinador",datos("coordinador")).andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("coordinador"));
        assertThat(consultas.queryForObject("SELECT telefono FROM usuarios WHERE id=?",String.class,coordinador)).isEqualTo("+525512345678");
    }
    @Test void impideMatriculaDuplicadaSinGuardarOtrosCambios()throws Exception{
        var datos=datos("estudiante");datos.put("matricula","PERFIL-AJENO");
        guardar(alumno,"estudiante",datos).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.erroresPorCampo.matricula").exists());
        assertThat(consultas.queryForObject("SELECT nombre FROM usuarios WHERE id=?",String.class,alumno)).isEqualTo("alumno");
        assertThat(consultas.queryForObject("SELECT semestre FROM situaciones_academicas WHERE id=?",Integer.class,actual)).isEqualTo(4);
    }
    @Test void impideAvisosSinTelefono()throws Exception{
        var datos=datos("estudiante");datos.put("telefono","");datos.put("aceptaSms",true);
        guardar(alumno,"estudiante",datos).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.erroresPorCampo.telefono").exists());
    }
    @Test void noPermiteCambiarDatosAcademicosDePeriodoCerrado()throws Exception{
        consultas.update("UPDATE periodos_academicos SET estado='cerrado' WHERE id=?",periodo);
        assertThat(servicio.consultar(Long.toString(alumno)).puedeEditarAcademico()).isFalse();
        guardar(alumno,"estudiante",datos("estudiante")).andExpect(status().isUnprocessableEntity());
        var datos=datos("estudiante");datos.put("carreraId",carrera);datos.put("semestre",4);
        guardar(alumno,"estudiante",datos).andExpect(status().isOk());
    }
    @Test void detectaCambioDePeriodoMientrasSeEditaba()throws Exception{
        var datos=datos("estudiante");datos.put("periodoId",periodo+10000);
        guardar(alumno,"estudiante",datos).andExpect(status().isConflict());
    }
    @Test void rechazaCarreraInactivaYDepartamentoInactivo()throws Exception{
        consultas.update("UPDATE carreras SET activa=false WHERE id=?",otraCarrera);
        guardar(alumno,"estudiante",datos("estudiante")).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.erroresPorCampo.carreraId").exists());
        consultas.update("UPDATE departamentos SET activo=false WHERE id=?",otroDepartamento);
        guardar(tutor,"tutor",datos("tutor")).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.erroresPorCampo.departamentoId").exists());
    }
    @Test void permiteConservarCarreraODepartamentoAnteriorAunqueSeDesactive()throws Exception{
        consultas.update("UPDATE departamentos SET activo=false WHERE id=?",departamento);
        var datos=datos("estudiante");datos.put("carreraId",carrera);guardar(alumno,"estudiante",datos).andExpect(status().isOk());
        var datosTutor=datos("tutor");datosTutor.put("departamentoId",departamento);guardar(tutor,"tutor",datosTutor).andExpect(status().isOk());
    }
    @Test void rechazaCamposDeOtroRol()throws Exception{
        guardar(coordinador,"coordinador",datos("tutor")).andExpect(status().isUnprocessableEntity());
        var datos=datos("estudiante");datos.put("departamentoId",departamento);
        guardar(alumno,"estudiante",datos).andExpect(status().isUnprocessableEntity());
    }
    @Test void idsRolesYEstadoDelCuerpoNoSePuedenModificar()throws Exception{
        var datos=datos("coordinador");datos.put("id",ajeno);datos.put("roles",List.of("estudiante"));datos.put("estado","inactivo");
        guardar(coordinador,"coordinador",datos).andExpect(status().isOk());
        assertThat(consultas.queryForObject("SELECT correo FROM usuarios WHERE id=?",String.class,coordinador)).isEqualTo("perfil-coordinador@example.invalid");
        assertThat(consultas.queryForObject("SELECT estado FROM usuarios WHERE id=?",String.class,coordinador)).isEqualTo("activo");
        assertThat(consultas.queryForObject("SELECT nombre FROM usuarios WHERE id=?",String.class,ajeno)).isEqualTo("ajeno");
        assertThat(servicio.consultar(Long.toString(coordinador)).rol()).isEqualTo("coordinador");
    }
    @Test void sinExpedienteSoloEditaDatosPersonalesYMatricula()throws Exception{
        var datos=datos("coordinador");datos.put("matricula","PERFIL-AJENO");
        guardar(ajeno,"estudiante",datos).andExpect(status().isOk()).andExpect(jsonPath("$.puedeEditarAcademico").value(false));
        assertThat(consultas.queryForObject("SELECT COUNT(*) FROM situaciones_academicas WHERE estudiante_id=?",Long.class,otroEstudiante)).isZero();
    }
    @Test void protegeLaSesionYValidaEntradas()throws Exception{
        cliente.perform(get("/api/perfil")).andExpect(status().isUnauthorized());
        var datos=datos("coordinador");datos.put("nombre","   ");datos.put("telefono","5512345678");
        guardar(coordinador,"coordinador",datos).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.erroresPorCampo.nombre").exists()).andExpect(jsonPath("$.erroresPorCampo.telefono").exists());
    }
    @Test void cuentaInactivaNoPuedeEditar()throws Exception{
        var usuario=usuarios.findById(alumno).orElseThrow();usuario.desactivar();usuarios.flush();
        guardar(alumno,"estudiante",datos("estudiante")).andExpect(status().isForbidden());
    }
    @Test void cuentaNormalCambiaCorreoYConservaSuContrasenaParaIniciarSesion()throws Exception{
        consultas.update("UPDATE usuarios SET password_hash=? WHERE id=?",codificador.encode("PerfilPrueba!2026"),coordinador);entidades.clear();
        var datos=datos("coordinador");datos.put("correo","  Perfil.Nuevo@Gmail.com  ");
        guardar(coordinador,"coordinador",datos).andExpect(status().isOk()).andExpect(jsonPath("$.usuario.correo").value("perfil.nuevo@gmail.com"))
            .andExpect(jsonPath("$.puedeEditarCorreo").value(true));
        entidades.clear();
        cliente.perform(post("/api/autenticacion/iniciar-sesion").contentType("application/json").content(json.writeValueAsString(Map.of("correo","perfil.nuevo@gmail.com","contrasena","PerfilPrueba!2026"))))
            .andExpect(status().isOk());
        cliente.perform(post("/api/autenticacion/iniciar-sesion").contentType("application/json").content(json.writeValueAsString(Map.of("correo","perfil-coordinador@example.invalid","contrasena","PerfilPrueba!2026"))))
            .andExpect(status().isUnauthorized());
    }
    @Test void correoDuplicadoNoGuardaOtrosDatos()throws Exception{
        var datos=datos("estudiante");datos.put("correo","  PERFIL-AJENO@example.invalid ");
        guardar(alumno,"estudiante",datos).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.erroresPorCampo.correo").exists());
        assertThat(consultas.queryForObject("SELECT nombre FROM usuarios WHERE id=?",String.class,alumno)).isEqualTo("alumno");
        assertThat(consultas.queryForObject("SELECT semestre FROM situaciones_academicas WHERE id=?",Integer.class,actual)).isEqualTo(4);
    }
    @Test void vinculoConGoogleBloqueaCorreoEnLosTresRolesAunqueTenganContrasena()throws Exception{
        for(var entrada:Map.of(alumno,"estudiante",tutor,"tutor",coordinador,"coordinador").entrySet()) {
            long usuario=entrada.getKey();String rol=entrada.getValue();
            consultas.update("UPDATE usuarios SET password_hash=? WHERE id=?","a".repeat(60),usuario);
            insertar("INSERT INTO identidades_externas(usuario_id,proveedor,identificador_externo) VALUES (?,'google',?)",usuario,"perfil-google-"+usuario);
            String correo=consultas.queryForObject("SELECT correo FROM usuarios WHERE id=?",String.class,usuario);
            assertThat(servicio.consultar(Long.toString(usuario)).puedeEditarCorreo()).isFalse();
            var datos=datos(rol);datos.put("correo","cambiado-"+usuario+"@example.invalid");
            guardar(usuario,rol,datos).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.erroresPorCampo.correo").exists());
            assertThat(consultas.queryForObject("SELECT correo FROM usuarios WHERE id=?",String.class,usuario)).isEqualTo(correo);
            datos.put("correo",correo);guardar(usuario,rol,datos).andExpect(status().isOk()).andExpect(jsonPath("$.puedeEditarCorreo").value(false));
        }
    }
    @Test void gmailSinVinculoConGooglePermiteEditarSuCorreo()throws Exception{
        consultas.update("UPDATE usuarios SET correo='perfil.local@gmail.com' WHERE id=?",coordinador);
        assertThat(servicio.consultar(Long.toString(coordinador)).puedeEditarCorreo()).isTrue();
        var datos=datos("coordinador");datos.put("correo","otra.cuenta@gmail.com");
        guardar(coordinador,"coordinador",datos).andExpect(status().isOk()).andExpect(jsonPath("$.usuario.correo").value("otra.cuenta@gmail.com"));
    }
    @Test void cambiarElCorreoInvalidaCodigosDeRecuperacionAnteriores()throws Exception{
        consultas.update("INSERT INTO recuperaciones_contrasena(usuario_id,codigo_hash,solicitado_en,vence_en,intentos,utilizado) VALUES (?,?,UTC_TIMESTAMP(),UTC_TIMESTAMP()+INTERVAL 10 MINUTE,0,false)",coordinador,"a".repeat(60));
        var datos=datos("coordinador");datos.put("correo","perfil.recuperacion@example.invalid");guardar(coordinador,"coordinador",datos).andExpect(status().isOk());
        assertThat(consultas.queryForObject("SELECT COUNT(*) FROM recuperaciones_contrasena WHERE usuario_id=?",Long.class,coordinador)).isZero();
    }
    @Test void conservarElCorreoNoEliminaCodigosDeRecuperacion()throws Exception{
        consultas.update("INSERT INTO recuperaciones_contrasena(usuario_id,codigo_hash,solicitado_en,vence_en,intentos,utilizado) VALUES (?,?,UTC_TIMESTAMP(),UTC_TIMESTAMP()+INTERVAL 10 MINUTE,0,false)",coordinador,"a".repeat(60));
        guardar(coordinador,"coordinador",datos("coordinador")).andExpect(status().isOk());
        assertThat(consultas.queryForObject("SELECT COUNT(*) FROM recuperaciones_contrasena WHERE usuario_id=?",Long.class,coordinador)).isEqualTo(1);
    }
    @Test void validaElFormatoDelCorreo()throws Exception{
        var datos=datos("coordinador");datos.put("correo","correo-invalido");
        guardar(coordinador,"coordinador",datos).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.erroresPorCampo.correo").exists());
    }
}
