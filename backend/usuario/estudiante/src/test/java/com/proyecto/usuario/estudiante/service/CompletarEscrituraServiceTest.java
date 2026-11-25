package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.dto.CompletarNivelResponse;
import com.proyecto.usuario.estudiante.exception.EvidenciaNoEncontradaException;
import com.proyecto.usuario.estudiante.exception.TextoInvalidoException;
import com.proyecto.usuario.estudiante.model.ReglaPuntuacion;
import com.proyecto.usuario.estudiante.model.TextoCalificadoIa;
import com.proyecto.usuario.estudiante.repository.EvidenciaTextoRepository;
import com.proyecto.usuario.estudiante.repository.ReglasPuntuacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// T-435 (US-21): el texto calificado cuenta como intento del nivel con la regla de escritura
class CompletarEscrituraServiceTest {

    private EvidenciaTextoRepository evidencias;
    private ReglasPuntuacionRepository reglas;
    private NivelService niveles;
    private CompletarEscrituraService servicio;

    @BeforeEach
    void preparar() {
        evidencias = mock(EvidenciaTextoRepository.class);
        reglas = mock(ReglasPuntuacionRepository.class);
        niveles = mock(NivelService.class);
        servicio = new CompletarEscrituraService(evidencias, reglas, niveles);
        when(reglas.de("escritura")).thenReturn(new ReglaPuntuacion(20, 60, 80, 95));
    }

    @Test
    void registraElIntentoConLosPuntosYEstrellasDeLaRegla() {
        when(evidencias.puntaje(80L, 7L)).thenReturn(Optional.of(new TextoCalificadoIa(2, 85)));
        CompletarNivelResponse respuesta = new CompletarNivelResponse(2, "completado", 17, 17, 2, true, 1, 3, "Lee y cuenta", true);
        when(niveles.completar(7L, 2, 17, 2)).thenReturn(respuesta);

        assertEquals(respuesta, servicio.completar(7L, 80L));   // 20 × 85 / 100 = 17 puntos; 85 → 2 estrellas
    }

    @Test
    void siElTextoNoTienePuntajeTodaviaNoRegistraNada() {
        when(evidencias.puntaje(80L, 7L)).thenReturn(Optional.of(new TextoCalificadoIa(2, null)));

        assertThrows(TextoInvalidoException.class, () -> servicio.completar(7L, 80L));
        verifyNoInteractions(niveles);
    }

    @Test
    void elTextoDeOtroAlumnoResponde404() {
        when(evidencias.puntaje(81L, 7L)).thenReturn(Optional.empty());

        assertThrows(EvidenciaNoEncontradaException.class, () -> servicio.completar(7L, 81L));
        verify(niveles, never()).completar(anyLong(), anyInt(), anyInt(), anyInt());
    }
}
