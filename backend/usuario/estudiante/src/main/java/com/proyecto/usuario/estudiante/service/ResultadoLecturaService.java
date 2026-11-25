package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.dto.ResultadoLecturaResponse;
import com.proyecto.usuario.estudiante.repository.EvidenciaRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

// T-425/T-428 (US-14): resultado de la lectura con el texto y las palabras por mejorar
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
            return evidencias.textoCalificado(evidenciaId)
                    .map(t -> r.conPalabras(marcador.marcar(t.texto(), t.tipos(), r.evaluadas())))
                    .orElse(r);
        });
    }
}
