
package com.progweb.backend.servicios;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientResponseException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class ServicioWhatsappCloud {

private static final Logger LOGGER =
        LoggerFactory.getLogger(ServicioWhatsappCloud.class);

    private final RestClient cliente;

    @Value("${mensajeria.whatsapp.habilitado:false}")
    private boolean habilitado;

    @Value("${mensajeria.whatsapp.token:}")
    private String token;

    @Value("${mensajeria.whatsapp.phone-number-id:}")
    private String phoneNumberId;

    @Value("${mensajeria.whatsapp.version:}")
    private String version;

    @Value("${mensajeria.whatsapp.plantilla:}")
    private String plantilla;

    @Value("${mensajeria.whatsapp.idioma:es_MX}")
    private String idioma;

    public ServicioWhatsappCloud() {
    this.cliente = RestClient.builder()
            .baseUrl("https://graph.facebook.com")
            .build();
}

    public void enviarCodigo(String telefono, String codigo) {
        validarConfiguracion();

        if (telefono == null || !telefono.matches("^\\+[1-9]\\d{7,14}$")) {
            throw new IllegalArgumentException("El teléfono no tiene un formato válido.");
        }

        if (codigo == null || !codigo.matches("\\d{6}")) {
            throw new IllegalArgumentException("El código de verificación no es válido.");
        }

        Map<String, Object> componenteBody = Map.of(
                "type", "body",
                "parameters", List.of(
                        Map.of(
                                "type", "text",
                                "text", codigo
                        )
                )
        );

        Map<String, Object> componenteBoton = Map.of(
                "type", "button",
                "sub_type", "url",
                "index", "0",
                "parameters", List.of(
                        Map.of(
                                "type", "text",
                                "text", codigo
                        )
                )
        );

        Map<String, Object> plantillaMensaje = Map.of(
                "name", plantilla,
                "language", Map.of("code", idioma),
                "components", List.of(componenteBody, componenteBoton)
        );

        Map<String, Object> mensaje = Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", telefono,
                "type", "template",
                "template", plantillaMensaje
        );

        try {
            cliente.post()
                    .uri("/{version}/{phoneNumberId}/messages",
                            version, phoneNumberId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(mensaje)
                    .retrieve()
                    .toBodilessEntity();

                } catch (RestClientResponseException excepcion) {

            LOGGER.error(
                    "WhatsApp Cloud API respondió con HTTP {}: {}",
                    excepcion.getStatusCode().value(),
                    excepcion.getResponseBodyAsString()
            );

            throw new IllegalStateException(
                    "No fue posible enviar el código por WhatsApp.");

        } catch (RestClientException excepcion) {

            LOGGER.error(
                    "Error de conexión con WhatsApp Cloud API: {}",
                    excepcion.getMessage()
            );

            throw new IllegalStateException(
                    "No fue posible conectar con WhatsApp Cloud API.");
        }
        }
    

    private void validarConfiguracion() {
        if (!habilitado) {
            throw new IllegalStateException(
                    "El envío de WhatsApp está deshabilitado.");
        }

        if (token == null || token.isBlank()
                || phoneNumberId == null || phoneNumberId.isBlank()
                || version == null || version.isBlank()
                || plantilla == null || plantilla.isBlank()
                || idioma == null || idioma.isBlank()) {
            throw new IllegalStateException(
                    "La configuración de WhatsApp está incompleta.");
        }
    }
}
