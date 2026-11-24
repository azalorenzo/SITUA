package com.progweb.backend.servicios;
import org.junit.jupiter.api.Test;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class EnvioCodigoSmtpPruebas {
    EnvioCodigoSmtp crear(boolean activo) { return new EnvioCodigoSmtp(activo,"smtp.gmail.com",587,"cuenta@example.invalid","clave-prueba","cuenta@example.invalid",false,true,true); }
    @Test void preparaDestinatarioCodigoYVencimientoSinEnviarCorreosReales() {
        var envio=crear(true);var cliente=mock(JavaMailSenderImpl.class);ReflectionTestUtils.setField(envio,"correo",cliente);
        envio.enviar("destino@example.invalid","123456");var mensaje=ArgumentCaptor.forClass(SimpleMailMessage.class);verify(cliente).send(mensaje.capture());
        assertThat(mensaje.getValue().getTo()).containsExactly("destino@example.invalid");
        assertThat(mensaje.getValue().getFrom()).isEqualTo("cuenta@example.invalid");
        assertThat(mensaje.getValue().getText()).contains("123456","10 minutos","una vez");
    }
    @Test void errorDelProveedorNoExponeCredenciales() {
        var envio=crear(true);var cliente=mock(JavaMailSenderImpl.class);ReflectionTestUtils.setField(envio,"correo",cliente);
        doThrow(new MailAuthenticationException("datos privados del proveedor")).when(cliente).send(any(SimpleMailMessage.class));
        assertThatThrownBy(()->envio.enviar("destino@example.invalid","123456")).hasMessage("No se pudo enviar el correo. Inténtalo más tarde.");
    }
    @Test void exigeConfiguracionYPermiteArrancarDeshabilitado() {
        assertThatThrownBy(()->new EnvioCodigoSmtp(true,"",587,"","","",false,true,true)).isInstanceOf(IllegalArgumentException.class);
        var envio=crear(false);assertThat(envio.habilitado()).isFalse();
        assertThatThrownBy(()->envio.enviar("destino@example.invalid","123456")).hasMessageContaining("no está disponible");
    }
    @Test void permitePostfixLocalSinUsuarioClaveNiTls() {
        var envio=new EnvioCodigoSmtp(true,"127.0.0.1",25,"","","tutoria@example.invalid",false,false,false);
        var cliente=(JavaMailSenderImpl)ReflectionTestUtils.getField(envio,"correo");
        assertThat(envio.habilitado()).isTrue();
        assertThat(cliente.getJavaMailProperties().getProperty("mail.smtp.auth")).isEqualTo("false");
        assertThat(cliente.getJavaMailProperties().getProperty("mail.smtp.starttls.required")).isEqualTo("false");
    }

}
