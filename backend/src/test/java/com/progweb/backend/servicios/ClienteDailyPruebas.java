package com.progweb.backend.servicios;

import java.net.http.*;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.Flow;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClienteDailyPruebas {
    final ObjectMapper json=new ObjectMapper();
    final String sala="situa-12345678-1234-1234-1234-123456789abc";
    final Instant vence=Instant.parse("2030-01-01T00:05:00Z");
    @SuppressWarnings("unchecked")
    HttpClient cliente(ClienteDaily daily,int estado,String cuerpo)throws Exception {
        var cliente=mock(HttpClient.class);var respuesta=mock(HttpResponse.class);
        when(respuesta.statusCode()).thenReturn(estado);when(respuesta.body()).thenReturn(cuerpo);
        when(cliente.send(any(),any())).thenReturn(respuesta);ReflectionTestUtils.setField(daily,"cliente",cliente);return cliente;
    }
    String cuerpo(HttpRequest peticion)throws Exception {
        var texto=new java.io.ByteArrayOutputStream();var terminado=new CompletableFuture<String>();
        peticion.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<java.nio.ByteBuffer>() {
            public void onSubscribe(Flow.Subscription suscripcion){suscripcion.request(Long.MAX_VALUE);}
            public void onNext(java.nio.ByteBuffer parte){byte[] bytes=new byte[parte.remaining()];parte.get(bytes);texto.writeBytes(bytes);}
            public void onError(Throwable fallo){terminado.completeExceptionally(fallo);}
            public void onComplete(){terminado.complete(texto.toString(java.nio.charset.StandardCharsets.UTF_8));}
        });
        return terminado.get(2,TimeUnit.SECONDS);
    }
    @Test void salaPrivadaConDosParticipantesYExpulsionAlVencer()throws Exception{
        var daily=new ClienteDaily(json,"clave-privada",true);var cliente=cliente(daily,200,"{\"name\":\""+sala+"\",\"url\":\"https://prueba.daily.co/"+sala+"\"}");
        assertThat(daily.crearSala(sala,"video",vence).nombre()).isEqualTo(sala);
        var captor=ArgumentCaptor.forClass(HttpRequest.class);verify(cliente).send(captor.capture(),any());
        var datos=json.readTree(cuerpo(captor.getValue()));assertThat(datos.path("privacy").asString()).isEqualTo("private");
        assertThat(datos.at("/properties/max_participants").asInt()).isEqualTo(2);
        assertThat(datos.at("/properties/eject_at_room_exp").asBoolean()).isTrue();
        assertThat(datos.at("/properties/exp").asLong()).isEqualTo(vence.getEpochSecond());
        assertThat(datos.at("/properties/permissions/canAdmin").asBoolean()).isFalse();
    }
    @Test void accesoDeAudioNoPermitePublicarVideoNiAdministrar()throws Exception{
        var daily=new ClienteDaily(json,"clave-privada",true);var cliente=cliente(daily,200,"{\"token\":\"acceso-personal\"}");
        assertThat(daily.crearAcceso(sala,"audio",17,"Ana",vence)).isEqualTo("acceso-personal");
        var captor=ArgumentCaptor.forClass(HttpRequest.class);verify(cliente).send(captor.capture(),any());
        var datos=json.readTree(cuerpo(captor.getValue())).path("properties");
        assertThat(datos.path("room_name").asString()).isEqualTo(sala);assertThat(datos.path("user_id").asString()).isEqualTo("17");
        assertThat(datos.path("permissions").path("canSend").toString()).isEqualTo("[\"audio\"]");
        assertThat(datos.path("is_owner").asBoolean()).isFalse();assertThat(datos.path("eject_at_token_exp").asBoolean()).isTrue();
    }
    @Test void noExponeErroresDelProveedorNiAceptaDireccionesAjenas()throws Exception{
        var daily=new ClienteDaily(json,"clave-privada",true);cliente(daily,401,"clave-privada datos de cuenta");
        assertThatThrownBy(()->daily.crearSala(sala,"video",vence)).hasMessageNotContaining("clave-privada").hasMessageContaining("No se pudo");
        cliente(daily,200,"{\"name\":\""+sala+"\",\"url\":\"https://daily.co.ejemplo.invalid/sala\"}");
        assertThatThrownBy(()->daily.crearSala(sala,"video",vence)).hasMessageContaining("No se pudo preparar");
    }
    @Test void noEntregaTokenVacioNiLlamaSinConfiguracion()throws Exception{
        var daily=new ClienteDaily(json,"clave-privada",true);cliente(daily,200,"{}");
        assertThatThrownBy(()->daily.crearAcceso(sala,"video",17,"Ana",vence)).hasMessageContaining("No se pudo entrar");
        assertThatThrownBy(()->new ClienteDaily(json,"",true)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new ClienteDaily(json,"",false).habilitado()).isFalse();
    }
    @Test void retirarUnaSalaQueYaNoExisteNoProvocaReintentos()throws Exception{
        var daily=new ClienteDaily(json,"clave-privada",true);cliente(daily,404,"{}");
        assertThat(daily.cerrarSala(sala,17,18)).isTrue();
    }
}
