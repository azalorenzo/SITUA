package com.progweb.backend.seguridad;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import com.progweb.backend.errores.AccesoRechazado;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

@Component
public class VerificadorGoogle {
    private final NimbusJwtDecoder lector;

    @Autowired
    public VerificadorGoogle(@Value("${seguridad.google.cliente-id:}") String cliente) {
        this(cliente, cliente.isBlank() ? null : NimbusJwtDecoder
                .withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .jwsAlgorithm(SignatureAlgorithm.RS256).build());
    }

    VerificadorGoogle(String cliente, NimbusJwtDecoder lector) {
        this.lector = lector;
        if (lector != null) {
            lector.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                    new JwtTimestampValidator(Duration.ZERO),
                    new JwtClaimValidator<String>("iss", emisor ->
                            emisor != null && List.of("https://accounts.google.com", "accounts.google.com").contains(emisor)),
                    new JwtClaimValidator<List<String>>("aud", audiencias ->
                            audiencias != null && audiencias.contains(cliente))));
        }
    }

    public DatosGoogle verificar(String credencial, String comprobacion) {
        if (lector == null) {
            throw new AccesoRechazado(503, "El acceso con Google todavía no está configurado.");
        }
        try {
            Jwt token = lector.decode(credencial);
            if (token.getExpiresAt() == null || !token.getExpiresAt().isAfter(Instant.now())
                    || !Boolean.TRUE.equals(token.getClaim("email_verified"))
                    || !comprobacion.equals(token.getClaimAsString("nonce"))) throw rechazo();
            String identificador = token.getSubject();
            String correo = token.getClaimAsString("email");
            if (identificador == null || !identificador.matches("[!-~]{1,255}")
                    || correo == null || correo.length() > 254
                    || !correo.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw rechazo();
            return new DatosGoogle(identificador, correo, token.getClaimAsString("name"),
                    token.getClaimAsString("picture"));
        } catch (JwtException | IllegalArgumentException excepcion) {
            throw rechazo();
        }
    }

    private AccesoRechazado rechazo() {
        return new AccesoRechazado(401, "No se pudo verificar tu cuenta con Google. Intenta nuevamente.");
    }
}
