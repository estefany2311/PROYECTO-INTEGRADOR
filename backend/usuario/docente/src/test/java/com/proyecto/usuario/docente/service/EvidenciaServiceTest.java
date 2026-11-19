package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.CorregirEvidenciaRequest;
import com.proyecto.usuario.docente.dto.EvidenciaDetalleResponse;
import com.proyecto.usuario.docente.dto.EvidenciaPendienteResponse;
import com.proyecto.usuario.docente.dto.HistorialCalificacionResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Aula;
import com.proyecto.usuario.docente.model.Evidencia;
import com.proyecto.usuario.docente.model.Estudiante;
import com.proyecto.usuario.docente.model.Grado;
import com.proyecto.usuario.docente.model.HistorialCalificacion;
import com.proyecto.usuario.docente.model.Nivel;
import com.proyecto.usuario.docente.repository.AulaRepository;
import com.proyecto.usuario.docente.repository.EvidenciaRepository;
import com.proyecto.usuario.docente.repository.HistorialCalificacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T-298 (US-31): la lista de evidencias pendientes del aula filtra por tipo,
 * resuelve alumno, grado y nivel, y valida el aula del docente.
 */
@ExtendWith(MockitoExtension.class)
class EvidenciaServiceTest {

    @Mock
    AulaRepository aulaRepository;

    @Mock
    EvidenciaRepository evidenciaRepository;

    @Mock
    PuntajeService puntajeService;

    @Mock
    HistorialCalificacionRepository historialRepository;

    EvidenciaService service;
    Aula aula;
    Estudiante estudiante;
    Nivel nivelAudio;
    Nivel nivelTexto;

    @BeforeEach
    void preparar() {
        service = new EvidenciaService(aulaRepository, evidenciaRepository, puntajeService,
                historialRepository);

        aula = new Aula();
        ReflectionTestUtils.setField(aula, "id", 10L);
        aula.setDocenteId(1L);

        estudiante = new Estudiante();
        ReflectionTestUtils.setField(estudiante, "id", 7L);
        estudiante.setNombres("Minji");
        estudiante.setApellidos("Prueba");
        estudiante.setGrado(new Grado((short) 5, "5.º", (short) 5));

        nivelAudio = new Nivel((short) 1, (short) 1, "Primeros pasos",
                "lectura", "Lee palabras y oraciones cortas");
        nivelTexto = new Nivel((short) 2, (short) 2, "Palabras nuevas",
                "escritura", "Escribe palabras y frases sencillas");
    }

    @Test
    void devuelveLasEvidenciasPendientesConAlumnoGradoYNivel() {
        Evidencia audio = Evidencia.audio(estudiante, nivelAudio,
                "https://cdn.ejemplo.pe/evidencias/minji-nivel1.mp3");
        ReflectionTestUtils.setField(audio, "id", 30L);
        audio.setResultadoIa("Lee con claridad y buen ritmo.");
        ReflectionTestUtils.setField(audio, "creadoEn",
                LocalDateTime.of(2026, 11, 10, 9, 0));

        Evidencia texto = Evidencia.texto(estudiante, nivelTexto,
                "Mi mamá prepara sopa. Yo pongo la mesa.");
        ReflectionTestUtils.setField(texto, "id", 29L);
        texto.setResultadoIa("Escribe oraciones completas.");
        ReflectionTestUtils.setField(texto, "creadoEn",
                LocalDateTime.of(2026, 11, 9, 15, 30));

        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(evidenciaRepository
                .findByEstudianteAulaIdAndEstadoRevisionOrderByCreadoEnDescIdDesc(
                        10L, Evidencia.PENDIENTE))
                .thenReturn(List.of(audio, texto));

        List<EvidenciaPendienteResponse> pendientes = service.pendientes(1L, 10L, null);

        assertEquals(2, pendientes.size());

        EvidenciaPendienteResponse primera = pendientes.get(0);
        assertEquals(30L, primera.id());
        assertEquals(7L, primera.estudianteId());
        assertEquals("Minji", primera.nombres());
        assertEquals("Prueba", primera.apellidos());
        assertEquals((short) 5, primera.gradoId());
        assertEquals("5.º", primera.gradoNombre());
        assertEquals((short) 1, primera.nivelOrden());
        assertEquals("Primeros pasos", primera.nivelNombre());
        assertEquals(Evidencia.AUDIO, primera.tipo());
        assertEquals("https://cdn.ejemplo.pe/evidencias/minji-nivel1.mp3",
                primera.audioUrl());
        assertNull(primera.texto());
        assertEquals("Lee con claridad y buen ritmo.", primera.resultadoIa());
        assertEquals(LocalDateTime.of(2026, 11, 10, 9, 0), primera.creadoEn());

        EvidenciaPendienteResponse segunda = pendientes.get(1);
        assertEquals(Evidencia.TEXTO, segunda.tipo());
        assertEquals("Mi mamá prepara sopa. Yo pongo la mesa.", segunda.texto());
        assertNull(segunda.audioUrl());
    }

