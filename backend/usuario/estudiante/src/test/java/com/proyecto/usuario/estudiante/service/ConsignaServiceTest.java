package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.dto.ConsignaNivelResponse;
import com.proyecto.usuario.estudiante.dto.NivelDetalleResponse;
import com.proyecto.usuario.estudiante.exception.ConsignaNoDisponibleException;
import com.proyecto.usuario.estudiante.exception.NivelBloqueadoException;
import com.proyecto.usuario.estudiante.model.Consigna;
import com.proyecto.usuario.estudiante.repository.ConsignaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// T-320 (US-17): qué consigna recibe el alumno en un nivel
@ExtendWith(MockitoExtension.class)
class ConsignaServiceTest {

    @Mock NivelService niveles;
    @Mock ConsignaRepository consignas;
    @InjectMocks ConsignaService service;

    private static NivelDetalleResponse nivel(int id, String tipo) {
        return new NivelDetalleResponse(id, id, "Palabras nuevas", tipo, "Escribe palabras", "desbloqueado", 0);
    }

    @Test
    void nivelDeEscrituraDevuelveSuConsigna() {
        when(niveles.detalle(1L, 2)).thenReturn(nivel(2, "escritura"));
        when(consignas.delNivel(2)).thenReturn(Optional.of(
                new Consigna(2, "Escribe tres oraciones sobre tu animal favorito.", "Cuenta cómo es.", "animal", 15)));

        ConsignaNivelResponse r = service.consignaDelNivel(1L, 2);

        assertEquals("Palabras nuevas", r.nombreNivel());
        assertEquals("Escribe tres oraciones sobre tu animal favorito.", r.consigna());
        assertEquals("animal", r.ilustracion());
        assertEquals(15, r.palabrasMinimas());
    }

    @Test
    void nivelQueNoEsDeEscrituraNoTieneConsigna() {
        when(niveles.detalle(1L, 1)).thenReturn(nivel(1, "lectura"));

        ConsignaNoDisponibleException e = assertThrows(ConsignaNoDisponibleException.class,
                () -> service.consignaDelNivel(1L, 1));

        assertEquals("Este nivel no es de escritura", e.getMessage());
        verify(consignas, never()).delNivel(anyInt());
    }

    @Test
    void nivelDeEscrituraSinConsignaCargada() {
        when(niveles.detalle(1L, 5)).thenReturn(nivel(5, "escritura"));
        when(consignas.delNivel(5)).thenReturn(Optional.empty());

        ConsignaNoDisponibleException e = assertThrows(ConsignaNoDisponibleException.class,
                () -> service.consignaDelNivel(1L, 5));

        assertEquals("Este nivel todavía no tiene una consigna para ti", e.getMessage());
    }

    // Un nivel bloqueado no muestra su consigna: se respeta el mismo control que el detalle
    @Test
    void nivelBloqueadoNoMuestraLaConsigna() {
        when(niveles.detalle(1L, 5)).thenThrow(new NivelBloqueadoException("Idea principal"));

        assertThrows(NivelBloqueadoException.class, () -> service.consignaDelNivel(1L, 5));
        verify(consignas, never()).delNivel(anyInt());
    }
}
