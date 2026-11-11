package com.progweb.backend.repositorios;

import java.util.Optional;

import com.progweb.backend.entidades.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface RepositorioRoles extends JpaRepository<Rol, Long> {

    @Query("select rol from Rol rol where rol.codigo = :codigo")
    Optional<Rol> buscarPorCodigo(@Param("codigo") String codigo);
}
