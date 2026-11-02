package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.PromedioCompetenciaResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Aula;
import com.proyecto.usuario.docente.repository.AulaRepository;
import com.proyecto.usuario.docente.repository.EvaluacionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * T-276 (US-27): consulta del rendimiento del aula para el dashboard docente.
 * Devuelve el promedio de cada alumno activo en cada competencia.
 */
@Service
public class RendimientoService {

    private final EvaluacionRepository evaluacionRepository;
    private final AulaRepository aulaRepository;

    public RendimientoService(EvaluacionRepository evaluacionRepository,
                              AulaRepository aulaRepository) {
        this.evaluacionRepository = evaluacionRepository;
        this.aulaRepository = aulaRepository;
    }

    @Transactional(readOnly = true)
    public List<PromedioCompetenciaResponse> promediosPorAula(Long docenteId, Long aulaId) {
        Aula aula = aulaRepository.findById(aulaId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "El aula no existe", HttpStatus.NOT_FOUND));
        if (!aula.getDocenteId().equals(docenteId)) {
            throw new ReglaNegocioException(
                    "El aula no pertenece al docente", HttpStatus.FORBIDDEN);
        }

        return evaluacionRepository.promediosPorAula(aulaId).stream()
                .map(p -> new PromedioCompetenciaResponse(
                        p.getEstudianteId(), p.getCompetencia(), p.getPromedio()))
                .toList();
    }
}
