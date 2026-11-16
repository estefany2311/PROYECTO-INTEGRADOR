package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.EstudianteRequest;
import com.proyecto.usuario.docente.dto.EstudianteResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Aula;
import com.proyecto.usuario.docente.model.Estudiante;
import com.proyecto.usuario.docente.model.Grado;
import com.proyecto.usuario.docente.repository.AulaRepository;
import com.proyecto.usuario.docente.repository.EstudianteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(MockitoExtension.class)
class EstudianteServiceTest {

    @Mock
    EstudianteRepository estudianteRepository;

    @Mock
    AulaRepository aulaRepository;

    @InjectMocks
    EstudianteService service;

    private Grado primero;
    private Aula aula; // aula del docente 1 que atiende solo 1.º

    @BeforeEach
    void preparar() {
        primero = new Grado((short) 1, "1.º", (short) 1);
        aula = new Aula();
        aula.setNombre("Aula Rural");
        aula.setDocenteId(1L);
        Set<Grado> grados = new HashSet<>();
        grados.add(primero);
        aula.setGrados(grados);
    }

    @Test
    void registraAlumnoEnAulaDelDocente() {
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(estudianteRepository.existsByAulaIdAndNombresIgnoreCaseAndApellidosIgnoreCase(
                10L, "Ana", "Quispe")).thenReturn(false);
        when(estudianteRepository.save(any(Estudiante.class))).thenAnswer(i -> i.getArgument(0));

        EstudianteResponse resp = service.registrar(1L, 10L,
                new EstudianteRequest("  Ana ", " Quispe ", (short) 1));

        assertEquals("Ana", resp.nombres());
        assertEquals("Quispe", resp.apellidos());
        assertEquals("1.º", resp.gradoNombre());
        assertTrue(resp.activo());
    }

    @Test
    void rechazaAulaInexistente() {
        when(aulaRepository.findById(99L)).thenReturn(Optional.empty());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.registrar(1L, 99L, new EstudianteRequest("Ana", "Quispe", (short) 1)));

