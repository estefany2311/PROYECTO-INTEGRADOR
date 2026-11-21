package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.repository.EstudianteRepository;
import com.proyecto.usuario.docente.repository.PuntajeEstudianteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

// T-404 (US-22): estrellas según los umbrales 60 / 80 / 95 de la US-53
class PuntajeServiceEstrellasTest {

    private final PuntajeService service = new PuntajeService(
            mock(EstudianteRepository.class), mock(PuntajeEstudianteRepository.class));

    @Test
    void lasEstrellasSiguenLosUmbrales() {
        assertEquals(0, service.estrellasPara("escritura", 59));
        assertEquals(1, service.estrellasPara("escritura", 60));
        assertEquals(2, service.estrellasPara("escritura", 80));
        assertEquals(3, service.estrellasPara("escritura", 95));
        assertEquals(3, service.estrellasPara("Escritura", 100));
    }

    @Test
    void unTipoOUnPuntajeInvalidoDevuelve400() {
        ReglaNegocioException tipo = assertThrows(ReglaNegocioException.class,
                () -> service.estrellasPara("baile", 80));
        ReglaNegocioException puntaje = assertThrows(ReglaNegocioException.class,
                () -> service.estrellasPara("escritura", 101));

        assertEquals(HttpStatus.BAD_REQUEST, tipo.getEstado());
        assertEquals(HttpStatus.BAD_REQUEST, puntaje.getEstado());
    }
}
