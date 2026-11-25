package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.PuntajeEstudianteResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Estudiante;
import com.proyecto.usuario.docente.model.Evidencia;
import com.proyecto.usuario.docente.model.Nivel;
import com.proyecto.usuario.docente.model.PuntajeEstudiante;
import com.proyecto.usuario.docente.model.PuntajeNivelEstudiante;
import com.proyecto.usuario.docente.model.ReglaPuntuacion;
import com.proyecto.usuario.docente.repository.EstudianteRepository;
import com.proyecto.usuario.docente.repository.EvidenciaRepository;
import com.proyecto.usuario.docente.repository.PuntajeEstudianteRepository;
import com.proyecto.usuario.docente.repository.PuntajeNivelEstudianteRepository;
import com.proyecto.usuario.docente.repository.ReglaPuntuacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regla única de puntos y estrellas (US-09 / US-53): puntos = máximo × nota / 100,
 * estrellas por umbrales, y solo cuenta el mejor intento por alumno y nivel.
 */
@ExtendWith(MockitoExtension.class)
class PuntajeServiceTest {

    @Mock EstudianteRepository estudianteRepository;
    @Mock PuntajeEstudianteRepository puntajeEstudianteRepository;
    @Mock PuntajeNivelEstudianteRepository puntajeNivelRepository;
    @Mock EvidenciaRepository evidenciaRepository;
    @Mock ReglaPuntuacionRepository reglaRepository;

    private PuntajeService service;
    private Estudiante estudiante;
    private Nivel nivelLectura;
    private Nivel nivelOralidad;
    private PuntajeEstudiante total;

    @BeforeEach
    void preparar() {
        service = new PuntajeService(estudianteRepository, puntajeEstudianteRepository,
                puntajeNivelRepository, evidenciaRepository, reglaRepository);

        estudiante = new Estudiante();
        ReflectionTestUtils.setField(estudiante, "id", 7L);
        nivelLectura = new Nivel((short) 1, (short) 1, "Primeros pasos", "lectura", "Lee palabras");
        nivelOralidad = new Nivel((short) 3, (short) 3, "Cuento corto", "oralidad", "Cuenta un cuento");

        // reglas de Daniela por defecto: lectura 10 puntos, umbrales 50 / 70 / 90
        lenient().when(reglaRepository.porTipo("lectura"))
                .thenReturn(new ReglaPuntuacion("lectura", 10, 50, 70, 90));
        lenient().when(reglaRepository.porTipo("oralidad"))
                .thenReturn(new ReglaPuntuacion("oralidad", 15, 55, 75, 90));
        lenient().when(reglaRepository.porTipo("escritura"))
                .thenReturn(ReglaPuntuacion.porDefecto("escritura"));

        total = PuntajeEstudiante.inicial(estudiante);
        lenient().when(estudianteRepository.findById(7L)).thenReturn(Optional.of(estudiante));
        lenient().when(puntajeEstudianteRepository.findByEstudianteId(7L)).thenReturn(Optional.of(total));
        lenient().when(puntajeEstudianteRepository.save(any(PuntajeEstudiante.class)))
                .thenAnswer(i -> i.getArgument(0));
        lenient().when(puntajeNivelRepository.save(any(PuntajeNivelEstudiante.class)))
                .thenAnswer(i -> i.getArgument(0));
    }

    private Evidencia audio(Nivel nivel, Integer puntajeIa) {
        Evidencia e = Evidencia.audio(estudiante, nivel, "https://cdn.ejemplo.pe/a.mp3");
        if (puntajeIa != null) {
            e.setPuntajeIa(puntajeIa.shortValue());
        }
        return e;
    }

    private void dadoLosIntentos(Nivel nivel, Evidencia... intentos) {
        when(evidenciaRepository.findByEstudianteIdAndNivelId(7L, nivel.getId()))
                .thenReturn(List.of(intentos));
    }

    private void dadaUnaFilaDelNivel(Nivel nivel, int nota, int puntos, int estrellas) {
        PuntajeNivelEstudiante fila = new PuntajeNivelEstudiante(7L, nivel.getId());
        ReflectionTestUtils.setField(fila, "id", 99L);
        fila.registrar(nota, puntos, estrellas);
        when(puntajeNivelRepository.findByEstudianteIdAndNivelId(7L, nivel.getId()))
                .thenReturn(Optional.of(fila));
    }

    // ---- nota del intento ----

    @Test
    void laNotaDeLecturaEsLaPrecisionDeLaIa() {
        assertEquals(85, PuntajeService.notaDe(audio(nivelLectura, 85), "lectura"));
    }

