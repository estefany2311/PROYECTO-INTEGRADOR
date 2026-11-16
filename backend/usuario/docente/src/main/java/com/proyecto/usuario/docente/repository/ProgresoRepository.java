package com.proyecto.usuario.docente.repository;

import com.proyecto.usuario.docente.model.Progreso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// T-197: avance del alumno nivel a nivel
public interface ProgresoRepository extends JpaRepository<Progreso, Long> {

    List<Progreso> findByEstudianteId(Long estudianteId);

    Optional<Progreso> findByEstudianteIdAndNivelId(Long estudianteId, Short nivelId);
}
