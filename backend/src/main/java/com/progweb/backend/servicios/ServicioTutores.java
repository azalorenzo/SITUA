package com.progweb.backend.servicios;

import java.util.List;
import java.util.Locale;
import java.nio.charset.StandardCharsets;
import com.progweb.backend.entidades.Usuario;
import com.progweb.backend.errores.*;
import com.progweb.backend.repositorios.*;
import com.progweb.backend.respuestas.*;
import com.progweb.backend.solicitudes.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class ServicioTutores {
    private final RepositorioTutores tutores;
    private final RepositorioUsuarios usuarios;
    private final RepositorioRoles roles;
    private final ServicioAutenticacion autenticacion;
    private final PasswordEncoder codificador;
    public ServicioTutores(RepositorioTutores tutores,RepositorioUsuarios usuarios,RepositorioRoles roles,
            ServicioAutenticacion autenticacion,PasswordEncoder codificador) {
        this.tutores=tutores;this.usuarios=usuarios;this.roles=roles;this.autenticacion=autenticacion;this.codificador=codificador;
    }
    public PaginaTutores listar(int pagina,int tamano,String estado,Long departamentoId,String busqueda) {
        if(pagina<1) throw new SolicitudRechazada(422,"La página debe ser mayor que cero.","pagina");
        if(tamano<1||tamano>100) throw new SolicitudRechazada(422,"El tamaño debe estar entre 1 y 100.","tamano");
        if(estado!=null&&!List.of("activo","inactivo").contains(estado)) throw new SolicitudRechazada(422,"El estado no es válido.","estado");
        if(departamentoId!=null&&departamentoId<1) throw new SolicitudRechazada(422,"El departamento no es válido.","departamentoId");
        if(busqueda.length()>160) throw new SolicitudRechazada(422,"La búsqueda no debe superar 160 caracteres.","busqueda");
        String termino=busqueda.strip();long total=tutores.contar(estado,departamentoId,termino);
        int paginas=Math.toIntExact((total+tamano-1)/tamano);int actual=paginas==0?1:Math.min(pagina,paginas);
        return new PaginaTutores(tutores.listar(estado,departamentoId,termino,tamano,(actual-1L)*tamano),actual,tamano,total,paginas,tutores.resumen());
    }
    public List<RespuestaTutorCoordinacion> opciones() { return tutores.opciones(); }
    public List<RespuestaDepartamento> departamentos() { return tutores.departamentos(); }
    private long verificarAutor(String id) {
        var autor=autenticacion.consultarSesion(id);
        if(!autor.roles().contains("coordinador")) throw new AccesoRechazado(403,"Esta operación requiere acceso de coordinación.");
        return autor.id();
    }
    private void validarCorreo(String correo,Long usuarioActual) {
        usuarios.buscarPorCorreo(correo).ifPresent(existente -> {
            if(!existente.obtenerId().equals(usuarioActual)) throw new SolicitudRechazada(409,"Ya hay una cuenta con este correo.","correo");
        });
    }
    private void validarDepartamento(long departamentoId) {
        if(!tutores.departamentoActivo(departamentoId)) throw new SolicitudRechazada(409,"Selecciona un departamento activo.","departamentoId");
    }
    @Transactional
    public RespuestaTutorCoordinacion crear(SolicitudCrearTutor solicitud,String autor) {
        verificarAutor(autor);validarDepartamento(solicitud.departamentoId());
        String correo=solicitud.correo().strip().toLowerCase(Locale.ROOT);validarCorreo(correo,null);
        if(solicitud.contrasena().getBytes(StandardCharsets.UTF_8).length>72) throw new SolicitudRechazada(422,"La contraseña es demasiado larga.","contrasena");
        Usuario usuario=new Usuario(solicitud.nombre().strip(),correo,codificador.encode(solicitud.contrasena()));
        // El formulario de tutores no permite escoger otros permisos.
        usuario.asignarRol(roles.buscarPorCodigo("tutor").orElseThrow());
        usuarios.saveAndFlush(usuario);
        long id=tutores.crear(usuario.obtenerId(),solicitud.departamentoId());
        return tutores.buscar(id).orElseThrow();
    }
    @Transactional
    public RespuestaTutorCoordinacion editar(long id,SolicitudEditarTutor solicitud,String autor) {
        verificarAutor(autor);
        var tutor=tutores.bloquear(id).orElseThrow(() -> new AccesoRechazado(404,"No se encontró el tutor."));
        if(!tutor.departamentoId().equals(solicitud.departamentoId())) validarDepartamento(solicitud.departamentoId());
        String correo=solicitud.correo().strip().toLowerCase(Locale.ROOT);validarCorreo(correo,tutor.usuarioId());
        var usuario=usuarios.findById(tutor.usuarioId()).orElseThrow();
        usuario.cambiarNombre(solicitud.nombre().strip());usuario.cambiarCorreo(correo);usuarios.flush();
        tutores.cambiarDepartamento(id,solicitud.departamentoId());
        return tutores.buscar(id).orElseThrow();
    }
    @Transactional
    public RespuestaTutorCoordinacion cambiarEstado(long id,boolean activar,String identificador) {
        long autor=verificarAutor(identificador);
        var tutor=tutores.bloquear(id).orElseThrow(() -> new AccesoRechazado(404,"No se encontró el tutor."));
        var usuario=usuarios.buscarPorIdConRoles(tutor.usuarioId()).orElseThrow();
        if(!activar&&(usuario.obtenerId().equals(autor)||usuario.obtenerRoles().stream().anyMatch(rol -> rol.obtenerCodigo().equals("coordinador"))))
            throw new AccesoRechazado(409,"Una cuenta de coordinación no se puede desactivar desde Tutores.");
        if(activar) usuario.activar();else usuario.desactivar();usuarios.flush();
        return tutores.buscar(id).orElseThrow();
    }
}
