package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.dto.CompletarNivelResponse;
import com.proyecto.usuario.estudiante.exception.EvidenciaNoEncontradaException;
import com.proyecto.usuario.estudiante.exception.TextoInvalidoException;
import com.proyecto.usuario.estudiante.model.ReglaPuntuacion;
import com.proyecto.usuario.estudiante.model.TextoCalificadoIa;
import com.proyecto.usuario.estudiante.repository.EvidenciaTextoRepository;
import com.proyecto.usuario.estudiante.repository.ReglasPuntuacionRepository;
import org.springframework.stereotype.Service;

// T-435 (US-21): después de que el backend docente califica el texto, registra el intento y desbloquea
// el siguiente nivel con la misma regla de puntos y estrellas que la lectura (reglas_puntuacion)
@Service
public class CompletarEscrituraService {

    private final EvidenciaTextoRepository evidencias;
    private final ReglasPuntuacionRepository reglas;
    private final NivelService niveles;

    public CompletarEscrituraService(EvidenciaTextoRepository evidencias, ReglasPuntuacionRepository reglas,
                                     NivelService niveles) {
        this.evidencias = evidencias;
        this.reglas = reglas;
        this.niveles = niveles;
    }

    public CompletarNivelResponse completar(long estudianteId, long evidenciaId) {
        TextoCalificadoIa texto = evidencias.puntaje(evidenciaId, estudianteId)
                .orElseThrow(EvidenciaNoEncontradaException::new);
        if (texto.puntaje() == null) {
            throw new TextoInvalidoException("Tu texto todavía no tiene puntaje");
        }
        // Los puntos y estrellas salen de aquí, no de la app: así no se pueden cambiar desde la tablet
        ReglaPuntuacion regla = reglas.de(TextoService.ESCRITURA);
        int nota = texto.puntaje();
        return niveles.completar(estudianteId, texto.nivelId(), regla.puntos(nota), regla.estrellas(nota));
    }
}
