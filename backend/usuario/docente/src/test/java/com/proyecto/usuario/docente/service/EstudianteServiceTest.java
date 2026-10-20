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
}