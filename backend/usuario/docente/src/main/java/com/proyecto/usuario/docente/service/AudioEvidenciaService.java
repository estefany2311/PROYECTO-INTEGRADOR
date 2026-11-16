package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.almacen.AlmacenAudio;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Aula;
import com.proyecto.usuario.docente.model.Evidencia;
import com.proyecto.usuario.docente.repository.AulaRepository;
import com.proyecto.usuario.docente.repository.EvidenciaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;

/**
 * T-372 (US-32): entrega el archivo de audio de una evidencia solo al docente
 * dueño del aula.
 *
 * <p>El control valida en este orden: el aula existe (404), pertenece al docente
 * (403), la evidencia existe en esa aula (404) y es de audio (400). Recién
 * después se resuelve el archivo en la carpeta configurada (404 si no está).</p>
 */
@Service
public class AudioEvidenciaService {

    private final AulaRepository aulaRepository;
    private final EvidenciaRepository evidenciaRepository;
    private final AlmacenAudio almacen;

    public AudioEvidenciaService(AulaRepository aulaRepository,
                                 EvidenciaRepository evidenciaRepository,
                                 AlmacenAudio almacen) {
        this.aulaRepository = aulaRepository;
        this.evidenciaRepository = evidenciaRepository;
        this.almacen = almacen;
    }

    @Transactional(readOnly = true)
    public ArchivoAudio audio(Long docenteId, Long aulaId, Long evidenciaId) {
        Aula aula = aulaRepository.findById(aulaId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "El aula no existe", HttpStatus.NOT_FOUND));
        if (!aula.getDocenteId().equals(docenteId)) {
            throw new ReglaNegocioException(
                    "El aula no pertenece al docente", HttpStatus.FORBIDDEN);
        }

        Evidencia evidencia = evidenciaRepository
                .findByIdAndEstudianteAulaId(evidenciaId, aulaId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "La evidencia no existe en esta aula", HttpStatus.NOT_FOUND));
        if (!Evidencia.AUDIO.equals(evidencia.getTipo())) {
            throw new ReglaNegocioException(
                    "La evidencia no es de audio", HttpStatus.BAD_REQUEST);
        }

        Path archivo = almacen.resolver(evidencia.getAudioUrl());
        return new ArchivoAudio(
                archivo,
                archivo.getFileName().toString(),
                almacen.tipoContenido(archivo),
                archivo.toFile().length());
    }

    /** Datos que el controlador necesita para responder el archivo. */
    public record ArchivoAudio(Path ruta, String nombre, String tipoContenido, long tamanio) {
    }
}
