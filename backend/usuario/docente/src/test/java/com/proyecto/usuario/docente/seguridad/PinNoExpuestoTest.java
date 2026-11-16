package com.proyecto.usuario.docente.seguridad;

import com.proyecto.usuario.docente.controller.AccesoEstudianteController;
import com.proyecto.usuario.docente.controller.EstudianteController;
import com.proyecto.usuario.docente.dto.EstudianteResponse;
import com.proyecto.usuario.docente.dto.PinGeneradoResponse;
import com.proyecto.usuario.docente.exception.ManejadorErrores;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Estudiante;
import com.proyecto.usuario.docente.service.AccesoEstudianteService;
import com.proyecto.usuario.docente.service.EstudianteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-188: comprueba que el PIN (y su hash BCrypt) nunca viaje en las respuestas.
 * Única excepción por diseño: POST .../pin, que devuelve el PIN en claro UNA vez.
 */
class PinNoExpuestoTest {

    private EstudianteService estudianteService;
    private AccesoEstudianteService accesoService;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        estudianteService = mock(EstudianteService.class);
        accesoService = mock(AccesoEstudianteService.class);
        mvc = MockMvcBuilders.standaloneSetup(
                        new EstudianteController(estudianteService),
                        new AccesoEstudianteController(accesoService))
                .setControllerAdvice(new ManejadorErrores())
                .build();
    }

    // alumno con avatar y PIN ya asignados (tienePin = true)
    private EstudianteResponse alumnoConPin() {
        return new EstudianteResponse(5L, "Ana", "Quispe", (short) 1, "1.º", true,
                (short) 2, "https://img/buho.png", true);
    }

    @Test
    void listarAlumnos_noDevuelveElPinNiElHash() throws Exception {
        when(estudianteService.listar(1L, 10L)).thenReturn(List.of(alumnoConPin()));

        mvc.perform(get("/api/docente/aulas/10/estudiantes").header("X-Docente-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tienePin").value(true))       // solo dice SI tiene PIN
                .andExpect(jsonPath("$[0].pin").doesNotExist())
                .andExpect(jsonPath("$[0].pinHash").doesNotExist())
                .andExpect(jsonPath("$[0].pin_hash").doesNotExist())
                .andExpect(content().string(not(containsString("$2a$"))));
    }

    @Test
    void listarSoloActivos_tampocoDevuelveElPin() throws Exception {
        when(estudianteService.listarActivos(1L, 10L)).thenReturn(List.of(alumnoConPin()));

        mvc.perform(get("/api/docente/aulas/10/estudiantes?soloActivos=true")
                        .header("X-Docente-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pin").doesNotExist())
                .andExpect(content().string(not(containsString("$2a$"))));
    }

    @Test
    void generarPin_esElUnicoLugarDondeViajaElPinEnClaro() throws Exception {
        when(accesoService.generarPin(1L, 10L, 5L)).thenReturn(new PinGeneradoResponse(5L, "4821"));

        mvc.perform(post("/api/docente/aulas/10/estudiantes/5/pin").header("X-Docente-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pin").value("4821"))            // el docente lo necesita UNA vez
                .andExpect(jsonPath("$.pinHash").doesNotExist())
                .andExpect(content().string(not(containsString("$2a$"))));
    }

    @Test
    void asignarAvatar_noDevuelveElPin() throws Exception {
        when(accesoService.asignarAvatar(1L, 10L, 5L, (short) 2)).thenReturn(alumnoConPin());

        mvc.perform(put("/api/docente/aulas/10/estudiantes/5/avatar")
                        .header("X-Docente-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"avatarId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pin").doesNotExist())
                .andExpect(content().string(not(containsString("$2a$"))));
    }

    @Test
    void cuandoHayError_laRespuestaNoLlevaElPin() throws Exception {
        when(accesoService.generarPin(1L, 10L, 5L)).thenThrow(
                new ReglaNegocioException("El alumno no existe en esta aula", HttpStatus.NOT_FOUND));

        mvc.perform(post("/api/docente/aulas/10/estudiantes/5/pin").header("X-Docente-Id", 1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("El alumno no existe en esta aula"))
                .andExpect(jsonPath("$.pin").doesNotExist())
                .andExpect(content().string(not(containsString("$2a$"))));
    }

    @Test
    void pinGeneradoResponse_ocultaElPinTambienEnListasYConcatenado() {
        PinGeneradoResponse r = new PinGeneradoResponse(5L, "4821");

        assertFalse(r.toString().contains("4821"));
        assertFalse(List.of(r).toString().contains("4821")); // al imprimir la lista completa
        assertFalse(("PIN: " + r).contains("4821"));         // al concatenar en un log
        assertTrue(r.toString().contains("****"));
    }

    @Test
    void laEntidadEstudiante_noTieneGetterQueExpongaElPin() {
        // Jackson solo serializa getters públicos: sin getter no hay fuga posible
        boolean hayGetterConPin = Arrays.stream(Estudiante.class.getMethods())
                .anyMatch(m -> m.getName().toLowerCase().startsWith("get")
                        && m.getName().toLowerCase().contains("pin"));

        assertFalse(hayGetterConPin, "no debe existir un getter de pin/pinHash en la entidad");
    }
}
