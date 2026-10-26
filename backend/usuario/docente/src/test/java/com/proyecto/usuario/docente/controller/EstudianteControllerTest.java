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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    // T-227: el alumno ya existe en el aula -> 409
    @Test
    void alumnoDuplicadoDevuelve409() throws Exception {
        when(service.registrar(eq(1L), eq(10L), any(EstudianteRequest.class)))
                .thenThrow(new ReglaNegocioException(
                        "Ya existe un alumno con ese nombre en esta aula", HttpStatus.CONFLICT));

        mvc.perform(post("/api/docente/aulas/10/estudiantes")
                        .header("X-Docente-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombres\":\"Ana\",\"apellidos\":\"Quispe\",\"gradoId\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje")
                        .value("Ya existe un alumno con ese nombre en esta aula"));
    }

    // T-227: el grado no es uno de los que atiende el aula -> 400
    @Test
    void gradoQueElAulaNoAtiendeDevuelve400() throws Exception {
        when(service.registrar(eq(1L), eq(10L), any(EstudianteRequest.class)))
                .thenThrow(new ReglaNegocioException(
                        "El grado no corresponde a esta aula", HttpStatus.BAD_REQUEST));

        mvc.perform(post("/api/docente/aulas/10/estudiantes")
                        .header("X-Docente-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombres\":\"Ana\",\"apellidos\":\"Quispe\",\"gradoId\":6}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El grado no corresponde a esta aula"));
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
    @Test
    void actualizarDevuelve200() throws Exception {
        when(service.actualizar(eq(1L), eq(10L), eq(5L), any(EstudianteRequest.class)))
                .thenReturn(new EstudianteResponse(5L, "Ana", "Quispe", (short) 1, "1.º", true));

        mvc.perform(put("/api/docente/aulas/10/estudiantes/5")
                        .header("X-Docente-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombres\":\"Ana\",\"apellidos\":\"Quispe\",\"gradoId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombres").value("Ana"));
    }

    @Test
    void actualizarConNombresVacioDevuelve400() throws Exception {
        mvc.perform(put("/api/docente/aulas/10/estudiantes/5")
                        .header("X-Docente-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombres\":\"\",\"apellidos\":\"Quispe\",\"gradoId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Los nombres son obligatorios"));

        verifyNoInteractions(service);
    }

    @Test
    void desactivarDevuelve200() throws Exception {
        when(service.cambiarEstado(1L, 10L, 5L, false))
                .thenReturn(new EstudianteResponse(5L, "Ana", "Quispe", (short) 1, "1.º", false));

        mvc.perform(patch("/api/docente/aulas/10/estudiantes/5/estado")
                        .header("X-Docente-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void estadoSinValorDevuelve400() throws Exception {
        mvc.perform(patch("/api/docente/aulas/10/estudiantes/5/estado")
                        .header("X-Docente-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Debes indicar el estado del alumno"));

        verifyNoInteractions(service);
    }

    @Test
    void alumnoInexistenteDevuelve404() throws Exception {
        when(service.cambiarEstado(1L, 10L, 99L, false))
                .thenThrow(new ReglaNegocioException(
                        "El alumno no existe en esta aula", HttpStatus.NOT_FOUND));

        mvc.perform(patch("/api/docente/aulas/10/estudiantes/99/estado")
                        .header("X-Docente-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("El alumno no existe en esta aula"));
    }

    // T-228: el alumno pertenece a otro aula -> 404 al intentar editarlo
    @Test
    void actualizarAlumnoDeOtraAulaDevuelve404() throws Exception {
        when(service.actualizar(eq(1L), eq(10L), eq(99L), any(EstudianteRequest.class)))
                .thenThrow(new ReglaNegocioException(
                        "El alumno no existe en esta aula", HttpStatus.NOT_FOUND));

        mvc.perform(put("/api/docente/aulas/10/estudiantes/99")
                        .header("X-Docente-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombres\":\"Ana\",\"apellidos\":\"Quispe\",\"gradoId\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("El alumno no existe en esta aula"));
    }

    // T-228: desactivar en un aula de otro docente no se permite
    @Test
    void desactivarEnAulaAjenaDevuelve403() throws Exception {
        when(service.cambiarEstado(2L, 10L, 5L, false))
                .thenThrow(new ReglaNegocioException(
                        "El aula no pertenece al docente", HttpStatus.FORBIDDEN));

        mvc.perform(patch("/api/docente/aulas/10/estudiantes/5/estado")
                        .header("X-Docente-Id", 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("El aula no pertenece al docente"));
    }
}