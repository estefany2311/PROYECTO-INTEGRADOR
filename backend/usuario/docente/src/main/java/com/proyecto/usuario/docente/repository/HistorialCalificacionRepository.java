package com.proyecto.usuario.docente.repository;

import com.proyecto.usuario.docente.model.HistorialCalificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * T-380 (US-33): historial de calificaciones de una evidencia, del más reciente
 * al más antiguo.
 */
public interface HistorialCalificacionRepository
        extends JpaRepository<HistorialCalificacion, Long> {

    List<HistorialCalificacion> findByEvidenciaIdOrderByCreadoEnDescIdDesc(Long evidenciaId);
}
