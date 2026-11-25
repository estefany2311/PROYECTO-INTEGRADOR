package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.ComentarioEvidenciaResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Evidencia;
import com.proyecto.usuario.docente.repository.EvidenciaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * T-386 (US-34): el alumno lee el comentario que su docente dejó en una de sus
 * evidencias. El id del alumno sale del token, y solo puede ver sus propias
 * evidencias.
 */
@Service
public class ComentarioEstudianteService {

    private final EvidenciaRepository evidenciaRepository;

    public ComentarioEstudianteService(EvidenciaRepository evidenciaRepository) {
        this.evidenciaRepository = evidenciaRepository;
    }

    @Transactional(readOnly = true)
    public ComentarioEvidenciaResponse obtenerComentario(Long estudianteId, Long evidenciaId) {
        Evidencia evidencia = evidenciaRepository
                .findByIdAndEstudianteId(evidenciaId, estudianteId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "La evidencia no existe", HttpStatus.NOT_FOUND));
        return new ComentarioEvidenciaResponse(evidencia.getId(), evidencia.getComentario());
    }
}
