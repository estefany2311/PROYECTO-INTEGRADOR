package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.CompetenciaPromedioResponse;
import com.proyecto.usuario.docente.dto.DashboardAulaResponse;
import com.proyecto.usuario.docente.dto.EstudianteDashboardResponse;
import com.proyecto.usuario.docente.dto.GradoResponse;
import com.proyecto.usuario.docente.dto.ResumenDashboardResponse;
import com.proyecto.usuario.docente.exception.ManejadorErrores;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.ColorSemaforo;
import com.proyecto.usuario.docente.service.RendimientoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-278: ruta, forma de la respuesta y errores del dashboard del aula.
 */
@ExtendWith(MockitoExtension.class)
class DashboardAulaControllerTest {

    @Mock
    RendimientoService service;

    @InjectMocks
    DashboardAulaController controller;

    MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ManejadorErrores())
                .build();
    }

    @Test
    void devuelveElDashboardDelAula() throws Exception {
        when(service.dashboardAula(1L, 10L)).thenReturn(new DashboardAulaResponse(
                10L,
                "Aula Rural",
                List.of(new GradoResponse((short) 1, "1.º")),
                new ResumenDashboardResponse(3, 1, 1, 1, 0),
                List.of(new EstudianteDashboardResponse(
                        7L, "Minji", "Prueba", (short) 5, "5.º", 85, ColorSemaforo.VERDE,
                        List.of(new CompetenciaPromedioResponse("lectura", 90.0)))),
                List.of(new CompetenciaPromedioResponse("lectura", 73.0)),
                68,
                ColorSemaforo.AMARILLO));

        mvc.perform(get("/api/docente/aulas/10/dashboard").header("X-Docente-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aulaId").value(10))
                .andExpect(jsonPath("$.aulaNombre").value("Aula Rural"))
                .andExpect(jsonPath("$.grados[0].nombre").value("1.º"))
                .andExpect(jsonPath("$.resumen.totalEstudiantes").value(3))
                .andExpect(jsonPath("$.resumen.enNivelEsperado").value(1))
                .andExpect(jsonPath("$.resumen.enProceso").value(1))
                .andExpect(jsonPath("$.resumen.necesitanRefuerzo").value(1))
                .andExpect(jsonPath("$.estudiantes[0].nombres").value("Minji"))
                .andExpect(jsonPath("$.estudiantes[0].promedio").value(85))
                .andExpect(jsonPath("$.estudiantes[0].color").value("verde"))
                .andExpect(jsonPath("$.estudiantes[0].competencias[0].competencia").value("lectura"))
                .andExpect(jsonPath("$.estudiantes[0].competencias[0].promedio").value(90.0))
                .andExpect(jsonPath("$.promediosPorCompetencia[0].competencia").value("lectura"))
                .andExpect(jsonPath("$.promediosPorCompetencia[0].promedio").value(73.0))
                .andExpect(jsonPath("$.promedioGeneral").value(68))
                .andExpect(jsonPath("$.colorGeneral").value("amarillo"));
    }

    @Test
    void aulaDeOtroDocenteDevuelve403() throws Exception {
        when(service.dashboardAula(1L, 10L))
                .thenThrow(new ReglaNegocioException(
                        "El aula no pertenece al docente", HttpStatus.FORBIDDEN));

        mvc.perform(get("/api/docente/aulas/10/dashboard").header("X-Docente-Id", 1))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("El aula no pertenece al docente"));
    }

    @Test
    void aulaInexistenteDevuelve404() throws Exception {
        when(service.dashboardAula(1L, 99L))
                .thenThrow(new ReglaNegocioException(
                        "El aula no existe", HttpStatus.NOT_FOUND));

        mvc.perform(get("/api/docente/aulas/99/dashboard").header("X-Docente-Id", 1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("El aula no existe"));
    }
}
