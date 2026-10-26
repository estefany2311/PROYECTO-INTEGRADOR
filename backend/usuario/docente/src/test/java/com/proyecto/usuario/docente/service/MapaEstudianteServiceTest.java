package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.MapaResponse;
import com.proyecto.usuario.docente.dto.NivelMapaResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Estudiante;
import com.proyecto.usuario.docente.model.Grado;
import com.proyecto.usuario.docente.model.Nivel;
import com.proyecto.usuario.docente.model.Progreso;
import com.proyecto.usuario.docente.repository.EstudianteRepository;
import com.proyecto.usuario.docente.repository.NivelRepository;
import com.proyecto.usuario.docente.repository.ProgresoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

// T-197: el mapa arma los niveles en orden y decide el estado de cada uno
@ExtendWith(MockitoExtension.class)
class MapaEstudianteServiceTest {

    @Mock EstudianteRepository estudianteRepository;
    @Mock NivelRepository nivelRepository;
    @Mock ProgresoRepository progresoRepository;

    private MapaEstudianteService service;
    private Estudiante alumno;
    private Nivel nivel1;
    private Nivel nivel2;
    private Nivel nivel3;

    @BeforeEach
    void preparar() {
        service = new MapaEstudianteService(estudianteRepository, nivelRepository, progresoRepository);

        alumno = new Estudiante();
        alumno.setNombres("Ana");
        alumno.setApellidos("Quispe");
        alumno.setGrado(new Grado((short) 1, "1.º", (short) 1));
        ReflectionTestUtils.setField(alumno, "id", 1L);

        nivel1 = new Nivel((short) 1, (short) 1, "Primeros pasos", "lectura", "Lee palabras");
        nivel2 = new Nivel((short) 2, (short) 2, "Palabras nuevas", "escritura", "Escribe frases");
        nivel3 = new Nivel((short) 3, (short) 3, "Cuento corto", "oralidad", "Cuenta un cuento");

        lenient().when(estudianteRepository.findById(1L)).thenReturn(Optional.of(alumno));
        lenient().when(nivelRepository.findByActivoTrueOrderByOrdenAsc())
                .thenReturn(List.of(nivel1, nivel2, nivel3));
    }

    private NivelMapaResponse nivelDelMapa(MapaResponse mapa, int posicion) {
        return mapa.niveles().get(posicion);
    }

    @Test
    void sinProgreso_elPrimerNivelEstaDesbloqueadoYElRestoBloqueado() {
        lenient().when(progresoRepository.findByEstudianteId(1L)).thenReturn(List.of());

        MapaResponse mapa = service.obtenerMapa(1L);

        assertEquals(3, mapa.totalNiveles());
        assertEquals(Progreso.DESBLOQUEADO, nivelDelMapa(mapa, 0).estado());
        assertEquals(Progreso.BLOQUEADO, nivelDelMapa(mapa, 1).estado());
        assertEquals(Progreso.BLOQUEADO, nivelDelMapa(mapa, 2).estado());
        assertEquals(0, mapa.completados());
        assertEquals(0, mapa.puntos());
    }

    @Test
    void conElPrimerNivelCompletado_seDesbloqueaElSegundo() {
        lenient().when(progresoRepository.findByEstudianteId(1L))
                .thenReturn(List.of(new Progreso(alumno, nivel1, Progreso.COMPLETADO, 10)));

        MapaResponse mapa = service.obtenerMapa(1L);

        assertEquals(Progreso.COMPLETADO, nivelDelMapa(mapa, 0).estado());
        assertEquals(Progreso.DESBLOQUEADO, nivelDelMapa(mapa, 1).estado());
        assertEquals(Progreso.BLOQUEADO, nivelDelMapa(mapa, 2).estado());
    }

    @Test
    void elProgresoGuardadoMandaAunqueElNivelAnteriorEsteCompletado() {
        // caso raro pero posible: se completó el 1 y el 2 quedó bloqueado a propósito
        lenient().when(progresoRepository.findByEstudianteId(1L)).thenReturn(List.of(
                new Progreso(alumno, nivel1, Progreso.COMPLETADO, 10),
                new Progreso(alumno, nivel2, Progreso.BLOQUEADO, 0)));

        MapaResponse mapa = service.obtenerMapa(1L);

        assertEquals(Progreso.BLOQUEADO, nivelDelMapa(mapa, 1).estado());
    }

    @Test
    void calculaCompletadosYPuntosTotales() {
        lenient().when(progresoRepository.findByEstudianteId(1L)).thenReturn(List.of(
                new Progreso(alumno, nivel1, Progreso.COMPLETADO, 10),
                new Progreso(alumno, nivel2, Progreso.COMPLETADO, 15)));

        MapaResponse mapa = service.obtenerMapa(1L);

        assertEquals(2, mapa.completados());
        assertEquals(25, mapa.puntos());
        assertEquals(Progreso.DESBLOQUEADO, nivelDelMapa(mapa, 2).estado());
        assertEquals(10, nivelDelMapa(mapa, 0).puntos());
    }

    @Test
    void devuelveLosDatosDelAlumnoYElTipoDeReto() {
        lenient().when(progresoRepository.findByEstudianteId(1L)).thenReturn(List.of());

        MapaResponse mapa = service.obtenerMapa(1L);

        assertEquals(1L, mapa.estudianteId());
        assertEquals("Ana", mapa.nombres());
        assertEquals("Quispe", mapa.apellidos());
        assertEquals("lectura", nivelDelMapa(mapa, 0).tipoReto());
        assertEquals("oralidad", nivelDelMapa(mapa, 2).tipoReto());
    }

    @Test
    void alumnoInexistente_lanza404() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.obtenerMapa(99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getEstado());
        assertEquals("El alumno no existe", ex.getMessage());
    }

    @Test
    void siNoHayNiveles_elMapaVacioNoFalla() {
        lenient().when(nivelRepository.findByActivoTrueOrderByOrdenAsc()).thenReturn(List.of());
        lenient().when(progresoRepository.findByEstudianteId(1L)).thenReturn(List.of());

        MapaResponse mapa = service.obtenerMapa(1L);

        assertEquals(0, mapa.totalNiveles());
        assertTrue(mapa.niveles().isEmpty());
    }
}
