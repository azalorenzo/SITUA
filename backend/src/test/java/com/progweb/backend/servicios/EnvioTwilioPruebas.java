package com.progweb.backend.servicios;

import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.Flow;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EnvioTwilioPruebas {
    EnvioTwilio crear(boolean sms) {
        return new EnvioTwilio(new ObjectMapper(),sms,"AC"+"a".repeat(32),"clave-prueba","+15551234567","America/Mexico_City","");
    }
    EnvioAvisoTelefonico.Confirmacion datos() { return new EnvioAvisoTelefonico.Confirmacion("Tutor prueba",LocalDateTime.of(2030,1,2,18,0),"Aula 5"); }
    String cuerpo(HttpRequest peticion) throws Exception {
        var texto=new java.io.ByteArrayOutputStream();var terminado=new CompletableFuture<String>();
        peticion.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<java.nio.ByteBuffer>() {
            public void onSubscribe(Flow.Subscription suscripcion) { suscripcion.request(Long.MAX_VALUE); }
            public void onNext(java.nio.ByteBuffer datos) { byte[] bytes=new byte[datos.remaining()];datos.get(bytes);texto.writeBytes(bytes); }
            public void onError(Throwable fallo) { terminado.completeExceptionally(fallo); }
            public void onComplete() { terminado.complete(URLDecoder.decode(texto.toString(StandardCharsets.UTF_8),StandardCharsets.UTF_8)); }
        });return terminado.get(2,TimeUnit.SECONDS);
    }
    @SuppressWarnings("unchecked")
    HttpClient cliente(EnvioTwilio envio,int estado,String contenido) throws Exception {
        var cliente=mock(HttpClient.class);var respuesta=(HttpResponse<String>)mock(HttpResponse.class);
        when(respuesta.statusCode()).thenReturn(estado);when(respuesta.body()).thenReturn(contenido);
        when(cliente.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(respuesta);
        ReflectionTestUtils.setField(envio,"cliente",cliente);return cliente;
    }
    @Test void preparaSmsConNumeroInternacionalYHoraLocal() throws Exception {
        var envio=crear(true);var cliente=cliente(envio,201,"{\"sid\":\"SM"+"c".repeat(32)+"\"}");
        assertThat(envio.enviar("sms","+525512345678",datos()).estado()).isEqualTo("aceptada");
        var peticion=org.mockito.ArgumentCaptor.forClass(HttpRequest.class);verify(cliente).send(peticion.capture(),any());
        assertThat(cuerpo(peticion.getValue())).contains("To=+525512345678","From=+15551234567","02/01/2030","12:00","Aula 5").doesNotContain("ContentSid");
    }
    @Test void noEnviaCanalesDesactivadosNiTelefonosInvalidos() throws Exception {
        var envio=crear(false);var cliente=cliente(envio,201,"{}");
        assertThat(envio.enviar("sms","+525512345678",datos()).estado()).isEqualTo("omitida");verifyNoInteractions(cliente);
        var activo=crear(true);var otro=cliente(activo,201,"{}");
        assertThat(activo.enviar("sms","5512345678",datos()).codigo()).isEqualTo("telefono_invalido");verifyNoInteractions(otro);
    }
    @Test void distingueRechazoDeEnvioSinConfirmacionYNoExponeRespuestaPrivada() throws Exception {
        var envio=crear(true);cliente(envio,400,"datos privados");
        assertThat(envio.enviar("sms","+525512345678",datos()).codigo()).isEqualTo("http_400");
        cliente(envio,503,"datos privados");assertThat(envio.enviar("sms","+525512345678",datos()).estado()).isEqualTo("incierta");
        var cliente=cliente(envio,200,"{}");when(cliente.send(any(),any())).thenThrow(new java.io.IOException("datos privados"));
        assertThat(envio.enviar("sms","+525512345678",datos()).codigo()).isEqualTo("sin_confirmacion");
    }
    @Test void exigeCredencialesSoloAlHabilitarSms() {
        assertThatThrownBy(()->new EnvioTwilio(new ObjectMapper(),true,"","","","UTC","")).isInstanceOf(IllegalArgumentException.class);
        assertThat(new EnvioTwilio(new ObjectMapper(),false,"","","","UTC","").habilitado("sms")).isFalse();
    }
    @Test void smsDePruebaEnviaLaPlantillaEventNotificationsSinDatosPersonalizados() throws Exception {
        var envio=new EnvioTwilio(new ObjectMapper(),true,"AC"+"a".repeat(32),"clave-prueba","+15551234567","America/Mexico_City","sms_event_notifications");
        var cliente=cliente(envio,201,"{\"sid\":\"SM"+"c".repeat(32)+"\"}");
        assertThat(envio.enviar("sms","+525512345678",datos()).estado()).isEqualTo("aceptada");
        var peticion=org.mockito.ArgumentCaptor.forClass(HttpRequest.class);verify(cliente).send(peticion.capture(),any());
        assertThat(cuerpo(peticion.getValue())).isEqualTo("To=+525512345678&From=+15551234567&Body=sms_event_notifications");
    }
    @Test void noPermiteUnIdentificadorDePlantillaDePruebaDesconocido() {
        assertThatThrownBy(()->new EnvioTwilio(new ObjectMapper(),true,"AC"+"a".repeat(32),"clave-prueba","+15551234567","UTC","no_existe"))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("plantilla SMS de prueba");
    }

}
