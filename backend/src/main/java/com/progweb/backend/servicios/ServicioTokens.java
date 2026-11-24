package com.progweb.backend.servicios;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import com.progweb.backend.respuestas.RespuestaInicioSesion;
import com.progweb.backend.respuestas.RespuestaUsuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class ServicioTokens {
    private final JwtEncoder generador;
    private final long minutosVigencia;
    private final com.progweb.backend.repositorios.RepositorioSesionesAcceso sesiones;

    public ServicioTokens(JwtEncoder generador,com.progweb.backend.repositorios.RepositorioSesionesAcceso sesiones,
            @Value("${seguridad.jwt.minutos-vigencia}") long minutosVigencia) {
        if (minutosVigencia < 1 || minutosVigencia > 1440) {
            throw new IllegalArgumentException("La duración del token debe estar entre 1 y 1440 minutos.");
        }
        this.generador = generador;
        this.sesiones = sesiones;
        this.minutosVigencia = minutosVigencia;
    }

    public RespuestaInicioSesion crearToken(RespuestaUsuario usuario) {
        Instant ahora = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant vencimiento = ahora.plus(minutosVigencia, ChronoUnit.MINUTES);
        Long version=sesiones.version(usuario.id());
        JwtClaimsSet datos = JwtClaimsSet.builder()
                .issuer("tutoria-api")
                .audience(java.util.List.of("tutoria-web"))
                .subject(usuario.id().toString())
                .issuedAt(ahora)
                .expiresAt(vencimiento)
                .id(UUID.randomUUID().toString())
                .claim("roles", usuario.roles())
                .claim("versionSesion",version==null?0:version)
                .build();
        String token = generador.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), datos)).getTokenValue();
        return new RespuestaInicioSesion(token, "Bearer", vencimiento, usuario);
    }
}
