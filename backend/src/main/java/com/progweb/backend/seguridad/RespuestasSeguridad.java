package com.progweb.backend.seguridad;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.progweb.backend.errores.RespuestaError;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class RespuestasSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ObjectMapper conversor;

    public RespuestasSeguridad(ObjectMapper conversor) {
        this.conversor = conversor;
    }

    @Override
    public void commence(HttpServletRequest solicitud, HttpServletResponse respuesta,
            AuthenticationException excepcion) throws IOException, ServletException {
        respuesta.setHeader("WWW-Authenticate", "Bearer");
        responder(respuesta, 401, "La sesión no es válida o venció. Inicia sesión nuevamente.");
    }

    @Override
    public void handle(HttpServletRequest solicitud, HttpServletResponse respuesta,
            AccessDeniedException excepcion) throws IOException, ServletException {
        responder(respuesta, 403, "No tienes permiso para realizar esta acción.");
    }

    private void responder(HttpServletResponse respuesta, int estado, String mensaje) throws IOException {
        respuesta.setStatus(estado);
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding("UTF-8");
        conversor.writeValue(respuesta.getOutputStream(), new RespuestaError(estado, mensaje, Map.of()));
    }
}
