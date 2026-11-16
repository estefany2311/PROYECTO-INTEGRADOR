package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.EvidenciaPendienteResponse;
import com.proyecto.usuario.docente.exception.ManejadorErrores;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.service.EvidenciaService;
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
 * T-298 (US-31): ruta, forma de la respuesta, filtro por tipo y errores del
 * endpoint de evidencias pendientes.
 */
@ExtendWith(MockitoExtension.class)
class EvidenciaControllerTest {

    @Mock
    EvidenciaService service;

    @InjectMocks
    EvidenciaController controller;

    MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ManejadorErrores())
                .build();
    }

    @Test
    void devuelveLasEvidenciasPendientes() throws Exception {
        when(service.pendientes(1L, 10L, null)).thenReturn(List.of(
                new EvidenciaPendienteResponse(
                        30L, 7L, "Minji", "Prueba", (short) 5, "5.º",
                        (short) 1, "Primeros pasos", "audio",
                        "https://cdn.ejemplo.pe/evidencias/minji-nivel1.mp3", null,
                        "Lee con claridad y buen ritmo.",
                        LocalDateTime.of(2026, 11, 10, 9, 0))));

        mvc.perform(get("/api/docente/aulas/10/evidencias")
                        .header("X-Docente-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(30))
                .andExpect(jsonPath("$[0].estudianteId").value(7))
                .andExpect(jsonPath("$[0].nombres").value("Minji"))
                .andExpect(jsonPath("$[0].apellidos").value("Prueba"))
                .andExpect(jsonPath("$[0].gradoId").value(5))
                .andExpect(jsonPath("$[0].gradoNombre").value("5.º"))
                .andExpect(jsonPath("$[0].nivelOrden").value(1))
                .andExpect(jsonPath("$[0].nivelNombre").value("Primeros pasos"))
                .andExpect(jsonPath("$[0].tipo").value("audio"))
                .andExpect(jsonPath("$[0].audioUrl")
                        .value("https://cdn.ejemplo.pe/evidencias/minji-nivel1.mp3"))
                .andExpect(jsonPath("$[0].texto").doesNotExist())
                .andExpect(jsonPath("$[0].resultadoIa")
                        .value("Lee con claridad y buen ritmo."))
                .andExpect(jsonPath("$[0].creadoEn").exists());

        verify(service).pendientes(1L, 10L, null);
    }

    @Test
    void pasaElFiltroDeTipoAlServicio() throws Exception {
        when(service.pendientes(1L, 10L, "texto")).thenReturn(List.of());

        mvc.perform(get("/api/docente/aulas/10/evidencias?tipo=texto")
                        .header("X-Docente-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(service).pendientes(1L, 10L, "texto");
    }

    @Test
    void devuelveUnaEvidenciaDeTextoConSuContenido() throws Exception {
        when(service.pendientes(1L, 10L, "texto")).thenReturn(List.of(
                new EvidenciaPendienteResponse(
                        29L, 8L, "Breysi", "Zavala", (short) 1, "1.º",
                        (short) 2, "Palabras nuevas", "texto",
                        null, "Mi mamá prepara sopa. Yo pongo la mesa.",
                        "Escribe oraciones completas.",
                        LocalDateTime.of(2026, 11, 9, 15, 30))));

        mvc.perform(get("/api/docente/aulas/10/evidencias?tipo=texto")
                        .header("X-Docente-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(29))
                .andExpect(jsonPath("$[0].tipo").value("texto"))
                .andExpect(jsonPath("$[0].texto")
                        .value("Mi mamá prepara sopa. Yo pongo la mesa."))
                .andExpect(jsonPath("$[0].audioUrl").doesNotExist())
                .andExpect(jsonPath("$[0].resultadoIa")
                        .value("Escribe oraciones completas."));

        verify(service).pendientes(1L, 10L, "texto");
    }

    @Test
    void reenviaElTipoEnMayusculasAlServicio() throws Exception {
        when(service.pendientes(1L, 10L, "AUDIO")).thenReturn(List.of());

        mvc.perform(get("/api/docente/aulas/10/evidencias?tipo=AUDIO")
                        .header("X-Docente-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(service).pendientes(1L, 10L, "AUDIO");
    }

    @Test
    void tipoInvalidoDevuelve400() throws Exception {
        when(service.pendientes(1L, 10L, "video"))
                .thenThrow(new ReglaNegocioException(
                        "El tipo de evidencia no es válido", HttpStatus.BAD_REQUEST));

        mvc.perform(get("/api/docente/aulas/10/evidencias?tipo=video")
                        .header("X-Docente-Id", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje")
                        .value("El tipo de evidencia no es válido"));
    }

    @Test
    void aulaDeOtroDocenteDevuelve403() throws Exception {
        when(service.pendientes(2L, 10L, null))
                .thenThrow(new ReglaNegocioException(
                        "El aula no pertenece al docente", HttpStatus.FORBIDDEN));

        mvc.perform(get("/api/docente/aulas/10/evidencias")
                        .header("X-Docente-Id", 2))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("El aula no pertenece al docente"));
    }

    @Test
    void aulaInexistenteDevuelve404() throws Exception {
        when(service.pendientes(1L, 99L, null))
                .thenThrow(new ReglaNegocioException(
                        "El aula no existe", HttpStatus.NOT_FOUND));

        mvc.perform(get("/api/docente/aulas/99/evidencias")
                        .header("X-Docente-Id", 1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("El aula no existe"));
    }

    @Test
    void exigeElHeaderDelDocente() throws Exception {
        mvc.perform(get("/api/docente/aulas/10/evidencias"))
                .andExpect(status().isBadRequest());
    }
}
