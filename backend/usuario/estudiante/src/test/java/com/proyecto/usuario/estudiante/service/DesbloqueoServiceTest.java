package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.config.ReglasReto;
import com.proyecto.usuario.estudiante.dto.CompletarNivelResponse;
import com.proyecto.usuario.estudiante.exception.NivelBloqueadoException;
import com.proyecto.usuario.estudiante.model.Nivel;
import com.proyecto.usuario.estudiante.repository.IntentoRetoRepository;
import com.proyecto.usuario.estudiante.repository.NivelRepository;
import com.proyecto.usuario.estudiante.repository.NivelRepository.Progreso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DesbloqueoServiceTest {

    private static final long ANA = 1L;
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-04T15:00:00Z"), ZoneId.of("America/Lima"));
    private static final LocalDateTime AHORA = LocalDateTime.now(RELOJ);
    private static final Nivel PRIMEROS_PASOS = new Nivel(1, 1, "Primeros pasos", "lectura", "Lee palabras y oraciones cortas");
    private static final Nivel PALABRAS_NUEVAS = new Nivel(2, 2, "Palabras nuevas", "escritura", "Escribe palabras y frases sencillas");

    @Mock
    NivelRepository niveles;

    @Mock
    IntentoRetoRepository intentos;

    private NivelService service;

    @BeforeEach
    void setUp() {
        service = conMinimoDeEstrellas(1);
    }

    private NivelService conMinimoDeEstrellas(int minimo) {
        return new NivelService(niveles, intentos, new ReglasReto(minimo), RELOJ);
    }

    // El nivel 1 está abierto para Ana (primer nivel, sin progreso) o con el progreso que se indique
    private void nivelUnoAbierto(Optional<Progreso> progreso) {
        when(niveles.buscarActivo(1)).thenReturn(Optional.of(PRIMEROS_PASOS));
        when(niveles.progreso(ANA, 1)).thenReturn(progreso);
        when(niveles.anterior(1)).thenReturn(Optional.empty());
    }

    // ---------- T-198 · completar y desbloquear ----------

    @Test
    void conEstrellasSeCompletaYSeDesbloqueaElSiguiente() {
        nivelUnoAbierto(Optional.empty());
        when(niveles.marcarCompletado(ANA, 1, 10, AHORA)).thenReturn(10);
        when(niveles.siguiente(1)).thenReturn(Optional.of(PALABRAS_NUEVAS));
        when(niveles.desbloquear(ANA, 2, AHORA)).thenReturn(true);

        CompletarNivelResponse r = service.completar(ANA, 1, 10, 2);

        assertTrue(r.aprobado());
        assertEquals("completado", r.estado());
        assertEquals(2, r.siguienteNivelId());
        assertEquals("Palabras nuevas", r.siguienteNivelNombre());
        assertTrue(r.siguienteRecienDesbloqueado());
        verify(niveles).desbloquear(ANA, 2, AHORA);
    }

    @Test
    void enElUltimoNivelNoHayNadaQueDesbloquear() {
        nivelUnoAbierto(Optional.empty());
        when(niveles.marcarCompletado(ANA, 1, 10, AHORA)).thenReturn(10);
        when(niveles.siguiente(1)).thenReturn(Optional.empty());

        CompletarNivelResponse r = service.completar(ANA, 1, 10, 3);

        assertTrue(r.aprobado());
        assertNull(r.siguienteNivelId());
        assertFalse(r.siguienteRecienDesbloqueado());
        verify(niveles, never()).desbloquear(anyLong(), anyInt(), any());
    }

    // Un nivel bloqueado no se puede completar escribiendo su id a mano, y no deja intento
    @Test
    void unNivelBloqueadoNoSePuedeCompletar() {
        when(niveles.buscarActivo(2)).thenReturn(Optional.of(PALABRAS_NUEVAS));
        when(niveles.progreso(ANA, 2)).thenReturn(Optional.empty());
        when(niveles.anterior(2)).thenReturn(Optional.of(PRIMEROS_PASOS));
        when(niveles.progreso(ANA, 1)).thenReturn(Optional.empty());

        assertThrows(NivelBloqueadoException.class, () -> service.completar(ANA, 2, 10, 3));

        verifyNoInteractions(intentos);
        verify(niveles, never()).marcarCompletado(anyLong(), anyInt(), anyInt(), any());
    }

    // ---------- T-308 · el intento siempre queda registrado ----------

    @Test
    void cadaIntentoQuedaRegistradoConSusPuntosYEstrellas() {
        nivelUnoAbierto(Optional.empty());
        when(niveles.marcarCompletado(ANA, 1, 7, AHORA)).thenReturn(7);
        when(niveles.siguiente(1)).thenReturn(Optional.empty());

        service.completar(ANA, 1, 7, 1);

        verify(intentos).registrar(ANA, 1, 7, 1, AHORA);
    }

    // ---------- T-309 · al menos 1 estrella ----------

    @Test
    void sinEstrellasSoloQuedaElIntentoYNoSeDesbloqueaNada() {
        nivelUnoAbierto(Optional.empty());

        CompletarNivelResponse r = service.completar(ANA, 1, 3, 0);

        assertFalse(r.aprobado());
        assertEquals("desbloqueado", r.estado());       // sigue como estaba: puede volver a intentarlo
        assertNull(r.siguienteNivelId());
        verify(intentos).registrar(ANA, 1, 3, 0, AHORA);
        verify(niveles, never()).marcarCompletado(anyLong(), anyInt(), anyInt(), any());
        verify(niveles, never()).desbloquear(anyLong(), anyInt(), any());
    }

    @Test
    void elMinimoDeEstrellasEsConfigurable() {
        service = conMinimoDeEstrellas(2);
        nivelUnoAbierto(Optional.empty());

        CompletarNivelResponse r = service.completar(ANA, 1, 5, 1);

        assertFalse(r.aprobado());
        assertEquals(2, r.estrellasMinimas());
        verify(niveles, never()).marcarCompletado(anyLong(), anyInt(), anyInt(), any());
    }

    // ---------- T-310 · repetir un reto ----------

    @Test
    void alRepetirConMenosPuntosSeConservaElMejorYNoSeDesbloqueaDeNuevo() {
        nivelUnoAbierto(Optional.of(new Progreso("completado", 10)));
        when(niveles.marcarCompletado(ANA, 1, 4, AHORA)).thenReturn(10);    // la base guarda el mejor
        when(niveles.siguiente(1)).thenReturn(Optional.of(PALABRAS_NUEVAS));
        when(niveles.desbloquear(ANA, 2, AHORA)).thenReturn(false);        // ya estaba abierto

        CompletarNivelResponse r = service.completar(ANA, 1, 4, 1);

        assertTrue(r.aprobado());
        assertEquals(4, r.puntos());
        assertEquals(10, r.mejoresPuntos());
        assertFalse(r.siguienteRecienDesbloqueado());
    }

    // Un mal intento en un nivel ya completado no le quita nada al niño
    @Test
    void repetirSinEstrellasUnNivelCompletadoNoLoQuitaNiPierdePuntos() {
        nivelUnoAbierto(Optional.of(new Progreso("completado", 10)));

        CompletarNivelResponse r = service.completar(ANA, 1, 0, 0);

        assertFalse(r.aprobado());
        assertEquals("completado", r.estado());
        assertEquals(10, r.mejoresPuntos());
        verify(niveles, never()).marcarCompletado(anyLong(), anyInt(), anyInt(), any());
    }
}