    @Test
    void laNotaDeOralidadEsLaFluidezYNoLaPrecision() {
        Evidencia e = audio(nivelOralidad, 90);
        e.setPuntajeFluidez((short) 64);

        assertEquals(64, PuntajeService.notaDe(e, "oralidad"));
    }

    @Test
    void laCalificacionFinalDelDocenteManda() {
        Evidencia e = audio(nivelLectura, 85);
        e.setCalificacionFinal((short) 70);

        assertEquals(70, PuntajeService.notaDe(e, "lectura"));
    }

    @Test
    void sinNotaTodavia() {
        assertNull(PuntajeService.notaDe(audio(nivelLectura, null), "lectura"));
        assertNull(PuntajeService.notaDe(audio(nivelOralidad, 90), "oralidad"));
    }

    // ---- puntos y estrellas de una nota ----

    @Test
    void lasEstrellasYLosPuntosSalenDeLasReglasDeDaniela() {
        assertEquals(0, service.estrellasPara("lectura", 49));
        assertEquals(1, service.estrellasPara("lectura", 50));
        assertEquals(2, service.estrellasPara("lectura", 70));
        assertEquals(3, service.estrellasPara("Lectura", 90));
        assertEquals(9, service.puntosPara("lectura", 85)); // 10 × 85 / 100 = 8.5 -> 9
    }

    @Test
    void sinReglaDelAdminSeUsan100PuntosYUmbrales60_80_95() {
        assertEquals(100, service.puntosPara("escritura", 100));
        assertEquals(0, service.estrellasPara("escritura", 59));
        assertEquals(1, service.estrellasPara("escritura", 60));
        assertEquals(2, service.estrellasPara("escritura", 80));
        assertEquals(3, service.estrellasPara("escritura", 95));
    }

