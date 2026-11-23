package com.proyecto.usuario.docente.repository;

import com.proyecto.usuario.docente.model.Evidencia;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * T-296 (US-31): acceso a las evidencias de audio y texto.
 * T-298 (US-31): consultas de la lista de pendientes de un aula, con el alumno,
 * su grado y el nivel cargados en la misma consulta.
 */
public interface EvidenciaRepository extends JpaRepository<Evidencia, Long> {

    @EntityGraph(attributePaths = {"estudiante", "estudiante.grado", "nivel"})
    List<Evidencia> findByEstudianteAulaIdAndEstadoRevisionOrderByCreadoEnDescIdDesc(
            Long aulaId, String estadoRevision);

    @EntityGraph(attributePaths = {"estudiante", "estudiante.grado", "nivel"})
    List<Evidencia> findByEstudianteAulaIdAndEstadoRevisionAndTipoOrderByCreadoEnDescIdDesc(
            Long aulaId, String estadoRevision, String tipo);

    // T-371 (US-32): detalle de una evidencia del aula, sin importar su estado.
    // El filtro por aula evita que un docente lea evidencias de otro.
    @EntityGraph(attributePaths = {"estudiante", "estudiante.grado", "nivel"})
    Optional<Evidencia> findByIdAndEstudianteAulaId(Long id, Long aulaId);

    // T-386 (US-34): el alumno solo puede leer sus propias evidencias
    Optional<Evidencia> findByIdAndEstudianteId(Long id, Long estudianteId);

    // puntos y estrellas: los intentos de un alumno en un nivel
    List<Evidencia> findByEstudianteIdAndNivelId(Long estudianteId, Short nivelId);

    // puntos y estrellas: todos los intentos de un alumno, con el nivel cargado
    @EntityGraph(attributePaths = {"nivel"})
    List<Evidencia> findByEstudianteId(Long estudianteId);
}
