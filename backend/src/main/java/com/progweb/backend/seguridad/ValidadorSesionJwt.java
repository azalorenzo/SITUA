package com.progweb.backend.seguridad;

import com.progweb.backend.repositorios.RepositorioSesionesAcceso;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class ValidadorSesionJwt implements OAuth2TokenValidator<Jwt> {
    private final RepositorioSesionesAcceso sesiones;
    public ValidadorSesionJwt(RepositorioSesionesAcceso sesiones){this.sesiones=sesiones;}
    @Override public OAuth2TokenValidatorResult validate(Jwt token){
        var rechazo=OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token","La sesión ya no es válida.",null));
        try{
            long usuario=Long.parseLong(token.getSubject());
            String identificador=token.getId();
            if(identificador==null||identificador.isBlank()||identificador.length()>128)return rechazo;
            long version=token.hasClaim("versionSesion")?Long.parseLong(token.getClaimAsString("versionSesion")):0;
            Long actual=sesiones.version(usuario);
            return actual!=null&&actual==version&&!sesiones.revocado(identificador)?OAuth2TokenValidatorResult.success():rechazo;
        }catch(IllegalArgumentException fallo){return rechazo;}
    }
}
