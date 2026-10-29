package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.AsignacionGrupoResponse;
import com.proyecto.usuario.docente.dto.AsignarGrupoRequest;
import com.proyecto.usuario.docente.dto.GrupoAlumnoResponse;
import com.proyecto.usuario.docente.dto.GrupoRequest;
import com.proyecto.usuario.docente.dto.GrupoResponse;
import com.proyecto.usuario.docente.exception.ManejadorErrores;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.service.GrupoService;
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

/**
 * T-195 (US-37): rutas de grupos, 201 al crear/asignar y errores traducidos a HTTP.
 */
@ExtendWith(MockitoExtension.class)
class GrupoControllerTest {

    @Mock
    GrupoService service;

    @InjectMocks
    GrupoController controller;

    MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ManejadorErrores())
                .build();
    }

    @Test
    void crearGrupoDevuelve201() throws Exception {
        when(service.crear(eq(1L), eq(10L), any(GrupoRequest.class)))
                .thenReturn(new GrupoResponse(1L, "Los exploradores", "Refuerzo",
                        List.of(new GrupoAlumnoResponse(5L, "Ana", "Quispe", "1.º"))));

        mvc.perform(post("/api/docente/aulas/10/grupos")
                        .header("X-Docente-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Los exploradores\",\"descripcion\":\"Refuerzo\",\"estudiantesIds\":[5]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Los exploradores"))
                .andExpect(jsonPath("$.alumnos[0].apellidos").value("Quispe"));
    }

    @Test
    void nombreVacioDevuelve400() throws Exception {
        mvc.perform(post("/api/docente/aulas/10/grupos")
                        .header("X-Docente-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"estudiantesIds\":[5]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El nombre del grupo es obligatorio"));

        verifyNoInteractions(service);
    }

    @Test
    void listarGruposDevuelve200() throws Exception {
        when(service.listar(1L, 10L))
                .thenReturn(List.of(new GrupoResponse(1L, "Los exploradores", null,
                        List.of(new GrupoAlumnoResponse(5L, "Ana", "Quispe", "1.º")))));

        mvc.perform(get("/api/docente/aulas/10/grupos")
                        .header("X-Docente-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Los exploradores"))
                .andExpect(jsonPath("$[0].alumnos.length()").value(1));
    }

    @Test
    void asignarLecturaAlGrupoDevuelve201() throws Exception {
        when(service.asignar(eq(1L), eq(10L), eq(3L), any(AsignarGrupoRequest.class)))
                .thenReturn(new AsignacionGrupoResponse(3L, 7L, 2, 0));

        mvc.perform(post("/api/docente/aulas/10/grupos/3/asignaciones")
                        .header("X-Docente-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lecturaId\":7}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.grupoId").value(3))
                .andExpect(jsonPath("$.lecturaId").value(7))
                .andExpect(jsonPath("$.asignados").value(2))
                .andExpect(jsonPath("$.yaTenían").value(0));
    }

    @Test
    void grupoDuplicadoDevuelve409() throws Exception {
        when(service.crear(eq(1L), eq(10L), any(GrupoRequest.class)))
                .thenThrow(new ReglaNegocioException(
                        "Ya existe un grupo con ese nombre en esta aula", HttpStatus.CONFLICT));

        mvc.perform(post("/api/docente/aulas/10/grupos")
                        .header("X-Docente-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Los exploradores\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje")
                        .value("Ya existe un grupo con ese nombre en esta aula"));
    }

    @Test
    void asignarSinLecturaDevuelve400() throws Exception {
        mvc.perform(post("/api/docente/aulas/10/grupos/3/asignaciones")
                        .header("X-Docente-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Debes elegir la lectura"));

        verifyNoInteractions(service);
    }
}
