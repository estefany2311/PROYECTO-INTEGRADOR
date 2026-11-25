package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.dto.CompletarNivelResponse;
import com.proyecto.usuario.estudiante.model.DatosLectura;
import com.proyecto.usuario.estudiante.model.PalabraAlineada;
import com.proyecto.usuario.estudiante.model.PalabraAlineada.Tipo;
import com.proyecto.usuario.estudiante.model.ReglaPuntuacion;
import com.proyecto.usuario.estudiante.model.Transcripcion;
import com.proyecto.usuario.estudiante.repository.ConfiguracionEvaluacionRepository;
import com.proyecto.usuario.estudiante.repository.EvidenciaRepository;
import com.proyecto.usuario.estudiante.repository.ReglasPuntuacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// T-424/T-425 (US-14): al terminar Whisper se califica, se guarda y se registra el intento
class CalificadorLecturaTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 23, 10, 0);
    private static final String TEXTO = "El gato negro duerme en la casa.";

    private EvidenciaRepository evidencias;
    private NivelService niveles;
    private CalificadorLectura calificador;

    @BeforeEach
    void preparar() {
        evidencias = mock(EvidenciaRepository.class);
        ConfiguracionEvaluacionRepository configuracion = mock(ConfiguracionEvaluacionRepository.class);
        when(configuracion.umbralPrecision(7L)).thenReturn(Optional.of(70));
        // Los valores de Daniela para lectura: 10 puntos, umbrales 50/70/90
        ReglasPuntuacionRepository reglas = mock(ReglasPuntuacionRepository.class);
        when(reglas.de("lectura")).thenReturn(new ReglaPuntuacion(10, 50, 70, 90));
        niveles = mock(NivelService.class);
        when(niveles.completar(anyLong(), anyInt(), anyInt(), anyInt()))
                .thenReturn(new CompletarNivelResponse(1, "completado", 7, 7, 2, true, 1, 2, "Mi relato", true));
        calificador = new CalificadorLectura(evidencias, new EvaluadorLectura(
                new NormalizadorTexto(), new AlineadorPalabras(), new CalculadorPrecision(), configuracion), reglas, niveles);
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

    // T-425: 71 % con la regla de lectura (10 puntos, 50/70/90) da 2 estrellas y 7 puntos
    @Test
    void calculaEstrellasYPuntosConLaReglaDeDaniela() {
        calificador.calificar(50L, escuchado("el pato duerme en la casa"), AHORA);

        verify(niveles).completar(7L, 1, 7, 2);
        verify(evidencias).guardarEstrellas(50L, 2, 7, 2);   // y abrió el nivel 2 por primera vez
    }

    @Test
    void siYaEstabaAbiertoElSiguienteNoSeMarcaComoDesbloqueado() {
        when(niveles.completar(anyLong(), anyInt(), anyInt(), anyInt()))
                .thenReturn(new CompletarNivelResponse(1, "completado", 10, 10, 3, true, 1, 2, "Mi relato", false));

        calificador.calificar(50L, escuchado("el gato negro duerme en la casa"), AHORA);

        verify(evidencias).guardarEstrellas(50L, 3, 10, null);
    }

    // Si falla el registro del intento, la nota igual queda guardada
    @Test
    void siFallaElIntentoLaNotaIgualQueda() {
        when(niveles.completar(anyLong(), anyInt(), anyInt(), anyInt())).thenThrow(new IllegalStateException("nivel bloqueado"));

        calificador.calificar(50L, escuchado("el gato negro duerme en la casa"), AHORA);

        verify(evidencias).guardarResultado(eq(50L), any(), any(), any(), any());
        verify(evidencias).guardarEstrellas(50L, 3, 10, null);
    }
}
