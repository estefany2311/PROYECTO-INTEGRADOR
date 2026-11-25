package com.proyecto.usuario.docente.repository;

import com.proyecto.usuario.docente.model.Asignacion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * T-191: acceso a las tareas asignadas.
 * Las consultas por alumno son las que usará la tablet y el modal de T-193.
 */
public interface AsignacionRepository extends JpaRepository<Asignacion, Long> {

    List<Asignacion> findByEstudianteIdOrderByFechaAsignacionDesc(Long estudianteId);

    // evita asignar dos veces la misma lectura al mismo alumno
    boolean existsByEstudianteIdAndLecturaId(Long estudianteId, Long lecturaId);

    // T-389 (US-38): todas las tareas de los alumnos del aula, con el alumno y su grado
    @EntityGraph(attributePaths = {"estudiante", "estudiante.grado"})
    List<Asignacion> findByEstudianteAulaId(Long aulaId);

    // T-389 (US-38): el alumno solo entrega sus propias tareas
    Optional<Asignacion> findByIdAndEstudianteId(Long id, Long estudianteId);
}
