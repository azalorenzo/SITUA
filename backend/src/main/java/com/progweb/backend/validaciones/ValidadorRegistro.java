package com.progweb.backend.validaciones;

import java.nio.charset.StandardCharsets;
import com.progweb.backend.solicitudes.SolicitudRegistro;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidadorRegistro implements ConstraintValidator<RegistroCoherente, SolicitudRegistro> {
    @Override
    public boolean isValid(SolicitudRegistro solicitud, ConstraintValidatorContext contexto) {
        if (solicitud == null) return true;
        boolean valido = true;
        contexto.disableDefaultConstraintViolation();
        if (solicitud.contrasena() != null
                && solicitud.contrasena().getBytes(StandardCharsets.UTF_8).length > 72) {
            contexto.buildConstraintViolationWithTemplate("La contraseña es demasiado larga.")
                    .addPropertyNode("contrasena").addConstraintViolation();
            valido = false;
        }
        if (solicitud.contrasena() != null && solicitud.confirmarContrasena() != null
                && !solicitud.contrasena().equals(solicitud.confirmarContrasena())) {
            contexto.buildConstraintViolationWithTemplate("Las contraseñas no coinciden.")
                    .addPropertyNode("confirmarContrasena").addConstraintViolation();
            valido = false;
        }
        return valido;
    }
}
