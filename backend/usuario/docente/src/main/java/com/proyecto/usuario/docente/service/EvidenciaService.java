package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.EvidenciaDetalleResponse;
import com.proyecto.usuario.docente.dto.EvidenciaPendienteResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Aula;
import com.proyecto.usuario.docente.model.Evidencia;
import com.proyecto.usuario.docente.model.Estudiante;
import com.proyecto.usuario.docente.repository.AulaRepository;
import com.proyecto.usuario.docente.repository.EvidenciaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * T-298 (US-31): lista de evidencias pendientes de revisión de un aula.
 *
 * <p>Devuelve solo las evidencias en estado {@code pendiente}, de la más
 * reciente a la más antigua, con el alumno, su grado y el nivel ya resueltos.
 * El filtro opcional por tipo ({@code audio} o {@code texto}) lo usa la
 * pantalla de T-299.</p>
 *
 * <p>T-371 (US-32): {@link #detalle} devuelve una evidencia concreta del aula,
 * en cualquier estado, con la transcripción y el puntaje de la IA.</p>
 */
@Service
public class EvidenciaService {

    private static final List<String> TIPOS_VALIDOS =
            List.of(Evidencia.AUDIO, Evidencia.TEXTO);

    private final AulaRepository aulaRepository;
    private final EvidenciaRepository evidenciaRepository;

    public EvidenciaService(AulaRepository aulaRepository,
                            EvidenciaRepository evidenciaRepository) {
        this.aulaRepository = aulaRepository;
        this.evidenciaRepository = evidenciaRepository;
    }

    @Transactional(readOnly = true)
    public List<EvidenciaPendienteResponse> pendientes(Long docenteId, Long aulaId, String tipo) {
        buscarAulaDelDocente(docenteId, aulaId);
        String tipoFiltro = normalizarTipo(tipo);

        List<Evidencia> evidencias = tipoFiltro == null
                ? evidenciaRepository
                        .findByEstudianteAulaIdAndEstadoRevisionOrderByCreadoEnDescIdDesc(
                                aulaId, Evidencia.PENDIENTE)
                : evidenciaRepository
                        .findByEstudianteAulaIdAndEstadoRevisionAndTipoOrderByCreadoEnDescIdDesc(
                                aulaId, Evidencia.PENDIENTE, tipoFiltro);

        return evidencias.stream()
                .map(this::respuesta)
                .toList();
    }

    // T-371 (US-32): detalle de una evidencia del aula, en cualquier estado
    @Transactional(readOnly = true)
    public EvidenciaDetalleResponse detalle(Long docenteId, Long aulaId, Long evidenciaId) {
        buscarAulaDelDocente(docenteId, aulaId);
        Evidencia evidencia = evidenciaRepository
                .findByIdAndEstudianteAulaId(evidenciaId, aulaId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "La evidencia no existe en esta aula", HttpStatus.NOT_FOUND));
        return detalleRespuesta(evidencia);
    }

    private EvidenciaDetalleResponse detalleRespuesta(Evidencia evidencia) {
        Estudiante estudiante = evidencia.getEstudiante();
        return new EvidenciaDetalleResponse(
                evidencia.getId(),
                estudiante.getId(),
                estudiante.getNombres(),
                estudiante.getApellidos(),
                estudiante.getGrado().getId(),
                estudiante.getGrado().getNombre(),
                evidencia.getNivel().getOrden(),
                evidencia.getNivel().getNombre(),
                evidencia.getNivel().getTipoReto(),
                evidencia.getTipo(),
                evidencia.getAudioUrl(),
                evidencia.getTexto(),
                evidencia.getTranscripcion(),
                palabrasMarcadas(evidencia.getPalabrasMarcadas()),
                evidencia.getPuntajeIa(),
                evidencia.getResultadoIa(),
                evidencia.getEstadoRevision(),
                evidencia.getCreadoEn(),
                evidencia.getRevisadoEn());
    }

    // T-375 (US-32): la base guarda las palabras marcadas separadas por comas;
    // el contrato las expone como lista y descarta espacios y duplicados.
    private List<String> palabrasMarcadas(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        return Arrays.stream(texto.split(","))
                .map(String::trim)
                .filter(palabra -> !palabra.isEmpty())
                .distinct()
                .toList();
    }

    private EvidenciaPendienteResponse respuesta(Evidencia evidencia) {
        Estudiante estudiante = evidencia.getEstudiante();
        return new EvidenciaPendienteResponse(
                evidencia.getId(),
                estudiante.getId(),
                estudiante.getNombres(),
                estudiante.getApellidos(),
                estudiante.getGrado().getId(),
                estudiante.getGrado().getNombre(),
                evidencia.getNivel().getOrden(),
                evidencia.getNivel().getNombre(),
                evidencia.getTipo(),
                evidencia.getAudioUrl(),
                evidencia.getTexto(),
                evidencia.getResultadoIa(),
                evidencia.getCreadoEn());
    }

    private String normalizarTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return null;
        }
        String normalizado = tipo.trim().toLowerCase(Locale.ROOT);
        if (!TIPOS_VALIDOS.contains(normalizado)) {
            throw new ReglaNegocioException(
                    "El tipo de evidencia no es válido", HttpStatus.BAD_REQUEST);
        }
        return normalizado;
    }

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
