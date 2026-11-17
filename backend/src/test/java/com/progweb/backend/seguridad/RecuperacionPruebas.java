package com.progweb.backend.seguridad;
import java.time.*;
import com.progweb.backend.servicios.*;
import com.progweb.backend.solicitudes.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest
@Transactional
class RecuperacionPruebas {
    @Autowired ServicioRecuperacion servicio;
    @Autowired JdbcTemplate consultas;
    @Autowired PasswordEncoder codificador;
    @MockitoBean EnvioCodigoRecuperacion envio;
    @MockitoBean(name="relojInicio") Clock reloj;
    long usuario;
    final String correo="recuperacion-prueba@example.invalid";
    @BeforeEach void preparar() {
        when(reloj.instant()).thenReturn(Instant.parse("2026-01-01T12:00:00Z"));
        when(envio.habilitado()).thenReturn(true);
        consultas.update("INSERT INTO usuarios(nombre,correo,password_hash) VALUES (?,?,?)","Recuperación prueba",correo,codificador.encode("Anterior!123"));
        usuario=consultas.queryForObject("SELECT id FROM usuarios WHERE correo=?",Long.class,correo);
    }
    String solicitar() {
        servicio.solicitar(new SolicitudCodigoRecuperacion(correo));
        var codigo=ArgumentCaptor.forClass(String.class);verify(envio).enviar(eq(correo),codigo.capture());return codigo.getValue();
    }
    SolicitudCambiarContrasena cambio(String codigo) { return new SolicitudCambiarContrasena(correo,codigo,"NuevaClave!123","NuevaClave!123"); }
    @Test void codigoProtegidoCambiaClaveUnaVez() {
        String codigo=solicitar();assertThat(codigo).matches("[0-9]{6}");
        String guardado=consultas.queryForObject("SELECT codigo_hash FROM recuperaciones_contrasena WHERE usuario_id=?",String.class,usuario);
        assertThat(guardado).isNotEqualTo(codigo);assertThat(codificador.matches(codigo,guardado)).isTrue();
        servicio.cambiar(cambio(codigo));
        assertThat(codificador.matches("NuevaClave!123",consultas.queryForObject("SELECT password_hash FROM usuarios WHERE id=?",String.class,usuario))).isTrue();
        assertThatThrownBy(()->servicio.cambiar(cambio(codigo))).hasMessageContaining("código no es válido");
    }
    @Test void cincoFallosBloqueanInclusoElCodigoCorrecto() {
        String codigo=solicitar();String incorrecto=codigo.equals("000000")?"111111":"000000";
        for(int intento=0;intento<5;intento++)assertThatThrownBy(()->servicio.cambiar(cambio(incorrecto))).isInstanceOf(RuntimeException.class);
        assertThat(consultas.queryForObject("SELECT intentos FROM recuperaciones_contrasena WHERE usuario_id=?",Integer.class,usuario)).isEqualTo(5);
        assertThatThrownBy(()->servicio.cambiar(cambio(codigo))).isInstanceOf(RuntimeException.class);
    }
    @Test void codigoVenceALosDiezMinutos() {
        String codigo=solicitar();when(reloj.instant()).thenReturn(Instant.parse("2026-01-01T12:10:00Z"));
        assertThatThrownBy(()->servicio.cambiar(cambio(codigo))).isInstanceOf(RuntimeException.class);
    }
    @Test void esperaEntreSolicitudesYReemplazaCodigoAnterior() {
        String anterior=solicitar();servicio.solicitar(new SolicitudCodigoRecuperacion(correo));verify(envio,times(1)).enviar(anyString(),anyString());
        when(reloj.instant()).thenReturn(Instant.parse("2026-01-01T12:01:00Z"));servicio.solicitar(new SolicitudCodigoRecuperacion(correo));verify(envio,times(2)).enviar(anyString(),anyString());
    }
    @Test void noEnviaParaGoogleInactivoODesconocido() {
        assertThatThrownBy(()->servicio.solicitar(new SolicitudCodigoRecuperacion("no-existe@example.invalid"))).hasMessageContaining("No hay una cuenta");
        consultas.update("UPDATE usuarios SET password_hash=NULL WHERE id=?",usuario);assertThatThrownBy(()->servicio.solicitar(new SolicitudCodigoRecuperacion(correo))).isInstanceOf(RuntimeException.class);
        consultas.update("UPDATE usuarios SET password_hash=?,estado='inactivo' WHERE id=?",codificador.encode("Anterior!123"),usuario);assertThatThrownBy(()->servicio.solicitar(new SolicitudCodigoRecuperacion(correo))).isInstanceOf(RuntimeException.class);
        verify(envio,never()).enviar(anyString(),anyString());
    }
    @Test void confirmaClaveYRechazaCorreoNoConfigurado() {
        String codigo=solicitar();assertThatThrownBy(()->servicio.cambiar(new SolicitudCambiarContrasena(correo,codigo,"NuevaClave!123","otra"))).hasMessageContaining("no coinciden");
        when(envio.habilitado()).thenReturn(false);assertThatThrownBy(()->servicio.solicitar(new SolicitudCodigoRecuperacion(correo))).hasMessageContaining("todavía no está disponible");
    }
}
