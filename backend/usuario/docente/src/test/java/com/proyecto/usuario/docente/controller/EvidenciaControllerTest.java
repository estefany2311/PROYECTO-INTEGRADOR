package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.dto.CorregirEvidenciaRequest;
import com.proyecto.usuario.docente.dto.EvidenciaDetalleResponse;
import com.proyecto.usuario.docente.dto.HistorialCalificacionResponse;
import com.proyecto.usuario.docente.dto.EvidenciaPendienteResponse;
import com.proyecto.usuario.docente.exception.ManejadorErrores;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.service.AudioEvidenciaService;
import com.proyecto.usuario.docente.service.AudioEvidenciaService.ArchivoAudio;
import com.proyecto.usuario.docente.service.EvidenciaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

    @Mock
    AudioEvidenciaService audioService;

    @InjectMocks
    EvidenciaController controller;

    MockMvc mvc;

    @TempDir
    Path carpeta;

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

    // T-371 (US-32): detalle de la evidencia

    @Test
    void devuelveElDetalleDeUnaEvidenciaDeAudio() throws Exception {
        when(service.detalle(1L, 10L, 30L)).thenReturn(new EvidenciaDetalleResponse(
                30L, 7L, "Minji", "Prueba", (short) 5, "5.º",
                (short) 1, "Primeros pasos", "lectura", "audio",
                "https://cdn.ejemplo.pe/evidencias/minji-nivel1.mp3", null,
                "El sol brilla. Los niños juegan en el parque.",
                List.of("brilla", "juegan"),
                (short) 85, null, null, "Lee con claridad y buen ritmo.",
                "pendiente", LocalDateTime.of(2026, 11, 10, 9, 0), null,
                List.of(new HistorialCalificacionResponse(
                        1L, 1L, (short) 85, (short) 85, null, "aprobada",
                        LocalDateTime.of(2026, 11, 17, 9, 0)))));

        mvc.perform(get("/api/docente/aulas/10/evidencias/30")
                        .header("X-Docente-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(30))
                .andExpect(jsonPath("$.estudianteId").value(7))
                .andExpect(jsonPath("$.nombres").value("Minji"))
                .andExpect(jsonPath("$.nivelOrden").value(1))
                .andExpect(jsonPath("$.tipoReto").value("lectura"))
                .andExpect(jsonPath("$.tipo").value("audio"))
                .andExpect(jsonPath("$.audioUrl")
                        .value("https://cdn.ejemplo.pe/evidencias/minji-nivel1.mp3"))
                .andExpect(jsonPath("$.texto").doesNotExist())
                .andExpect(jsonPath("$.transcripcion")
                        .value("El sol brilla. Los niños juegan en el parque."))
                .andExpect(jsonPath("$.palabrasMarcadas[0]").value("brilla"))
                .andExpect(jsonPath("$.palabrasMarcadas[1]").value("juegan"))
                .andExpect(jsonPath("$.puntajeIa").value(85))
                .andExpect(jsonPath("$.resultadoIa")
                        .value("Lee con claridad y buen ritmo."))
                .andExpect(jsonPath("$.estadoRevision").value("pendiente"))
                .andExpect(jsonPath("$.creadoEn").exists())
                .andExpect(jsonPath("$.revisadoEn").doesNotExist())
                .andExpect(jsonPath("$.historial").isNotEmpty())
                .andExpect(jsonPath("$.historial[0].accion").value("aprobada"))
                .andExpect(jsonPath("$.historial[0].notaDocente").value(85));

        verify(service).detalle(1L, 10L, 30L);
    }

    @Test
    void devuelveElDetalleDeUnaEvidenciaDeTexto() throws Exception {
        when(service.detalle(1L, 10L, 29L)).thenReturn(new EvidenciaDetalleResponse(
                29L, 8L, "Breysi", "Zavala", (short) 1, "1.º",
                (short) 2, "Palabras nuevas", "escritura", "texto",
                null, "Mi mamá prepara sopa. Yo pongo la mesa.", null,
                List.of(),
                (short) 80, (short) 80, null, "Escribe oraciones completas.",
                "aprobada", LocalDateTime.of(2026, 11, 9, 15, 30),
                LocalDateTime.of(2026, 11, 10, 8, 0), List.of()));

        mvc.perform(get("/api/docente/aulas/10/evidencias/29")
                        .header("X-Docente-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(29))
                .andExpect(jsonPath("$.tipo").value("texto"))
                .andExpect(jsonPath("$.audioUrl").doesNotExist())
                .andExpect(jsonPath("$.texto")
                        .value("Mi mamá prepara sopa. Yo pongo la mesa."))
                .andExpect(jsonPath("$.transcripcion").doesNotExist())
                .andExpect(jsonPath("$.palabrasMarcadas").isEmpty())
                .andExpect(jsonPath("$.puntajeIa").value(80))
                .andExpect(jsonPath("$.estadoRevision").value("aprobada"))
                .andExpect(jsonPath("$.revisadoEn").exists());

        verify(service).detalle(1L, 10L, 29L);
    }

    @Test
    void devuelveElDetalleDeUnaEvidenciaRechazada() throws Exception {
        when(service.detalle(1L, 10L, 31L)).thenReturn(new EvidenciaDetalleResponse(
                31L, 7L, "Minji", "Prueba", (short) 5, "5.º",
                (short) 3, "Cuento corto", "oralidad", "audio",
                "https://cdn.ejemplo.pe/evidencias/minji-nivel3.mp3", null,
                "Había una vez un conejo que quería volar.",
                List.of("conejo", "despegaba"),
                (short) 40, null, null, "El audio no responde al reto.",
                "rechazada", LocalDateTime.of(2026, 11, 8, 10, 0),
                LocalDateTime.of(2026, 11, 9, 9, 0), List.of()));

        mvc.perform(get("/api/docente/aulas/10/evidencias/31")
                        .header("X-Docente-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoRevision").value("rechazada"))
                .andExpect(jsonPath("$.revisadoEn").exists())
                .andExpect(jsonPath("$.puntajeIa").value(40))
                .andExpect(jsonPath("$.palabrasMarcadas[0]").value("conejo"))
                .andExpect(jsonPath("$.palabrasMarcadas[1]").value("despegaba"));

        verify(service).detalle(1L, 10L, 31L);
    }

    @Test
    void elDetalleSinEvaluacionDeLaIaDevuelveCamposVacios() throws Exception {
        when(service.detalle(1L, 10L, 30L)).thenReturn(new EvidenciaDetalleResponse(
                30L, 7L, "Minji", "Prueba", (short) 5, "5.º",
                (short) 1, "Primeros pasos", "lectura", "audio",
                "https://cdn.ejemplo.pe/evidencias/minji-nivel1.mp3", null,
                null, List.of(), null, null, null, null,
                "pendiente", LocalDateTime.of(2026, 11, 10, 9, 0), null,
                List.of()));

        mvc.perform(get("/api/docente/aulas/10/evidencias/30")
                        .header("X-Docente-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transcripcion").doesNotExist())
                .andExpect(jsonPath("$.puntajeIa").doesNotExist())
                .andExpect(jsonPath("$.resultadoIa").doesNotExist())
                .andExpect(jsonPath("$.palabrasMarcadas").isEmpty())
                .andExpect(jsonPath("$.revisadoEn").doesNotExist());

        verify(service).detalle(1L, 10L, 30L);
    }

    @Test
    void unaEvidenciaDeOtraAulaDevuelve404() throws Exception {
        when(service.detalle(1L, 10L, 99L))
                .thenThrow(new ReglaNegocioException(
                        "La evidencia no existe en esta aula", HttpStatus.NOT_FOUND));

        mvc.perform(get("/api/docente/aulas/10/evidencias/99")
                        .header("X-Docente-Id", 1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje")
                        .value("La evidencia no existe en esta aula"));
    }

    // T-378 (US-33): aprobar y corregir la calificación de la IA

    @Test
    void apruebaLaCalificacionDeLaIa() throws Exception {
        when(service.aprobar(1L, 10L, 30L)).thenReturn(new EvidenciaDetalleResponse(
                30L, 7L, "Minji", "Prueba", (short) 5, "5.º",
                (short) 1, "Primeros pasos", "lectura", "audio",
                "https://cdn.ejemplo.pe/evidencias/minji-nivel1.mp3", null,
                "El sol brilla. Los niños juegan en el parque.",
                List.of("brilla", "juegan"),
                (short) 85, (short) 85, null, "Lee con claridad y buen ritmo.",
                "aprobada", LocalDateTime.of(2026, 11, 10, 9, 0),
                LocalDateTime.of(2026, 11, 17, 10, 0), List.of()));

        mvc.perform(post("/api/docente/aulas/10/evidencias/30/aprobar")
                        .header("X-Docente-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoRevision").value("aprobada"))
                .andExpect(jsonPath("$.calificacionFinal").value(85))
                .andExpect(jsonPath("$.motivoCorreccion").doesNotExist())
                .andExpect(jsonPath("$.revisadoEn").exists());

        verify(service).aprobar(1L, 10L, 30L);
    }

    @Test
    void corrigeLaCalificacionConSuMotivo() throws Exception {
        when(service.corregir(1L, 10L, 30L,
                new CorregirEvidenciaRequest((short) 70, "El audio tiene ruido de fondo.")))
                .thenReturn(new EvidenciaDetalleResponse(
                        30L, 7L, "Minji", "Prueba", (short) 5, "5.º",
                        (short) 1, "Primeros pasos", "lectura", "audio",
                        "https://cdn.ejemplo.pe/evidencias/minji-nivel1.mp3", null,
                        "El sol brilla. Los niños juegan en el parque.",
                        List.of("brilla", "juegan"),
                        (short) 85, (short) 70, "El audio tiene ruido de fondo.",
                        "Lee con claridad y buen ritmo.",
                        "corregida", LocalDateTime.of(2026, 11, 10, 9, 0),
                        LocalDateTime.of(2026, 11, 17, 10, 0), List.of()));

        mvc.perform(post("/api/docente/aulas/10/evidencias/30/corregir")
                        .header("X-Docente-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"calificacionFinal": 70,
                                 "motivo": "El audio tiene ruido de fondo."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoRevision").value("corregida"))
                .andExpect(jsonPath("$.puntajeIa").value(85))
                .andExpect(jsonPath("$.calificacionFinal").value(70))
                .andExpect(jsonPath("$.motivoCorreccion")
                        .value("El audio tiene ruido de fondo."));

        verify(service).corregir(1L, 10L, 30L,
                new CorregirEvidenciaRequest((short) 70, "El audio tiene ruido de fondo."));
    }

    @Test
    void laCorreccionExigeElMotivo() throws Exception {
        mvc.perform(post("/api/docente/aulas/10/evidencias/30/corregir")
                        .header("X-Docente-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"calificacionFinal": 70, "motivo": "   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje")
                        .value("Debes indicar el motivo de la corrección"));
    }

    @Test
    void laCorreccionValidaElRangoDeLaNota() throws Exception {
        mvc.perform(post("/api/docente/aulas/10/evidencias/30/corregir")
                        .header("X-Docente-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"calificacionFinal": 120, "motivo": "Nota ajustada."}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje")
                        .value("La calificación final no puede ser mayor que 100"));
    }

    @Test
    void aprobarUnaEvidenciaSinEvaluacionDeLaIaDevuelve400() throws Exception {
        when(service.aprobar(1L, 10L, 30L))
                .thenThrow(new ReglaNegocioException(
                        "La evidencia no tiene evaluación de la IA", HttpStatus.BAD_REQUEST));

        mvc.perform(post("/api/docente/aulas/10/evidencias/30/aprobar")
                        .header("X-Docente-Id", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje")
                        .value("La evidencia no tiene evaluación de la IA"));
    }

    @Test
    void elDetalleDeUnAulaAjenaDevuelve403() throws Exception {
        when(service.detalle(2L, 10L, 30L))
                .thenThrow(new ReglaNegocioException(
                        "El aula no pertenece al docente", HttpStatus.FORBIDDEN));

        mvc.perform(get("/api/docente/aulas/10/evidencias/30")
                        .header("X-Docente-Id", 2))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("El aula no pertenece al docente"));
    }

    @Test
    void elDetalleExigeElHeaderDelDocente() throws Exception {
        mvc.perform(get("/api/docente/aulas/10/evidencias/30"))
                .andExpect(status().isBadRequest());
    }

    // T-372 (US-32): el audio solo lo recibe el docente dueño del aula

    @Test
    void entregaElAudioCompleto() throws Exception {
        byte[] audio = {1, 2, 3, 4, 5, 6, 7, 8};
        Path archivo = archivoDeAudio(audio);
        when(audioService.audio(1L, 10L, 30L))
                .thenReturn(new ArchivoAudio(archivo, "evidencia-30.mp3", "audio/mpeg", audio.length));

        mvc.perform(get("/api/docente/aulas/10/evidencias/30/audio")
                        .header("X-Docente-Id", 1))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "bytes"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, max-age=0"))
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, audio.length))
                .andExpect(content().contentType("audio/mpeg"))
                .andExpect(content().bytes(audio));

        verify(audioService).audio(1L, 10L, 30L);
    }

    @Test
    void entregaSoloLaPartePedidaConRange() throws Exception {
        byte[] audio = {1, 2, 3, 4, 5, 6, 7, 8};
        Path archivo = archivoDeAudio(audio);
        when(audioService.audio(1L, 10L, 30L))
                .thenReturn(new ArchivoAudio(archivo, "evidencia-30.mp3", "audio/mpeg", audio.length));

        mvc.perform(get("/api/docente/aulas/10/evidencias/30/audio")
                        .header("X-Docente-Id", 1)
                        .header(HttpHeaders.RANGE, "bytes=0-3"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string(HttpHeaders.CONTENT_RANGE, "bytes 0-3/8"))
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, 4))
                .andExpect(content().contentType("audio/mpeg"))
                .andExpect(content().bytes(new byte[]{1, 2, 3, 4}));
    }

    @Test
    void unRangeFueraDelArchivoDevuelve416() throws Exception {
        byte[] audio = {1, 2, 3, 4, 5, 6, 7, 8};
        Path archivo = archivoDeAudio(audio);
        when(audioService.audio(1L, 10L, 30L))
                .thenReturn(new ArchivoAudio(archivo, "evidencia-30.mp3", "audio/mpeg", audio.length));

        mvc.perform(get("/api/docente/aulas/10/evidencias/30/audio")
                        .header("X-Docente-Id", 1)
                        .header(HttpHeaders.RANGE, "bytes=100-200"))
                .andExpect(status().isRequestedRangeNotSatisfiable())
                .andExpect(header().string(HttpHeaders.CONTENT_RANGE, "bytes */8"));
    }

    @Test
    void unRangeConFormatoInvalidoDevuelve416() throws Exception {
        byte[] audio = {1, 2, 3, 4, 5, 6, 7, 8};
        Path archivo = archivoDeAudio(audio);
        when(audioService.audio(1L, 10L, 30L))
                .thenReturn(new ArchivoAudio(archivo, "evidencia-30.mp3", "audio/mpeg", audio.length));

        mvc.perform(get("/api/docente/aulas/10/evidencias/30/audio")
                        .header("X-Docente-Id", 1)
                        .header(HttpHeaders.RANGE, "lineas=0-3"))
                .andExpect(status().isRequestedRangeNotSatisfiable())
                .andExpect(header().string(HttpHeaders.CONTENT_RANGE, "bytes */8"));
    }

    @Test
    void unaEvidenciaDeTextoNoTieneAudio() throws Exception {
        when(audioService.audio(1L, 10L, 29L))
                .thenThrow(new ReglaNegocioException(
                        "La evidencia no es de audio", HttpStatus.BAD_REQUEST));

        mvc.perform(get("/api/docente/aulas/10/evidencias/29/audio")
                        .header("X-Docente-Id", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La evidencia no es de audio"));
    }

    @Test
    void elAudioDeUnAulaAjenaDevuelve403() throws Exception {
        when(audioService.audio(2L, 10L, 30L))
                .thenThrow(new ReglaNegocioException(
                        "El aula no pertenece al docente", HttpStatus.FORBIDDEN));

        mvc.perform(get("/api/docente/aulas/10/evidencias/30/audio")
                        .header("X-Docente-Id", 2))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("El aula no pertenece al docente"));
    }

    @Test
    void unAudioQueNoEstaEnLaCarpetaDevuelve404() throws Exception {
        when(audioService.audio(1L, 10L, 30L))
                .thenThrow(new ReglaNegocioException(
                        "El audio no está disponible", HttpStatus.NOT_FOUND));

        mvc.perform(get("/api/docente/aulas/10/evidencias/30/audio")
                        .header("X-Docente-Id", 1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("El audio no está disponible"));
    }

    @Test
    void elAudioExigeElHeaderDelDocente() throws Exception {
        mvc.perform(get("/api/docente/aulas/10/evidencias/30/audio"))
                .andExpect(status().isBadRequest());
    }

    private Path archivoDeAudio(byte[] contenido) throws Exception {
        Path archivo = carpeta.resolve("evidencia-30.mp3");
        Files.write(archivo, contenido);
        return archivo;
    }
}
