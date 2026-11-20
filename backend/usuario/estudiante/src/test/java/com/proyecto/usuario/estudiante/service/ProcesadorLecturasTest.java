package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.event.AudioRecibido;
import com.proyecto.usuario.estudiante.exception.ServicioVozNoDisponibleException;
import com.proyecto.usuario.estudiante.model.PendienteVoz;
import com.proyecto.usuario.estudiante.model.Transcripcion;
import com.proyecto.usuario.estudiante.repository.EvidenciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.dao.DataAccessResourceFailureException;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static com.proyecto.usuario.estudiante.service.AudiosDePrueba.wav;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// T-416 (US-13): la lectura se transcribe en segundo plano y queda en listo o error
class ProcesadorLecturasTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"), ZoneId.of("America/Lima"));
    private static final LocalDateTime AHORA = LocalDateTime.now(RELOJ);
    private static final Transcripcion TRANSCRIPCION = new Transcripcion(
            "Había una vez", "es", 2.0,
            List.of(new Transcripcion.Palabra("Había", 0.0, 0.4, 0.95)), "small");

    @TempDir Path carpeta;

    private AlmacenAudios almacen;
    private ClienteVoz voz;
    private EvidenciaRepository evidencias;
    private ProcesadorLecturas procesador;

    @BeforeEach
    void preparar() {
        almacen = new AlmacenAudios(carpeta.toString(), RELOJ);
        voz = mock(ClienteVoz.class);
        evidencias = mock(EvidenciaRepository.class);
        procesador = new ProcesadorLecturas(almacen, voz, evidencias, new SyncTaskExecutor(), RELOJ);
    }

    @Test
    void transcribeYGuardaElTextoComoListo() {
        byte[] audio = wav(2_000);
        String clave = almacen.guardar(7L, 1, audio);
        when(voz.transcribir(audio)).thenReturn(TRANSCRIPCION);

        procesador.alRecibir(new AudioRecibido(50L, clave));

        verify(evidencias).guardarTranscripcion(50L, TRANSCRIPCION, AHORA);
        verify(evidencias, never()).marcarError(anyLong(), any(), any());
    }

    @Test
    void siElServicioDeVozNoRespondeQuedaEnError() {
        String clave = almacen.guardar(7L, 1, wav(2_000));
        when(voz.transcribir(any())).thenThrow(new ServicioVozNoDisponibleException("apagado", null));

        procesador.procesar(50L, clave);

        verify(evidencias).marcarError(50L, "El servicio de voz no respondió", AHORA);
        verify(evidencias, never()).guardarTranscripcion(anyLong(), any(), any());
    }

    @Test
    void siElArchivoNoExisteQuedaEnError() {
        procesador.procesar(50L, "est_7/no_existe.wav");

        verify(evidencias).marcarError(50L, "No se encontró el audio guardado", AHORA);
        verifyNoInteractions(voz);
    }

    @Test
    void unErrorInesperadoTambienQuedaEnError() {
        String clave = almacen.guardar(7L, 1, wav(2_000));
        when(voz.transcribir(any())).thenReturn(TRANSCRIPCION);
        doThrow(new IllegalStateException("base caída")).when(evidencias).guardarTranscripcion(anyLong(), any(), any());

        procesador.procesar(50L, clave);

        verify(evidencias).marcarError(50L, "Error al procesar la lectura", AHORA);
    }

    @Test
    void alArrancarRetomaLasQueQuedaronProcesando() {
        String clave = almacen.guardar(7L, 1, wav(2_000));
        when(evidencias.pendientes()).thenReturn(List.of(new PendienteVoz(50L, clave)));
        when(voz.transcribir(any())).thenReturn(TRANSCRIPCION);

        procesador.retomarPendientes();

        verify(evidencias).guardarTranscripcion(50L, TRANSCRIPCION, AHORA);
    }

    // En el CI la base está vacía y no existe la tabla evidencia: no debe impedir que arranque
    @Test
    void siNoPuedeLeerLaBaseAlArrancarNoFalla() {
        when(evidencias.pendientes()).thenThrow(new DataAccessResourceFailureException("sin tabla"));

        procesador.retomarPendientes();

        verifyNoInteractions(voz);
    }
}
