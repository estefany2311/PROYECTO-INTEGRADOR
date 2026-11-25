package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.CorregirEvidenciaRequest;
import com.proyecto.usuario.docente.dto.EvidenciaDetalleResponse;
import com.proyecto.usuario.docente.dto.EvidenciaPendienteResponse;
import com.proyecto.usuario.docente.dto.GuardarComentarioRequest;
import com.proyecto.usuario.docente.dto.HistorialCalificacionResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Aula;
import com.proyecto.usuario.docente.model.Evidencia;
import com.proyecto.usuario.docente.model.Estudiante;
import com.proyecto.usuario.docente.model.HistorialCalificacion;
import com.proyecto.usuario.docente.repository.AulaRepository;
import com.proyecto.usuario.docente.repository.EvidenciaRepository;
import com.proyecto.usuario.docente.repository.HistorialCalificacionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
 *
 * <p>T-378 (US-33): {@link #aprobar} acepta la nota de la IA tal como llegó y
 * {@link #corregir} guarda la nota final del docente con su motivo.</p>
 *
 * <p>T-379 (US-33): al aprobar o corregir se recalculan los puntos y las
 * estrellas del alumno con la nota final.</p>
 *
 * <p>T-384 (US-34): {@link #guardarComentario} guarda la retroalimentación que
 * el docente deja en la evidencia.</p>
 *
 * <p>T-380 (US-33): cada aprobación o corrección deja una fila en el historial
 * de calificaciones, y el detalle devuelve ese historial.</p>
 */
@Service
public class EvidenciaService {

    private static final List<String> TIPOS_VALIDOS =
            List.of(Evidencia.AUDIO, Evidencia.TEXTO);

    private final AulaRepository aulaRepository;
    private final EvidenciaRepository evidenciaRepository;
    private final PuntajeService puntajeService;
    private final HistorialCalificacionRepository historialRepository;

    public EvidenciaService(AulaRepository aulaRepository,
                            EvidenciaRepository evidenciaRepository,
                            PuntajeService puntajeService,
                            HistorialCalificacionRepository historialRepository) {
        this.aulaRepository = aulaRepository;
        this.evidenciaRepository = evidenciaRepository;
        this.puntajeService = puntajeService;
        this.historialRepository = historialRepository;
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
        return detalleRespuesta(evidenciaDelAula(docenteId, aulaId, evidenciaId));
    }

    // T-378 (US-33): aprueba la nota de la IA y la deja como calificación final
    @Transactional
    public EvidenciaDetalleResponse aprobar(Long docenteId, Long aulaId, Long evidenciaId) {
        Evidencia evidencia = evidenciaDelAula(docenteId, aulaId, evidenciaId);
        if (evidencia.getPuntajeIa() == null) {
            throw new ReglaNegocioException(
                    "La evidencia no tiene evaluación de la IA", HttpStatus.BAD_REQUEST);
        }
        evidencia.setCalificacionFinal(evidencia.getPuntajeIa());
        evidencia.setMotivoCorreccion(null);
        evidencia.setEstadoRevision(Evidencia.APROBADA);
        evidencia.setRevisadoEn(LocalDateTime.now());
        recalcularPuntaje(evidencia);
        registrarHistorial(docenteId, evidencia);
        return detalleRespuesta(evidencia);
    }

    // T-378 (US-33): corrige la nota de la IA con la nota final y el motivo
    @Transactional
    public EvidenciaDetalleResponse corregir(Long docenteId, Long aulaId, Long evidenciaId,
                                             CorregirEvidenciaRequest req) {
        Evidencia evidencia = evidenciaDelAula(docenteId, aulaId, evidenciaId);
        evidencia.setCalificacionFinal(req.calificacionFinal());
        evidencia.setMotivoCorreccion(req.motivo().trim());
        evidencia.setEstadoRevision(Evidencia.CORREGIDA);
        evidencia.setRevisadoEn(LocalDateTime.now());
        recalcularPuntaje(evidencia);
        registrarHistorial(docenteId, evidencia);
        return detalleRespuesta(evidencia);
    }

    // T-384 (US-34): el docente deja su comentario de retroalimentación
    @Transactional
    public EvidenciaDetalleResponse guardarComentario(Long docenteId, Long aulaId, Long evidenciaId,
                                                      GuardarComentarioRequest req) {
        Evidencia evidencia = evidenciaDelAula(docenteId, aulaId, evidenciaId);
        evidencia.setComentario(req.comentario().trim());
        return detalleRespuesta(evidencia);
    }

    // T-379 (US-33): la nota del docente cambia lo que el nivel aporta al total del
    // alumno: se recalculan sus puntos y estrellas con el mejor intento del nivel.
    private void recalcularPuntaje(Evidencia evidencia) {
        puntajeService.recalcularNivel(evidencia.getEstudiante().getId(), evidencia.getNivel());
    }

    // T-380 (US-33): deja constancia de la revisión con la nota de la IA y la
    // nota final del docente, para conservar el historial completo.
    private void registrarHistorial(Long docenteId, Evidencia evidencia) {
        historialRepository.save(HistorialCalificacion.registrar(
                evidencia,
                docenteId,
                evidencia.getEstadoRevision(),
                evidencia.getPuntajeIa(),
                evidencia.getCalificacionFinal(),
                evidencia.getMotivoCorreccion()));
    }

    // T-380 (US-33): historial del más reciente al más antiguo
    private List<HistorialCalificacionResponse> historialDe(Evidencia evidencia) {
        return historialRepository
                .findByEvidenciaIdOrderByCreadoEnDescIdDesc(evidencia.getId())
                .stream()
                .map(historial -> new HistorialCalificacionResponse(
                        historial.getId(),
                        historial.getDocenteId(),
                        historial.getNotaIa(),
                        historial.getNotaDocente(),
                        historial.getMotivo(),
                        historial.getAccion(),
                        historial.getCreadoEn()))
                .toList();
    }

    private Evidencia evidenciaDelAula(Long docenteId, Long aulaId, Long evidenciaId) {
        buscarAulaDelDocente(docenteId, aulaId);
        return evidenciaRepository.findByIdAndEstudianteAulaId(evidenciaId, aulaId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "La evidencia no existe en esta aula", HttpStatus.NOT_FOUND));
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
                evidencia.getCalificacionFinal(),
                evidencia.getMotivoCorreccion(),
                evidencia.getComentario(),
                evidencia.getResultadoIa(),
                evidencia.getEstadoRevision(),
                evidencia.getCreadoEn(),
                evidencia.getRevisadoEn(),
                historialDe(evidencia));
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
