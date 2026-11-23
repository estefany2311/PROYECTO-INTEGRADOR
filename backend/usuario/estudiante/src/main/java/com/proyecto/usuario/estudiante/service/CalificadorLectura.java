package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.model.DatosLectura;
import com.proyecto.usuario.estudiante.model.PalabraAlineada;
import com.proyecto.usuario.estudiante.model.Transcripcion;
import com.proyecto.usuario.estudiante.repository.EvidenciaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

// T-424 (US-14): califica la lectura cuando Whisper termina y guarda el resultado en la evidencia
@Service
public class CalificadorLectura {

    private final EvidenciaRepository evidencias;
    private final EvaluadorLectura evaluador;

    public CalificadorLectura(EvidenciaRepository evidencias, EvaluadorLectura evaluador) {
        this.evidencias = evidencias;
        this.evaluador = evaluador;
    }

    public EvaluadorLectura.Evaluacion calificar(long evidenciaId, Transcripcion transcripcion, LocalDateTime ahora) {
        DatosLectura datos = evidencias.datosLectura(evidenciaId)
                .orElseThrow(() -> new IllegalStateException("La evidencia " + evidenciaId + " no tiene lectura"));
        EvaluadorLectura.Evaluacion evaluacion =
                evaluador.evaluar(datos.estudianteId(), datos.textoLectura(), transcripcion.texto(), datos.duracionMs());

        evidencias.guardarResultado(evidenciaId, transcripcion, evaluacion.precision(), evaluacion.alineacion(), ahora);
        // Para el panel del docente y el puntaje de Noemí: la nota de la IA y las palabras a mejorar
        evidencias.copiarAEvidencia(evidenciaId, transcripcion.texto(), evaluacion.precision().precision(),
                palabrasMarcadas(evaluacion.alineacion()));
        return evaluacion;
    }

    // Formato acordado con Noemí: cada aparición, en minúsculas, separadas por comas sin espacios
    static String palabrasMarcadas(List<PalabraAlineada> alineacion) {
        return alineacion.stream()
                .filter(p -> p.tipo() == PalabraAlineada.Tipo.CAMBIADA || p.tipo() == PalabraAlineada.Tipo.OMITIDA)
                .map(PalabraAlineada::esperada)
                .collect(Collectors.joining(","));
    }
}
