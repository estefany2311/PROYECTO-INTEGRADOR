package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.EstudianteRequest;
import com.proyecto.usuario.docente.dto.EstudianteResponse;
import com.proyecto.usuario.docente.exception.ManejadorErrores;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.service.EstudianteService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EstudianteControllerTest {

    @Mock
    EstudianteService service;

    @InjectMocks
    EstudianteController controller;

    MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ManejadorErrores())
                .build();
    }

    @Test
    void registrarDevuelve201() throws Exception {
        when(service.registrar(eq(1L), eq(10L), any(EstudianteRequest.class)))
                .thenReturn(new EstudianteResponse(1L, "Ana", "Quispe", (short) 1, "1.º", true));

        mvc.perform(post("/api/docente/aulas/10/estudiantes")
                        .header("X-Docente-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombres\":\"Ana\",\"apellidos\":\"Quispe\",\"gradoId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombres").value("Ana"))
                .andExpect(jsonPath("$.gradoNombre").value("1.º"));
    }

    @Test
    void nombresVacioDevuelve400() throws Exception {
        mvc.perform(post("/api/docente/aulas/10/estudiantes")
                        .header("X-Docente-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombres\":\"\",\"apellidos\":\"Quispe\",\"gradoId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Los nombres son obligatorios"));

        verifyNoInteractions(service);
    }

    @Test
    void sinGradoDevuelve400() throws Exception {
        mvc.perform(post("/api/docente/aulas/10/estudiantes")
                        .header("X-Docente-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombres\":\"Ana\",\"apellidos\":\"Quispe\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Debes seleccionar el grado del alumno"));

        verifyNoInteractions(service);
    }

    @Test
    void aulaAjenaDevuelve403() throws Exception {
        when(service.registrar(eq(2L), eq(10L), any(EstudianteRequest.class)))
                .thenThrow(new ReglaNegocioException(
                        "El aula no pertenece al docente", HttpStatus.FORBIDDEN));

        mvc.perform(post("/api/docente/aulas/10/estudiantes")
                        .header("X-Docente-Id", 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombres\":\"Ana\",\"apellidos\":\"Quispe\",\"gradoId\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("El aula no pertenece al docente"));
    }

    @Test
    void listarDevuelve200ConLosAlumnos() throws Exception {
        when(service.listar(1L, 10L)).thenReturn(List.of(
                new EstudianteResponse(1L, "Ana", "Quispe", (short) 1, "1.º", true)));

        mvc.perform(get("/api/docente/aulas/10/estudiantes")
                        .header("X-Docente-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].apellidos").value("Quispe"));
    }
}