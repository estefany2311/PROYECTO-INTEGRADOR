package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.AsignacionResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Asignacion;
import com.proyecto.usuario.docente.model.Estudiante;
import com.proyecto.usuario.docente.repository.AsignacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

// T-391 (US-38): entregar una tarea la pasa a "entregada" y conserva la primera entrega
@ExtendWith(MockitoExtension.class)
class EntregaTareaServiceTest {

    @Mock AsignacionRepository asignacionRepository;

    private EntregaTareaService service;
    private Asignacion asignacion;

    @BeforeEach
    void preparar() {
        service = new EntregaTareaService(asignacionRepository);
        asignacion = new Asignacion(7L, new Estudiante(), LocalDate.of(2026, 11, 27));
        ReflectionTestUtils.setField(asignacion, "id", 40L);
    }

    @Test
    void entregarLaTareaGuardaElEstadoYElMomento() {
        when(asignacionRepository.findByIdAndEstudianteId(40L, 5L))
                .thenReturn(Optional.of(asignacion));
        LocalDateTime ahora = LocalDateTime.of(2026, 11, 24, 10, 30);

        AsignacionResponse resp = service.entregar(5L, 40L, ahora);

        assertEquals("entregada", resp.estado());
        assertEquals("entregada", asignacion.getEstado());
        assertEquals(ahora, asignacion.getEntregadaEn());
        assertEquals(LocalDate.of(2026, 11, 27), resp.fechaLimite());
    }

    @Test
    void entregarDosVecesConservaLaPrimeraFecha() {
        when(asignacionRepository.findByIdAndEstudianteId(40L, 5L))
                .thenReturn(Optional.of(asignacion));
        LocalDateTime primera = LocalDateTime.of(2026, 11, 24, 10, 30);

        service.entregar(5L, 40L, primera);
        service.entregar(5L, 40L, primera.plusDays(2));

        assertEquals(primera, asignacion.getEntregadaEn());
    }

    @Test
    void laTareaDeOtroAlumnoDevuelve404() {
        when(asignacionRepository.findByIdAndEstudianteId(40L, 6L))
                .thenReturn(Optional.empty());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.entregar(6L, 40L, LocalDateTime.now()));

        assertEquals(HttpStatus.NOT_FOUND, error.getEstado());
        assertEquals("La tarea no existe", error.getMessage());
    }
}
