package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.ResultadoEscrituraResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Estudiante;
import com.proyecto.usuario.docente.model.Evidencia;
import com.proyecto.usuario.docente.model.Nivel;
import com.proyecto.usuario.docente.repository.ConsignaEscrituraRepository;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// T-404 (US-22): puntaje de escritura con textos de ejemplo, guardado y estrellas una sola vez
@ExtendWith(MockitoExtension.class)
class EscrituraServiceTest {

    static final String TEXTO_BUENO =
            "Mi animal favorito es el cuy. El cuy come pasto y alfalfa. Vive en mi casa con mi familia.";
    static final String TEXTO_FLOJO = "mi animal es el perro. el perro come";
    static final String TEXTO_REGULAR = "Mi gato es negro. Tiene ojos verdes. Le gusta jugar";

    @Mock EvidenciaRepository evidenciaRepository;
    @Mock ConsignaEscrituraRepository consignaRepository;
    @Mock PuntajeService puntajeService;

    private EscrituraService service;
    private Nivel nivelEscritura;

    @BeforeEach
    void preparar() {
        service = new EscrituraService(evidenciaRepository, consignaRepository, puntajeService);
        nivelEscritura = new Nivel((short) 2, (short) 2, "Palabras nuevas", "escritura", "Escribe frases");
    }

    private Evidencia texto(String contenido, String marcadas) {
        Evidencia evidencia = Evidencia.texto(new Estudiante(), nivelEscritura, contenido);
        ReflectionTestUtils.setField(evidencia, "id", 31L);
        evidencia.setPalabrasMarcadas(marcadas);
        when(evidenciaRepository.findByIdAndEstudianteId(31L, 5L)).thenReturn(Optional.of(evidencia));
        return evidencia;
    }

    @Test
    void unTextoCompletoYSinErroresObtiene100ConTresEstrellas() {
        texto(TEXTO_BUENO, null);
        when(consignaRepository.palabrasMinimas((short) 2)).thenReturn(15);
        when(puntajeService.estrellasPara("escritura", 100)).thenReturn(3);

        ResultadoEscrituraResponse r = service.calcular(5L, 31L);

        assertEquals(100, r.puntajeEscritura());
        assertEquals(19, r.palabras());
        assertEquals(15, r.palabrasMinimas());
        assertEquals(0, r.errores());
        assertEquals(3, r.oraciones());
        assertEquals(3, r.oracionesCompletas());
        assertEquals(3, r.estrellas());
        assertEquals("¡Excelente! Escribiste muy bien.", r.mensaje());
    }

    @Test
    void unTextoFlojoConErroresObtienePuntajeBajoYConsejoDeOraciones() {
        texto(TEXTO_FLOJO, "kome, perro");
        when(consignaRepository.palabrasMinimas((short) 2)).thenReturn(15);
        when(puntajeService.estrellasPara("escritura", 41)).thenReturn(0);

        ResultadoEscrituraResponse r = service.calcular(5L, 31L);

        // ortografía 50 (2 errores en 8 palabras), longitud 53, consigna 0
        assertEquals(50, r.puntajeOrtografia());
        assertEquals(53, r.puntajeLongitud());
        assertEquals(0, r.puntajeConsigna());
        assertEquals(41, r.puntajeEscritura());
        assertEquals(2, r.errores());
        assertEquals("Sigue practicando, tú puedes. "
                + "Recuerda empezar cada oración con mayúscula y terminarla con punto.", r.mensaje());
    }

    @Test
    void unTextoRegularCombinaLasTresPartes() {
        texto(TEXTO_REGULAR, "negro");
        when(consignaRepository.palabrasMinimas((short) 2)).thenReturn(12);
        when(puntajeService.estrellasPara("escritura", 78)).thenReturn(1);

        ResultadoEscrituraResponse r = service.calcular(5L, 31L);

        // ortografía 80, longitud 83, consigna 67 (2 de 3 oraciones completas)
        assertEquals(80, r.puntajeOrtografia());
        assertEquals(83, r.puntajeLongitud());
        assertEquals(67, r.puntajeConsigna());
        assertEquals(78, r.puntajeEscritura());
        assertEquals(1, r.estrellas());
    }

    @Test
    void calcularNoGuardaNiSumaNada() {
        Evidencia evidencia = texto(TEXTO_BUENO, null);
        when(consignaRepository.palabrasMinimas((short) 2)).thenReturn(15);
        when(puntajeService.estrellasPara(anyString(), anyInt())).thenReturn(3);

        service.calcular(5L, 31L);

        assertNull(evidencia.getPuntajeIa());
        verify(puntajeService, never()).recalcularNivel(any(), any());
    }

    @Test
    void evaluarGuardaElPuntajeYElMensajeYSumaLasEstrellas() {
        Evidencia evidencia = texto(TEXTO_BUENO, null);
        when(consignaRepository.palabrasMinimas((short) 2)).thenReturn(15);
        when(puntajeService.estrellasPara("escritura", 100)).thenReturn(3);

        ResultadoEscrituraResponse r = service.evaluar(5L, 31L);

        assertEquals(100, r.puntajeEscritura());
        assertEquals((short) 100, evidencia.getPuntajeIa());
        assertEquals("¡Excelente! Escribiste muy bien.", evidencia.getResultadoIa());
        verify(puntajeService).recalcularNivel(5L, nivelEscritura);
    }

    @Test
    void evaluarDosVecesDevuelveElMismoResultado() {
        texto(TEXTO_BUENO, null);
        when(consignaRepository.palabrasMinimas((short) 2)).thenReturn(15);
        when(puntajeService.estrellasPara("escritura", 100)).thenReturn(3);

        ResultadoEscrituraResponse primera = service.evaluar(5L, 31L);
        ResultadoEscrituraResponse segunda = service.evaluar(5L, 31L);

        assertEquals(primera, segunda);
        // PuntajeService cuenta solo el mejor intento del nivel, así que repetir no duplica
        verify(puntajeService, times(2)).recalcularNivel(5L, nivelEscritura);
    }

    @Test
    void laEvidenciaDeOtroAlumnoDevuelve404() {
        when(evidenciaRepository.findByIdAndEstudianteId(31L, 6L)).thenReturn(Optional.empty());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.evaluar(6L, 31L));

        assertEquals(HttpStatus.NOT_FOUND, error.getEstado());
        verifyNoInteractions(puntajeService);
    }

    @Test
    void unaEvidenciaDeAudioNoTienePuntajeDeEscritura() {
        Evidencia audio = Evidencia.audio(new Estudiante(),
                new Nivel((short) 1, (short) 1, "Primeros pasos", "lectura", "Lee palabras"),
                "https://cdn.ejemplo.pe/evidencias/ana-nivel1.mp3");
        when(evidenciaRepository.findByIdAndEstudianteId(30L, 5L)).thenReturn(Optional.of(audio));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.evaluar(5L, 30L));

        assertEquals(HttpStatus.BAD_REQUEST, error.getEstado());
        verifyNoInteractions(puntajeService);
    }
}
