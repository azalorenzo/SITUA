package com.progweb.backend.repositorios;

import java.util.Optional;
import com.progweb.backend.entidades.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioEstudiantes extends JpaRepository<Estudiante, Long> {
    @Query("select estudiante from Estudiante estudiante where estudiante.matricula = :matricula")
    Optional<Estudiante> buscarPorMatricula(@Param("matricula") String matricula);
}