    @Test
    void filtraPorTipoSinImportarMayusculas() {
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(evidenciaRepository
                .findByEstudianteAulaIdAndEstadoRevisionAndTipoOrderByCreadoEnDescIdDesc(
                        10L, Evidencia.PENDIENTE, Evidencia.AUDIO))
                .thenReturn(List.of());

        service.pendientes(1L, 10L, "  AUDIO ");

        verify(evidenciaRepository)
                .findByEstudianteAulaIdAndEstadoRevisionAndTipoOrderByCreadoEnDescIdDesc(
                        10L, Evidencia.PENDIENTE, Evidencia.AUDIO);
    }

    @Test
    void rechazaUnTipoQueNoExiste() {
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.pendientes(1L, 10L, "video"));

        assertEquals(HttpStatus.BAD_REQUEST, error.getEstado());
        assertEquals("El tipo de evidencia no es válido", error.getMessage());
        verify(evidenciaRepository, never())
                .findByEstudianteAulaIdAndEstadoRevisionOrderByCreadoEnDescIdDesc(
                        any(), anyString());
    }

    @Test
    void unTipoEnBlancoDevuelveTodasLasPendientes() {
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(evidenciaRepository
                .findByEstudianteAulaIdAndEstadoRevisionOrderByCreadoEnDescIdDesc(
                        10L, Evidencia.PENDIENTE))
                .thenReturn(List.of());

        assertTrue(service.pendientes(1L, 10L, "   ").isEmpty());

        verify(evidenciaRepository)
                .findByEstudianteAulaIdAndEstadoRevisionOrderByCreadoEnDescIdDesc(
                        10L, Evidencia.PENDIENTE);
        verify(evidenciaRepository, never())
                .findByEstudianteAulaIdAndEstadoRevisionAndTipoOrderByCreadoEnDescIdDesc(
                        any(), anyString(), anyString());
    }

    @Test
    void filtraPorTextoYDevuelveSoloEseTipo() {
        Evidencia texto = Evidencia.texto(estudiante, nivelTexto,
                "Mi mamá prepara sopa. Yo pongo la mesa.");
        ReflectionTestUtils.setField(texto, "id", 29L);
        texto.setResultadoIa("Escribe oraciones completas.");

        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(evidenciaRepository
                .findByEstudianteAulaIdAndEstadoRevisionAndTipoOrderByCreadoEnDescIdDesc(
                        10L, Evidencia.PENDIENTE, Evidencia.TEXTO))
                .thenReturn(List.of(texto));

        List<EvidenciaPendienteResponse> pendientes = service.pendientes(1L, 10L, "texto");

        assertEquals(1, pendientes.size());
        assertEquals(29L, pendientes.get(0).id());
        assertEquals(Evidencia.TEXTO, pendientes.get(0).tipo());
        assertEquals("Mi mamá prepara sopa. Yo pongo la mesa.",
                pendientes.get(0).texto());
        assertNull(pendientes.get(0).audioUrl());
        assertEquals("Escribe oraciones completas.", pendientes.get(0).resultadoIa());
        verify(evidenciaRepository, never())
                .findByEstudianteAulaIdAndEstadoRevisionOrderByCreadoEnDescIdDesc(
                        any(), anyString());
    }

    @Test
    void unaEvidenciaSinResultadoDeIaSeDevuelveConResultadoNulo() {
        Evidencia audio = Evidencia.audio(estudiante, nivelAudio,
                "https://cdn.ejemplo.pe/evidencias/minji-nivel1.mp3");
        ReflectionTestUtils.setField(audio, "id", 30L);

        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(evidenciaRepository
                .findByEstudianteAulaIdAndEstadoRevisionOrderByCreadoEnDescIdDesc(
                        10L, Evidencia.PENDIENTE))
                .thenReturn(List.of(audio));

        List<EvidenciaPendienteResponse> pendientes = service.pendientes(1L, 10L, null);

        assertEquals(1, pendientes.size());
        assertEquals(Evidencia.AUDIO, pendientes.get(0).tipo());
        assertNull(pendientes.get(0).resultadoIa());
    }

    @Test
    void devuelveListaVaciaCuandoNoHayPendientes() {
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(evidenciaRepository
                .findByEstudianteAulaIdAndEstadoRevisionOrderByCreadoEnDescIdDesc(
                        10L, Evidencia.PENDIENTE))
                .thenReturn(List.of());

        assertTrue(service.pendientes(1L, 10L, null).isEmpty());
    }

    @Test
    void aulaInexistenteDevuelve404() {
        when(aulaRepository.findById(99L)).thenReturn(Optional.empty());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.pendientes(1L, 99L, null));

        assertEquals(HttpStatus.NOT_FOUND, error.getEstado());
        assertEquals("El aula no existe", error.getMessage());
    }

    @Test
    void aulaDeOtroDocenteDevuelve403() {
        Aula ajena = new Aula();
        ReflectionTestUtils.setField(ajena, "id", 10L);
        ajena.setDocenteId(2L);
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(ajena));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.pendientes(1L, 10L, null));

        assertEquals(HttpStatus.FORBIDDEN, error.getEstado());
        assertEquals("El aula no pertenece al docente", error.getMessage());
        verify(evidenciaRepository, never())
                .findByEstudianteAulaIdAndEstadoRevisionOrderByCreadoEnDescIdDesc(
                        any(), anyString());
    }

    // T-371 (US-32): detalle de la evidencia con transcripción y puntaje de la IA

    @Test
    void devuelveElDetalleDeUnAudioConTranscripcionYPuntaje() {
        Evidencia audio = Evidencia.audio(estudiante, nivelAudio,
                "https://cdn.ejemplo.pe/evidencias/minji-nivel1.mp3");
        ReflectionTestUtils.setField(audio, "id", 30L);
        audio.setTranscripcion("El sol brilla. Los niños juegan en el parque.");
        audio.setPalabrasMarcadas("brilla, juegan");
        audio.setPuntajeIa((short) 85);
        audio.setResultadoIa("Lee con claridad y buen ritmo.");
        ReflectionTestUtils.setField(audio, "creadoEn",
                LocalDateTime.of(2026, 11, 10, 9, 0));

        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(evidenciaRepository.findByIdAndEstudianteAulaId(30L, 10L))
                .thenReturn(Optional.of(audio));

        EvidenciaDetalleResponse detalle = service.detalle(1L, 10L, 30L);

        assertEquals(30L, detalle.id());
        assertEquals(7L, detalle.estudianteId());
        assertEquals("Minji", detalle.nombres());
        assertEquals("Prueba", detalle.apellidos());
        assertEquals((short) 5, detalle.gradoId());
        assertEquals("5.º", detalle.gradoNombre());
        assertEquals((short) 1, detalle.nivelOrden());
        assertEquals("Primeros pasos", detalle.nivelNombre());
        assertEquals("lectura", detalle.tipoReto());
        assertEquals(Evidencia.AUDIO, detalle.tipo());
        assertEquals("https://cdn.ejemplo.pe/evidencias/minji-nivel1.mp3",
                detalle.audioUrl());
        assertNull(detalle.texto());
        assertEquals("El sol brilla. Los niños juegan en el parque.",
                detalle.transcripcion());
        assertEquals(List.of("brilla", "juegan"), detalle.palabrasMarcadas());
        assertEquals((short) 85, detalle.puntajeIa());
        assertEquals("Lee con claridad y buen ritmo.", detalle.resultadoIa());
        assertEquals(Evidencia.PENDIENTE, detalle.estadoRevision());
        assertEquals(LocalDateTime.of(2026, 11, 10, 9, 0), detalle.creadoEn());
        assertNull(detalle.revisadoEn());
    }

    @Test
    void normalizaLasPalabrasMarcadasYDescartaDuplicados() {
        Evidencia audio = Evidencia.audio(estudiante, nivelAudio,
                "https://cdn.ejemplo.pe/evidencias/minji-nivel1.mp3");
        ReflectionTestUtils.setField(audio, "id", 30L);
        audio.setPalabrasMarcadas("  brilla , juegan, brilla ,, ");
        audio.setPuntajeIa((short) 85);

        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(evidenciaRepository.findByIdAndEstudianteAulaId(30L, 10L))
                .thenReturn(Optional.of(audio));

        EvidenciaDetalleResponse detalle = service.detalle(1L, 10L, 30L);

        assertEquals(List.of("brilla", "juegan"), detalle.palabrasMarcadas());
    }

    @Test
    void unaEvidenciaSinPalabrasMarcadasDevuelveListaVacia() {
        Evidencia texto = Evidencia.texto(estudiante, nivelTexto,
                "Mi mamá prepara sopa. Yo pongo la mesa.");
        ReflectionTestUtils.setField(texto, "id", 29L);

        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(evidenciaRepository.findByIdAndEstudianteAulaId(29L, 10L))
                .thenReturn(Optional.of(texto));

        EvidenciaDetalleResponse detalle = service.detalle(1L, 10L, 29L);

        assertEquals(List.of(), detalle.palabrasMarcadas());
    }

    @Test
    void devuelveLasPalabrasMarcadasDeUnaEvidenciaDeTexto() {
        Evidencia texto = Evidencia.texto(estudiante, nivelTexto,
                "Mi mamá prepara sopa. Yo pongo la mesa.");
        ReflectionTestUtils.setField(texto, "id", 29L);
        texto.setPalabrasMarcadas("sopa, mesa");
        texto.setEstadoRevision(Evidencia.RECHAZADA);
        texto.setRevisadoEn(LocalDateTime.of(2026, 11, 11, 8, 0));

        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(evidenciaRepository.findByIdAndEstudianteAulaId(29L, 10L))
                .thenReturn(Optional.of(texto));

        EvidenciaDetalleResponse detalle = service.detalle(1L, 10L, 29L);

        assertEquals(List.of("sopa", "mesa"), detalle.palabrasMarcadas());
        assertEquals(Evidencia.RECHAZADA, detalle.estadoRevision());
        assertEquals(LocalDateTime.of(2026, 11, 11, 8, 0), detalle.revisadoEn());
    }

    @Test
    void devuelveElDetalleDeUnTextoRevisadoConSuPuntaje() {
        Evidencia texto = Evidencia.texto(estudiante, nivelTexto,
                "Mi mamá prepara sopa. Yo pongo la mesa.");
        ReflectionTestUtils.setField(texto, "id", 29L);
        texto.setPuntajeIa((short) 80);
        texto.setCalificacionFinal((short) 80);
        texto.setResultadoIa("Escribe oraciones completas.");
        texto.setEstadoRevision(Evidencia.APROBADA);
        texto.setRevisadoEn(LocalDateTime.of(2026, 11, 11, 8, 0));

        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(evidenciaRepository.findByIdAndEstudianteAulaId(29L, 10L))
                .thenReturn(Optional.of(texto));

        EvidenciaDetalleResponse detalle = service.detalle(1L, 10L, 29L);

        assertEquals(29L, detalle.id());
        assertEquals(Evidencia.TEXTO, detalle.tipo());
        assertEquals("Mi mamá prepara sopa. Yo pongo la mesa.", detalle.texto());
        assertNull(detalle.audioUrl());
        assertNull(detalle.transcripcion());
        assertEquals(List.of(), detalle.palabrasMarcadas());
        assertEquals((short) 80, detalle.puntajeIa());
        assertEquals((short) 80, detalle.calificacionFinal());
        assertNull(detalle.motivoCorreccion());
        assertEquals(Evidencia.APROBADA, detalle.estadoRevision());
        assertEquals(LocalDateTime.of(2026, 11, 11, 8, 0), detalle.revisadoEn());
    }

    @Test
    void unaEvidenciaDeOtraAulaDevuelve404() {
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(evidenciaRepository.findByIdAndEstudianteAulaId(99L, 10L))
                .thenReturn(Optional.empty());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.detalle(1L, 10L, 99L));

        assertEquals(HttpStatus.NOT_FOUND, error.getEstado());
        assertEquals("La evidencia no existe en esta aula", error.getMessage());
    }

    @Test
    void elDetalleDeUnAulaInexistenteDevuelve404() {
        when(aulaRepository.findById(99L)).thenReturn(Optional.empty());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.detalle(1L, 99L, 30L));

        assertEquals(HttpStatus.NOT_FOUND, error.getEstado());
        assertEquals("El aula no existe", error.getMessage());
        verify(evidenciaRepository, never())
                .findByIdAndEstudianteAulaId(any(), any());
    }

    @Test
    void elDetalleDeUnAulaAjenaDevuelve403() {
        Aula ajena = new Aula();
        ReflectionTestUtils.setField(ajena, "id", 10L);
        ajena.setDocenteId(2L);
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(ajena));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.detalle(1L, 10L, 30L));

        assertEquals(HttpStatus.FORBIDDEN, error.getEstado());
        assertEquals("El aula no pertenece al docente", error.getMessage());
        verify(evidenciaRepository, never())
                .findByIdAndEstudianteAulaId(any(), any());
    }

    // T-378 (US-33): aprobar y corregir la calificación de la IA

    @Test
    void aprobarCopiaElPuntajeDeLaIaYLaDejaAprobada() {
        Evidencia audio = Evidencia.audio(estudiante, nivelAudio,
                "https://cdn.ejemplo.pe/evidencias/minji-nivel1.mp3");
        ReflectionTestUtils.setField(audio, "id", 30L);
        audio.setPuntajeIa((short) 85);
        audio.setEstadoRevision(Evidencia.RECHAZADA);
        audio.setMotivoCorreccion("Motivo anterior.");

        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(evidenciaRepository.findByIdAndEstudianteAulaId(30L, 10L))
                .thenReturn(Optional.of(audio));

        EvidenciaDetalleResponse detalle = service.aprobar(1L, 10L, 30L);

        assertEquals(Evidencia.APROBADA, detalle.estadoRevision());
        assertEquals((short) 85, detalle.calificacionFinal());
        assertNull(detalle.motivoCorreccion());
        assertNotNull(detalle.revisadoEn());
        assertEquals(Evidencia.APROBADA, audio.getEstadoRevision());
        assertNull(audio.getMotivoCorreccion());
    }

    @Test
    void aprobarUnaEvidenciaSinEvaluacionDeLaIaFalla() {
        Evidencia audio = Evidencia.audio(estudiante, nivelAudio,
                "https://cdn.ejemplo.pe/evidencias/minji-nivel1.mp3");
        ReflectionTestUtils.setField(audio, "id", 30L);

        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(evidenciaRepository.findByIdAndEstudianteAulaId(30L, 10L))
                .thenReturn(Optional.of(audio));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.aprobar(1L, 10L, 30L));

        assertEquals(HttpStatus.BAD_REQUEST, error.getEstado());
        assertEquals("La evidencia no tiene evaluación de la IA", error.getMessage());
        assertEquals(Evidencia.PENDIENTE, audio.getEstadoRevision());
        assertNull(audio.getCalificacionFinal());
    }

    @Test
    void corregirGuardaLaNotaFinalYElMotivo() {
        Evidencia audio = Evidencia.audio(estudiante, nivelAudio,
                "https://cdn.ejemplo.pe/evidencias/minji-nivel1.mp3");
        ReflectionTestUtils.setField(audio, "id", 30L);
        audio.setPuntajeIa((short) 85);

        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(evidenciaRepository.findByIdAndEstudianteAulaId(30L, 10L))
                .thenReturn(Optional.of(audio));

        EvidenciaDetalleResponse detalle = service.corregir(1L, 10L, 30L,
                new CorregirEvidenciaRequest((short) 70, "  El audio tiene ruido.  "));

        assertEquals(Evidencia.CORREGIDA, detalle.estadoRevision());
        assertEquals((short) 85, detalle.puntajeIa());
        assertEquals((short) 70, detalle.calificacionFinal());
        assertEquals("El audio tiene ruido.", detalle.motivoCorreccion());
        assertNotNull(detalle.revisadoEn());
    }

    @Test
    void corregirUnaEvidenciaDeOtraAulaDevuelve404() {
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(evidenciaRepository.findByIdAndEstudianteAulaId(99L, 10L))
                .thenReturn(Optional.empty());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.corregir(1L, 10L, 99L,
                        new CorregirEvidenciaRequest((short) 70, "Nota ajustada.")));

        assertEquals(HttpStatus.NOT_FOUND, error.getEstado());
        assertEquals("La evidencia no existe en esta aula", error.getMessage());
    }

    // T-379 (US-33): el acumulado del alumno se ajusta si cambia la nota

    @Test
    void corregirRecalculaLasEstrellasEntreLasDosNotas() {
        Evidencia audio = Evidencia.audio(estudiante, nivelAudio,
                "https://cdn.ejemplo.pe/evidencias/minji-nivel1.mp3");
        ReflectionTestUtils.setField(audio, "id", 30L);
        audio.setPuntajeIa((short) 85);

        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(evidenciaRepository.findByIdAndEstudianteAulaId(30L, 10L))
                .thenReturn(Optional.of(audio));

        service.corregir(1L, 10L, 30L,
                new CorregirEvidenciaRequest((short) 70, "El audio tiene ruido."));

        // la nota vigente era la de la IA (85) y baja a 70
        verify(puntajeService).recalcularPorCambioDeNota(7L, "lectura", 85, 70);
    }

    @Test
    void aprobarUnaEvidenciaCorregidaRecalculaDesdeLaNotaDelDocente() {
        Evidencia audio = Evidencia.audio(estudiante, nivelAudio,
                "https://cdn.ejemplo.pe/evidencias/minji-nivel1.mp3");
        ReflectionTestUtils.setField(audio, "id", 30L);
        audio.setPuntajeIa((short) 85);
        audio.setCalificacionFinal((short) 70);
        audio.setEstadoRevision(Evidencia.CORREGIDA);

        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(evidenciaRepository.findByIdAndEstudianteAulaId(30L, 10L))
                .thenReturn(Optional.of(audio));

        service.aprobar(1L, 10L, 30L);

        // vuelve a la nota de la IA: el ajuste parte de la corrección anterior
        verify(puntajeService).recalcularPorCambioDeNota(7L, "lectura", 70, 85);
    }

    @Test
    void noRecalculaSiLaEvidenciaNoTieneEvaluacionDeLaIa() {
        Evidencia audio = Evidencia.audio(estudiante, nivelAudio,
                "https://cdn.ejemplo.pe/evidencias/minji-nivel1.mp3");
        ReflectionTestUtils.setField(audio, "id", 30L);

        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(evidenciaRepository.findByIdAndEstudianteAulaId(30L, 10L))
                .thenReturn(Optional.of(audio));

        assertThrows(ReglaNegocioException.class,
                () -> service.aprobar(1L, 10L, 30L));

        verify(puntajeService, never())
                .recalcularPorCambioDeNota(any(), anyString(), anyInt(), anyInt());
    }

    // T-380 (US-33): historial de calificaciones (nota IA vs. nota docente)

    @Test
    void aprobarRegistraElHistorialConLaNotaDeLaIa() {
        Evidencia audio = Evidencia.audio(estudiante, nivelAudio,
                "https://cdn.ejemplo.pe/evidencias/minji-nivel1.mp3");
        ReflectionTestUtils.setField(audio, "id", 30L);
        audio.setPuntajeIa((short) 85);

        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(evidenciaRepository.findByIdAndEstudianteAulaId(30L, 10L))
                .thenReturn(Optional.of(audio));

        service.aprobar(1L, 10L, 30L);

        ArgumentCaptor<HistorialCalificacion> captor =
                ArgumentCaptor.forClass(HistorialCalificacion.class);
        verify(historialRepository).save(captor.capture());

        HistorialCalificacion historial = captor.getValue();
        assertEquals(audio, historial.getEvidencia());
        assertEquals(1L, historial.getDocenteId());
        assertEquals(Evidencia.APROBADA, historial.getAccion());
        assertEquals((short) 85, historial.getNotaIa());
        assertEquals((short) 85, historial.getNotaDocente());
        assertNull(historial.getMotivo());
    }

    @Test
    void corregirRegistraElHistorialConLaNotaDelDocenteYElMotivo() {
        Evidencia audio = Evidencia.audio(estudiante, nivelAudio,
                "https://cdn.ejemplo.pe/evidencias/minji-nivel1.mp3");
        ReflectionTestUtils.setField(audio, "id", 30L);
        audio.setPuntajeIa((short) 85);

        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(evidenciaRepository.findByIdAndEstudianteAulaId(30L, 10L))
                .thenReturn(Optional.of(audio));

        service.corregir(1L, 10L, 30L,
                new CorregirEvidenciaRequest((short) 70, "  El audio tiene ruido.  "));

        ArgumentCaptor<HistorialCalificacion> captor =
                ArgumentCaptor.forClass(HistorialCalificacion.class);
        verify(historialRepository).save(captor.capture());

        HistorialCalificacion historial = captor.getValue();
        assertEquals(Evidencia.CORREGIDA, historial.getAccion());
        assertEquals((short) 85, historial.getNotaIa());
        assertEquals((short) 70, historial.getNotaDocente());
        assertEquals("El audio tiene ruido.", historial.getMotivo());
        assertEquals(1L, historial.getDocenteId());
    }

    @Test
    void elDetalleDevuelveElHistorialDeLaMasRecienteALaMasAntigua() {
        Evidencia audio = Evidencia.audio(estudiante, nivelAudio,
                "https://cdn.ejemplo.pe/evidencias/minji-nivel1.mp3");
        ReflectionTestUtils.setField(audio, "id", 30L);
        audio.setPuntajeIa((short) 85);
        audio.setCalificacionFinal((short) 70);
        audio.setEstadoRevision(Evidencia.CORREGIDA);

        HistorialCalificacion correccion = HistorialCalificacion.registrar(
                audio, 1L, Evidencia.CORREGIDA, (short) 85, (short) 70, "Ajuste.");
        ReflectionTestUtils.setField(correccion, "id", 2L);
        ReflectionTestUtils.setField(correccion, "creadoEn",
                LocalDateTime.of(2026, 11, 18, 10, 0));

        HistorialCalificacion aprobacion = HistorialCalificacion.registrar(
                audio, 1L, Evidencia.APROBADA, (short) 85, (short) 85, null);
        ReflectionTestUtils.setField(aprobacion, "id", 1L);
        ReflectionTestUtils.setField(aprobacion, "creadoEn",
                LocalDateTime.of(2026, 11, 17, 9, 0));

        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(evidenciaRepository.findByIdAndEstudianteAulaId(30L, 10L))
                .thenReturn(Optional.of(audio));
        when(historialRepository.findByEvidenciaIdOrderByCreadoEnDescIdDesc(30L))
                .thenReturn(List.of(correccion, aprobacion));

        EvidenciaDetalleResponse detalle = service.detalle(1L, 10L, 30L);

        assertEquals(2, detalle.historial().size());
        HistorialCalificacionResponse masReciente = detalle.historial().get(0);
        assertEquals(2L, masReciente.id());
        assertEquals(Evidencia.CORREGIDA, masReciente.accion());
        assertEquals((short) 85, masReciente.notaIa());
        assertEquals((short) 70, masReciente.notaDocente());
        assertEquals("Ajuste.", masReciente.motivo());
        assertEquals(LocalDateTime.of(2026, 11, 18, 10, 0), masReciente.creadoEn());

        HistorialCalificacionResponse anterior = detalle.historial().get(1);
        assertEquals(Evidencia.APROBADA, anterior.accion());
        assertNull(anterior.motivo());
    }
}
