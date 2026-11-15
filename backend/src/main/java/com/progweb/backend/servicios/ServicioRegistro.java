package com.progweb.backend.servicios;

import java.util.Locale;
import com.progweb.backend.entidades.Estudiante;
import com.progweb.backend.entidades.Usuario;
import com.progweb.backend.errores.SolicitudRechazada;
import com.progweb.backend.repositorios.RepositorioEstudiantes;
import com.progweb.backend.repositorios.RepositorioRoles;
import com.progweb.backend.repositorios.RepositorioUsuarios;
import com.progweb.backend.respuestas.RespuestaInicioSesion;
import com.progweb.backend.solicitudes.SolicitudRegistro;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioRegistro {
    private final RepositorioUsuarios usuarios;
    private final RepositorioEstudiantes estudiantes;
    private final RepositorioRoles roles;
    private final PasswordEncoder codificador;
    private final ServicioAutenticacion autenticacion;
    private final ServicioTokens tokens;

    public ServicioRegistro(RepositorioUsuarios usuarios, RepositorioEstudiantes estudiantes,
            RepositorioRoles roles, PasswordEncoder codificador, ServicioAutenticacion autenticacion,
            ServicioTokens tokens) {
        this.usuarios = usuarios; this.estudiantes = estudiantes; this.roles = roles;
        this.codificador = codificador; this.autenticacion = autenticacion; this.tokens = tokens;
    }

    @Transactional
    public RespuestaInicioSesion registrar(SolicitudRegistro solicitud) {
        String correo = solicitud.correo().strip().toLowerCase(Locale.ROOT);
        if (usuarios.buscarPorCorreo(correo).isPresent()) {
            throw new SolicitudRechazada(409, "Ya hay una cuenta con este correo.", "correo");
        }
        Usuario usuario = new Usuario(solicitud.nombre().strip(), correo, codificador.encode(solicitud.contrasena()));
        crearEstudiante(usuario, solicitud.matricula());
        return tokens.crearToken(autenticacion.prepararUsuario(usuario));
    }

    public void crearEstudiante(Usuario usuario, String matricula) {
        String matriculaNormalizada = matricula.strip().toUpperCase(Locale.ROOT);
        if (estudiantes.buscarPorMatricula(matriculaNormalizada).isPresent()) {
            throw new SolicitudRechazada(409, "Ya hay una cuenta con esta matrícula.", "matricula");
        }
        // Desde el registro público solo se crean estudiantes.
        usuario.asignarRol(roles.buscarPorCodigo("estudiante").orElseThrow());
        usuarios.saveAndFlush(usuario);
        estudiantes.saveAndFlush(new Estudiante(usuario, matriculaNormalizada));
    }
}
