package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.FluidezResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.ConfiguracionFluidez;
import com.proyecto.usuario.docente.model.Estudiante;
import com.proyecto.usuario.docente.model.Evidencia;
import com.proyecto.usuario.docente.model.Grado;
import com.proyecto.usuario.docente.model.Nivel;
import com.proyecto.usuario.docente.model.PalabraLeida;
import com.proyecto.usuario.docente.repository.ConfiguracionFluidezRepository;
import com.proyecto.usuario.docente.repository.EvidenciaRepository;
import com.proyecto.usuario.docente.repository.PalabraLeidaRepository;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// T-398 (US-15): el puntaje se calcula con audios de ejemplo y la configuración del grado
@ExtendWith(MockitoExtension.class)
class FluidezServiceTest {

    @Mock EvidenciaRepository evidenciaRepository;
    @Mock PalabraLeidaRepository palabraRepository;
    @Mock ConfiguracionFluidezRepository configuracionRepository;

    private FluidezService service;
    private Evidencia audio;

    @BeforeEach
    void preparar() {
        service = new FluidezService(evidenciaRepository, palabraRepository, configuracionRepository);

        Estudiante ana = new Estudiante();
        ana.setGrado(new Grado((short) 3, "3.º", (short) 3));
        Nivel nivel = new Nivel((short) 1, (short) 1, "Primeros pasos", "lectura", "Lee palabras");
        audio = Evidencia.audio(ana, nivel, "https://cdn.ejemplo.pe/evidencias/ana-nivel1.mp3");
        ReflectionTestUtils.setField(audio, "id", 30L);
    }

    private void dadoQueExisteLaEvidencia() {
        when(evidenciaRepository.findByIdAndEstudianteId(30L, 5L)).thenReturn(Optional.of(audio));
    }

    @Test
    void calculaLaFluidezDeUnaLecturaVacilanteYLaGuardaEnLaEvidencia() {
        dadoQueExisteLaEvidencia();
        when(palabraRepository.porEvidencia(30L)).thenReturn(FormulaFluidezTest.lectura(
                0.6, 3.0, 0.6, 0.6, 1.5, 0.6, 0.6, 2.5, 0.6, 0.6));
        when(configuracionRepository.porGrado(3)).thenReturn(new ConfiguracionFluidez(3, 60, 2.0));

        FluidezResponse resp = service.calcular(5L, 30L);

        assertEquals(73, resp.puntajeFluidez());
        assertEquals(42, resp.palabrasPorMinuto());
        assertEquals(60, resp.palabrasEsperadas());
        assertEquals(2, resp.pausasLargas());
        assertEquals(1, resp.vacilaciones());
        assertEquals((short) 73, audio.getPuntajeFluidez());
    }

    @Test
    void unaLecturaFluidaObtiene100YMensajeDeFelicitacion() {
        dadoQueExisteLaEvidencia();
        when(palabraRepository.porEvidencia(30L))
                .thenReturn(FormulaFluidezTest.lectura(FormulaFluidezTest.pausasIguales(10, 0.6)));
        when(configuracionRepository.porGrado(3)).thenReturn(new ConfiguracionFluidez(3, 60, 2.0));

        FluidezResponse resp = service.calcular(5L, 30L);

        assertEquals(100, resp.puntajeFluidez());
        assertEquals("¡Excelente! Lees con un ritmo muy fluido.", resp.mensaje());
    }

    @Test
    void unGradoMasExigenteBajaElPuntajeDeLaMismaLectura() {
        dadoQueExisteLaEvidencia();
        when(palabraRepository.porEvidencia(30L))
                .thenReturn(FormulaFluidezTest.lectura(FormulaFluidezTest.pausasIguales(10, 0.6)));
        // 6.º: 95 ppm esperadas y 1.5 s de tolerancia
        when(configuracionRepository.porGrado(3)).thenReturn(new ConfiguracionFluidez(3, 95, 1.5));

        FluidezResponse resp = service.calcular(5L, 30L);

        // 63 ppm de 95 esperadas: ritmo 67, pausas 100 -> 80
        assertEquals(80, resp.puntajeFluidez());
        assertEquals(95, resp.palabrasEsperadas());
    }

    @Test
    void recalcularActualizaElPuntajeGuardado() {
        dadoQueExisteLaEvidencia();
        audio.setPuntajeFluidez((short) 10);
        when(palabraRepository.porEvidencia(30L))
                .thenReturn(FormulaFluidezTest.lectura(FormulaFluidezTest.pausasIguales(10, 0.6)));
        when(configuracionRepository.porGrado(3)).thenReturn(new ConfiguracionFluidez(3, 60, 2.0));

        service.calcular(5L, 30L);

        assertEquals((short) 100, audio.getPuntajeFluidez());
    }

    @Test
    void sinPalabrasTodaviaSeEstaProcesandoYNoGuardaNada() {
        dadoQueExisteLaEvidencia();
        when(palabraRepository.porEvidencia(30L)).thenReturn(List.of());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.calcular(5L, 30L));

        assertEquals(HttpStatus.CONFLICT, error.getEstado());
        assertEquals("Tu lectura todavía se está procesando", error.getMessage());
        assertNull(audio.getPuntajeFluidez());
        verifyNoInteractions(configuracionRepository);
    }

    @Test
    void conUnaSolaPalabraNoSePuedeMedirLaFluidez() {
        dadoQueExisteLaEvidencia();
        when(palabraRepository.porEvidencia(30L))
                .thenReturn(List.of(new PalabraLeida("sol", 1.0, 1.4, 0.9)));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.calcular(5L, 30L));

        assertEquals(HttpStatus.CONFLICT, error.getEstado());
    }

    @Test
    void laEvidenciaDeOtroAlumnoDevuelve404() {
        when(evidenciaRepository.findByIdAndEstudianteId(30L, 6L)).thenReturn(Optional.empty());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.calcular(6L, 30L));

        assertEquals(HttpStatus.NOT_FOUND, error.getEstado());
        verifyNoInteractions(palabraRepository);
    }

    @Test
    void unaEvidenciaDeTextoNoTieneFluidez() {
        Evidencia texto = Evidencia.texto(new Estudiante(),
                new Nivel((short) 2, (short) 2, "Palabras nuevas", "escritura", "Escribe frases"),
                "Mi mamá prepara sopa.");
        when(evidenciaRepository.findByIdAndEstudianteId(31L, 5L)).thenReturn(Optional.of(texto));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.calcular(5L, 31L));

        assertEquals(HttpStatus.BAD_REQUEST, error.getEstado());
        verifyNoInteractions(palabraRepository);
    }

    @Test
    void losMensajesSegunElPuntaje() {
        assertEquals("¡Excelente! Lees con un ritmo muy fluido.", FluidezService.mensajePara(90));
        assertEquals("¡Vas muy bien! Intenta leer sin detenerte tanto entre palabras.",
                FluidezService.mensajePara(70));
        assertEquals("Buen intento. Practica leyendo palabra tras palabra con más seguridad.",
                FluidezService.mensajePara(50));
        assertEquals("Sigue practicando: lee despacio y trata de no detenerte entre palabras.",
                FluidezService.mensajePara(49));
    }
}
