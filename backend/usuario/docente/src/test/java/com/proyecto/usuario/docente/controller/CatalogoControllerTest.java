package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.LecturaResponse;
import com.proyecto.usuario.docente.exception.ManejadorErrores;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.service.CatalogoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// T-189: ruta, filtros opcionales y forma de la respuesta
class CatalogoControllerTest {

    private CatalogoService service;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        service = mock(CatalogoService.class);
        mvc = MockMvcBuilders.standaloneSetup(new CatalogoController(service))
                .setControllerAdvice(new ManejadorErrores())
                .build();
    }

    @Test
    void devuelveElCatalogoDelAula() throws Exception {
        when(service.listar(1L, 10L, null, null, null)).thenReturn(List.of(
                new LecturaResponse(10L, "El cóndor y el zorro", "Había una vez...", "lectura",
                        "basica", (short) 1, "1.º", true)));

        mvc.perform(get("/api/docente/aulas/10/lecturas").header("X-Docente-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].titulo").value("El cóndor y el zorro"))
                .andExpect(jsonPath("$[0].gradoNombre").value("1.º"))
                .andExpect(jsonPath("$[0].dificultad").value("basica"));
    }

    @Test
    void pasaLosFiltrosDeLaPantallaAlServicio() throws Exception {
        when(service.listar(1L, 10L, (short) 2, "basica", "lectura")).thenReturn(List.of());

        mvc.perform(get("/api/docente/aulas/10/lecturas")
                        .header("X-Docente-Id", 1)
                        .param("gradoId", "2")
                        .param("dificultad", "basica")
                        .param("tipoReto", "lectura"))
                .andExpect(status().isOk());

        verify(service).listar(1L, 10L, (short) 2, "basica", "lectura");
    }

    @Test
    void aulaDeOtroDocente_devuelve403() throws Exception {
        when(service.listar(1L, 10L, null, null, null))
                .thenThrow(new ReglaNegocioException("El aula no pertenece al docente", HttpStatus.FORBIDDEN));

        mvc.perform(get("/api/docente/aulas/10/lecturas").header("X-Docente-Id", 1))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("El aula no pertenece al docente"));
    }

    // T-230: un aula que no existe responde 404
    @Test
    void aulaInexistente_devuelve404() throws Exception {
        when(service.listar(1L, 99L, null, null, null))
                .thenThrow(new ReglaNegocioException("El aula no existe", HttpStatus.NOT_FOUND));

        mvc.perform(get("/api/docente/aulas/99/lecturas").header("X-Docente-Id", 1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("El aula no existe"));
    }
}
