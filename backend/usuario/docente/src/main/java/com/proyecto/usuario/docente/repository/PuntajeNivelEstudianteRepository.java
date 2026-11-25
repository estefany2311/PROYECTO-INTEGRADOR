package com.proyecto.usuario.docente.repository;

import com.proyecto.usuario.docente.model.PuntajeNivelEstudiante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Mejor intento de cada alumno por nivel (migración V21).
 */
public interface PuntajeNivelEstudianteRepository extends JpaRepository<PuntajeNivelEstudiante, Long> {

    Optional<PuntajeNivelEstudiante> findByEstudianteIdAndNivelId(Long estudianteId, Short nivelId);
}
