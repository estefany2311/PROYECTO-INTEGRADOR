package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.AsignacionResponse;
import com.proyecto.usuario.docente.dto.AsignarLecturaRequest;
import com.proyecto.usuario.docente.exception.ManejadorErrores;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.service.AsignacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// T-192: ruta, cuerpo, 201 y traducción de errores a HTTP
@ExtendWith(MockitoExtension.class)
class AsignacionControllerTest {

    @Mock
    AsignacionService service;

    @InjectMocks
    AsignacionController controller;

    MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ManejadorErrores())
                .build();
    }

    @Test
    void crearAsignacionDevuelve201() throws Exception {
        when(service.asignar(eq(1L), eq(10L), any(AsignarLecturaRequest.class)))
                .thenReturn(new AsignacionResponse(1L, 7L, 5L,
                        LocalDateTime.of(2026, 10, 28, 10, 15)));

        mvc.perform(post("/api/docente/aulas/10/asignaciones")
                        .header("X-Docente-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lecturaId\":7,\"estudianteId\":5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.lecturaId").value(7))
                .andExpect(jsonPath("$.estudianteId").value(5))
                .andExpect(jsonPath("$.fechaAsignacion").exists());
    }

    @Test
    void sinLecturaDevuelve400() throws Exception {
        mvc.perform(post("/api/docente/aulas/10/asignaciones")
                        .header("X-Docente-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estudianteId\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Debes elegir la lectura"));

        verifyNoInteractions(service);
    }

    @Test
    void tareaRepetidaDevuelve409() throws Exception {
        when(service.asignar(eq(1L), eq(10L), any(AsignarLecturaRequest.class)))
                .thenThrow(new ReglaNegocioException(
                        "Ese alumno ya tiene esta tarea", HttpStatus.CONFLICT));

        mvc.perform(post("/api/docente/aulas/10/asignaciones")
                        .header("X-Docente-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lecturaId\":7,\"estudianteId\":5}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ese alumno ya tiene esta tarea"));
    }

    @Test
    void aulaAjenaDevuelve403() throws Exception {
        when(service.asignar(eq(1L), eq(10L), any(AsignarLecturaRequest.class)))
                .thenThrow(new ReglaNegocioException(
                        "El aula no pertenece al docente", HttpStatus.FORBIDDEN));

        mvc.perform(post("/api/docente/aulas/10/asignaciones")
                        .header("X-Docente-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lecturaId\":7,\"estudianteId\":5}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("El aula no pertenece al docente"));
    }
}
