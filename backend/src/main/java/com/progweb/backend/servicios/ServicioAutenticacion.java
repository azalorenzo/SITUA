package com.progweb.backend.servicios;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import com.progweb.backend.entidades.Rol;
import com.progweb.backend.entidades.Usuario;
import com.progweb.backend.errores.AccesoRechazado;
import com.progweb.backend.repositorios.RepositorioUsuarios;
import com.progweb.backend.respuestas.RespuestaInicioSesion;
import com.progweb.backend.respuestas.RespuestaUsuario;
import com.progweb.backend.solicitudes.SolicitudInicioSesion;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ServicioAutenticacion {
    private final RepositorioUsuarios repositorioUsuarios;
    private final PasswordEncoder codificador;
    private final ServicioTokens servicioTokens;
    private final String hashDeComparacion;

    public ServicioAutenticacion(RepositorioUsuarios repositorioUsuarios,
            PasswordEncoder codificador, ServicioTokens servicioTokens) {
        this.repositorioUsuarios = repositorioUsuarios;
        this.codificador = codificador;
        this.servicioTokens = servicioTokens;
        // También hacemos una comparación cuando el correo no existe.
        this.hashDeComparacion = codificador.encode(java.util.UUID.randomUUID().toString());
    }

    public RespuestaInicioSesion iniciarSesion(SolicitudInicioSesion solicitud) {
    Usuario usuario = repositorioUsuarios.buscarPorCorreoConRoles(
            solicitud.correo().strip().toLowerCase(Locale.ROOT)).orElse(null);

    String hash = usuario != null && usuario.obtenerContrasenaHash() != null
            ? usuario.obtenerContrasenaHash()
            : hashDeComparacion;

    boolean coincide = false;

    if (solicitud.contrasena().getBytes(StandardCharsets.UTF_8).length <= 72) {
        coincide = codificador.matches(solicitud.contrasena(), hash);
    }

    if (usuario == null || usuario.obtenerContrasenaHash() == null || !coincide) {
        throw new AccesoRechazado(
                401,
                "El correo o la contraseña son incorrectos.");
    }

    if (!usuario.tieneTelefonoVerificado()) {
        throw new AccesoRechazado(
                403,
                "Tu cuenta todavía no está verificada. Revisa tu WhatsApp e introduce el código de verificación.");
    }

    return servicioTokens.crearToken(prepararUsuario(usuario));
}

    public RespuestaUsuario consultarSesion(String identificador) {
        long id;
        try {
            id = Long.parseLong(identificador);
        } catch (NumberFormatException excepcion) {
            throw new AccesoRechazado(401, "La sesión no es válida. Inicia sesión nuevamente.");
        }
        Usuario usuario = repositorioUsuarios.buscarPorIdConRoles(id)
                .orElseThrow(() -> new AccesoRechazado(401, "La sesión ya no está disponible."));
        return prepararUsuario(usuario);
    }

    public RespuestaUsuario prepararUsuario(Usuario usuario) {
        if (!"activo".equals(usuario.obtenerEstado())) {
            throw new AccesoRechazado(403, "La cuenta está inactiva. Consulta con coordinación.");
        }
        List<String> roles = usuario.obtenerRoles().stream().map(Rol::obtenerCodigo).sorted().toList();
        if (roles.isEmpty()) {
            throw new AccesoRechazado(403, "La cuenta no tiene un rol asignado. Consulta con coordinación.");
        }
        return new RespuestaUsuario(usuario.obtenerId(), usuario.obtenerNombre(),
                usuario.obtenerCorreo(), usuario.obtenerAvatarUrl(), roles);
    }
}
