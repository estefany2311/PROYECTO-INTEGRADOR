package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.DetalleEstudianteResponse;
import com.proyecto.usuario.docente.dto.NivelMapaResponse;
import com.proyecto.usuario.docente.dto.PuntajeCompetenciaResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Aula;
import com.proyecto.usuario.docente.model.ColorSemaforo;
import com.proyecto.usuario.docente.model.Estudiante;
import com.proyecto.usuario.docente.model.Evaluacion;
import com.proyecto.usuario.docente.model.Grado;
import com.proyecto.usuario.docente.model.Progreso;
import com.proyecto.usuario.docente.repository.AulaRepository;
import com.proyecto.usuario.docente.repository.EstudianteRepository;
import com.proyecto.usuario.docente.repository.EvaluacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T-290 (US-29): el detalle reúne niveles, puntajes por competencia y
 * las últimas evidencias en una sola respuesta.
 */
@ExtendWith(MockitoExtension.class)
class DetalleEstudianteServiceTest {

    @Mock
    AulaRepository aulaRepository;

    @Mock
    EstudianteRepository estudianteRepository;

    @Mock
    EvaluacionRepository evaluacionRepository;

    @Mock
    MapaEstudianteService mapaEstudianteService;

    DetalleEstudianteService service;
    Estudiante estudiante;
    Aula aula;

    @BeforeEach
    void preparar() {
        service = new DetalleEstudianteService(
                aulaRepository, estudianteRepository, evaluacionRepository,
                mapaEstudianteService, new SemaforoService());

        estudiante = new Estudiante();
        ReflectionTestUtils.setField(estudiante, "id", 7L);
        estudiante.setNombres("Minji");
        estudiante.setApellidos("Prueba");
        estudiante.setGrado(new Grado((short) 5, "5.º", (short) 5));

        aula = mock(Aula.class);
        lenient().when(aula.getDocenteId()).thenReturn(1L);
    }

    @Test
    void devuelveNivelesPuntajesPorCompetenciaYUltimasEvidencias() {
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(estudianteRepository.findByIdAndAulaId(7L, 10L)).thenReturn(Optional.of(estudiante));

        Evaluacion evaluacionOralidad = evaluacion(3L, "oralidad", (short) 80, "2026-11-03T10:00");
        Evaluacion evaluacionEscritura = evaluacion(2L, "escritura", (short) 85, "2026-11-02T10:00");
        Evaluacion lecturaReciente = evaluacion(4L, "lectura", (short) 70, "2026-11-01T10:00");
        Evaluacion lecturaAntigua = evaluacion(1L, "lectura", (short) 90, "2026-10-30T10:00");
        when(evaluacionRepository.findByEstudianteIdOrderByEvaluadoEnDescIdDesc(7L))
                .thenReturn(List.of(evaluacionOralidad, evaluacionEscritura,
                        lecturaReciente, lecturaAntigua));

        when(mapaEstudianteService.nivelesDe(7L)).thenReturn(List.of(
                new NivelMapaResponse((short) 1, (short) 1, "Primeros pasos", "lectura",
                        "Lee palabras", Progreso.COMPLETADO, 10),
                new NivelMapaResponse((short) 2, (short) 2, "Palabras nuevas", "escritura",
                        "Escribe frases", Progreso.DESBLOQUEADO, 0)));

        DetalleEstudianteResponse detalle = service.detalle(1L, 10L, 7L);

        assertEquals(7L, detalle.estudianteId());
        assertEquals("Minji", detalle.nombres());
        assertEquals("Prueba", detalle.apellidos());
        assertEquals((short) 5, detalle.gradoId());
        assertEquals("5.º", detalle.gradoNombre());
        assertEquals(2, detalle.niveles().size());
        assertEquals(Progreso.COMPLETADO, detalle.niveles().get(0).estado());

        assertEquals(3, detalle.puntajesPorCompetencia().size());
        PuntajeCompetenciaResponse lectura = detalle.puntajesPorCompetencia().get(0);
        assertEquals("lectura", lectura.competencia());
        assertEquals(80.0, lectura.promedio(), 0.001);
        assertEquals(2, lectura.evaluaciones());
        assertEquals(70, lectura.ultimoPuntaje());
        assertEquals(ColorSemaforo.VERDE, lectura.color());

        PuntajeCompetenciaResponse escritura = detalle.puntajesPorCompetencia().get(1);
        assertEquals("escritura", escritura.competencia());
        assertEquals(85.0, escritura.promedio(), 0.001);

        PuntajeCompetenciaResponse oralidad = detalle.puntajesPorCompetencia().get(2);
        assertEquals("oralidad", oralidad.competencia());
        assertEquals(80.0, oralidad.promedio(), 0.001);

        assertEquals(82, detalle.promedioGeneral().intValue());
        assertEquals(ColorSemaforo.VERDE, detalle.colorGeneral());

        assertEquals(4, detalle.ultimasEvidencias().size());
        assertEquals("oralidad", detalle.ultimasEvidencias().get(0).competencia());
        assertEquals((short) 80, detalle.ultimasEvidencias().get(0).puntaje());
        assertEquals(LocalDateTime.of(2026, 11, 3, 10, 0),
                detalle.ultimasEvidencias().get(0).evaluadoEn());
    }

