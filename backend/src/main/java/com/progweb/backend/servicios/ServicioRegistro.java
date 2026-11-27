
package com.progweb.backend.servicios;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;

import com.progweb.backend.entidades.Estudiante;
import com.progweb.backend.entidades.Usuario;
import com.progweb.backend.errores.SolicitudRechazada;
import com.progweb.backend.repositorios.RepositorioEstudiantes;
import com.progweb.backend.repositorios.RepositorioRoles;
import com.progweb.backend.repositorios.RepositorioUsuarios;
import com.progweb.backend.respuestas.RespuestaRegistroPendiente;
import com.progweb.backend.solicitudes.SolicitudRegistro;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioRegistro {

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final int SEGUNDOS_VIGENCIA = 600;

    private final RepositorioUsuarios usuarios;
    private final RepositorioEstudiantes estudiantes;
    private final RepositorioRoles roles;
    private final PasswordEncoder codificador;
    private final ServicioWhatsappCloud whatsapp;

    public ServicioRegistro(
            RepositorioUsuarios usuarios,
            RepositorioEstudiantes estudiantes,
            RepositorioRoles roles,
            PasswordEncoder codificador,
            ServicioWhatsappCloud whatsapp) {

        this.usuarios = usuarios;
        this.estudiantes = estudiantes;
        this.roles = roles;
        this.codificador = codificador;
        this.whatsapp = whatsapp;
    }

    @Transactional
    public RespuestaRegistroPendiente registrar(SolicitudRegistro solicitud) {

        String correo = solicitud.correo().strip().toLowerCase(Locale.ROOT);

        if (usuarios.buscarPorCorreo(correo).isPresent()) {
            throw new SolicitudRechazada(
                    409,
                    "Ya hay una cuenta con este correo.",
                    "correo");
        }

        String telefono = solicitud.telefono().strip();

        // Comprobamos la matrícula antes de crear la cuenta.
        String matricula = solicitud.matricula().strip().toUpperCase(Locale.ROOT);

        if (estudiantes.buscarPorMatricula(matricula).isPresent()) {
            throw new SolicitudRechazada(
                    409,
                    "Ya hay una cuenta con esta matrícula.",
                    "matricula");
        }

        Usuario usuario = new Usuario(
                solicitud.nombre().strip(),
                correo,
                codificador.encode(solicitud.contrasena()));

        usuario.cambiarTelefono(telefono);
        usuario.cambiarTelefonoVerificado(false);

        String codigo = generarCodigo();

        LocalDateTime ahora = LocalDateTime.now();

        usuario.cambiarCodigoWhatsappHash(codificador.encode(codigo));
        usuario.cambiarCodigoWhatsappExpiraEn(
                ahora.plusSeconds(SEGUNDOS_VIGENCIA));
        usuario.cambiarIntentosWhatsapp(0);
        usuario.cambiarUltimoCodigoWhatsappEn(ahora);

        // Crea el estudiante y guarda el usuario con sus datos pendientes.
        crearEstudiante(usuario, matricula);

        // Si WhatsApp falla, se lanza una excepción y se revierte
        // la transacción de creación de la cuenta.
        whatsapp.enviarCodigo(telefono, codigo);

        return new RespuestaRegistroPendiente(
                "Te enviamos un código de verificación por WhatsApp.",
                correo,
                SEGUNDOS_VIGENCIA);
    }

    private String generarCodigo() {
        return String.format(
                Locale.ROOT,
                "%06d",
                ALEATORIO.nextInt(1_000_000));
    }

    @Transactional
    public void crearEstudiante(Usuario usuario, String matricula) {

        String matriculaNormalizada =
                matricula.strip().toUpperCase(Locale.ROOT);

        if (estudiantes.buscarPorMatricula(matriculaNormalizada).isPresent()) {
            throw new SolicitudRechazada(
                    409,
                    "Ya hay una cuenta con esta matrícula.",
                    "matricula");
        }

        // Desde el registro público solo se crean estudiantes.
        usuario.asignarRol(
                roles.buscarPorCodigo("estudiante").orElseThrow());

        usuarios.saveAndFlush(usuario);

        estudiantes.saveAndFlush(
                new Estudiante(usuario, matriculaNormalizada));
    }
}
