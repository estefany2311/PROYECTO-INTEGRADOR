package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.dto.NivelDetalleResponse;
import com.proyecto.usuario.estudiante.exception.NivelBloqueadoException;
import com.proyecto.usuario.estudiante.exception.NivelNoEncontradoException;
import com.proyecto.usuario.estudiante.model.Nivel;
import com.proyecto.usuario.estudiante.repository.NivelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * T-243 (US-07): detalle del nivel y la regla "un nivel bloqueado no se abre".
 * Niveles de prueba como la semilla V7: 1 "Primeros pasos" (lectura), 2 "Palabras nuevas" (escritura).
 */
@ExtendWith(MockitoExtension.class)
class NivelServiceTest {

    private static final long ANA = 1L;
    private static final Nivel PRIMEROS_PASOS = new Nivel(1, 1, "Primeros pasos", "lectura", "Lee palabras y oraciones cortas");
    private static final Nivel PALABRAS_NUEVAS = new Nivel(2, 2, "Palabras nuevas", "escritura", "Escribe palabras y frases sencillas");

    @Mock
    NivelRepository niveles;

    NivelService service;

    @BeforeEach
    void setUp() {
        service = new NivelService(niveles);
    }

    @Test
    void elPrimerNivelSeAbreSinProgreso() {
        when(niveles.buscarActivo(1)).thenReturn(Optional.of(PRIMEROS_PASOS));
        when(niveles.progreso(ANA, 1)).thenReturn(Optional.empty());
        when(niveles.anterior(1)).thenReturn(Optional.empty());

        NivelDetalleResponse detalle = service.detalle(ANA, 1);

        assertEquals("Primeros pasos", detalle.nombre());
        assertEquals("lectura", detalle.tipoReto());
        assertEquals("desbloqueado", detalle.estado());
        assertEquals(0, detalle.puntos());
    }

    // La regla principal de T-243: sin completar el anterior, el nivel no se abre
    @Test
    void unNivelBloqueadoNoSeAbreYDiceCualTerminarAntes() {
        when(niveles.buscarActivo(2)).thenReturn(Optional.of(PALABRAS_NUEVAS));
        when(niveles.progreso(ANA, 2)).thenReturn(Optional.empty());
        when(niveles.anterior(2)).thenReturn(Optional.of(PRIMEROS_PASOS));
        when(niveles.progreso(ANA, 1)).thenReturn(Optional.empty());

        NivelBloqueadoException e = assertThrows(NivelBloqueadoException.class, () -> service.detalle(ANA, 2));

        assertEquals("Primeros pasos", e.getNivelAnterior());
    }

    @Test
    void siElAnteriorEstaSoloDesbloqueadoElSiguienteSigueCerrado() {
        when(niveles.buscarActivo(2)).thenReturn(Optional.of(PALABRAS_NUEVAS));
        when(niveles.progreso(ANA, 2)).thenReturn(Optional.empty());
        when(niveles.anterior(2)).thenReturn(Optional.of(PRIMEROS_PASOS));
        when(niveles.progreso(ANA, 1)).thenReturn(Optional.of(new NivelRepository.Progreso("desbloqueado", 0)));

        assertThrows(NivelBloqueadoException.class, () -> service.detalle(ANA, 2));
    }

    @Test
    void alCompletarElAnteriorElSiguienteSeAbre() {
        when(niveles.buscarActivo(2)).thenReturn(Optional.of(PALABRAS_NUEVAS));
        when(niveles.progreso(ANA, 2)).thenReturn(Optional.empty());
        when(niveles.anterior(2)).thenReturn(Optional.of(PRIMEROS_PASOS));
        when(niveles.progreso(ANA, 1)).thenReturn(Optional.of(new NivelRepository.Progreso("completado", 10)));

        assertEquals("desbloqueado", service.detalle(ANA, 2).estado());
    }

    // El progreso guardado manda: un nivel completado se puede volver a jugar y trae sus puntos
    @Test
    void elProgresoGuardadoMandaYTraeLosPuntos() {
        when(niveles.buscarActivo(1)).thenReturn(Optional.of(PRIMEROS_PASOS));
        when(niveles.progreso(ANA, 1)).thenReturn(Optional.of(new NivelRepository.Progreso("completado", 10)));
        when(niveles.anterior(1)).thenReturn(Optional.empty());

        NivelDetalleResponse detalle = service.detalle(ANA, 1);

        assertEquals("completado", detalle.estado());
        assertEquals(10, detalle.puntos());
    }

    // Si el docente dejó el nivel bloqueado en el progreso, no se abre aunque el anterior esté completado
    @Test
    void unBloqueoGuardadoEnElProgresoTambienImpideAbrirlo() {
        when(niveles.buscarActivo(2)).thenReturn(Optional.of(PALABRAS_NUEVAS));
        when(niveles.progreso(ANA, 2)).thenReturn(Optional.of(new NivelRepository.Progreso("bloqueado", 0)));
        when(niveles.anterior(2)).thenReturn(Optional.of(PRIMEROS_PASOS));

        assertThrows(NivelBloqueadoException.class, () -> service.detalle(ANA, 2));
    }

    @Test
    void unNivelQueNoExisteODesactivadoResponde404() {
        when(niveles.buscarActivo(99)).thenReturn(Optional.empty());

        assertThrows(NivelNoEncontradoException.class, () -> service.detalle(ANA, 99));
        verify(niveles, never()).progreso(anyLong(), anyInt());
    }
}
