package com.progweb.backend.errores;

import java.util.List;
import java.util.Map;

public record RespuestaError(
        int estado,
        String mensaje,
        Map<String, List<String>> erroresPorCampo
) {
}
