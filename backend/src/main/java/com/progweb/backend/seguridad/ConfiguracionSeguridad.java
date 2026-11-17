package com.progweb.backend.seguridad;

import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.DispatcherType;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class ConfiguracionSeguridad {

    @Bean
    public PasswordEncoder codificadorContrasenas() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecretKey claveFirma(@Value("${seguridad.jwt.clave}") String clave) {
        byte[] contenido;
        try {
            contenido = Base64.getDecoder().decode(clave);
        } catch (IllegalArgumentException excepcion) {
            throw new IllegalArgumentException("JWT_CLAVE debe ser una clave en Base64.");
        }
        if (contenido.length < 32) {
            throw new IllegalArgumentException("JWT_CLAVE debe contener al menos 32 bytes aleatorios.");
        }
        return new SecretKeySpec(contenido, "HmacSHA256");
    }

    @Bean
    public JwtEncoder generadorJwt(SecretKey claveFirma) {
        return NimbusJwtEncoder.withSecretKey(claveFirma).build();
    }

    @Bean
    public JwtDecoder lectorJwt(SecretKey claveFirma) {
        NimbusJwtDecoder lector = NimbusJwtDecoder.withSecretKey(claveFirma)
                .macAlgorithm(MacAlgorithm.HS256).build();
        lector.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(Duration.ZERO), new JwtIssuerValidator("tutoria-api"),
                new JwtClaimValidator<List<String>>("aud",
                        audiencias -> audiencias != null && audiencias.contains("tutoria-web"))));
        return lector;
    }

    @Bean
    public SecurityFilterChain cadenaSeguridad(HttpSecurity configuracion,
            RespuestasSeguridad respuestasSeguridad, CorsConfigurationSource origenesPermitidos) throws Exception {
        JwtGrantedAuthoritiesConverter permisos = new JwtGrantedAuthoritiesConverter();
        permisos.setAuthoritiesClaimName("roles");
        permisos.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(permisos);

        return configuracion
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .cors(origenes -> origenes.configurationSource(origenesPermitidos))
                .sessionManagement(sesiones -> sesiones.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(permisosRuta -> permisosRuta
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/autenticacion/iniciar-sesion",
                                "/api/autenticacion/registrar", "/api/autenticacion/google",
                                "/api/autenticacion/recuperacion/solicitar",
                                "/api/autenticacion/recuperacion/cambiar-contrasena").permitAll()
                        .requestMatchers("/api/coordinacion/**").hasRole("coordinador")
                        .requestMatchers("/api/tutor/**").hasAnyRole("coordinador", "tutor")
                        .requestMatchers("/api/estudiante/**").hasAnyRole("coordinador", "estudiante")
                        .anyRequest().authenticated())
                .exceptionHandling(errores -> errores.authenticationEntryPoint(respuestasSeguridad)
                        .accessDeniedHandler(respuestasSeguridad))
                .oauth2ResourceServer(recursos -> recursos
                        .authenticationEntryPoint(respuestasSeguridad)
                        .accessDeniedHandler(respuestasSeguridad)
                        .jwt(token -> token.jwtAuthenticationConverter(conversor)))
                .build();
    }

    @Bean
    public CorsConfigurationSource origenesPermitidos(
            @Value("${seguridad.origenes-permitidos}") String origenes) {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(Arrays.stream(origenes.split(",")).map(String::strip)
                .filter(origen -> !origen.isEmpty()).toList());
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuracion.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource rutas = new UrlBasedCorsConfigurationSource();
        rutas.registerCorsConfiguration("/api/**", configuracion);
        return rutas;
    }
}
