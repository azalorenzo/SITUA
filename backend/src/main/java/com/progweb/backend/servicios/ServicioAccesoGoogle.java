package com.progweb.backend.servicios;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import com.progweb.backend.entidades.IdentidadExterna;
import com.progweb.backend.entidades.Usuario;
import com.progweb.backend.errores.AccesoRechazado;
import com.progweb.backend.errores.SolicitudRechazada;
import com.progweb.backend.repositorios.RepositorioIdentidades;
import com.progweb.backend.repositorios.RepositorioUsuarios;
import com.progweb.backend.respuestas.RespuestaInicioSesion;
import com.progweb.backend.seguridad.DatosGoogle;
import com.progweb.backend.seguridad.VerificadorGoogle;
import com.progweb.backend.solicitudes.SolicitudAccesoGoogle;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

@Service
public class ServicioAccesoGoogle {
    private final VerificadorGoogle verificador;
    private final RepositorioIdentidades identidades;
    private final RepositorioUsuarios usuarios;
    private final PasswordEncoder codificador;
    private final ServicioRegistro registro;
    private final ServicioAutenticacion autenticacion;
    private final ServicioTokens tokens;
    private final JdbcTemplate consultas;

    public ServicioAccesoGoogle(VerificadorGoogle verificador, RepositorioIdentidades identidades,
            RepositorioUsuarios usuarios, PasswordEncoder codificador, ServicioRegistro registro,
            ServicioAutenticacion autenticacion, ServicioTokens tokens,JdbcTemplate consultas) {
        this.verificador = verificador; this.identidades = identidades; this.usuarios = usuarios;
        this.codificador = codificador; this.registro = registro; this.autenticacion = autenticacion; this.tokens = tokens;this.consultas=consultas;
    }

    @Transactional
    public RespuestaInicioSesion iniciarSesion(SolicitudAccesoGoogle solicitud) {
        DatosGoogle datos = verificador.verificar(solicitud.credencial(), solicitud.comprobacion());
        IdentidadExterna identidad = identidades.buscarGoogle(datos.identificador()).orElse(null);
        if (identidad != null) return tokens.crearToken(autenticacion.prepararUsuario(identidad.obtenerUsuario()));

        String correo = datos.correo().strip().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarios.buscarPorCorreoConRoles(correo).orElse(null);
        if (usuario != null) {
            String correoActual=consultas.queryForObject("SELECT correo FROM usuarios WHERE id=? FOR UPDATE",String.class,usuario.obtenerId());
            if(!correo.equals(correoActual))throw new AccesoRechazado(409,"El correo de la cuenta cambió. Intenta nuevamente.");
            // Coincidir en correo no basta para unir una cuenta que ya existía.
            if (solicitud.contrasenaVinculacion() == null || solicitud.contrasenaVinculacion().isBlank()) {
                throw new SolicitudRechazada(422, "Escribe tu contraseña actual para vincular esta cuenta con Google.",
                        "contrasenaVinculacion");
            }
            String hash = usuario.obtenerContrasenaHash();
            if (hash == null || solicitud.contrasenaVinculacion().getBytes(StandardCharsets.UTF_8).length > 72
                    || !codificador.matches(solicitud.contrasenaVinculacion(), hash)) {
                throw new AccesoRechazado(401, "No se pudo vincular la cuenta. Revisa tu contraseña actual.");
            }
            autenticacion.prepararUsuario(usuario);
            if (identidades.buscarGoogleDeUsuario(usuario.obtenerId()).isPresent()) {
                throw new AccesoRechazado(409, "La cuenta ya tiene otro acceso de Google vinculado.");
            }
        } else {
            if (solicitud.matricula() == null || solicitud.matricula().isBlank()) {
                throw new SolicitudRechazada(422, "Completa tu matrícula para crear tu cuenta de estudiante.", "matricula");
            }
            String nombre = datos.nombre() != null && !datos.nombre().isBlank() ? datos.nombre().strip() : correo;
            if (nombre.length() > 160) nombre = nombre.substring(0, 160);
            usuario = new Usuario(nombre, correo, null);
            if (datos.imagen() != null && datos.imagen().startsWith("https://") && datos.imagen().length() <= 1024) {
                usuario.cambiarAvatarUrl(datos.imagen());
            }
            registro.crearEstudiante(usuario, solicitud.matricula());
        }
        identidades.saveAndFlush(new IdentidadExterna(usuario, datos.identificador()));
        return tokens.crearToken(autenticacion.prepararUsuario(usuario));
    }
}
