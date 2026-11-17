package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.PuntajeEstudianteResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Estudiante;
import com.proyecto.usuario.docente.model.PuntajeEstudiante;
import com.proyecto.usuario.docente.repository.EstudianteRepository;
import com.proyecto.usuario.docente.repository.PuntajeEstudianteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * T-302 (US-09): el servicio suma puntos y estrellas al completar un reto con
 * las reglas de la US-53, crea el acumulado si no existe y valida tipo,
 * puntaje y alumno.
 */
@ExtendWith(MockitoExtension.class)
class PuntajeServiceTest {

    @Mock
    EstudianteRepository estudianteRepository;

    @Mock
    PuntajeEstudianteRepository puntajeEstudianteRepository;

    PuntajeService service;
    Estudiante estudiante;

    @BeforeEach
    void preparar() {
        service = new PuntajeService(estudianteRepository, puntajeEstudianteRepository);

        estudiante = new Estudiante();
        ReflectionTestUtils.setField(estudiante, "id", 7L);
        estudiante.setNombres("Minji");
        estudiante.setApellidos("Prueba");
    }

    private void prepararAlumnoSinAcumulado() {
        when(estudianteRepository.findById(7L)).thenReturn(Optional.of(estudiante));
        when(puntajeEstudianteRepository.findByEstudianteId(7L))
                .thenReturn(Optional.empty());
        when(puntajeEstudianteRepository.save(any(PuntajeEstudiante.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void creaElAcumuladoYSumaTresEstrellas() {
        prepararAlumnoSinAcumulado();

        PuntajeEstudianteResponse respuesta =
                service.sumarAlCompletarReto(7L, "lectura", 95);

        assertEquals(7L, respuesta.estudianteId());
        assertEquals(100, respuesta.puntos());
        assertEquals(3, respuesta.estrellas());

        ArgumentCaptor<PuntajeEstudiante> captor =
                ArgumentCaptor.forClass(PuntajeEstudiante.class);
        verify(puntajeEstudianteRepository).save(captor.capture());
        assertEquals(estudiante, captor.getValue().getEstudiante());
        assertEquals(100, captor.getValue().getPuntos());
        assertEquals(3, captor.getValue().getEstrellas());
    }

    @Test
    void acumulaSobreLaFilaExistente() {
        PuntajeEstudiante acumulado = PuntajeEstudiante.inicial(estudiante);
        acumulado.setPuntos(250);
        acumulado.setEstrellas(4);

        when(estudianteRepository.findById(7L)).thenReturn(Optional.of(estudiante));
        when(puntajeEstudianteRepository.findByEstudianteId(7L))
                .thenReturn(Optional.of(acumulado));
        when(puntajeEstudianteRepository.save(any(PuntajeEstudiante.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PuntajeEstudianteResponse respuesta =
                service.sumarAlCompletarReto(7L, "escritura", 82);

        assertEquals(350, respuesta.puntos());
        assertEquals(6, respuesta.estrellas());
    }

    @Test
    void sumaPuntosAunqueNoGaneEstrellas() {
        prepararAlumnoSinAcumulado();

        PuntajeEstudianteResponse respuesta =
                service.sumarAlCompletarReto(7L, "lectura", 59);

        assertEquals(100, respuesta.puntos());
        assertEquals(0, respuesta.estrellas());
    }

    @Test
    void unPuntajeCeroSumaPuntosSinEstrellas() {
        prepararAlumnoSinAcumulado();

        PuntajeEstudianteResponse respuesta =
                service.sumarAlCompletarReto(7L, "escritura", 0);

        assertEquals(100, respuesta.puntos());
        assertEquals(0, respuesta.estrellas());
    }

    @Test
    void unPuntajeCienGanaTresEstrellas() {
        prepararAlumnoSinAcumulado();

        PuntajeEstudianteResponse respuesta =
                service.sumarAlCompletarReto(7L, "oralidad", 100);

        assertEquals(100, respuesta.puntos());
        assertEquals(3, respuesta.estrellas());
    }

    @Test
    void respetaElUmbralDeUnaEstrella() {
        prepararAlumnoSinAcumulado();

        PuntajeEstudianteResponse respuesta =
                service.sumarAlCompletarReto(7L, "lectura", 60);

        assertEquals(1, respuesta.estrellas());
    }

    @Test
    void noLlegaATresEstrellasAntesDelUmbral() {
        prepararAlumnoSinAcumulado();

        PuntajeEstudianteResponse respuesta =
                service.sumarAlCompletarReto(7L, "oralidad", 94);

        assertEquals(2, respuesta.estrellas());
    }

    @Test
    void normalizaElTipoConMayusculasYEspacios() {
        prepararAlumnoSinAcumulado();

        PuntajeEstudianteResponse respuesta =
                service.sumarAlCompletarReto(7L, "  LECTURA ", 70);

        assertEquals(100, respuesta.puntos());
        assertEquals(1, respuesta.estrellas());
    }

    @Test
    void rechazaUnTipoDeRetoInvalido() {
        for (String tipo : new String[]{null, "   ", "video"}) {
            ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                    () -> service.sumarAlCompletarReto(7L, tipo, 80));

            assertEquals(HttpStatus.BAD_REQUEST, error.getEstado());
            assertEquals("El tipo de reto no es válido", error.getMessage());
        }

        verifyNoInteractions(estudianteRepository, puntajeEstudianteRepository);
    }

    @Test
    void rechazaUnPuntajeFueraDeRango() {
        for (int puntaje : new int[]{-1, 101}) {
            ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                    () -> service.sumarAlCompletarReto(7L, "lectura", puntaje));

            assertEquals(HttpStatus.BAD_REQUEST, error.getEstado());
            assertEquals("El puntaje del reto debe estar entre 0 y 100",
                    error.getMessage());
        }

        verifyNoInteractions(estudianteRepository, puntajeEstudianteRepository);
    }

    @Test
    void alumnoInexistenteDevuelve404() {
        when(estudianteRepository.findById(99L)).thenReturn(Optional.empty());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.sumarAlCompletarReto(99L, "oralidad", 80));

        assertEquals(HttpStatus.NOT_FOUND, error.getEstado());
        assertEquals("El alumno no existe", error.getMessage());
        verify(puntajeEstudianteRepository, never())
                .save(any(PuntajeEstudiante.class));
    }

    // T-379 (US-33): ajustar las estrellas cuando el docente cambia la nota

    private PuntajeEstudiante acumuladoCon(int puntos, int estrellas) {
        PuntajeEstudiante acumulado = PuntajeEstudiante.inicial(estudiante);
        acumulado.setPuntos(puntos);
        acumulado.setEstrellas(estrellas);
        when(puntajeEstudianteRepository.findByEstudianteId(7L))
                .thenReturn(Optional.of(acumulado));
        when(puntajeEstudianteRepository.save(any(PuntajeEstudiante.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        return acumulado;
    }

    @Test
    void subeLasEstrellasCuandoLaNotaSubeDeTramo() {
        PuntajeEstudiante acumulado = acumuladoCon(300, 4);

        // 70 vale 1 estrella y 85 vale 2: el acumulado gana una estrella
        service.recalcularPorCambioDeNota(7L, "lectura", 70, 85);

        assertEquals(300, acumulado.getPuntos());
        assertEquals(5, acumulado.getEstrellas());
        verify(puntajeEstudianteRepository).save(acumulado);
    }

    @Test
    void bajaLasEstrellasCuandoLaNotaBajaDeTramo() {
        PuntajeEstudiante acumulado = acumuladoCon(300, 4);

        // 85 vale 2 estrellas y 60 vale 1: el acumulado pierde una estrella
        service.recalcularPorCambioDeNota(7L, "escritura", 85, 60);

        assertEquals(300, acumulado.getPuntos());
        assertEquals(3, acumulado.getEstrellas());
        verify(puntajeEstudianteRepository).save(acumulado);
    }

    @Test
    void lasEstrellasNoBajanDeCero() {
        PuntajeEstudiante acumulado = acumuladoCon(100, 1);

        // la nota cae a 0 estrellas, pero el acumulado no puede quedar negativo
        service.recalcularPorCambioDeNota(7L, "oralidad", 85, 40);

        assertEquals(100, acumulado.getPuntos());
        assertEquals(0, acumulado.getEstrellas());
    }

    @Test
    void noGuardaSiElAlumnoNoTieneAcumulado() {
        when(puntajeEstudianteRepository.findByEstudianteId(7L))
                .thenReturn(Optional.empty());

        service.recalcularPorCambioDeNota(7L, "lectura", 70, 85);

        verify(puntajeEstudianteRepository, never())
                .save(any(PuntajeEstudiante.class));
    }

    @Test
    void noGuardaSiLaNotaNoCambiaDeTramo() {
        // 85 y 90 valen las mismas 2 estrellas: no hay nada que ajustar
        service.recalcularPorCambioDeNota(7L, "lectura", 85, 90);

        verifyNoInteractions(puntajeEstudianteRepository);
    }
}
