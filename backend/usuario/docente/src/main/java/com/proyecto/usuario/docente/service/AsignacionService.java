package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.AsignacionResponse;
import com.proyecto.usuario.docente.dto.AsignarLecturaRequest;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Asignacion;
import com.proyecto.usuario.docente.model.Aula;
import com.proyecto.usuario.docente.model.Estudiante;
import com.proyecto.usuario.docente.model.Lectura;
import com.proyecto.usuario.docente.repository.AsignacionRepository;
import com.proyecto.usuario.docente.repository.AulaRepository;
import com.proyecto.usuario.docente.repository.EstudianteRepository;
import com.proyecto.usuario.docente.repository.LecturaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * T-192 (US-36): asigna una lectura del catálogo a un alumno concreto.
 *
 * Reglas:
 * - el aula debe existir (404) y ser del docente (403);
 * - el alumno debe pertenecer a esa aula (404);
 * - la lectura debe existir (404) y estar activa (400);
 * - un alumno no puede recibir dos veces la misma lectura (409).
 *
 * No se restringe el grado de la lectura: US-36 busca justamente adaptar
 * el contenido al nivel real del alumno, que puede estar por debajo del grado.
 */
@Service
public class AsignacionService {

    private final AsignacionRepository asignacionRepository;
    private final AulaRepository aulaRepository;
    private final EstudianteRepository estudianteRepository;
    private final LecturaRepository lecturaRepository;

    public AsignacionService(AsignacionRepository asignacionRepository,
                             AulaRepository aulaRepository,
                             EstudianteRepository estudianteRepository,
                             LecturaRepository lecturaRepository) {
        this.asignacionRepository = asignacionRepository;
        this.aulaRepository = aulaRepository;
        this.estudianteRepository = estudianteRepository;
        this.lecturaRepository = lecturaRepository;
    }

    @Transactional
    public AsignacionResponse asignar(Long docenteId, Long aulaId, AsignarLecturaRequest req) {
        buscarAulaDelDocente(docenteId, aulaId);

        Estudiante estudiante = estudianteRepository
                .findByIdAndAulaId(req.estudianteId(), aulaId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "El alumno no existe en esta aula", HttpStatus.NOT_FOUND));

        Lectura lectura = lecturaRepository.findById(req.lecturaId())
                .orElseThrow(() -> new ReglaNegocioException(
                        "La lectura no existe", HttpStatus.NOT_FOUND));

        if (!lectura.isActivo()) {
            throw new ReglaNegocioException(
                    "La lectura no está activa", HttpStatus.BAD_REQUEST);
        }

        if (asignacionRepository.existsByEstudianteIdAndLecturaId(
                req.estudianteId(), req.lecturaId())) {
            throw new ReglaNegocioException(
                    "Ese alumno ya tiene esta tarea", HttpStatus.CONFLICT);
        }

        Asignacion asignacion = new Asignacion(req.lecturaId(), estudiante);
        try {
            // flush: si dos peticiones llegan a la vez, el índice único de la BD decide
            asignacionRepository.saveAndFlush(asignacion);
        } catch (DataIntegrityViolationException e) {
            throw new ReglaNegocioException(
                    "Ese alumno ya tiene esta tarea", HttpStatus.CONFLICT);
        }

        return new AsignacionResponse(asignacion.getId(), req.lecturaId(),
                req.estudianteId(), asignacion.getFechaAsignacion());
    }

    // valida que el aula exista y pertenezca al docente autenticado
    private Aula buscarAulaDelDocente(Long docenteId, Long aulaId) {
        Aula aula = aulaRepository.findById(aulaId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "El aula no existe", HttpStatus.NOT_FOUND));
        if (!aula.getDocenteId().equals(docenteId)) {
            throw new ReglaNegocioException(
                    "El aula no pertenece al docente", HttpStatus.FORBIDDEN);
        }
        return aula;
    }
}
