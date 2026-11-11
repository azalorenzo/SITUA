package com.progweb.backend.errores;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaError> manejarCamposInvalidos(
            MethodArgumentNotValidException excepcion) {
        Map<String, List<String>> erroresPorCampo = new LinkedHashMap<>();

        // Un campo puede tener más de un detalle que corregir.
        excepcion.getBindingResult().getFieldErrors().forEach(error ->
                erroresPorCampo.computeIfAbsent(error.getField(), campo -> new ArrayList<>())
                        .add(error.getDefaultMessage() != null
                                ? error.getDefaultMessage() : "Revisa este campo."));

        // Algunos errores corresponden al formulario completo.
        excepcion.getBindingResult().getGlobalErrors().forEach(error ->
                erroresPorCampo.computeIfAbsent("general", campo -> new ArrayList<>())
                        .add(error.getDefaultMessage() != null
                                ? error.getDefaultMessage() : "Revisa los datos del formulario."));

        RespuestaError respuesta = new RespuestaError(
                422, "Revisa los campos del formulario.", erroresPorCampo);
        return ResponseEntity.status(422).body(respuesta);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespuestaError> manejarJsonInvalido(
            HttpMessageNotReadableException excepcion) {
        RespuestaError respuesta = new RespuestaError(
                400, "Envía un JSON válido con los datos y tipos esperados.", Map.of());
        return ResponseEntity.badRequest().body(respuesta);
    }
}
