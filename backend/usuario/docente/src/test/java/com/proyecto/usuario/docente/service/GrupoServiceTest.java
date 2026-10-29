package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.AsignacionGrupoResponse;
import com.proyecto.usuario.docente.dto.AsignarGrupoRequest;
import com.proyecto.usuario.docente.dto.GrupoAlumnoResponse;
import com.proyecto.usuario.docente.dto.GrupoRequest;
import com.proyecto.usuario.docente.dto.GrupoResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Asignacion;
import com.proyecto.usuario.docente.model.Aula;
import com.proyecto.usuario.docente.model.Estudiante;
import com.proyecto.usuario.docente.model.Grado;
import com.proyecto.usuario.docente.model.Grupo;
import com.proyecto.usuario.docente.model.Lectura;
import com.proyecto.usuario.docente.repository.AsignacionRepository;
import com.proyecto.usuario.docente.repository.AulaRepository;
import com.proyecto.usuario.docente.repository.EstudianteRepository;
import com.proyecto.usuario.docente.repository.GrupoRepository;
import com.proyecto.usuario.docente.repository.LecturaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * T-195 (US-37): creación de grupos, listado y asignación a todo el grupo.
 * T-232 añade los casos límite: grupo vacío, alumno repetido y asignación a todo el grupo.
 */
@ExtendWith(MockitoExtension.class)
class GrupoServiceTest {

    @Mock
    GrupoRepository grupoRepository;

    @Mock
    AulaRepository aulaRepository;

    @Mock
    EstudianteRepository estudianteRepository;

    @Mock
    LecturaRepository lecturaRepository;

    @Mock
    AsignacionRepository asignacionRepository;

    @InjectMocks
    GrupoService service;

    private Aula aula;
    private Grado primero;

    @BeforeEach
    void preparar() {
        primero = new Grado((short) 1, "1.º", (short) 1);
        aula = new Aula();
        aula.setNombre("Aula Rural");
        aula.setDocenteId(1L);
    }

    @Test
    void creaUnGrupoConAlumnosDelAula() {
        Estudiante ana = alumno("Ana", "Quispe");
        Estudiante luis = alumno("Luis", "Mamani");
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(grupoRepository.existsByAulaIdAndNombreIgnoreCase(10L, "Los exploradores"))
                .thenReturn(false);
        when(estudianteRepository.findByIdInAndAulaId(any(), eq(10L)))
                .thenReturn(List.of(ana, luis));
        when(grupoRepository.save(any(Grupo.class)))
                .thenAnswer(i -> i.getArgument(0));

        GrupoResponse resp = service.crear(1L, 10L,
                new GrupoRequest(" Los exploradores ", "  ", List.of(5L, 6L)));

        assertEquals("Los exploradores", resp.nombre());
        assertNull(resp.descripcion());
        assertEquals(List.of("Mamani", "Quispe"),
                resp.alumnos().stream().map(GrupoAlumnoResponse::apellidos).toList());

        ArgumentCaptor<Grupo> guardado = ArgumentCaptor.forClass(Grupo.class);
        verify(grupoRepository).save(guardado.capture());
        assertEquals("Los exploradores", guardado.getValue().getNombre());
        assertEquals(2, guardado.getValue().getAlumnos().size());
    }

    @Test
    void rechazaNombreDeGrupoDuplicado() {
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(grupoRepository.existsByAulaIdAndNombreIgnoreCase(10L, "Los exploradores"))
                .thenReturn(true);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.crear(1L, 10L,
                        new GrupoRequest("Los exploradores", null, List.of(5L))));

