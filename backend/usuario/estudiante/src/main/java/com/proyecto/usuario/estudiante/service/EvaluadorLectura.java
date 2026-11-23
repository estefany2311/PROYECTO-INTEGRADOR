package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.model.PalabraAlineada;
import com.proyecto.usuario.estudiante.model.ResultadoPrecision;
import com.proyecto.usuario.estudiante.repository.ConfiguracionEvaluacionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

// T-421/T-422/T-423 (US-14): compara lo que leyó el niño con el texto y calcula la precisión.
// En T-424 el procesador de lecturas (T-416) lo llama al terminar Whisper y guarda el resultado.
@Service
public class EvaluadorLectura {

    public static final int UMBRAL_POR_DEFECTO = 70;           // el mismo valor inicial del admin (US-51)
    public static final long LIMITE_GRABACION_MS = 118_000;    // la app corta a los 2 minutos (T-317)

    private final NormalizadorTexto normalizador;
    private final AlineadorPalabras alineador;
    private final CalculadorPrecision calculador;
    private final ConfiguracionEvaluacionRepository configuracion;

    public EvaluadorLectura(NormalizadorTexto normalizador, AlineadorPalabras alineador,
                            CalculadorPrecision calculador, ConfiguracionEvaluacionRepository configuracion) {
        this.normalizador = normalizador;
        this.alineador = alineador;
        this.calculador = calculador;
        this.configuracion = configuracion;
    }

    public Evaluacion evaluar(long estudianteId, String textoLectura, String transcripcion, long duracionMs) {
        List<PalabraAlineada> alineacion = alineador.alinear(
                normalizador.palabras(textoLectura), normalizador.palabras(transcripcion));
        int umbral = configuracion.umbralPrecision(estudianteId).orElse(UMBRAL_POR_DEFECTO);
        ResultadoPrecision precision = calculador.calcular(alineacion, umbral, duracionMs >= LIMITE_GRABACION_MS);
        return new Evaluacion(alineacion, precision);
    }

    public record Evaluacion(List<PalabraAlineada> alineacion, ResultadoPrecision precision) {}
}