    @Test
    void unTipoOUnaNotaInvalidaDevuelve400() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ReglaNegocioException.class,
                () -> service.estrellasPara("baile", 80)).getEstado());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ReglaNegocioException.class,
                () -> service.puntosPara("lectura", 101)).getEstado());
    }

    // ---- mejor intento por alumno y nivel ----

    @Test
    void elPrimerIntentoSumaSusPuntosYEstrellasAlTotal() {
        dadoLosIntentos(nivelLectura, audio(nivelLectura, 85));
        when(puntajeNivelRepository.findByEstudianteIdAndNivelId(7L, (short) 1))
                .thenReturn(Optional.empty());

        service.recalcularNivel(7L, nivelLectura);

        assertEquals(9, total.getPuntos());       // 10 × 85 / 100 = 8.5 -> 9
        assertEquals(2, total.getEstrellas());    // 85 >= 70
    }

    @Test
    void siMejoraSeSumaSoloLaDiferencia() {
        dadoLosIntentos(nivelLectura, audio(nivelLectura, 60), audio(nivelLectura, 95));
        dadaUnaFilaDelNivel(nivelLectura, 60, 6, 1);
        total.setPuntos(6);
        total.setEstrellas(1);

        service.recalcularNivel(7L, nivelLectura);

        assertEquals(10, total.getPuntos());      // 6 + (10 - 6)
        assertEquals(3, total.getEstrellas());    // 1 + (3 - 1)
    }

    @Test
    void unIntentoPeorNoRestaNiSuma() {
        dadoLosIntentos(nivelLectura, audio(nivelLectura, 95), audio(nivelLectura, 40));
        dadaUnaFilaDelNivel(nivelLectura, 95, 10, 3);
        total.setPuntos(10);
        total.setEstrellas(3);

        service.recalcularNivel(7L, nivelLectura);

        assertEquals(10, total.getPuntos());
        assertEquals(3, total.getEstrellas());
        verify(puntajeEstudianteRepository, never()).save(any(PuntajeEstudiante.class));
    }

    @Test
    void recalcularDosVecesSeguidasNoSumaDosVeces() {
        dadoLosIntentos(nivelLectura, audio(nivelLectura, 85));
        when(puntajeNivelRepository.findByEstudianteIdAndNivelId(7L, (short) 1))
                .thenReturn(Optional.empty());

        service.recalcularNivel(7L, nivelLectura);
        // la fila que se guardó es la que se lee la segunda vez
        dadaUnaFilaDelNivel(nivelLectura, 85, 9, 2);
        service.recalcularNivel(7L, nivelLectura);

        assertEquals(9, total.getPuntos());
        assertEquals(2, total.getEstrellas());
    }

    @Test
    void siElDocenteBajaLaNotaSeRecalculaYSeRestaLaDiferencia() {
        Evidencia intento = audio(nivelLectura, 95);
        intento.setCalificacionFinal((short) 50);
        dadoLosIntentos(nivelLectura, intento);
        dadaUnaFilaDelNivel(nivelLectura, 95, 10, 3);
        total.setPuntos(10);
        total.setEstrellas(3);

        service.recalcularNivel(7L, nivelLectura);

        assertEquals(5, total.getPuntos());       // 10 × 50 / 100
        assertEquals(1, total.getEstrellas());    // 50 >= 50
    }

    @Test
    void siElDocenteBajaLaNotaDelMejorQuedaElSegundoMejorIntento() {
        Evidencia corregido = audio(nivelLectura, 95);
        corregido.setCalificacionFinal((short) 40);
        dadoLosIntentos(nivelLectura, corregido, audio(nivelLectura, 75));
        dadaUnaFilaDelNivel(nivelLectura, 95, 10, 3);
        total.setPuntos(10);
        total.setEstrellas(3);

        service.recalcularNivel(7L, nivelLectura);

        assertEquals(8, total.getPuntos());       // 10 × 75 / 100 = 7.5 -> 8
        assertEquals(2, total.getEstrellas());
    }

    @Test
    void elTotalNuncaQuedaNegativo() {
        Evidencia intento = audio(nivelLectura, 95);
        intento.setCalificacionFinal((short) 0);
        dadoLosIntentos(nivelLectura, intento);
        dadaUnaFilaDelNivel(nivelLectura, 95, 10, 3);
        total.setPuntos(4);      // el total ya estaba por debajo (datos anteriores)
        total.setEstrellas(1);

        service.recalcularNivel(7L, nivelLectura);

        assertEquals(0, total.getPuntos());
        assertEquals(0, total.getEstrellas());
    }

    @Test
    void sinNingunaNotaNoSeCuentaNada() {
        dadoLosIntentos(nivelLectura, audio(nivelLectura, null));

        service.recalcularNivel(7L, nivelLectura);

        verify(puntajeNivelRepository, never()).save(any(PuntajeNivelEstudiante.class));
        verify(puntajeEstudianteRepository, never()).save(any(PuntajeEstudiante.class));
    }

    @Test
    void sinTotalPrevioLoCreaEnCero() {
        when(puntajeEstudianteRepository.findByEstudianteId(7L)).thenReturn(Optional.empty());
        dadoLosIntentos(nivelLectura, audio(nivelLectura, 100));
        when(puntajeNivelRepository.findByEstudianteIdAndNivelId(7L, (short) 1))
                .thenReturn(Optional.empty());

        service.recalcularNivel(7L, nivelLectura);

        org.mockito.ArgumentCaptor<PuntajeEstudiante> captor =
                org.mockito.ArgumentCaptor.forClass(PuntajeEstudiante.class);
        verify(puntajeEstudianteRepository).save(captor.capture());
        assertEquals(10, captor.getValue().getPuntos());
        assertEquals(3, captor.getValue().getEstrellas());
    }

    @Test
    void laOralidadCuentaConLaFluidezYLasReglasDeOralidad() {
        Evidencia e = audio(nivelOralidad, 90);
        e.setPuntajeFluidez((short) 80);
        when(evidenciaRepository.findByEstudianteIdAndNivelId(7L, (short) 3)).thenReturn(List.of(e));
        when(puntajeNivelRepository.findByEstudianteIdAndNivelId(7L, (short) 3))
                .thenReturn(Optional.empty());

        service.recalcularNivel(7L, nivelOralidad);

        assertEquals(12, total.getPuntos());      // 15 × 80 / 100
        assertEquals(2, total.getEstrellas());    // 80 >= 75
    }

    @Test
    void recalcularTodoCuentaLasLecturasQueQuedaronSinContar() {
        Evidencia sinContar = audio(nivelLectura, 90);
        Evidencia oral = audio(nivelOralidad, 50);
        oral.setPuntajeFluidez((short) 100);
        when(evidenciaRepository.findByEstudianteId(7L)).thenReturn(List.of(sinContar, oral));
        when(puntajeNivelRepository.findByEstudianteIdAndNivelId(7L, (short) 1))
                .thenReturn(Optional.empty());
        when(puntajeNivelRepository.findByEstudianteIdAndNivelId(7L, (short) 3))
                .thenReturn(Optional.empty());

        service.recalcularTodo(7L);

        assertEquals(24, total.getPuntos());      // 9 (10 × 90 / 100) + 15 (15 × 100 / 100)
        assertEquals(6, total.getEstrellas());    // 3 + 3
    }

    @Test
    void elAcumuladoDeUnAlumnoSinPuntosEsCero() {
        when(puntajeEstudianteRepository.findByEstudianteId(8L)).thenReturn(Optional.empty());

        assertEquals(new PuntajeEstudianteResponse(8L, 0, 0), service.acumuladoDe(8L));
    }
}
