package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.dto.ResultadoLecturaResponse;
import com.proyecto.usuario.estudiante.repository.EvidenciaRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

// T-425/T-428/T-432 (US-14, US-16): resultado de la lectura con las palabras por mejorar y su mejor intento
@Service
public class ResultadoLecturaService {

    private static final String LISTO = "listo";

    private final EvidenciaRepository evidencias;
    private final MarcadorPalabras marcador;

    public ResultadoLecturaService(EvidenciaRepository evidencias, MarcadorPalabras marcador) {
        this.evidencias = evidencias;
        this.marcador = marcador;
    }

    // Vacío si la evidencia no existe o es de otro alumno
    public Optional<ResultadoLecturaResponse> resultado(long evidenciaId, long estudianteId) {
        return evidencias.resultado(evidenciaId, estudianteId).map(r -> {
            if (!LISTO.equals(r.estado()) || r.evaluadas() == null) return r;
            ResultadoLecturaResponse conPalabras = evidencias.textoCalificado(evidenciaId)
                    .map(t -> r.conPalabras(marcador.marcar(t.texto(), t.tipos(), r.evaluadas())))
                    .orElse(r);
            // T-432: la app compara con su mejor intento anterior para decirle si lo superó
            return conPalabras.conMejorAnterior(evidencias.mejorAnterior(evidenciaId));
        });
    }
}
