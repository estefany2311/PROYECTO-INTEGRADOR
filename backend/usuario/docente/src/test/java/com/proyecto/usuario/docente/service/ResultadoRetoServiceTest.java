package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.PuntajeEstudianteResponse;
import com.proyecto.usuario.docente.dto.ResultadoRetoResponse;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// la nota de lectura es la precisión; la de oralidad, la fluidez; el total cuenta el mejor intento
@ExtendWith(MockitoExtension.class)
class ResultadoRetoServiceTest {

    @Mock EvidenciaRepository evidenciaRepository;
    @Mock FluidezService fluidezService;
    @Mock PuntajeService puntajeService;

    private ResultadoRetoService service;
    private Estudiante estudiante;
    private final Nivel nivelLectura = new Nivel((short) 1, (short) 1, "Primeros pasos", "lectura", "Lee palabras");
    private final Nivel nivelOralidad = new Nivel((short) 3, (short) 3, "Cuento corto", "oralidad", "Cuenta un cuento");

    @BeforeEach
    void preparar() {
        service = new ResultadoRetoService(evidenciaRepository, fluidezService, puntajeService);
        estudiante = new Estudiante();
    }

    private Evidencia audio(Nivel nivel) {
        Evidencia e = Evidencia.audio(estudiante, nivel, "https://cdn.ejemplo.pe/evidencias/a.mp3");
        ReflectionTestUtils.setField(e, "id", 30L);
        when(evidenciaRepository.findByIdAndEstudianteId(30L, 5L)).thenReturn(Optional.of(e));
        return e;
    }

    @Test
    void unaLecturaListaCuentaConLaPrecisionDeLaIa() {
        Evidencia e = audio(nivelLectura);
        e.setPuntajeIa((short) 85);
        when(puntajeService.puntosPara("lectura", 85)).thenReturn(9);
        when(puntajeService.estrellasPara("lectura", 85)).thenReturn(2);
        when(puntajeService.acumuladoDe(5L)).thenReturn(new PuntajeEstudianteResponse(5L, 40, 7));

        ResultadoRetoResponse r = service.registrar(5L, 30L);

        assertEquals(new ResultadoRetoResponse(30L, "lectura", 85, 9, 2, 40, 7), r);
        verify(puntajeService).recalcularTodo(5L);
        verifyNoInteractions(fluidezService);
    }

    @Test
    void laLecturaSinPrecisionTodaviaSeEstaProcesando() {
        audio(nivelLectura);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.registrar(5L, 30L));

        assertEquals(HttpStatus.CONFLICT, error.getEstado());
        verifyNoInteractions(puntajeService);
    }

    @Test
    void laOralidadCalculaLaFluidezYCuentaConEsaNota() {
        Evidencia e = audio(nivelOralidad);
        e.setPuntajeIa((short) 90); // la precisión no cuenta en oralidad
        e.setPuntajeFluidez((short) 80);
        when(puntajeService.puntosPara("oralidad", 80)).thenReturn(12);
        when(puntajeService.estrellasPara("oralidad", 80)).thenReturn(2);
        when(puntajeService.acumuladoDe(5L)).thenReturn(new PuntajeEstudianteResponse(5L, 12, 2));

        ResultadoRetoResponse r = service.registrar(5L, 30L);

        assertEquals(80, r.nota());
        assertEquals("oralidad", r.tipoReto());
        verify(fluidezService).calcular(5L, 30L);
        verify(puntajeService).recalcularTodo(5L);
    }

    @Test
    void laCalificacionDelDocenteManda() {
        Evidencia e = audio(nivelLectura);
        e.setPuntajeIa((short) 85);
        e.setCalificacionFinal((short) 60);
        when(puntajeService.puntosPara("lectura", 60)).thenReturn(6);
        when(puntajeService.estrellasPara("lectura", 60)).thenReturn(1);
        when(puntajeService.acumuladoDe(5L)).thenReturn(new PuntajeEstudianteResponse(5L, 6, 1));

        assertEquals(60, service.registrar(5L, 30L).nota());
    }

    @Test
    void siLaFluidezSigueProcesandoseNoCuentaNada() {
        audio(nivelOralidad);
        when(fluidezService.calcular(5L, 30L)).thenThrow(new ReglaNegocioException(
                "Tu lectura todavía se está procesando", HttpStatus.CONFLICT));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.registrar(5L, 30L));

        assertEquals(HttpStatus.CONFLICT, error.getEstado());
        verifyNoInteractions(puntajeService);
    }

    @Test
    void laEvidenciaDeOtroAlumnoDevuelve404() {
        when(evidenciaRepository.findByIdAndEstudianteId(30L, 6L)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ReglaNegocioException.class,
                () -> service.registrar(6L, 30L)).getEstado());
        verifyNoInteractions(puntajeService);
    }

    @Test
    void unTextoUsaElResultadoDeEscritura() {
        Evidencia texto = Evidencia.texto(estudiante,
                new Nivel((short) 2, (short) 2, "Palabras nuevas", "escritura", "Escribe frases"),
                "Mi mamá prepara sopa.");
        when(evidenciaRepository.findByIdAndEstudianteId(31L, 5L)).thenReturn(Optional.of(texto));

        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ReglaNegocioException.class,
                () -> service.registrar(5L, 31L)).getEstado());
        verifyNoInteractions(puntajeService);
    }

    @Test
    void unAudioEnUnNivelDeEscrituraSeRechaza() {
        audio(new Nivel((short) 2, (short) 2, "Palabras nuevas", "escritura", "Escribe frases"));

        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ReglaNegocioException.class,
                () -> service.registrar(5L, 30L)).getEstado());
    }
}
