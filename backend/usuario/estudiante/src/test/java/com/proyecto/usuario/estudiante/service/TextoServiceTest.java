package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.dto.ErrorOrtografico;
import com.proyecto.usuario.estudiante.dto.NivelDetalleResponse;
import com.proyecto.usuario.estudiante.dto.TextoRecibidoResponse;
import com.proyecto.usuario.estudiante.dto.VerificarTextoResponse;
import com.proyecto.usuario.estudiante.exception.ConsignaNoDisponibleException;
import com.proyecto.usuario.estudiante.exception.NivelBloqueadoException;
import com.proyecto.usuario.estudiante.exception.TextoInvalidoException;
import com.proyecto.usuario.estudiante.model.Consigna;
import com.proyecto.usuario.estudiante.repository.ConsignaRepository;
import com.proyecto.usuario.estudiante.repository.EvidenciaTextoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// T-434 (US-21): crear la evidencia de texto con las palabras que marcó el corrector
class TextoServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-25T15:00:00Z"), ZoneId.of("America/Lima"));
    private static final String TEXTO = "Mi perro es muy jugeton. Le gusta corer en el parque.";   // 11 palabras

    private NivelService niveles;
    private ConsignaRepository consignas;
    private OrtografiaService ortografia;
    private EvidenciaTextoRepository evidencias;
    private TextoService servicio;

    @BeforeEach
    void preparar() {
        niveles = mock(NivelService.class);
        consignas = mock(ConsignaRepository.class);
        ortografia = mock(OrtografiaService.class);
        evidencias = mock(EvidenciaTextoRepository.class);
        servicio = new TextoService(niveles, consignas, ortografia, evidencias, RELOJ);

        when(niveles.detalle(7L, 2)).thenReturn(nivel("escritura"));
        when(consignas.delNivel(2)).thenReturn(Optional.of(consigna(10)));
        when(ortografia.verificar(eq(7L), anyString())).thenReturn(new VerificarTextoResponse(List.of(
                new ErrorOrtografico("jugeton", 16, 23, "tilde", "juguetón"),
                new ErrorOrtografico("Corer", 37, 42, "ortografia", "correr")), "media", false));
        when(evidencias.crear(anyLong(), anyInt(), anyString(), anyString(), any())).thenReturn(80L);
    }

    @Test
    void creaLaEvidenciaConLasPalabrasMarcadas() {
        TextoRecibidoResponse r = servicio.recibir(7L, 2, "  " + TEXTO + "\n");

        assertEquals(new TextoRecibidoResponse(80L, 2, 11, List.of("jugeton", "corer")), r);
        verify(evidencias).crear(7L, 2, TEXTO, "jugeton,corer", LocalDateTime.now(RELOJ));
    }

    @Test
    void sinErroresLasPalabrasMarcadasQuedanVacias() {
        when(ortografia.verificar(eq(7L), anyString())).thenReturn(new VerificarTextoResponse(List.of(), "media", false));

        assertTrue(servicio.recibir(7L, 2, TEXTO).palabrasMarcadas().isEmpty());
        verify(evidencias).crear(7L, 2, TEXTO, "", LocalDateTime.now(RELOJ));
    }

    @Test
    void siLeFaltanPalabrasNoSeEnvia() {
        when(consignas.delNivel(2)).thenReturn(Optional.of(consigna(15)));

        TextoInvalidoException e = assertThrows(TextoInvalidoException.class, () -> servicio.recibir(7L, 2, TEXTO));
        assertEquals("Te faltan 4 palabras para enviar tu texto", e.getMessage());
        verifyNoInteractions(evidencias);
    }

    @Test
    void conUnaPalabraMenosDiceFaltaUna() {
        when(consignas.delNivel(2)).thenReturn(Optional.of(consigna(12)));

        TextoInvalidoException e = assertThrows(TextoInvalidoException.class, () -> servicio.recibir(7L, 2, TEXTO));
        assertEquals("Te falta 1 palabra para enviar tu texto", e.getMessage());
    }

    @Test
    void unNivelDeLecturaNoRecibeTexto() {
        when(niveles.detalle(7L, 1)).thenReturn(nivel("lectura"));

        assertThrows(TextoInvalidoException.class, () -> servicio.recibir(7L, 1, TEXTO));
        verifyNoInteractions(evidencias);
    }

    @Test
    void unNivelSinConsignaResponde404() {
        when(consignas.delNivel(2)).thenReturn(Optional.empty());

        assertThrows(ConsignaNoDisponibleException.class, () -> servicio.recibir(7L, 2, TEXTO));
    }

    @Test
    void unNivelBloqueadoNoRecibeTexto() {
        when(niveles.detalle(7L, 5)).thenThrow(new NivelBloqueadoException("Palabras nuevas"));

        assertThrows(NivelBloqueadoException.class, () -> servicio.recibir(7L, 5, TEXTO));
        verifyNoInteractions(evidencias);
    }

    @Test
    void cuentaLasPalabrasIgualQueLaApp() {
        assertEquals(5, TextoService.contarPalabras("¡Hola, niño! Tengo 8 años."));
    }

    private static NivelDetalleResponse nivel(String tipoReto) {
        return new NivelDetalleResponse(2, 2, "Palabras nuevas", tipoReto, "Escribe", "desbloqueado", 0);
    }

    private static Consigna consigna(int minimas) {
        return new Consigna(2, "Escribe sobre tu animal favorito.", null, "animal", minimas);
    }
}
