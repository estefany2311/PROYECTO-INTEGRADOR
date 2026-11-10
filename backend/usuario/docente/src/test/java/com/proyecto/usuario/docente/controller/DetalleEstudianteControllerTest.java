package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.DetalleEstudianteResponse;
import com.proyecto.usuario.docente.dto.EvidenciaEstudianteResponse;
import com.proyecto.usuario.docente.dto.NivelMapaResponse;
import com.proyecto.usuario.docente.dto.PuntajeCompetenciaResponse;
import com.proyecto.usuario.docente.exception.ManejadorErrores;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.ColorSemaforo;
import com.proyecto.usuario.docente.model.Progreso;
import com.proyecto.usuario.docente.service.DetalleEstudianteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-290 (US-29): ruta, forma de la respuesta y errores del detalle del alumno.
 */
@ExtendWith(MockitoExtension.class)
class DetalleEstudianteControllerTest {

    @Mock
    DetalleEstudianteService service;

    @InjectMocks
    DetalleEstudianteController controller;

    MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ManejadorErrores())
                .build();
    }

    @Test
    void devuelveElDetalleDelEstudiante() throws Exception {
        when(service.detalle(1L, 10L, 7L)).thenReturn(new DetalleEstudianteResponse(
                7L, "Minji", "Prueba", (short) 5, "5.º", 82, ColorSemaforo.VERDE,
                List.of(new NivelMapaResponse((short) 1, (short) 1, "Primeros pasos",
                        "lectura", "Lee palabras", Progreso.COMPLETADO, 10)),
                List.of(new PuntajeCompetenciaResponse(
                        "lectura", 80.0, 2, 70, ColorSemaforo.VERDE)),
                List.of(new EvidenciaEstudianteResponse(
                        4L, "lectura", (short) 70, LocalDateTime.of(2026, 11, 1, 10, 0)))));

        mvc.perform(get("/api/docente/aulas/10/estudiantes/7/detalle")
                        .header("X-Docente-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estudianteId").value(7))
                .andExpect(jsonPath("$.nombres").value("Minji"))
                .andExpect(jsonPath("$.apellidos").value("Prueba"))
                .andExpect(jsonPath("$.gradoId").value(5))
                .andExpect(jsonPath("$.gradoNombre").value("5.º"))
                .andExpect(jsonPath("$.promedioGeneral").value(82))
                .andExpect(jsonPath("$.colorGeneral").value("verde"))
                .andExpect(jsonPath("$.niveles[0].nombre").value("Primeros pasos"))
                .andExpect(jsonPath("$.niveles[0].estado").value("completado"))
                .andExpect(jsonPath("$.niveles[0].puntos").value(10))
                .andExpect(jsonPath("$.puntajesPorCompetencia[0].competencia").value("lectura"))
                .andExpect(jsonPath("$.puntajesPorCompetencia[0].promedio").value(80.0))
                .andExpect(jsonPath("$.puntajesPorCompetencia[0].evaluaciones").value(2))
                .andExpect(jsonPath("$.puntajesPorCompetencia[0].ultimoPuntaje").value(70))
                .andExpect(jsonPath("$.puntajesPorCompetencia[0].color").value("verde"))
                .andExpect(jsonPath("$.ultimasEvidencias[0].competencia").value("lectura"))
                .andExpect(jsonPath("$.ultimasEvidencias[0].puntaje").value(70))
                .andExpect(jsonPath("$.ultimasEvidencias[0].evaluadoEn").exists());

        verify(service).detalle(1L, 10L, 7L);
    }

    @Test
    void alumnoInexistenteDevuelve404() throws Exception {
        when(service.detalle(1L, 10L, 99L))
                .thenThrow(new ReglaNegocioException("El alumno no existe", HttpStatus.NOT_FOUND));

        mvc.perform(get("/api/docente/aulas/10/estudiantes/99/detalle")
                        .header("X-Docente-Id", 1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("El alumno no existe"));
    }

    @Test
    void aulaDeOtroDocenteDevuelve403() throws Exception {
        when(service.detalle(2L, 10L, 7L))
                .thenThrow(new ReglaNegocioException(
                        "El aula no pertenece al docente", HttpStatus.FORBIDDEN));

        mvc.perform(get("/api/docente/aulas/10/estudiantes/7/detalle")
                        .header("X-Docente-Id", 2))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("El aula no pertenece al docente"));
    }
}
