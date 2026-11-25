package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.dto.ResultadoLecturaResponse;
import com.proyecto.usuario.estudiante.model.PalabraTexto;
import com.proyecto.usuario.estudiante.model.TextoCalificado;
import com.proyecto.usuario.estudiante.repository.EvidenciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

// T-428 (US-14): el resultado lleva el texto con las palabras por mejorar solo cuando ya está calificado
class ResultadoLecturaServiceTest {

    private EvidenciaRepository evidencias;
    private ResultadoLecturaService servicio;

    @BeforeEach
    void preparar() {
        evidencias = mock(EvidenciaRepository.class);
        servicio = new ResultadoLecturaService(evidencias, new MarcadorPalabras(new NormalizadorTexto()));
    }

    @Test
    void cuandoEstaListoAgregaLasPalabras() {
        when(evidencias.resultado(50L, 7L)).thenReturn(Optional.of(resultado("listo", 2)));
        when(evidencias.textoCalificado(50L))
                .thenReturn(Optional.of(new TextoCalificado("Hola, amigo.", List.of("correcta", "omitida"))));

        List<PalabraTexto> palabras = servicio.resultado(50L, 7L).orElseThrow().palabras();

        assertEquals(List.of(new PalabraTexto("Hola,", false), new PalabraTexto("amigo.", true)), palabras);
    }

    @Test
    void mientrasProcesaNoBuscaLasPalabras() {
        when(evidencias.resultado(50L, 7L)).thenReturn(Optional.of(resultado("procesando", null)));

        assertTrue(servicio.resultado(50L, 7L).orElseThrow().palabras().isEmpty());
        verify(evidencias, never()).textoCalificado(anyLong());
    }

    @Test
    void laEvidenciaDeOtroAlumnoNoDevuelveNada() {
        when(evidencias.resultado(51L, 7L)).thenReturn(Optional.empty());

        assertTrue(servicio.resultado(51L, 7L).isEmpty());
    }

    private static ResultadoLecturaResponse resultado(String estado, Integer evaluadas) {
        return new ResultadoLecturaResponse(50L, 1, estado, null, null, null, evaluadas, null, null, null,
                null, null, null, null, null, List.of());
    }
}
