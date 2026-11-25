package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.AsignacionResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Asignacion;
import com.proyecto.usuario.docente.repository.AsignacionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * T-389 (US-38): el alumno entrega una tarea asignada. La app de Karla llama a
 * este servicio cuando el alumno termina el reto de la lectura asignada; así el
 * seguimiento del docente pasa la tarea a "entregada".
 *
 * <p>Es idempotente: entregar dos veces conserva la primera fecha de entrega.
 * El alumno solo puede entregar sus propias tareas.</p>
 */
@Service
public class EntregaTareaService {

    private final AsignacionRepository asignacionRepository;

    public EntregaTareaService(AsignacionRepository asignacionRepository) {
        this.asignacionRepository = asignacionRepository;
    }

    @Transactional
    public AsignacionResponse entregar(Long estudianteId, Long asignacionId) {
        return entregar(estudianteId, asignacionId, LocalDateTime.now());
    }

    // "ahora" se recibe para poder probar entregas a tiempo y tardías
    @Transactional
    public AsignacionResponse entregar(Long estudianteId, Long asignacionId, LocalDateTime ahora) {
        Asignacion asignacion = asignacionRepository
                .findByIdAndEstudianteId(asignacionId, estudianteId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "La tarea no existe", HttpStatus.NOT_FOUND));

        if (!Asignacion.ENTREGADA.equals(asignacion.getEstado())) {
            asignacion.marcarEntregada(ahora);
        }

        return new AsignacionResponse(asignacion.getId(), asignacion.getLecturaId(),
                estudianteId, asignacion.getFechaAsignacion(),
                asignacion.getFechaLimite(), asignacion.getEstado());
    }
}
