package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.EstudianteResponse;
import com.proyecto.usuario.docente.dto.PinGeneradoResponse;
import com.proyecto.usuario.docente.exception.ManejadorErrores;
import com.proyecto.usuario.docente.service.AccesoEstudianteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// T-186: rutas, validación y forma de las respuestas
class AccesoEstudianteControllerTest {

    private AccesoEstudianteService service;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        service = mock(AccesoEstudianteService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AccesoEstudianteController(service))
                .setControllerAdvice(new ManejadorErrores())
                .build();
    }

    @Test
    void asignarAvatar_devuelveElAlumnoConSuAvatar() throws Exception {
        when(service.asignarAvatar(1L, 10L, 5L, (short) 2)).thenReturn(
                new EstudianteResponse(5L, "Ana", "Quispe", (short) 1, "1.º", true,
                        (short) 2, "https://img/buho.png", false));

        mvc.perform(put("/api/docente/aulas/10/estudiantes/5/avatar")
                        .header("X-Docente-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"avatarId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("https://img/buho.png"));
    }

    @Test
    void asignarAvatar_sinAvatarId_devuelve400() throws Exception {
        mvc.perform(put("/api/docente/aulas/10/estudiantes/5/avatar")
                        .header("X-Docente-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void generarPin_devuelveElPinEnLaRespuesta() throws Exception {
        when(service.generarPin(1L, 10L, 5L)).thenReturn(new PinGeneradoResponse(5L, "4821"));

        mvc.perform(post("/api/docente/aulas/10/estudiantes/5/pin").header("X-Docente-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estudianteId").value(5))
                .andExpect(jsonPath("$.pin").value("4821"));
    }
}