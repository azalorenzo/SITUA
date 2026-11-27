package com.progweb.backend.servicios;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

import com.progweb.backend.entidades.Usuario;
import com.progweb.backend.errores.AccesoRechazado;
import com.progweb.backend.repositorios.RepositorioUsuarios;
import com.progweb.backend.respuestas.RespuestaInicioSesion;
import com.progweb.backend.respuestas.RespuestaRegistroPendiente;
import com.progweb.backend.solicitudes.SolicitudReenviarCodigoWhatsapp;
import com.progweb.backend.solicitudes.SolicitudVerificarWhatsapp;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioVerificacionWhatsapp {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private static final int SEGUNDOS_VIGENCIA = 600;
    private static final int SEGUNDOS_REENVIO = 60;
    private static final int MAXIMO_INTENTOS = 5;

    private final RepositorioUsuarios usuarios;
    private final PasswordEncoder codificador;
    private final ServicioWhatsappCloud whatsapp;
    private final ServicioAutenticacion autenticacion;
    private final ServicioTokens tokens;

    public ServicioVerificacionWhatsapp(
            RepositorioUsuarios usuarios,
            PasswordEncoder codificador,
            ServicioWhatsappCloud whatsapp,
            ServicioAutenticacion autenticacion,
            ServicioTokens tokens) {

        this.usuarios = usuarios;
        this.codificador = codificador;
        this.whatsapp = whatsapp;
        this.autenticacion = autenticacion;
        this.tokens = tokens;
    }

    @Transactional(noRollbackFor = AccesoRechazado.class)
    public RespuestaInicioSesion verificar(SolicitudVerificarWhatsapp solicitud) {

        String correo = normalizarCorreo(solicitud.correo());

        Usuario usuario = usuarios.buscarPorCorreoConBloqueo(correo)
                .orElseThrow(() -> new AccesoRechazado(
                        400,
                        "No existe una cuenta pendiente de verificación con ese correo."));

        if (usuario.tieneTelefonoVerificado()) {
            throw new AccesoRechazado(
                    409,
                    "Esta cuenta ya tiene su teléfono verificado.");
        }

        String hash = usuario.obtenerCodigoWhatsappHash();
        LocalDateTime expiracion = usuario.obtenerCodigoWhatsappExpiraEn();

        if (hash == null || expiracion == null) {
            throw new AccesoRechazado(
                    400,
                    "No tienes un código de verificación vigente. Solicita uno nuevo.");
        }

        LocalDateTime ahora = LocalDateTime.now();

        if (!expiracion.isAfter(ahora)) {
            usuario.cambiarCodigoWhatsappHash(null);
            usuario.cambiarCodigoWhatsappExpiraEn(null);
            usuario.cambiarIntentosWhatsapp(0);

            usuarios.saveAndFlush(usuario);

            throw new AccesoRechazado(
                    400,
                    "El código ha expirado. Solicita uno nuevo.");
        }

        if (usuario.obtenerIntentosWhatsapp() >= MAXIMO_INTENTOS) {
            throw new AccesoRechazado(
                    429,
                    "Has agotado los intentos de verificación. Solicita un nuevo código.");
        }

        if (!codificador.matches(solicitud.codigo(), hash)) {

            int intentos = usuario.obtenerIntentosWhatsapp() + 1;

            usuario.cambiarIntentosWhatsapp(intentos);
            usuarios.saveAndFlush(usuario);

            if (intentos >= MAXIMO_INTENTOS) {
                throw new AccesoRechazado(
                        429,
                        "Has agotado los cinco intentos permitidos. Solicita un nuevo código.");
            }

            throw new AccesoRechazado(
                    400,
                    "El código es incorrecto. Intentos restantes: "
                            + (MAXIMO_INTENTOS - intentos) + ".");
        }

        usuario.confirmarTelefono();

        usuarios.saveAndFlush(usuario);

        return tokens.crearToken(
                autenticacion.prepararUsuario(usuario));
    }

    @Transactional
    public RespuestaRegistroPendiente reenviar(
            SolicitudReenviarCodigoWhatsapp solicitud) {

        String correo = normalizarCorreo(solicitud.correo());

        Usuario usuario = usuarios.buscarPorCorreoConBloqueo(correo)
                .orElseThrow(() -> new AccesoRechazado(
                        400,
                        "No existe una cuenta pendiente de verificación con ese correo."));

        if (usuario.tieneTelefonoVerificado()) {
            throw new AccesoRechazado(
                    409,
                    "Esta cuenta ya tiene su teléfono verificado.");
        }

        if (usuario.obtenerTelefono() == null
                || usuario.obtenerTelefono().isBlank()) {
            throw new AccesoRechazado(
                    400,
                    "La cuenta no tiene un número de WhatsApp registrado.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime ultimoEnvio = usuario.obtenerUltimoCodigoWhatsappEn();

        if (ultimoEnvio != null) {

            long segundosTranscurridos = Duration.between(
                    ultimoEnvio, ahora).getSeconds();

            if (segundosTranscurridos < SEGUNDOS_REENVIO) {

                long restantes = SEGUNDOS_REENVIO - segundosTranscurridos;

                throw new AccesoRechazado(
                        429,
                        "Espera " + restantes
                                + " segundos antes de solicitar otro código.");
            }
        }

        String codigo = generarCodigo();

        usuario.cambiarCodigoWhatsappHash(codificador.encode(codigo));
        usuario.cambiarCodigoWhatsappExpiraEn(
                ahora.plusSeconds(SEGUNDOS_VIGENCIA));
        usuario.cambiarIntentosWhatsapp(0);
        usuario.cambiarUltimoCodigoWhatsappEn(ahora);

        usuarios.saveAndFlush(usuario);

        whatsapp.enviarCodigo(usuario.obtenerTelefono(), codigo);

        return new RespuestaRegistroPendiente(
                "Te enviamos un nuevo código de verificación por WhatsApp.",
                correo,
                SEGUNDOS_VIGENCIA);
    }

    private String generarCodigo() {
        return String.format(Locale.ROOT, "%06d",
                ALEATORIO.nextInt(1_000_000));
    }

    private String normalizarCorreo(String correo) {
        return correo.strip().toLowerCase(Locale.ROOT);
    }
}