        assertEquals(HttpStatus.CONFLICT, ex.getEstado());
        verify(grupoRepository, never()).save(any());
    }

    @Test
    void rechazaAlumnoQueNoEsDelAula() {
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(grupoRepository.existsByAulaIdAndNombreIgnoreCase(10L, "Los exploradores"))
                .thenReturn(false);
        when(estudianteRepository.findByIdInAndAulaId(any(), eq(10L)))
                .thenReturn(List.of(alumno("Ana", "Quispe")));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.crear(1L, 10L,
                        new GrupoRequest("Los exploradores", null, List.of(5L, 6L))));

        assertEquals(HttpStatus.NOT_FOUND, ex.getEstado());
        assertEquals("Algún alumno no pertenece a esta aula", ex.getMessage());
        verify(grupoRepository, never()).save(any());
    }

    @Test
    void listaLosGruposConSusAlumnos() {
        Grupo exploradores = new Grupo(aula, "Los exploradores");
        exploradores.agregarAlumno(alumno("Ana", "Quispe"));
        Grupo artistas = new Grupo(aula, "Los artistas");
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(grupoRepository.findByAulaIdOrderByNombreAsc(10L))
                .thenReturn(List.of(exploradores, artistas));

        List<GrupoResponse> lista = service.listar(1L, 10L);

        assertEquals(2, lista.size());
        assertEquals("Los exploradores", lista.get(0).nombre());
        assertEquals(1, lista.get(0).alumnos().size());
        assertEquals("Quispe", lista.get(0).alumnos().get(0).apellidos());
        assertTrue(lista.get(1).alumnos().isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void asignaLaLecturaATodoElGrupo() {
        Estudiante ana = alumnoConId(5L);
        Estudiante luis = alumnoConId(6L);
        Grupo grupo = grupoConAlumnos(3L, ana, luis);
        Lectura lectura = lecturaActiva();
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(grupoRepository.findByIdAndAulaId(3L, 10L)).thenReturn(Optional.of(grupo));
        when(lecturaRepository.findById(7L)).thenReturn(Optional.of(lectura));
        when(asignacionRepository.existsByEstudianteIdAndLecturaId(5L, 7L)).thenReturn(false);
        when(asignacionRepository.existsByEstudianteIdAndLecturaId(6L, 7L)).thenReturn(false);

        AsignacionGrupoResponse resp = service.asignar(1L, 10L, 3L,
                new AsignarGrupoRequest(7L));

        assertEquals(3L, resp.grupoId());
        assertEquals(7L, resp.lecturaId());
        assertEquals(2, resp.asignados());
        assertEquals(0, resp.yaTenían());
        ArgumentCaptor<List<Asignacion>> captor = ArgumentCaptor.forClass(List.class);
        verify(asignacionRepository).saveAllAndFlush(captor.capture());
        assertEquals(2, captor.getValue().size());
        assertTrue(captor.getValue().stream()
                .allMatch(a -> a.getLecturaId().equals(7L)));
    }

    @Test
    void omiteAlAlumnoQueYaTeniaLaLectura() {
        Estudiante ana = alumnoConId(5L);
        Estudiante luis = alumnoConId(6L);
        Grupo grupo = grupoConAlumnos(3L, ana, luis);
        Lectura lectura = lecturaActiva();
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(grupoRepository.findByIdAndAulaId(3L, 10L)).thenReturn(Optional.of(grupo));
        when(lecturaRepository.findById(7L)).thenReturn(Optional.of(lectura));
        when(asignacionRepository.existsByEstudianteIdAndLecturaId(5L, 7L)).thenReturn(true);
        when(asignacionRepository.existsByEstudianteIdAndLecturaId(6L, 7L)).thenReturn(false);

        AsignacionGrupoResponse resp = service.asignar(1L, 10L, 3L,
                new AsignarGrupoRequest(7L));

        assertEquals(1, resp.asignados());
        assertEquals(1, resp.yaTenían());
    }

    @Test
    void rechazaGrupoDeOtraAula() {
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(grupoRepository.findByIdAndAulaId(3L, 10L)).thenReturn(Optional.empty());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.asignar(1L, 10L, 3L, new AsignarGrupoRequest(7L)));

        assertEquals(HttpStatus.NOT_FOUND, ex.getEstado());
        assertEquals("El grupo no existe en esta aula", ex.getMessage());
        verifyNoInteractions(lecturaRepository);
        verify(asignacionRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void rechazaAulaDeOtroDocente() {
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.crear(2L, 10L,
                        new GrupoRequest("Los exploradores", null, List.of())));

        assertEquals(HttpStatus.FORBIDDEN, ex.getEstado());
        verifyNoInteractions(grupoRepository);
    }

    @Test
    void rechazaLecturaInactiva() {
        Grupo grupo = mock(Grupo.class);
        Lectura lectura = lecturaInactiva();
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(grupoRepository.findByIdAndAulaId(3L, 10L)).thenReturn(Optional.of(grupo));
        when(lecturaRepository.findById(7L)).thenReturn(Optional.of(lectura));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.asignar(1L, 10L, 3L, new AsignarGrupoRequest(7L)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getEstado());
        assertEquals("La lectura no está activa", ex.getMessage());
        verify(asignacionRepository, never()).saveAllAndFlush(any());
    }

    // T-232: un grupo sin alumnos no puede recibir una lectura
    @Test
    void rechazaAsignarLecturaAGrupoVacio() {
        Grupo grupo = mock(Grupo.class);
        Lectura lectura = lecturaActiva();
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(grupoRepository.findByIdAndAulaId(3L, 10L)).thenReturn(Optional.of(grupo));
        when(lecturaRepository.findById(7L)).thenReturn(Optional.of(lectura));
        when(grupo.estaVacio()).thenReturn(true);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.asignar(1L, 10L, 3L, new AsignarGrupoRequest(7L)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getEstado());
        assertEquals("El grupo no tiene alumnos", ex.getMessage());
        verify(asignacionRepository, never()).saveAllAndFlush(any());
    }

    // T-232: si el docente repite un id, el alumno se guarda una sola vez
    @Test
    @SuppressWarnings("unchecked")
    void creaGrupoSinDuplicarAlumnosRepetidos() {
        Estudiante ana = alumno("Ana", "Quispe");
        Estudiante luis = alumno("Luis", "Mamani");
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(grupoRepository.existsByAulaIdAndNombreIgnoreCase(10L, "Los exploradores"))
                .thenReturn(false);
        when(estudianteRepository.findByIdInAndAulaId(any(), eq(10L)))
                .thenReturn(List.of(ana, luis));
        when(grupoRepository.save(any(Grupo.class)))
                .thenAnswer(i -> i.getArgument(0));

        GrupoResponse resp = service.crear(1L, 10L,
                new GrupoRequest("Los exploradores", null, List.of(5L, 5L, 6L)));

        assertEquals(2, resp.alumnos().size());
        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(estudianteRepository).findByIdInAndAulaId(captor.capture(), eq(10L));
        assertEquals(List.of(5L, 6L), List.copyOf(captor.getValue()));
    }

    // T-232: la lectura se crea para cada alumno del grupo, no solo para el primero
    @Test
    @SuppressWarnings("unchecked")
    void asignaLaLecturaACadaAlumnoDelGrupo() {
        Estudiante ana = alumnoConId(5L);
        Estudiante luis = alumnoConId(6L);
        Estudiante eva = alumnoConId(8L);
        Grupo grupo = grupoConAlumnos(3L, ana, luis, eva);
        Lectura lectura = lecturaActiva();
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        when(grupoRepository.findByIdAndAulaId(3L, 10L)).thenReturn(Optional.of(grupo));
        when(lecturaRepository.findById(7L)).thenReturn(Optional.of(lectura));
        when(asignacionRepository.existsByEstudianteIdAndLecturaId(5L, 7L)).thenReturn(false);
        when(asignacionRepository.existsByEstudianteIdAndLecturaId(6L, 7L)).thenReturn(false);
        when(asignacionRepository.existsByEstudianteIdAndLecturaId(8L, 7L)).thenReturn(false);

        AsignacionGrupoResponse resp = service.asignar(1L, 10L, 3L,
                new AsignarGrupoRequest(7L));

        assertEquals(3, resp.asignados());
        assertEquals(0, resp.yaTenían());
        ArgumentCaptor<List<Asignacion>> captor = ArgumentCaptor.forClass(List.class);
        verify(asignacionRepository).saveAllAndFlush(captor.capture());
        List<Long> ids = captor.getValue().stream()
                .map(a -> a.getEstudiante().getId())
                .toList();
        assertEquals(3, ids.size());
        assertTrue(ids.containsAll(List.of(5L, 6L, 8L)));
    }

    private Estudiante alumno(String nombres, String apellidos) {
        Estudiante e = new Estudiante();
        e.setNombres(nombres);
        e.setApellidos(apellidos);
        e.setAula(aula);
        e.setGrado(primero);
        return e;
    }

    // para asignar solo se necesita el id; el resto se lee de la BD
    private Estudiante alumnoConId(Long id) {
        Estudiante e = mock(Estudiante.class);
        when(e.getId()).thenReturn(id);
        return e;
    }

    private Grupo grupoConAlumnos(Long id, Estudiante... alumnos) {
        Grupo g = mock(Grupo.class);
        when(g.getId()).thenReturn(id);
        when(g.estaVacio()).thenReturn(alumnos.length == 0);
        when(g.getAlumnos()).thenReturn(new HashSet<>(List.of(alumnos)));
        return g;
    }

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
