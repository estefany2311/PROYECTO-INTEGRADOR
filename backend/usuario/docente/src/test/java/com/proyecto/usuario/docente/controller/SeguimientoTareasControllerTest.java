package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.AsignacionResponse;
import com.proyecto.usuario.docente.dto.SeguimientoTareasResponse;
import com.proyecto.usuario.docente.dto.TareaSeguimientoResponse;
import com.proyecto.usuario.docente.exception.ManejadorErrores;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.service.AsignacionService;
import com.proyecto.usuario.docente.service.EntregaTareaService;
import com.proyecto.usuario.docente.service.SeguimientoTareasService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// T-391 (US-38): forma del seguimiento, la entrega del alumno y la fecha límite en la asignación
class SeguimientoTareasControllerTest {

    private SeguimientoTareasService seguimiento;
    private AsignacionService asignacionService;
    private EntregaTareaService entrega;
    private MockMvc mvcDocente;
    private MockMvc mvcAlumno;

    @BeforeEach
    void preparar() {
        seguimiento = mock(SeguimientoTareasService.class);
        asignacionService = mock(AsignacionService.class);
        entrega = mock(EntregaTareaService.class);
        mvcDocente = MockMvcBuilders
                .standaloneSetup(new AsignacionController(asignacionService, seguimiento))
                .setControllerAdvice(new ManejadorErrores())
                .build();
        mvcAlumno = MockMvcBuilders
                .standaloneSetup(new EntregaTareaController(entrega))
                .setControllerAdvice(new ManejadorErrores())
                .build();
    }

    @Test
    void devuelveElResumenYLasTareasConSuEstado() throws Exception {
        when(seguimiento.seguimiento(1L, 10L, null)).thenReturn(new SeguimientoTareasResponse(
                1, 1, 1, List.of(
                new TareaSeguimientoResponse(2L, 7L, "El zorro", 5L, "Ana", "Quispe", "1.º",
                        LocalDateTime.of(2026, 11, 10, 9, 0), LocalDate.of(2026, 11, 15),
                        "vencida", null, null),
                new TareaSeguimientoResponse(1L, 7L, "El zorro", 6L, "Luis", "Mamani", "1.º",
                        LocalDateTime.of(2026, 11, 10, 9, 0), LocalDate.of(2026, 11, 20),
                        "entregada", LocalDateTime.of(2026, 11, 19, 8, 0), true))));

        mvcDocente.perform(get("/api/docente/aulas/10/asignaciones").header("X-Docente-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendientes").value(1))
                .andExpect(jsonPath("$.vencidas").value(1))
                .andExpect(jsonPath("$.tareas[0].estado").value("vencida"))
                .andExpect(jsonPath("$.tareas[0].fechaLimite").value("2026-11-15"))
                .andExpect(jsonPath("$.tareas[1].estado").value("entregada"))
                .andExpect(jsonPath("$.tareas[1].entregadaATiempo").value(true));
    }

    @Test
    void pasaElFiltroDeEstadoAlServicio() throws Exception {
        when(seguimiento.seguimiento(1L, 10L, "vencida"))
                .thenReturn(new SeguimientoTareasResponse(0, 0, 0, List.of()));

        mvcDocente.perform(get("/api/docente/aulas/10/asignaciones")
                        .header("X-Docente-Id", 1).param("estado", "vencida"))
                .andExpect(status().isOk());

        verify(seguimiento).seguimiento(1L, 10L, "vencida");
    }

    @Test
    void unEstadoInvalidoSeTraduceA400() throws Exception {
        when(seguimiento.seguimiento(1L, 10L, "atrasada"))
                .thenThrow(new ReglaNegocioException(
                        "El estado debe ser pendiente, entregada o vencida", HttpStatus.BAD_REQUEST));

        mvcDocente.perform(get("/api/docente/aulas/10/asignaciones")
                        .header("X-Docente-Id", 1).param("estado", "atrasada"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje")
                        .value("El estado debe ser pendiente, entregada o vencida"));
    }

    @Test
    void asignarConFechaLimiteLaPasaAlServicio() throws Exception {
        when(asignacionService.asignar(any(), any(), any())).thenReturn(new AsignacionResponse(
                1L, 7L, 5L, LocalDateTime.of(2026, 11, 20, 10, 0),
                LocalDate.of(2099, 12, 31), "pendiente"));

        mvcDocente.perform(post("/api/docente/aulas/10/asignaciones")
                        .header("X-Docente-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lecturaId": 7, "estudianteId": 5, "fechaLimite": "2099-12-31"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fechaLimite").value("2099-12-31"))
                .andExpect(jsonPath("$.estado").value("pendiente"));
    }

    @Test
    void unaFechaLimiteEnElPasadoSeRechaza() throws Exception {
        mvcDocente.perform(post("/api/docente/aulas/10/asignaciones")
                        .header("X-Docente-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lecturaId": 7, "estudianteId": 5, "fechaLimite": "2020-01-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje")
                        .value("La fecha límite no puede estar en el pasado"));
    }

    @Test
    void elAlumnoEntregaSuTarea() throws Exception {
        when(entrega.entregar(5L, 40L)).thenReturn(new AsignacionResponse(
                40L, 7L, 5L, LocalDateTime.of(2026, 11, 20, 10, 0),
                LocalDate.of(2026, 11, 27), "entregada"));

        mvcAlumno.perform(post("/api/estudiante/asignaciones/40/entregar")
                        .requestAttr("estudianteId", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("entregada"));
    }

    @Test
    void entregarLaTareaDeOtroAlumnoDevuelve404() throws Exception {
        when(entrega.entregar(6L, 40L))
                .thenThrow(new ReglaNegocioException("La tarea no existe", HttpStatus.NOT_FOUND));

        mvcAlumno.perform(post("/api/estudiante/asignaciones/40/entregar")
                        .requestAttr("estudianteId", 6L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("La tarea no existe"));
    }
}
