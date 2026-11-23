package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.dto.CompletarNivelResponse;
import com.proyecto.usuario.estudiante.model.DatosLectura;
import com.proyecto.usuario.estudiante.model.PalabraAlineada;
import com.proyecto.usuario.estudiante.model.ReglaPuntuacion;
import com.proyecto.usuario.estudiante.model.Transcripcion;
import com.proyecto.usuario.estudiante.repository.EvidenciaRepository;
import com.proyecto.usuario.estudiante.repository.ReglasPuntuacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

// T-424/T-425 (US-14): califica la lectura cuando Whisper termina, guarda el resultado y registra el intento
@Service
public class CalificadorLectura {

    private static final Logger log = LoggerFactory.getLogger(CalificadorLectura.class);
    static final String TIPO_LECTURA = "lectura";

    private final EvidenciaRepository evidencias;
    private final EvaluadorLectura evaluador;
    private final ReglasPuntuacionRepository reglas;
    private final NivelService niveles;

    public CalificadorLectura(EvidenciaRepository evidencias, EvaluadorLectura evaluador,
                              ReglasPuntuacionRepository reglas, NivelService niveles) {
        this.evidencias = evidencias;
        this.evaluador = evaluador;
        this.reglas = reglas;
        this.niveles = niveles;
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

        // T-425: estrellas y puntos con la regla única (reglas_puntuacion), y el intento para el desbloqueo
        int nota = evaluacion.precision().precision();
        ReglaPuntuacion regla = reglas.de(TIPO_LECTURA);
        int estrellas = regla.estrellas(nota);
        int puntos = regla.puntos(nota);
        evidencias.guardarEstrellas(evidenciaId, estrellas, puntos, registrarIntento(datos, puntos, estrellas));
        return evaluacion;
    }

    // Devuelve el nivel que se abrió por primera vez, o null. Si falla, la nota ya quedó guardada
    private Integer registrarIntento(DatosLectura datos, int puntos, int estrellas) {
        try {
            CompletarNivelResponse r = niveles.completar(datos.estudianteId(), datos.nivelId(), puntos, estrellas);
            return r.siguienteRecienDesbloqueado() ? r.siguienteNivelId() : null;
        } catch (RuntimeException e) {
            log.warn("No se pudo registrar el intento del nivel {}: {}", datos.nivelId(), e.getMessage());
            return null;
        }
    }

    // Formato acordado con Noemí: cada aparición, en minúsculas, separadas por comas sin espacios
    static String palabrasMarcadas(List<PalabraAlineada> alineacion) {
        return alineacion.stream()
                .filter(p -> p.tipo() == PalabraAlineada.Tipo.CAMBIADA || p.tipo() == PalabraAlineada.Tipo.OMITIDA)
                .map(PalabraAlineada::esperada)
                .collect(Collectors.joining(","));
    }
}