        assertEquals(HttpStatus.NOT_FOUND, ex.getEstado());
        verify(estudianteRepository, never()).save(any());
    }

    @Test
    void rechazaAulaDeOtroDocente() {
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.registrar(2L, 10L, new EstudianteRequest("Ana", "Quispe", (short) 1)));

        assertEquals(HttpStatus.FORBIDDEN, ex.getEstado());
        verify(estudianteRepository, never()).save(any());
    }

    @Test
    void rechazaGradoQueElAulaNoAtiende() {
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.registrar(1L, 10L, new EstudianteRequest("Ana", "Quispe", (short) 6)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getEstado());
        verify(estudianteRepository, never()).save(any());
    }

    @Test
    void rechazaAlumnoDuplicadoEnElAula() {
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(estudianteRepository.existsByAulaIdAndNombresIgnoreCaseAndApellidosIgnoreCase(
                10L, "Ana", "Quispe")).thenReturn(true);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.registrar(1L, 10L, new EstudianteRequest("Ana", "Quispe", (short) 1)));

        assertEquals(HttpStatus.CONFLICT, ex.getEstado());
        verify(estudianteRepository, never()).save(any());
    }

    @Test
    void listaLosAlumnosDelAula() {
        Estudiante e = new Estudiante();
        e.setNombres("Ana");
        e.setApellidos("Quispe");
        e.setAula(aula);
        e.setGrado(primero);
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(estudianteRepository.findByAulaIdOrderByApellidosAscNombresAsc(10L))
                .thenReturn(List.of(e));

        List<EstudianteResponse> lista = service.listar(1L, 10L);

        assertEquals(1, lista.size());
        assertEquals("Ana", lista.get(0).nombres());
    }

    @Test
    void noListaAlumnosDeAulaAjena() {
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.listar(2L, 10L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getEstado());
        verify(estudianteRepository, never()).findByAulaIdOrderByApellidosAscNombresAsc(any());
    }
    @Test
    void actualizaLosDatosDelAlumno() {
        Estudiante e = alumno("Luis", "Mamani");
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(estudianteRepository.findByIdAndAulaId(5L, 10L)).thenReturn(Optional.of(e));
        when(estudianteRepository.existsByAulaIdAndNombresIgnoreCaseAndApellidosIgnoreCaseAndIdNot(
                10L, "Ana", "Quispe", 5L)).thenReturn(false);
        when(estudianteRepository.save(any(Estudiante.class))).thenAnswer(i -> i.getArgument(0));

        EstudianteResponse resp = service.actualizar(1L, 10L, 5L,
                new EstudianteRequest(" Ana ", "Quispe", (short) 1));

        assertEquals("Ana", resp.nombres());
        assertEquals("Quispe", resp.apellidos());
    }

    @Test
    void rechazaActualizarAlumnoInexistente() {
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(estudianteRepository.findByIdAndAulaId(5L, 10L)).thenReturn(Optional.empty());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.actualizar(1L, 10L, 5L, new EstudianteRequest("Ana", "Quispe", (short) 1)));

        assertEquals(HttpStatus.NOT_FOUND, ex.getEstado());
        verify(estudianteRepository, never()).save(any());
    }

    @Test
    void rechazaActualizarConNombreDuplicado() {
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(estudianteRepository.findByIdAndAulaId(5L, 10L))
                .thenReturn(Optional.of(alumno("Luis", "Mamani")));
        when(estudianteRepository.existsByAulaIdAndNombresIgnoreCaseAndApellidosIgnoreCaseAndIdNot(
                10L, "Ana", "Quispe", 5L)).thenReturn(true);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.actualizar(1L, 10L, 5L, new EstudianteRequest("Ana", "Quispe", (short) 1)));

        assertEquals(HttpStatus.CONFLICT, ex.getEstado());
        verify(estudianteRepository, never()).save(any());
    }

    @Test
    void rechazaActualizarConGradoQueElAulaNoAtiende() {
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(estudianteRepository.findByIdAndAulaId(5L, 10L))
                .thenReturn(Optional.of(alumno("Luis", "Mamani")));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.actualizar(1L, 10L, 5L, new EstudianteRequest("Ana", "Quispe", (short) 6)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getEstado());
        verify(estudianteRepository, never()).save(any());
    }

    @Test
    void rechazaActualizarEnAulaDeOtroDocente() {
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.actualizar(2L, 10L, 5L, new EstudianteRequest("Ana", "Quispe", (short) 1)));

        assertEquals(HttpStatus.FORBIDDEN, ex.getEstado());
        verify(estudianteRepository, never()).save(any());
    }

    @Test
    void desactivaAlAlumno() {
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(estudianteRepository.findByIdAndAulaId(5L, 10L))
                .thenReturn(Optional.of(alumno("Ana", "Quispe")));
        when(estudianteRepository.save(any(Estudiante.class))).thenAnswer(i -> i.getArgument(0));

        EstudianteResponse resp = service.cambiarEstado(1L, 10L, 5L, false);

        assertFalse(resp.activo());
    }

    @Test
    void reactivaAlAlumno() {
        Estudiante e = alumno("Ana", "Quispe");
        e.setActivo(false);
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(estudianteRepository.findByIdAndAulaId(5L, 10L)).thenReturn(Optional.of(e));
        when(estudianteRepository.save(any(Estudiante.class))).thenAnswer(i -> i.getArgument(0));

        EstudianteResponse resp = service.cambiarEstado(1L, 10L, 5L, true);

        assertTrue(resp.activo());
    }

    @Test
    void rechazaCambiarEstadoDeAlumnoInexistente() {
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(estudianteRepository.findByIdAndAulaId(5L, 10L)).thenReturn(Optional.empty());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.cambiarEstado(1L, 10L, 5L, false));

        assertEquals(HttpStatus.NOT_FOUND, ex.getEstado());
        verify(estudianteRepository, never()).save(any());
    }

    // T-228: editar los datos no debe reactivar a un alumno dado de baja
    @Test
    void editarNoReactivaAUnAlumnoInactivo() {
        Estudiante e = alumno("Luis", "Mamani");
        e.setActivo(false);
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(estudianteRepository.findByIdAndAulaId(5L, 10L)).thenReturn(Optional.of(e));
        when(estudianteRepository.existsByAulaIdAndNombresIgnoreCaseAndApellidosIgnoreCaseAndIdNot(
                10L, "Luis", "Mamani", 5L)).thenReturn(false);
        when(estudianteRepository.save(any(Estudiante.class))).thenAnswer(i -> i.getArgument(0));

        EstudianteResponse resp = service.actualizar(1L, 10L, 5L,
                new EstudianteRequest("Luis", "Mamani", (short) 1));

        assertFalse(resp.activo());
    }

    // T-228: la tablet pide solo activos; el inactivo sigue en el portal docente pero no ahí
    @Test
    void laTabletNoVeAlAlumnoInactivo() {
        Estudiante ana = alumno("Ana", "Quispe");
        Estudiante luis = alumno("Luis", "Mamani");
        luis.setActivo(false);
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(estudianteRepository.findByAulaIdOrderByApellidosAscNombresAsc(10L))
                .thenReturn(List.of(ana, luis));
        when(estudianteRepository.findByAulaIdAndActivoTrueOrderByApellidosAscNombresAsc(10L))
                .thenReturn(List.of(ana));

        List<EstudianteResponse> portal = service.listar(1L, 10L);
        List<EstudianteResponse> tablet = service.listarActivos(1L, 10L);

        assertEquals(2, portal.size()); // el docente sí lo sigue viendo
        assertEquals(1, tablet.size()); // la tablet no
        assertEquals("Ana", tablet.get(0).nombres());
    }

    // crea un alumno de prueba en el aula del docente 1, en 1.º grado
    private Estudiante alumno(String nombres, String apellidos) {
        Estudiante e = new Estudiante();
        e.setNombres(nombres);
        e.setApellidos(apellidos);
        e.setAula(aula);
        e.setGrado(primero);
        return e;
    }
}