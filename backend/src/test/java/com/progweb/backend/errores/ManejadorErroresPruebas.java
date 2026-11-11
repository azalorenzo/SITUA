package com.progweb.backend.errores;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ManejadorErroresPruebas {

    private MockMvc cliente;
    private LocalValidatorFactoryBean validador;

    @BeforeEach
    void prepararCliente() {
        validador = new LocalValidatorFactoryBean();
        validador.afterPropertiesSet();
        cliente = MockMvcBuilders.standaloneSetup(new ControladorPrueba())
                .setControllerAdvice(new ManejadorErrores())
                .setValidator(validador)
                .build();
    }

    @AfterEach
    void cerrarValidador() {
        validador.close();
    }

    @Test
    void devuelveLosErroresBajoCadaCampo() throws Exception {
        cliente.perform(post("/pruebas/formulario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"correo\":\"correo-invalido\",\"edad\":20}"))
                .andExpect(status().is(422))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.estado").value(422))
                .andExpect(jsonPath("$.mensaje").value("Revisa los campos del formulario."))
                .andExpect(jsonPath("$.erroresPorCampo.nombre", containsInAnyOrder(
                        "Escribe tu nombre.", "El nombre debe tener al menos 3 caracteres.")))
                .andExpect(jsonPath("$.erroresPorCampo.correo[0]").value("Escribe un correo válido."))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void identificaCamposQueNoSeEnviaron() throws Exception {
        cliente.perform(post("/pruebas/formulario")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.erroresPorCampo.nombre[0]").value("Escribe tu nombre."))
                .andExpect(jsonPath("$.erroresPorCampo.correo[0]").value("Escribe tu correo."));
    }

    @Test
    void rechazaUnJsonMalEscrito() throws Exception {
        comprobarJsonInvalido("{\"nombre\":");
    }

    @Test
    void rechazaTiposDeDatosIncorrectos() throws Exception {
        comprobarJsonInvalido("{\"nombre\":\"Ana\",\"correo\":\"ana@example.com\",\"edad\":\"dato-privado\"}");
    }

    @Test
    void rechazaUnCuerpoVacio() throws Exception {
        comprobarJsonInvalido("");
    }

    @Test
    void dejaPasarUnFormularioCorrecto() throws Exception {
        cliente.perform(post("/pruebas/formulario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"correo\":\"ana@example.com\",\"edad\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana"));
    }

    private void comprobarJsonInvalido(String contenido) throws Exception {
        cliente.perform(post("/pruebas/formulario")
                        .contentType(MediaType.APPLICATION_JSON).content(contenido))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.mensaje").value("Envía un JSON válido con los datos y tipos esperados."))
                .andExpect(jsonPath("$.erroresPorCampo").isEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    // Este formulario solo existe en las pruebas, no agrega rutas a la aplicación.
    @RestController
    static class ControladorPrueba {
        @PostMapping("/pruebas/formulario")
        DatosPrueba revisarFormulario(@Valid @RequestBody DatosPrueba datos) {
            return datos;
        }
    }

    record DatosPrueba(
            @NotBlank(message = "Escribe tu nombre.")
            @Size(min = 3, message = "El nombre debe tener al menos 3 caracteres.") String nombre,
            @NotBlank(message = "Escribe tu correo.")
            @Email(message = "Escribe un correo válido.") String correo,
            Integer edad
    ) {
    }
}
