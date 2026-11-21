package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.SeguimientoTareasResponse;
import com.proyecto.usuario.docente.dto.TareaSeguimientoResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Asignacion;
import com.proyecto.usuario.docente.model.Aula;
import com.proyecto.usuario.docente.model.Estudiante;
import com.proyecto.usuario.docente.model.Grado;
import com.proyecto.usuario.docente.model.Lectura;
import com.proyecto.usuario.docente.repository.AsignacionRepository;
import com.proyecto.usuario.docente.repository.AulaRepository;
import com.proyecto.usuario.docente.repository.LecturaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// T-391 (US-38): el estado de cada tarea (pendiente, entregada, vencida) y la entrega a tiempo
@ExtendWith(MockitoExtension.class)
class SeguimientoTareasServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 20);

    @Mock AsignacionRepository asignacionRepository;
    @Mock AulaRepository aulaRepository;
    @Mock LecturaRepository lecturaRepository;

    private SeguimientoTareasService service;
    private Aula aula;
    private Estudiante ana;
    private Estudiante luis;

    @BeforeEach
    void preparar() {
        service = new SeguimientoTareasService(asignacionRepository, aulaRepository, lecturaRepository);

        aula = new Aula();
        aula.setDocenteId(1L);

        ana = alumno(5L, "Ana", "Quispe");
        luis = alumno(6L, "Luis", "Mamani");
    }

    private Estudiante alumno(Long id, String nombres, String apellidos) {
        Estudiante e = new Estudiante();
        e.setNombres(nombres);
        e.setApellidos(apellidos);
        e.setGrado(new Grado((short) 1, "1.º", (short) 1));
        ReflectionTestUtils.setField(e, "id", id);
        return e;
    }

    private Asignacion tarea(Long id, Long lecturaId, Estudiante alumno, LocalDate limite) {
        Asignacion a = new Asignacion(lecturaId, alumno, limite);
        ReflectionTestUtils.setField(a, "id", id);
        return a;
    }

    private Lectura lectura(Long id, String titulo) {
        Lectura l = mock(Lectura.class);
        when(l.getId()).thenReturn(id);
        when(l.getTitulo()).thenReturn(titulo);
        return l;
    }

    private void dado(List<Asignacion> asignaciones, Lectura... lecturas) {
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(asignacionRepository.findByEstudianteAulaId(10L)).thenReturn(asignaciones);
        when(lecturaRepository.findAllById(org.mockito.ArgumentMatchers.<Iterable<Long>>any()))
                .thenReturn(List.of(lecturas));
    }

    @Test
    void sinFechaLimiteLaTareaPendienteNuncaVence() {
        dado(List.of(tarea(1L, 7L, ana, null)), lectura(7L, "El zorro"));

        SeguimientoTareasResponse r = service.seguimiento(1L, 10L, null, HOY);

        assertEquals("pendiente", r.tareas().get(0).estado());
        assertEquals(1, r.pendientes());
        assertEquals(0, r.vencidas());
    }

    @Test
    void elMismoDiaDeLaFechaLimiteSigueSiendoPendiente() {
        dado(List.of(tarea(1L, 7L, ana, HOY)), lectura(7L, "El zorro"));

        assertEquals("pendiente", service.seguimiento(1L, 10L, null, HOY).tareas().get(0).estado());
    }

    @Test
    void despuesDeLaFechaLimiteSinEntregarEstaVencida() {
        dado(List.of(tarea(1L, 7L, ana, HOY.minusDays(1))), lectura(7L, "El zorro"));

        SeguimientoTareasResponse r = service.seguimiento(1L, 10L, null, HOY);

        assertEquals("vencida", r.tareas().get(0).estado());
        assertEquals(1, r.vencidas());
        assertNull(r.tareas().get(0).entregadaATiempo());
    }

    @Test
    void entregadaAntesDelLimiteEsATiempo() {
        Asignacion a = tarea(1L, 7L, ana, HOY);
        a.marcarEntregada(HOY.atTime(18, 0));
        dado(List.of(a), lectura(7L, "El zorro"));

        TareaSeguimientoResponse t = service.seguimiento(1L, 10L, null, HOY.plusDays(3)).tareas().get(0);

        // aunque hoy ya pasó la fecha, una tarea entregada no se considera vencida
        assertEquals("entregada", t.estado());
        assertTrue(t.entregadaATiempo());
    }

    @Test
    void entregadaDespuesDelLimiteSeMarcaFueraDePlazo() {
        Asignacion a = tarea(1L, 7L, ana, HOY.minusDays(2));
        a.marcarEntregada(HOY.minusDays(1).atTime(9, 0));
        dado(List.of(a), lectura(7L, "El zorro"));

        TareaSeguimientoResponse t = service.seguimiento(1L, 10L, null, HOY).tareas().get(0);

        assertEquals("entregada", t.estado());
        assertFalse(t.entregadaATiempo());
    }

    @Test
    void entregadaSinFechaLimiteSiempreEsATiempo() {
        Asignacion a = tarea(1L, 7L, ana, null);
        a.marcarEntregada(LocalDateTime.of(2026, 11, 19, 10, 0));
        dado(List.of(a), lectura(7L, "El zorro"));

        assertTrue(service.seguimiento(1L, 10L, null, HOY).tareas().get(0).entregadaATiempo());
    }

    @Test
    void ordenaVencidasPrimeroLuegoPendientesYAlFinalEntregadas() {
        Asignacion entregada = tarea(1L, 7L, ana, HOY);
        entregada.marcarEntregada(HOY.atTime(8, 0));
        Asignacion sinFecha = tarea(2L, 7L, luis, null);
        Asignacion proxima = tarea(3L, 7L, ana, HOY.plusDays(2));
        Asignacion vencida = tarea(4L, 7L, luis, HOY.minusDays(5));
        dado(List.of(entregada, sinFecha, proxima, vencida), lectura(7L, "El zorro"));

        List<Long> ids = service.seguimiento(1L, 10L, null, HOY).tareas().stream()
                .map(TareaSeguimientoResponse::id).toList();

        assertEquals(List.of(4L, 3L, 2L, 1L), ids);
    }

    @Test
    void filtraPorEstadoPeroElResumenCuentaTodoElAula() {
        Asignacion entregada = tarea(1L, 7L, ana, null);
        entregada.marcarEntregada(HOY.atTime(8, 0));
        Asignacion vencida = tarea(2L, 7L, luis, HOY.minusDays(1));
        Asignacion pendiente = tarea(3L, 7L, ana, HOY.plusDays(1));
        dado(List.of(entregada, vencida, pendiente), lectura(7L, "El zorro"));

        SeguimientoTareasResponse r = service.seguimiento(1L, 10L, "vencida", HOY);

        assertEquals(1, r.tareas().size());
        assertEquals(2L, r.tareas().get(0).id());
        assertEquals(1, r.pendientes());
        assertEquals(1, r.entregadas());
        assertEquals(1, r.vencidas());
    }

    @Test
    void incluyeElTituloDeLaLecturaYElAlumno() {
        dado(List.of(tarea(1L, 7L, ana, HOY.plusDays(1))), lectura(7L, "El zorro y el cuervo"));

        TareaSeguimientoResponse t = service.seguimiento(1L, 10L, null, HOY).tareas().get(0);

        assertEquals("El zorro y el cuervo", t.lecturaTitulo());
        assertEquals("Ana", t.nombres());
        assertEquals("1.º", t.gradoNombre());
        assertEquals(HOY.plusDays(1), t.fechaLimite());
    }

    @Test
    void unEstadoDesconocidoDevuelve400() {
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.seguimiento(1L, 10L, "atrasada", HOY));

        assertEquals(HttpStatus.BAD_REQUEST, error.getEstado());
        verifyNoInteractions(asignacionRepository);
    }

    @Test
    void elAulaDeOtroDocenteDevuelve403() {
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.seguimiento(2L, 10L, null, HOY));

        assertEquals(HttpStatus.FORBIDDEN, error.getEstado());
        verifyNoInteractions(asignacionRepository);
    }

    @Test
    void unAulaInexistenteDevuelve404() {
        when(aulaRepository.findById(99L)).thenReturn(Optional.empty());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.seguimiento(1L, 99L, null, HOY));

        assertEquals(HttpStatus.NOT_FOUND, error.getEstado());
    }
}
