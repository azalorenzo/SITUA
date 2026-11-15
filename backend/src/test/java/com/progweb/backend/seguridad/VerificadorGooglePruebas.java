package com.progweb.backend.seguridad;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.progweb.backend.errores.AccesoRechazado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import static org.assertj.core.api.Assertions.*;

class VerificadorGooglePruebas {
    KeyPair llaves;
    VerificadorGoogle verificador;
    @BeforeEach void preparar() throws Exception {
        var generador = KeyPairGenerator.getInstance("RSA"); generador.initialize(2048); llaves = generador.generateKeyPair();
        verificador = new VerificadorGoogle("cliente-prueba", NimbusJwtDecoder.withPublicKey((RSAPublicKey) llaves.getPublic()).build());
    }
    String firmar(String cambio, KeyPair claves) {
        var clave = new RSAKey.Builder((RSAPublicKey) claves.getPublic()).privateKey((RSAPrivateKey) claves.getPrivate()).keyID("prueba").build();
        var generador = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(clave)));
        var contenido = JwtClaimsSet.builder().issuer(cambio.equals("emisor") ? "https://otro.example" : "https://accounts.google.com")
            .audience(java.util.List.of(cambio.equals("audiencia") ? "otro-cliente" : "cliente-prueba"))
            .subject("identificador-google").issuedAt(Instant.now().minusSeconds(600))
            .expiresAt(cambio.equals("vencimiento") ? Instant.now().minusSeconds(60) : Instant.now().plusSeconds(600))
            .claim("email", "estudiante@example.invalid").claim("email_verified", !cambio.equals("correo"))
            .claim("nonce", cambio.equals("comprobacion") ? "otra-comprobacion" : "comprobacion-prueba").build();
        return generador.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).keyId("prueba").build(), contenido)).getTokenValue();
    }
    @Test void verificaFirmaYDatosDeGoogle() {
        var datos = verificador.verificar(firmar("ninguno", llaves), "comprobacion-prueba");
        assertThat(datos.identificador()).isEqualTo("identificador-google");
        assertThat(datos.correo()).isEqualTo("estudiante@example.invalid");
    }
    @ParameterizedTest @ValueSource(strings = {"emisor", "audiencia", "vencimiento", "correo", "comprobacion"})
    void rechazaCredencialesConDatosIncorrectos(String cambio) {
        assertThatThrownBy(() -> verificador.verificar(firmar(cambio, llaves), "comprobacion-prueba")).isInstanceOf(AccesoRechazado.class);
    }
    @Test void rechazaFirmaDeOtraLlave() throws Exception {
        var generador = KeyPairGenerator.getInstance("RSA"); generador.initialize(2048);
        var otras = generador.generateKeyPair();
        assertThatThrownBy(() -> verificador.verificar(firmar("ninguno", otras), "comprobacion-prueba")).isInstanceOf(AccesoRechazado.class);
    }
    @Test void informaQueFaltaConfigurarGoogle() {
        assertThatThrownBy(() -> new VerificadorGoogle("").verificar("credencial", "comprobacion-prueba"))
            .isInstanceOf(AccesoRechazado.class).hasMessageContaining("no está configurado");
    }
}
