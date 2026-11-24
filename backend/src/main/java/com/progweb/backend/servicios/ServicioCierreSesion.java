package com.progweb.backend.servicios;

import java.time.*;
import com.progweb.backend.repositorios.RepositorioSesionesAcceso;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioCierreSesion {
    private final RepositorioSesionesAcceso sesiones;
    public ServicioCierreSesion(RepositorioSesionesAcceso sesiones){this.sesiones=sesiones;}
    @Transactional public void cerrar(Jwt token){
        var ahora=Instant.now();
        if(token.getExpiresAt()!=null&&token.getExpiresAt().isAfter(ahora))sesiones.revocar(Long.parseLong(token.getSubject()),token.getId(),
                LocalDateTime.ofInstant(token.getExpiresAt(),ZoneOffset.UTC),LocalDateTime.ofInstant(ahora,ZoneOffset.UTC));
    }
}
