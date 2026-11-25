package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.almacen.AlmacenAudio;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Aula;
import com.proyecto.usuario.docente.model.Evidencia;
import com.proyecto.usuario.docente.model.Estudiante;
import com.proyecto.usuario.docente.model.Nivel;
import com.proyecto.usuario.docente.repository.AulaRepository;
import com.proyecto.usuario.docente.repository.EvidenciaRepository;
import com.proyecto.usuario.docente.service.AudioEvidenciaService.ArchivoAudio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T-372 (US-32): el audio solo se entrega al docente dueño del aula y solo si
 * la evidencia es de audio y el archivo está en la carpeta configurada.
 */
@ExtendWith(MockitoExtension.class)
class AudioEvidenciaServiceTest {

    @Mock
    AulaRepository aulaRepository;

    @Mock
    EvidenciaRepository evidenciaRepository;

    @Mock
    AlmacenAudio almacen;

    AudioEvidenciaService service;
    Aula aula;
    Estudiante estudiante;
    Nivel nivel;

    @TempDir
    Path carpeta;

    @BeforeEach
    void preparar() {
        service = new AudioEvidenciaService(aulaRepository, evidenciaRepository, almacen);

        aula = new Aula();
        ReflectionTestUtils.setField(aula, "id", 10L);
        aula.setDocenteId(1L);

        estudiante = new Estudiante();
        ReflectionTestUtils.setField(estudiante, "id", 7L);
        nivel = new Nivel((short) 1, (short) 1, "Primeros pasos", "lectura", null);
    }

    @Test
    void entregaElArchivoDeUnaEvidenciaDeAudioDelAula() throws Exception {
        Evidencia audio = Evidencia.audio(estudiante, nivel, "evidencia-30.mp3");
        ReflectionTestUtils.setField(audio, "id", 30L);

        Path archivo = carpeta.resolve("evidencia-30.mp3");
        Files.write(archivo, new byte[]{1, 2, 3, 4, 5});

        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(evidenciaRepository.findByIdAndEstudianteAulaId(30L, 10L))
                .thenReturn(Optional.of(audio));
        when(almacen.resolver("evidencia-30.mp3")).thenReturn(archivo);
        when(almacen.tipoContenido(archivo)).thenReturn("audio/mpeg");

        ArchivoAudio resultado = service.audio(1L, 10L, 30L);

        assertEquals(archivo, resultado.ruta());
        assertEquals("evidencia-30.mp3", resultado.nombre());
        assertEquals("audio/mpeg", resultado.tipoContenido());
        assertEquals(5L, resultado.tamanio());
    }

    @Test
    void unaEvidenciaDeTextoDevuelve400() {
        Evidencia texto = Evidencia.texto(estudiante, nivel, "Mi mamá prepara sopa.");
        ReflectionTestUtils.setField(texto, "id", 29L);

        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(evidenciaRepository.findByIdAndEstudianteAulaId(29L, 10L))
                .thenReturn(Optional.of(texto));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.audio(1L, 10L, 29L));

        assertEquals(HttpStatus.BAD_REQUEST, error.getEstado());
        assertEquals("La evidencia no es de audio", error.getMessage());
        verify(almacen, never()).resolver(any());
    }

    @Test
    void unaEvidenciaDeOtraAulaDevuelve404() {
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(evidenciaRepository.findByIdAndEstudianteAulaId(99L, 10L))
                .thenReturn(Optional.empty());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.audio(1L, 10L, 99L));

        assertEquals(HttpStatus.NOT_FOUND, error.getEstado());
        assertEquals("La evidencia no existe en esta aula", error.getMessage());
        verify(almacen, never()).resolver(any());
    }

    @Test
    void unAulaInexistenteDevuelve404() {
        when(aulaRepository.findById(99L)).thenReturn(Optional.empty());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.audio(1L, 99L, 30L));

        assertEquals(HttpStatus.NOT_FOUND, error.getEstado());
        assertEquals("El aula no existe", error.getMessage());
        verify(evidenciaRepository, never()).findByIdAndEstudianteAulaId(any(), any());
    }

    @Test
    void unAulaAjenaDevuelve403() {
        Aula ajena = new Aula();
        ReflectionTestUtils.setField(ajena, "id", 10L);
        ajena.setDocenteId(2L);
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(ajena));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.audio(1L, 10L, 30L));

        assertEquals(HttpStatus.FORBIDDEN, error.getEstado());
        assertEquals("El aula no pertenece al docente", error.getMessage());
        verify(evidenciaRepository, never()).findByIdAndEstudianteAulaId(any(), any());
    }

    @Test
    void unAudioQueNoEstaEnLaCarpetaDevuelve404() {
        Evidencia audio = Evidencia.audio(estudiante, nivel, "evidencia-30.mp3");
        ReflectionTestUtils.setField(audio, "id", 30L);

        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(evidenciaRepository.findByIdAndEstudianteAulaId(30L, 10L))
                .thenReturn(Optional.of(audio));
        when(almacen.resolver("evidencia-30.mp3"))
                .thenThrow(new ReglaNegocioException(
                        "El audio no está disponible", HttpStatus.NOT_FOUND));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.audio(1L, 10L, 30L));

        assertEquals(HttpStatus.NOT_FOUND, error.getEstado());
        assertEquals("El audio no está disponible", error.getMessage());
    }
}
