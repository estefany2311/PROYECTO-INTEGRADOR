package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.MapaResponse;
import com.proyecto.usuario.docente.dto.NivelMapaResponse;
import com.proyecto.usuario.docente.exception.ManejadorErrores;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.service.MapaEstudianteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// T-197: forma de la respuesta que consume la tablet
class MapaEstudianteControllerTest {

    private MapaEstudianteService service;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        service = mock(MapaEstudianteService.class);
        mvc = MockMvcBuilders.standaloneSetup(new MapaEstudianteController(service))
                .setControllerAdvice(new ManejadorErrores())
                .build();
    }

    @Test
    void devuelveElMapaConElEstadoDeCadaNivel() throws Exception {
        MapaResponse mapa = new MapaResponse(1L, "Ana", "Quispe", 2, 1, 10, List.of(
                new NivelMapaResponse((short) 1, (short) 1, "Primeros pasos", "lectura",
                        "Lee palabras", "completado", 10),
                new NivelMapaResponse((short) 2, (short) 2, "Palabras nuevas", "escritura",
                        "Escribe frases", "desbloqueado", 0)));
        when(service.obtenerMapa(1L)).thenReturn(mapa);

        mvc.perform(get("/api/estudiante/1/mapa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estudianteId").value(1))
                .andExpect(jsonPath("$.totalNiveles").value(2))
                .andExpect(jsonPath("$.completados").value(1))
                .andExpect(jsonPath("$.puntos").value(10))
                .andExpect(jsonPath("$.niveles[0].estado").value("completado"))
                .andExpect(jsonPath("$.niveles[1].estado").value("desbloqueado"))
                .andExpect(jsonPath("$.niveles[1].tipoReto").value("escritura"));
    }

    @Test
    void alumnoInexistente_devuelve404ConMensaje() throws Exception {
        when(service.obtenerMapa(99L))
                .thenThrow(new ReglaNegocioException("El alumno no existe", HttpStatus.NOT_FOUND));

        mvc.perform(get("/api/estudiante/99/mapa"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("El alumno no existe"));
    }
}
