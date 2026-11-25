package com.proyecto.usuario.docente.repository;

import com.proyecto.usuario.docente.model.PuntajeEstudiante;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * T-301 (US-09): acceso a los puntos y estrellas acumulados por alumno.
 */
public interface PuntajeEstudianteRepository extends JpaRepository<PuntajeEstudiante, Long> {

    @EntityGraph(attributePaths = "estudiante")
    Optional<PuntajeEstudiante> findByEstudianteId(Long estudianteId);
}
