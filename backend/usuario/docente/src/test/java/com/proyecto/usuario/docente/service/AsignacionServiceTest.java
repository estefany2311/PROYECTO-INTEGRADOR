package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.AsignacionResponse;
import com.proyecto.usuario.docente.dto.AsignarLecturaRequest;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Asignacion;
import com.proyecto.usuario.docente.model.Aula;
import com.proyecto.usuario.docente.model.Estudiante;
import com.proyecto.usuario.docente.model.Lectura;
import com.proyecto.usuario.docente.repository.AsignacionRepository;
import com.proyecto.usuario.docente.repository.AulaRepository;
import com.proyecto.usuario.docente.repository.EstudianteRepository;
import com.proyecto.usuario.docente.repository.LecturaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * T-192 (US-36): reglas del endpoint que asigna una lectura a un alumno.
 * Las pruebas de "alumno de otra aula" y "lectura inactiva" llegan en T-231.
 */
@ExtendWith(MockitoExtension.class)
class AsignacionServiceTest {

    @Mock
    AsignacionRepository asignacionRepository;

    @Mock
    AulaRepository aulaRepository;

    @Mock
    EstudianteRepository estudianteRepository;

    @Mock
    LecturaRepository lecturaRepository;

    @InjectMocks
    AsignacionService service;

    private Aula aula;
    private Estudiante ana;

    @BeforeEach
    void preparar() {
        aula = new Aula();
        aula.setNombre("Aula Rural");
        aula.setDocenteId(1L);

        ana = new Estudiante();
        ana.setNombres("Ana");
        ana.setApellidos("Quispe");
        ana.setAula(aula);
    }

    @Test
    void asignaLaLecturaAlAlumnoDelAula() {
        Lectura lectura = lecturaActiva();
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(estudianteRepository.findByIdAndAulaId(5L, 10L)).thenReturn(Optional.of(ana));
        when(lecturaRepository.findById(7L)).thenReturn(Optional.of(lectura));
        when(asignacionRepository.existsByEstudianteIdAndLecturaId(5L, 7L)).thenReturn(false);
        when(asignacionRepository.saveAndFlush(any(Asignacion.class)))
                .thenAnswer(i -> i.getArgument(0));

        AsignacionResponse resp = service.asignar(1L, 10L, new AsignarLecturaRequest(7L, 5L));

        assertEquals(7L, resp.lecturaId());
        assertEquals(5L, resp.estudianteId());
        assertNotNull(resp.fechaAsignacion());

        ArgumentCaptor<Asignacion> guardada = ArgumentCaptor.forClass(Asignacion.class);
        verify(asignacionRepository).saveAndFlush(guardada.capture());
        assertEquals(7L, guardada.getValue().getLecturaId());
        assertSame(ana, guardada.getValue().getEstudiante());
    }

    @Test
    void rechazaAulaInexistente() {
        when(aulaRepository.findById(99L)).thenReturn(Optional.empty());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.asignar(1L, 99L, new AsignarLecturaRequest(7L, 5L)));

        assertEquals(HttpStatus.NOT_FOUND, ex.getEstado());
        verify(asignacionRepository, never()).saveAndFlush(any());
    }

    @Test
    void rechazaAulaDeOtroDocente() {
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.asignar(2L, 10L, new AsignarLecturaRequest(7L, 5L)));

        assertEquals(HttpStatus.FORBIDDEN, ex.getEstado());
        verify(asignacionRepository, never()).saveAndFlush(any());
    }

    @Test
    void rechazaAlumnoDeOtraAula() {
        // el repositorio busca por (id, aulaId): un alumno de otra aula no aparece
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(estudianteRepository.findByIdAndAulaId(5L, 10L)).thenReturn(Optional.empty());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.asignar(1L, 10L, new AsignarLecturaRequest(7L, 5L)));

        assertEquals(HttpStatus.NOT_FOUND, ex.getEstado());
        assertEquals("El alumno no existe en esta aula", ex.getMessage());
        verifyNoInteractions(lecturaRepository); // ni siquiera se busca la lectura
        verify(asignacionRepository, never()).saveAndFlush(any());
    }

    @Test
    void rechazaLecturaInexistente() {
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(estudianteRepository.findByIdAndAulaId(5L, 10L)).thenReturn(Optional.of(ana));
        when(lecturaRepository.findById(99L)).thenReturn(Optional.empty());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.asignar(1L, 10L, new AsignarLecturaRequest(99L, 5L)));

        assertEquals(HttpStatus.NOT_FOUND, ex.getEstado());
        verify(asignacionRepository, never()).saveAndFlush(any());
    }

    // T-231: una lectura desactivada no se puede asignar
    @Test
    void rechazaLecturaInactiva() {
        Lectura lectura = lecturaInactiva();
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(estudianteRepository.findByIdAndAulaId(5L, 10L)).thenReturn(Optional.of(ana));
        when(lecturaRepository.findById(7L)).thenReturn(Optional.of(lectura));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.asignar(1L, 10L, new AsignarLecturaRequest(7L, 5L)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getEstado());
        assertEquals("La lectura no está activa", ex.getMessage());
        verify(asignacionRepository, never()).saveAndFlush(any());
    }

    @Test
    void rechazaLaMismaLecturaDosVecesParaElMismoAlumno() {
        Lectura lectura = lecturaActiva();
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(estudianteRepository.findByIdAndAulaId(5L, 10L)).thenReturn(Optional.of(ana));
        when(lecturaRepository.findById(7L)).thenReturn(Optional.of(lectura));
        when(asignacionRepository.existsByEstudianteIdAndLecturaId(5L, 7L)).thenReturn(true);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.asignar(1L, 10L, new AsignarLecturaRequest(7L, 5L)));

        assertEquals(HttpStatus.CONFLICT, ex.getEstado());
        verify(asignacionRepository, never()).saveAndFlush(any());
    }

    // dos peticiones simultáneas: el índice único de la BD también responde 409
    @Test
    void traduceLaViolacionDelIndiceUnicoAConflicto() {
        Lectura lectura = lecturaActiva();
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(estudianteRepository.findByIdAndAulaId(5L, 10L)).thenReturn(Optional.of(ana));
        when(lecturaRepository.findById(7L)).thenReturn(Optional.of(lectura));
        when(asignacionRepository.existsByEstudianteIdAndLecturaId(5L, 7L)).thenReturn(false);
        when(asignacionRepository.saveAndFlush(any(Asignacion.class)))
                .thenThrow(new DataIntegrityViolationException("uq_asignacion_lectura_estudiante"));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.asignar(1L, 10L, new AsignarLecturaRequest(7L, 5L)));

        assertEquals(HttpStatus.CONFLICT, ex.getEstado());
    }

    // Lectura no tiene setters (solo se lee): se simula con Mockito
    private Lectura lecturaActiva() {
        Lectura lectura = mock(Lectura.class);
        when(lectura.isActivo()).thenReturn(true);
        return lectura;
    }

    private Lectura lecturaInactiva() {
        Lectura lectura = mock(Lectura.class);
        when(lectura.isActivo()).thenReturn(false);
        return lectura;
    }
}
