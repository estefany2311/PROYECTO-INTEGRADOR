package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.model.DatosLectura;
import com.proyecto.usuario.estudiante.model.PalabraAlineada;
import com.proyecto.usuario.estudiante.model.PalabraAlineada.Tipo;
import com.proyecto.usuario.estudiante.model.Transcripcion;
import com.proyecto.usuario.estudiante.repository.ConfiguracionEvaluacionRepository;
import com.proyecto.usuario.estudiante.repository.EvidenciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// T-424 (US-14): al terminar Whisper se califica y se guarda en la evidencia
class CalificadorLecturaTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 23, 10, 0);
    private static final String TEXTO = "El gato negro duerme en la casa.";

    private EvidenciaRepository evidencias;
    private CalificadorLectura calificador;

    @BeforeEach
    void preparar() {
        evidencias = mock(EvidenciaRepository.class);
        ConfiguracionEvaluacionRepository configuracion = mock(ConfiguracionEvaluacionRepository.class);
        when(configuracion.umbralPrecision(7L)).thenReturn(Optional.of(70));
        calificador = new CalificadorLectura(evidencias, new EvaluadorLectura(
                new NormalizadorTexto(), new AlineadorPalabras(), new CalculadorPrecision(), configuracion));
        when(evidencias.datosLectura(50L)).thenReturn(Optional.of(new DatosLectura(7L, 1, 5L, TEXTO, 10_000)));
    }

    private static Transcripcion escuchado(String texto) {
        return new Transcripcion(texto, "es", 3.0, List.of(), "small");
    }

    @Test
    void guardaLaPrecisionYCadaPalabraCalificada() {
        Transcripcion t = escuchado("el pato duerme en la casa");   // cambió "gato" y se saltó "negro"

        calificador.calificar(50L, t, AHORA);

        verify(evidencias).guardarResultado(eq(50L), eq(t), argThat(p -> p.precision() == 71 && p.correctas() == 5),
                argThat(a -> a.size() == 7 && a.get(1).tipo() == Tipo.CAMBIADA && a.get(2).tipo() == Tipo.OMITIDA), eq(AHORA));
    }

    @Test
    void copiaLaNotaYLasPalabrasAMejorarALaEvidenciaDelDocente() {
        calificador.calificar(50L, escuchado("el pato duerme en la casa"), AHORA);

        verify(evidencias).copiarAEvidencia(50L, "el pato duerme en la casa", 71, "gato,negro");
    }

    @Test
    void palabrasMarcadasRepiteCadaAparicion() {
        List<PalabraAlineada> alineacion = List.of(
                new PalabraAlineada("el", "el", Tipo.CORRECTA),
                new PalabraAlineada("sol", "son", Tipo.CAMBIADA),
                new PalabraAlineada(null, "eh", Tipo.AGREGADA),
                new PalabraAlineada("sol", null, Tipo.OMITIDA));

        assertEquals("sol,sol", CalificadorLectura.palabrasMarcadas(alineacion));
    }

    @Test
    void sinDatosDeLecturaFallaParaQueQuedeEnError() {
        when(evidencias.datosLectura(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> calificador.calificar(99L, escuchado("hola"), AHORA));
        verify(evidencias, never()).guardarResultado(anyLong(), any(), any(), any(), any());
    }
}
