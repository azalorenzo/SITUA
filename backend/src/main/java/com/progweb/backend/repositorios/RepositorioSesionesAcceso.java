package com.progweb.backend.repositorios;

import java.time.LocalDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioSesionesAcceso {
    private final JdbcTemplate consultas;
    public RepositorioSesionesAcceso(JdbcTemplate consultas){this.consultas=consultas;}
    public Long version(long usuario){return consultas.queryForList("SELECT version_sesion FROM usuarios WHERE id=?",Long.class,usuario).stream().findFirst().orElse(null);}
    public boolean revocado(String identificador){return consultas.queryForObject("SELECT COUNT(*) FROM tokens_revocados WHERE jti=?",Long.class,identificador)>0;}
    public void revocar(long usuario,String identificador,LocalDateTime vence,LocalDateTime ahora){
        consultas.update("INSERT IGNORE INTO tokens_revocados(usuario_id,jti,vence_en,revocado_en,motivo) VALUES (?,?,?,?,'logout')",usuario,identificador,vence,ahora);
    }
}
