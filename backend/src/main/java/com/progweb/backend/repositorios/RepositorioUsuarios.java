package com.progweb.backend.repositorios;

import java.util.Optional;

import com.progweb.backend.entidades.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface RepositorioUsuarios extends JpaRepository<Usuario, Long> {

    @Query("select usuario from Usuario usuario where usuario.correo = :correo")
    Optional<Usuario> buscarPorCorreo(@Param("correo") String correo);

    // En esta consulta traemos también los roles para usarlos juntos.
    @Query("select distinct usuario from Usuario usuario left join fetch usuario.roles "
            + "where usuario.correo = :correo")
    Optional<Usuario> buscarPorCorreoConRoles(@Param("correo") String correo);
}
