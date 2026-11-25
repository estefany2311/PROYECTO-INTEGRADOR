package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.ComentarioEvidenciaResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Estudiante;
import com.proyecto.usuario.docente.model.Evidencia;
import com.proyecto.usuario.docente.model.Nivel;
import com.proyecto.usuario.docente.repository.EvidenciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

// T-387 (US-34): el alumno solo lee el comentario de sus propias evidencias
@ExtendWith(MockitoExtension.class)
class ComentarioEstudianteServiceTest {

    @Mock EvidenciaRepository evidenciaRepository;

    private ComentarioEstudianteService service;
    private Evidencia evidencia;

    @BeforeEach
    void preparar() {
        service = new ComentarioEstudianteService(evidenciaRepository);
        Nivel nivel = new Nivel((short) 1, (short) 1, "Primeros pasos", "lectura", "Lee palabras");
        evidencia = Evidencia.audio(new Estudiante(), nivel,
                "https://cdn.ejemplo.pe/evidencias/ana-nivel1.mp3");
        ReflectionTestUtils.setField(evidencia, "id", 30L);
    }

    @Test
    void devuelveElComentarioDelDocente() {
        evidencia.setComentario("Muy bien leído; practica las pausas.");
        when(evidenciaRepository.findByIdAndEstudianteId(30L, 1L))
                .thenReturn(Optional.of(evidencia));

        ComentarioEvidenciaResponse respuesta = service.obtenerComentario(1L, 30L);

        assertEquals(30L, respuesta.evidenciaId());
        assertEquals("Muy bien leído; practica las pausas.", respuesta.comentario());
    }

    @Test
    void sinComentarioDevuelveNulo() {
        when(evidenciaRepository.findByIdAndEstudianteId(30L, 1L))
                .thenReturn(Optional.of(evidencia));

        assertNull(service.obtenerComentario(1L, 30L).comentario());
    }

    @Test
    void laEvidenciaDeOtroAlumnoDevuelve404() {
        when(evidenciaRepository.findByIdAndEstudianteId(30L, 2L))
                .thenReturn(Optional.empty());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.obtenerComentario(2L, 30L));

        assertEquals(HttpStatus.NOT_FOUND, error.getEstado());
        assertEquals("La evidencia no existe", error.getMessage());
    }
}