    @Test
    void sinEvaluacionesDevuelveGrisYListasVacias() {
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(estudianteRepository.findByIdAndAulaId(7L, 10L)).thenReturn(Optional.of(estudiante));
        when(evaluacionRepository.findByEstudianteIdOrderByEvaluadoEnDescIdDesc(7L))
                .thenReturn(List.of());
        when(mapaEstudianteService.nivelesDe(7L)).thenReturn(List.of());

        DetalleEstudianteResponse detalle = service.detalle(1L, 10L, 7L);

        assertNull(detalle.promedioGeneral());
        assertEquals(ColorSemaforo.GRIS, detalle.colorGeneral());
        assertTrue(detalle.puntajesPorCompetencia().isEmpty());
        assertTrue(detalle.ultimasEvidencias().isEmpty());
        assertTrue(detalle.niveles().isEmpty());
    }

    @Test
    void limitaLasEvidenciasAUltimasDiez() {
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(estudianteRepository.findByIdAndAulaId(7L, 10L)).thenReturn(Optional.of(estudiante));

        List<Evaluacion> evaluaciones = new ArrayList<>();
        for (int i = 12; i >= 1; i--) {
            evaluaciones.add(evaluacion((long) i, "lectura", (short) 50,
                    "2026-11-" + String.format("%02d", i) + "T10:00"));
        }
        when(evaluacionRepository.findByEstudianteIdOrderByEvaluadoEnDescIdDesc(7L))
                .thenReturn(evaluaciones);
        when(mapaEstudianteService.nivelesDe(7L)).thenReturn(List.of());

        DetalleEstudianteResponse detalle = service.detalle(1L, 10L, 7L);

        assertEquals(10, detalle.ultimasEvidencias().size());
        assertEquals(12L, detalle.ultimasEvidencias().get(0).id());
        assertEquals(3L, detalle.ultimasEvidencias().get(9).id());
        assertEquals(1, detalle.puntajesPorCompetencia().size());
        assertEquals(12, detalle.puntajesPorCompetencia().get(0).evaluaciones());
    }

    @Test
    void alumnoInexistenteDevuelve404() {
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(estudianteRepository.findByIdAndAulaId(99L, 10L)).thenReturn(Optional.empty());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.detalle(1L, 10L, 99L));

        assertEquals(HttpStatus.NOT_FOUND, error.getEstado());
        assertEquals("El alumno no existe en esta aula", error.getMessage());
    }

    @Test
    void alumnoDeOtraAulaDevuelve404() {
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(estudianteRepository.findByIdAndAulaId(7L, 10L)).thenReturn(Optional.empty());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.detalle(1L, 10L, 7L));

        assertEquals(HttpStatus.NOT_FOUND, error.getEstado());
        assertEquals("El alumno no existe en esta aula", error.getMessage());
    }

    @Test
    void aulaInexistenteDevuelve404() {
        when(aulaRepository.findById(99L)).thenReturn(Optional.empty());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.detalle(1L, 99L, 7L));

        assertEquals(HttpStatus.NOT_FOUND, error.getEstado());
        assertEquals("El aula no existe", error.getMessage());
    }

    @Test
    void aulaDeOtroDocenteDevuelve403() {
        Aula ajena = mock(Aula.class);
        when(ajena.getDocenteId()).thenReturn(2L);
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(ajena));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.detalle(1L, 10L, 7L));

        assertEquals(HttpStatus.FORBIDDEN, error.getEstado());
        assertEquals("El aula no pertenece al docente", error.getMessage());
    }

    private Evaluacion evaluacion(Long id, String competencia, short puntaje, String fecha) {
        Evaluacion evaluacion = new Evaluacion(estudiante, competencia, puntaje);
        ReflectionTestUtils.setField(evaluacion, "id", id);
        ReflectionTestUtils.setField(evaluacion, "evaluadoEn", LocalDateTime.parse(fecha));
        return evaluacion;
    }
}
