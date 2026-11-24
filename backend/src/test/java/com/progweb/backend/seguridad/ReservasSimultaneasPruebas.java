package com.progweb.backend.seguridad;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import com.progweb.backend.errores.AccesoRechazado;
import com.progweb.backend.servicios.ServicioSesionesAgenda;
import com.progweb.backend.solicitudes.SolicitudReservarSesion;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class ReservasSimultaneasPruebas {
    @Autowired JdbcTemplate consultas;
    @Autowired ServicioSesionesAgenda servicio;
    @Autowired com.progweb.backend.servicios.ServicioMensajesTutor mensajes;
    final List<Long> usuarios=new ArrayList<>(),estudiantes=new ArrayList<>();
    Long departamento,carrera,periodo,tutor,horario;
    final String marca=UUID.randomUUID().toString().substring(0,8);
    long insertar(String sql,Object... datos) {
        var llave=new org.springframework.jdbc.support.GeneratedKeyHolder();
        consultas.update(conexion->{var sentencia=conexion.prepareStatement(sql,java.sql.Statement.RETURN_GENERATED_KEYS);
            for(int indice=0;indice<datos.length;indice++)sentencia.setObject(indice+1,datos[indice]);return sentencia;},llave);
        return llave.getKey().longValue();
    }
    long usuario(String rol) {
        long id=insertar("INSERT INTO usuarios(nombre,correo) VALUES (?,?)","Reserva simultánea prueba","reserva-"+usuarios.size()+"-"+marca+"@example.invalid");usuarios.add(id);
        consultas.update("INSERT INTO usuario_roles(usuario_id,rol_id) SELECT ?,id FROM roles WHERE codigo=?",id,rol);return id;
    }
    @BeforeEach void preparar() {
        departamento=insertar("INSERT INTO departamentos(nombre) VALUES (?)","Reservas simultáneas "+marca);
        carrera=insertar("INSERT INTO carreras(departamento_id,clave,nombre) VALUES (?,?,?)",departamento,marca,"Carrera prueba simultánea");
        periodo=insertar("INSERT INTO periodos_academicos(clave,nombre,fecha_inicio,fecha_fin,estado) VALUES (?,?,?,?,?)",marca,"Periodo prueba simultánea","2000-01-01","2099-12-31","activo");
        long profesor=usuario("tutor");tutor=insertar("INSERT INTO tutores(usuario_id,departamento_id) VALUES (?,?)",profesor,departamento);
        for(int indice=0;indice<2;indice++) {
            long alumno=usuario("estudiante");long estudiante=insertar("INSERT INTO estudiantes(usuario_id,matricula) VALUES (?,?)",alumno,"SIM-"+indice+"-"+marca);estudiantes.add(estudiante);
            insertar("INSERT INTO situaciones_academicas(estudiante_id,periodo_id,carrera_id,semestre) VALUES (?,?,?,3)",estudiante,periodo,carrera);
            insertar("INSERT INTO asignaciones_tutor(estudiante_id,tutor_id,periodo_id,asignado_por,inicio_en,estado) VALUES (?,?,?,?,UTC_TIMESTAMP(6),'activa')",estudiante,tutor,periodo,alumno);
        }
        var inicio=LocalDateTime.now(ZoneOffset.UTC).plusDays(2);
        horario=insertar("INSERT INTO disponibilidades_tutor(tutor_id,inicio_en,fin_en,modalidad,ubicacion) VALUES (?,?,?,'presencial','Aula prueba')",tutor,inicio,inicio.plusHours(1));
    }
    @Test void dosSolicitudesCompitenPorUnSoloHorario() throws Exception {
        var preparados=new CountDownLatch(2);var salida=new CountDownLatch(1);
        try(var ejecutor=Executors.newFixedThreadPool(2)) {
            List<Future<Integer>> resultados=new ArrayList<>();
            for(long alumno:usuarios.subList(1,3))resultados.add(ejecutor.submit(()->{
                preparados.countDown();if(!salida.await(10,TimeUnit.SECONDS))throw new IllegalStateException("No comenzó la prueba.");
                try {servicio.solicitar(String.valueOf(alumno),new SolicitudReservarSesion(horario,periodo,"Reserva simultánea",null));return 201;}
                catch(AccesoRechazado fallo){return fallo.obtenerEstado();}
            }));
            assertThat(preparados.await(10,TimeUnit.SECONDS)).isTrue();salida.countDown();
            assertThat(List.of(resultados.get(0).get(20,TimeUnit.SECONDS),resultados.get(1).get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(201,409);
        }
        assertThat(consultas.queryForObject("SELECT COUNT(*) FROM sesiones WHERE disponibilidad_id=? AND estado='solicitada'",Long.class,horario)).isEqualTo(1);
    }
    @Test void ambasPersonasVenLaConversacionEnLaPrimeraConsultaSimultanea() throws Exception {
        for(int repeticion=0;repeticion<5;repeticion++) {
            limpiarConversaciones();
            var preparados=new CountDownLatch(2);var salida=new CountDownLatch(1);
            try(var ejecutor=Executors.newFixedThreadPool(2)) {
                var delTutor=ejecutor.submit(()->{preparados.countDown();if(!salida.await(10,TimeUnit.SECONDS))throw new IllegalStateException();return mensajes.conversaciones(String.valueOf(usuarios.get(0)),true,"");});
                var delEstudiante=ejecutor.submit(()->{preparados.countDown();if(!salida.await(10,TimeUnit.SECONDS))throw new IllegalStateException();return mensajes.conversaciones(String.valueOf(usuarios.get(1)),false,"");});
                assertThat(preparados.await(10,TimeUnit.SECONDS)).isTrue();salida.countDown();
                var tutorados=delTutor.get(20,TimeUnit.SECONDS);var estudiante=delEstudiante.get(20,TimeUnit.SECONDS);
                assertThat(estudiante).hasSize(1);
                assertThat(tutorados).extracting(fila->fila.id()).contains(estudiante.getFirst().id());
                assertThat(consultas.queryForObject("SELECT COUNT(*) FROM conversaciones WHERE usuario_menor_id=? AND usuario_mayor_id=?",Long.class,
                        Math.min(usuarios.get(0),usuarios.get(1)),Math.max(usuarios.get(0),usuarios.get(1)))).isEqualTo(1);
            }
        }
    }
    void limpiarConversaciones() {
        for(long usuario:usuarios) for(long conversacion:consultas.queryForList("SELECT conversacion_id FROM conversacion_participantes WHERE usuario_id=?",Long.class,usuario)) {
            consultas.update("DELETE FROM mensajes WHERE conversacion_id=?",conversacion);
            consultas.update("DELETE FROM conversacion_participantes WHERE conversacion_id=?",conversacion);
            consultas.update("DELETE FROM conversaciones WHERE id=?",conversacion);
        }
    }
    @AfterEach void limpiar() {
        limpiarConversaciones();
        // Solo retiramos los registros que creó esta prueba, después de terminar ambas solicitudes.
        if(horario!=null){consultas.update("DELETE c FROM sesion_cambios c JOIN sesiones s ON s.id=c.sesion_id WHERE s.disponibilidad_id=?",horario);consultas.update("DELETE FROM sesiones WHERE disponibilidad_id=?",horario);consultas.update("DELETE FROM disponibilidades_tutor WHERE id=?",horario);}
        for(long estudiante:estudiantes){consultas.update("DELETE FROM asignaciones_tutor WHERE estudiante_id=?",estudiante);consultas.update("DELETE FROM situaciones_academicas WHERE estudiante_id=?",estudiante);consultas.update("DELETE FROM estudiantes WHERE id=?",estudiante);}
        if(tutor!=null)consultas.update("DELETE FROM tutores WHERE id=?",tutor);
        for(long usuario:usuarios){consultas.update("DELETE FROM usuario_roles WHERE usuario_id=?",usuario);consultas.update("DELETE FROM usuarios WHERE id=?",usuario);}
        if(carrera!=null)consultas.update("DELETE FROM carreras WHERE id=?",carrera);
        if(departamento!=null)consultas.update("DELETE FROM departamentos WHERE id=?",departamento);
        if(periodo!=null)consultas.update("DELETE FROM periodos_academicos WHERE id=?",periodo);
    }
}
