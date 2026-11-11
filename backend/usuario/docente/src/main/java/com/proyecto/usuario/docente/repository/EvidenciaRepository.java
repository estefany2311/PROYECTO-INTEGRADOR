package com.proyecto.usuario.docente.repository;

import com.proyecto.usuario.docente.model.Evidencia;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * T-296 (US-31): acceso a las evidencias de audio y texto.
 * La consulta de pendientes por aula se agrega en T-298.
 */
public interface EvidenciaRepository extends JpaRepository<Evidencia, Long> {
}
