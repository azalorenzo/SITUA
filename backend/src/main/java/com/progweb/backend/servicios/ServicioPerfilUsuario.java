package com.progweb.backend.servicios;

import java.util.*;
import com.progweb.backend.errores.*;
import com.progweb.backend.respuestas.*;
import com.progweb.backend.respuestas.RespuestaPerfilUsuario.Opcion;
import com.progweb.backend.solicitudes.SolicitudEditarPerfil;
import com.progweb.backend.solicitudes.SolicitudContactoEstudiante;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@Transactional(readOnly=true)
public class ServicioPerfilUsuario {
    private final JdbcTemplate consultas;
    private final ServicioAutenticacion autenticacion;
    private final ServicioContactoEstudiante contacto;
    private record Academico(long id,long carrera,long periodo,String nombrePeriodo,int semestre,boolean editable) {}
    public ServicioPerfilUsuario(JdbcTemplate consultas,ServicioAutenticacion autenticacion,ServicioContactoEstudiante contacto){this.consultas=consultas;this.autenticacion=autenticacion;this.contacto=contacto;}
    private String rol(RespuestaUsuario usuario) {
        if(usuario.roles().contains("coordinador"))return "coordinador";
        if(usuario.roles().contains("tutor"))return "tutor";
        if(usuario.roles().contains("estudiante"))return "estudiante";
        throw new AccesoRechazado(403,"No tienes acceso a esta opción.");
    }
    private long perfil(String tabla,long usuario,boolean bloqueo) {
        var ids=consultas.queryForList("SELECT id FROM "+tabla+" WHERE usuario_id=?"+(bloqueo?" FOR UPDATE":""),Long.class,usuario);
        if(ids.isEmpty())throw new AccesoRechazado(404,"No se encontró tu perfil.");
        return ids.getFirst();
    }
    private boolean vinculadoConGoogle(long usuario) {
        return consultas.queryForObject("SELECT COUNT(*) FROM identidades_externas WHERE usuario_id=? AND proveedor='google'",Long.class,usuario)>0;
    }
    private Academico academico(long estudiante,boolean bloqueo) {
        var filas=consultas.query("""
            SELECT s.id,s.carrera_id,s.periodo_id,p.nombre,s.semestre,
                (p.estado='activo' AND s.estado_inscripcion='activo') editable
            FROM situaciones_academicas s JOIN periodos_academicos p ON p.id=s.periodo_id
            WHERE s.estudiante_id=?
            ORDER BY editable DESC,p.fecha_inicio DESC,p.id DESC LIMIT 1
            """+(bloqueo?" FOR UPDATE":""),(fila,posicion)->new Academico(fila.getLong(1),fila.getLong(2),fila.getLong(3),fila.getString(4),fila.getInt(5),fila.getBoolean(6)),estudiante);
        return filas.isEmpty()?null:filas.getFirst();
    }
    private RespuestaPerfilUsuario datos(RespuestaUsuario identidad) {
        long usuario=identidad.id();String rol=rol(identidad);
        String matricula=null;Long departamento=null;Academico academico=null;
        List<Opcion> carreras=List.of(),departamentos=List.of();
        if(rol.equals("estudiante")) {
            long estudiante=perfil("estudiantes",usuario,false);
            matricula=consultas.queryForObject("SELECT matricula FROM estudiantes WHERE id=?",String.class,estudiante);
            academico=academico(estudiante,false);
            carreras=consultas.query("SELECT c.id,c.nombre FROM carreras c JOIN departamentos d ON d.id=c.departamento_id WHERE (c.activa=true AND d.activo=true) OR c.id=? ORDER BY c.nombre,c.id",
                (fila,posicion)->new Opcion(fila.getLong(1),fila.getString(2)),academico==null?null:academico.carrera());
        } else if(rol.equals("tutor")) {
            long tutor=perfil("tutores",usuario,false);
            departamento=consultas.queryForObject("SELECT departamento_id FROM tutores WHERE id=?",Long.class,tutor);
            departamentos=consultas.query("SELECT id,nombre FROM departamentos WHERE activo=true OR id=? ORDER BY nombre,id",
                (fila,posicion)->new Opcion(fila.getLong(1),fila.getString(2)),departamento);
        }
        final String matriculaActual=matricula;final Long departamentoActual=departamento;final Academico actual=academico;
        final List<Opcion> opcionesCarreras=carreras,opcionesDepartamentos=departamentos;
        final boolean puedeEditarCorreo=!vinculadoConGoogle(usuario);
        return consultas.queryForObject("SELECT nombre,correo,avatar_url,telefono,acepta_sms FROM usuarios WHERE id=?",
            (fila,posicion)->new RespuestaPerfilUsuario(new RespuestaUsuario(usuario,fila.getString(1),fila.getString(2),fila.getString(3),identidad.roles()),rol,puedeEditarCorreo,fila.getString(4),matriculaActual,
                actual==null?null:actual.carrera(),actual==null?null:actual.periodo(),actual==null?null:actual.nombrePeriodo(),actual==null?null:actual.semestre(),actual!=null&&actual.editable(),
                departamentoActual,fila.getBoolean(5),opcionesCarreras,opcionesDepartamentos),usuario);
    }
    public RespuestaPerfilUsuario consultar(String sujeto){return datos(autenticacion.consultarSesion(sujeto));}
    private void rechazar(String mensaje,String campo){throw new SolicitudRechazada(422,mensaje,campo);}
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public RespuestaPerfilUsuario guardar(String sujeto,SolicitudEditarPerfil solicitud) {
        var identidad=autenticacion.consultarSesion(sujeto);long usuario=identidad.id();String rol=rol(identidad);
        String correoActual=consultas.queryForObject("SELECT correo FROM usuarios WHERE id=? FOR UPDATE",String.class,usuario);
        boolean cambioCorreo=!correoActual.equals(solicitud.correo());
        if(cambioCorreo) {
            if(vinculadoConGoogle(usuario))rechazar("El correo de esta cuenta está vinculado con Google.","correo");
            if(consultas.queryForObject("SELECT COUNT(*) FROM usuarios WHERE correo=? AND id<>?",Long.class,solicitud.correo(),usuario)>0)rechazar("Este correo ya está registrado.","correo");
        }
        if(!rol.equals("estudiante")&&(solicitud.matricula()!=null||solicitud.carreraId()!=null||solicitud.periodoId()!=null||solicitud.semestre()!=null))rechazar("Estos datos son para estudiantes.","matricula");
        if(!rol.equals("tutor")&&solicitud.departamentoId()!=null)rechazar("Este dato es para tutores.","departamentoId");
        if(rol.equals("estudiante")) {
            long estudiante=perfil("estudiantes",usuario,true);
            if(solicitud.matricula()==null)rechazar("Escribe tu matrícula.","matricula");
            if(consultas.queryForObject("SELECT COUNT(*) FROM estudiantes WHERE matricula=? AND id<>?",Long.class,solicitud.matricula(),estudiante)>0)rechazar("Esta matrícula ya está registrada.","matricula");
            if(solicitud.aceptaSms()&&solicitud.telefono().isEmpty())rechazar("Escribe tu teléfono para recibir avisos.","telefono");
            var actual=academico(estudiante,true);
            if(actual==null) {
                if(solicitud.carreraId()!=null||solicitud.periodoId()!=null||solicitud.semestre()!=null)rechazar("Completa primero tus datos académicos.","carreraId");
            } else {
                if(!Objects.equals(solicitud.periodoId(),actual.periodo()))throw new AccesoRechazado(409,"Tus datos académicos cambiaron. Vuelve a abrir tu perfil.");
                if(solicitud.carreraId()==null)rechazar("Selecciona tu carrera.","carreraId");
                if(solicitud.semestre()==null)rechazar("Escribe tu semestre.","semestre");
                boolean cambio=!Objects.equals(solicitud.carreraId(),actual.carrera())||!Objects.equals(solicitud.semestre(),actual.semestre());
                if(cambio&&!actual.editable())rechazar("Este periodo no permite cambios.","semestre");
                if(!Objects.equals(solicitud.carreraId(),actual.carrera())) {
                    var disponibles=consultas.queryForList("SELECT c.id FROM carreras c JOIN departamentos d ON d.id=c.departamento_id WHERE c.id=? AND c.activa=true AND d.activo=true FOR SHARE",Long.class,solicitud.carreraId());
                    if(disponibles.isEmpty())rechazar("Selecciona una carrera disponible.","carreraId");
                }
                if(cambio)consultas.update("UPDATE situaciones_academicas SET carrera_id=?,semestre=? WHERE id=?",solicitud.carreraId(),solicitud.semestre(),actual.id());
            }
            try{consultas.update("UPDATE estudiantes SET matricula=? WHERE id=?",solicitud.matricula(),estudiante);}
            catch(DuplicateKeyException fallo){rechazar("Esta matrícula ya está registrada.","matricula");}
        } else if(rol.equals("tutor")) {
            long tutor=perfil("tutores",usuario,true);
            if(solicitud.departamentoId()==null)rechazar("Selecciona tu departamento.","departamentoId");
            Long actual=consultas.queryForObject("SELECT departamento_id FROM tutores WHERE id=?",Long.class,tutor);
            if(!Objects.equals(actual,solicitud.departamentoId())) {
                var disponibles=consultas.queryForList("SELECT id FROM departamentos WHERE id=? AND activo=true FOR SHARE",Long.class,solicitud.departamentoId());
                if(disponibles.isEmpty())rechazar("Selecciona un departamento disponible.","departamentoId");
                consultas.update("UPDATE tutores SET departamento_id=? WHERE id=?",solicitud.departamentoId(),tutor);
            }
        }
        if(cambioCorreo) {
            try{consultas.update("UPDATE usuarios SET correo=? WHERE id=?",solicitud.correo(),usuario);}
            catch(DuplicateKeyException fallo){rechazar("Este correo ya está registrado.","correo");}
            consultas.update("DELETE FROM recuperaciones_contrasena WHERE usuario_id=?",usuario);
        }
        if(rol.equals("estudiante")) {
            consultas.update("UPDATE usuarios SET nombre=? WHERE id=?",solicitud.nombre(),usuario);
            contacto.guardar(sujeto,new SolicitudContactoEstudiante(solicitud.telefono(),solicitud.aceptaSms()));
        } else consultas.update("UPDATE usuarios SET nombre=?,telefono=? WHERE id=?",solicitud.nombre(),solicitud.telefono().isEmpty()?null:solicitud.telefono(),usuario);
        return datos(identidad);
    }
}
