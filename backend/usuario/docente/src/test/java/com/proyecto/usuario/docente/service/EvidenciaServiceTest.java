package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.EvidenciaPendienteResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Aula;
import com.proyecto.usuario.docente.model.Evidencia;
import com.proyecto.usuario.docente.model.Estudiante;
import com.proyecto.usuario.docente.model.Grado;
import com.proyecto.usuario.docente.model.Nivel;
import com.proyecto.usuario.docente.repository.AulaRepository;
import com.proyecto.usuario.docente.repository.EvidenciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

    EvidenciaService service;
    Aula aula;
    Estudiante estudiante;
    Nivel nivelAudio;
    Nivel nivelTexto;

    @BeforeEach
    void preparar() {
        service = new EvidenciaService(aulaRepository, evidenciaRepository);

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
}
