package com.proyecto.usuario.docente.repository;

import com.proyecto.usuario.docente.model.Evidencia;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

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
}
