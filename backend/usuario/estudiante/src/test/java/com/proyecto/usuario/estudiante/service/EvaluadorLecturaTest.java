package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.repository.ConfiguracionEvaluacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// T-421 a T-423 (US-14): de punta a punta con textos reales y el umbral del grado
class EvaluadorLecturaTest {

    private static final String TEXTO = "Había una vez un cóndor que vivía en los Andes. Cada mañana volaba sobre las montañas.";

    private ConfiguracionEvaluacionRepository configuracion;
    private EvaluadorLectura evaluador;

    @BeforeEach
    void preparar() {
        configuracion = mock(ConfiguracionEvaluacionRepository.class);
        evaluador = new EvaluadorLectura(new NormalizadorTexto(), new AlineadorPalabras(), new CalculadorPrecision(), configuracion);
    }

    @Test
    void whisperSinTildesNiPuntuacionIgualEsPerfecto() {
        when(configuracion.umbralPrecision(7L)).thenReturn(Optional.of(80));

        var r = evaluador.evaluar(7L, TEXTO, "habia una vez un condor que vivia en los andes cada mañana volaba sobre las montañas", 20_000);

        assertEquals(100, r.precision().precision());
        assertEquals(80, r.precision().umbral());
        assertTrue(r.precision().aprobado());
    }

    @Test
    void conUnaPalabraCambiadaBajaLaPrecision() {
        when(configuracion.umbralPrecision(7L)).thenReturn(Optional.of(95));

        var r = evaluador.evaluar(7L, TEXTO, "Había una vez un cóndor que bebía en los Andes. Cada mañana volaba sobre las montañas.", 20_000);

        assertEquals(94, r.precision().precision());     // 15 de 16
        assertFalse(r.precision().aprobado());           // el grado pide 95
    }

    @Test
    void sinConfiguracionDelGradoUsaElUmbralPorDefecto() {
        when(configuracion.umbralPrecision(7L)).thenReturn(Optional.empty());

        var r = evaluador.evaluar(7L, TEXTO, TEXTO, 20_000);

        assertEquals(EvaluadorLectura.UMBRAL_POR_DEFECTO, r.precision().umbral());
    }

    @Test
    void siLeCortoElTiempoSeCalificaLoQueAlcanzoALeer() {
        when(configuracion.umbralPrecision(7L)).thenReturn(Optional.of(70));

        var r = evaluador.evaluar(7L, TEXTO, "Había una vez un cóndor que vivía en los Andes", EvaluadorLectura.LIMITE_GRABACION_MS);

        assertEquals(100, r.precision().precision());
        assertEquals(63, r.precision().cobertura());     // 10 de 16
    }
}
