package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.CorregirEvidenciaRequest;
import com.proyecto.usuario.docente.dto.EvidenciaDetalleResponse;
import com.proyecto.usuario.docente.dto.EvidenciaPendienteResponse;
import com.proyecto.usuario.docente.dto.GuardarComentarioRequest;
import com.proyecto.usuario.docente.service.AudioEvidenciaService;
import com.proyecto.usuario.docente.service.AudioEvidenciaService.ArchivoAudio;
import com.proyecto.usuario.docente.service.EvidenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

/**
 * T-298 (US-31): evidencias pendientes de revisión del aula.
 * GET /api/docente/aulas/{aulaId}/evidencias
 *
 * <p>Devuelve las evidencias en estado {@code pendiente} de la más reciente a
 * la más antigua. {@code ?tipo=audio} o {@code ?tipo=texto} limita la lista a
 * ese tipo; la pantalla de T-299 usa el filtro. Requiere X-Docente-Id.</p>
 *
 * <p>T-371 (US-32): GET /api/docente/aulas/{aulaId}/evidencias/{evidenciaId}
 * devuelve el detalle de una evidencia (audio o texto, transcripción y puntaje
 * de la IA) para contrastar la evaluación automática.</p>
 *
 * <p>T-372 (US-32): GET .../evidencias/{evidenciaId}/audio entrega el archivo
 * de audio solo al docente dueño del aula, con soporte de Range para que el
 * reproductor pueda pedir partes del archivo.</p>
 *
 * <p>T-378 (US-33): POST .../evidencias/{evidenciaId}/aprobar acepta la nota de
 * la IA y POST .../evidencias/{evidenciaId}/corregir la cambia por la nota del
 * docente con su motivo.</p>
 *
 * <p>T-384 (US-34): PUT .../evidencias/{evidenciaId}/comentario guarda el
 * comentario de retroalimentación del docente.</p>
 */
@RestController
@RequestMapping("/api/docente/aulas/{aulaId}/evidencias")
@Tag(name = "Evidencias", description = "Evidencias de audio y texto pendientes de revisión.")
@SecurityRequirement(name = "docenteId")
public class EvidenciaController {

    private final EvidenciaService service;
    private final AudioEvidenciaService audioService;

    public EvidenciaController(EvidenciaService service, AudioEvidenciaService audioService) {
        this.service = service;
        this.audioService = audioService;
    }

    @GetMapping
    @Operation(summary = "Lista las evidencias pendientes (filtro opcional: tipo=audio|texto)")
    public List<EvidenciaPendienteResponse> pendientes(
            @RequestHeader("X-Docente-Id") Long docenteId,
            @PathVariable Long aulaId,
            @RequestParam(required = false) String tipo) {
        return service.pendientes(docenteId, aulaId, tipo);
    }

    // T-371 (US-32): detalle de una evidencia del aula, en cualquier estado
    @GetMapping("/{evidenciaId}")
    @Operation(summary = "Detalle de una evidencia (contenido, transcripción, puntaje y resultado de la IA)")
    public EvidenciaDetalleResponse detalle(
            @RequestHeader("X-Docente-Id") Long docenteId,
            @PathVariable Long aulaId,
            @PathVariable Long evidenciaId) {
        return service.detalle(docenteId, aulaId, evidenciaId);
    }

    // T-378 (US-33): el docente acepta la nota de la IA
    @PostMapping("/{evidenciaId}/aprobar")
    @Operation(summary = "Aprueba la calificación de la IA")
    public EvidenciaDetalleResponse aprobar(
            @RequestHeader("X-Docente-Id") Long docenteId,
            @PathVariable Long aulaId,
            @PathVariable Long evidenciaId) {
        return service.aprobar(docenteId, aulaId, evidenciaId);
    }

    // T-378 (US-33): el docente cambia la nota de la IA y explica por qué
    @PostMapping("/{evidenciaId}/corregir")
    @Operation(summary = "Corrige la calificación de la IA con un motivo")
    public EvidenciaDetalleResponse corregir(
            @RequestHeader("X-Docente-Id") Long docenteId,
            @PathVariable Long aulaId,
            @PathVariable Long evidenciaId,
            @Valid @RequestBody CorregirEvidenciaRequest req) {
        return service.corregir(docenteId, aulaId, evidenciaId, req);
    }

    // T-384 (US-34): el docente guarda su comentario de retroalimentación
    @PutMapping("/{evidenciaId}/comentario")
    @Operation(summary = "Guarda el comentario de retroalimentación de la evidencia")
    public EvidenciaDetalleResponse guardarComentario(
            @RequestHeader("X-Docente-Id") Long docenteId,
            @PathVariable Long aulaId,
            @PathVariable Long evidenciaId,
            @Valid @RequestBody GuardarComentarioRequest req) {
        return service.guardarComentario(docenteId, aulaId, evidenciaId, req);
    }

    // T-372 (US-32): el audio solo lo puede pedir el docente dueño del aula
    @GetMapping("/{evidenciaId}/audio")
    @Operation(summary = "Reproduce el audio de la evidencia (solo el docente del aula)")
    public ResponseEntity<?> audio(
            @RequestHeader("X-Docente-Id") Long docenteId,
            @PathVariable Long aulaId,
            @PathVariable Long evidenciaId,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rango)
            throws IOException {
        return respuestaAudio(audioService.audio(docenteId, aulaId, evidenciaId), rango);
    }

    private ResponseEntity<?> respuestaAudio(ArchivoAudio audio, String rango) throws IOException {
        long total = audio.tamanio();
        MediaType tipo = MediaType.parseMediaType(audio.tipoContenido());

        if (rango == null || rango.isBlank()) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CACHE_CONTROL, "private, max-age=0")
                    .contentType(tipo)
                    .contentLength(total)
                    .body(new FileSystemResource(audio.ruta()));
        }

        List<HttpRange> rangos;
        try {
            rangos = HttpRange.parseRanges(rango);
        } catch (IllegalArgumentException e) {
            return rangoNoSatisfactorio(total);
        }
        if (rangos.isEmpty()) {
            return rangoNoSatisfactorio(total);
        }

        long inicio;
        long fin;
        try {
            inicio = rangos.get(0).getRangeStart(total);
            fin = rangos.get(0).getRangeEnd(total);
        } catch (IllegalArgumentException e) {
            return rangoNoSatisfactorio(total);
        }
        if (inicio < 0 || inicio >= total || inicio > fin) {
            return rangoNoSatisfactorio(total);
        }

        byte[] datos = leerRango(audio, inicio, fin);
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_RANGE, "bytes " + inicio + "-" + fin + "/" + total)
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=0")
                .contentType(tipo)
                .contentLength(datos.length)
                .body(datos);
    }

    private byte[] leerRango(ArchivoAudio audio, long inicio, long fin) throws IOException {
        try (InputStream in = Files.newInputStream(audio.ruta())) {
            in.skipNBytes(inicio);
            return in.readNBytes((int) (fin - inicio + 1));
        }
    }

    private ResponseEntity<Void> rangoNoSatisfactorio(long total) {
        return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                .header(HttpHeaders.CONTENT_RANGE, "bytes */" + total)
                .build();
    }
}
