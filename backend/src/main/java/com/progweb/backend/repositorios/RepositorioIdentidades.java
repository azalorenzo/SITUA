package com.progweb.backend.repositorios;

import java.util.Optional;
import com.progweb.backend.entidades.IdentidadExterna;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioIdentidades extends JpaRepository<IdentidadExterna, Long> {
    @Query("select distinct identidad from IdentidadExterna identidad join fetch identidad.usuario usuario "
            + "left join fetch usuario.roles where identidad.proveedor = 'google' "
            + "and identidad.identificadorExterno = :identificador")
    Optional<IdentidadExterna> buscarGoogle(@Param("identificador") String identificador);

    @Query("select identidad from IdentidadExterna identidad where identidad.usuario.id = :usuario "
            + "and identidad.proveedor = 'google'")
    Optional<IdentidadExterna> buscarGoogleDeUsuario(@Param("usuario") Long usuario);
}
