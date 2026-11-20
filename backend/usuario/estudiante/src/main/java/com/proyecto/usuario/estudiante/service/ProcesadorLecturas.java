package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.config.ConfiguracionAsync;
import com.proyecto.usuario.estudiante.event.AudioRecibido;
import com.proyecto.usuario.estudiante.exception.ServicioVozNoDisponibleException;
import com.proyecto.usuario.estudiante.model.PendienteVoz;
import com.proyecto.usuario.estudiante.model.Transcripcion;
import com.proyecto.usuario.estudiante.repository.EvidenciaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Clock;
import java.time.LocalDateTime;

// T-416 (US-13): transcribe la lectura en segundo plano; la app no espera a Whisper
@Component
public class ProcesadorLecturas {

    private static final Logger log = LoggerFactory.getLogger(ProcesadorLecturas.class);

    private final AlmacenAudios almacen;
    private final ClienteVoz voz;
    private final EvidenciaRepository evidencias;
    private final TaskExecutor ejecutor;
    private final Clock reloj;

    public ProcesadorLecturas(AlmacenAudios almacen, ClienteVoz voz, EvidenciaRepository evidencias,
                              @Qualifier(ConfiguracionAsync.PROCESADOR_VOZ) TaskExecutor ejecutor, Clock reloj) {
        this.almacen = almacen;
        this.voz = voz;
        this.evidencias = evidencias;
        this.ejecutor = ejecutor;
        this.reloj = reloj;
    }

    // Recién cuando la evidencia ya está guardada en la base (AFTER_COMMIT)
    @Async(ConfiguracionAsync.PROCESADOR_VOZ)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alRecibir(AudioRecibido evento) {
        procesar(evento.evidenciaId(), evento.clave());
    }

    public void procesar(long evidenciaId, String clave) {
        try {
            byte[] audio = Files.readAllBytes(almacen.ruta(clave));
            Transcripcion transcripcion = voz.transcribir(audio);
            evidencias.guardarTranscripcion(evidenciaId, transcripcion, LocalDateTime.now(reloj));
        } catch (IOException e) {
            marcarError(evidenciaId, "No se encontró el audio guardado");
        } catch (ServicioVozNoDisponibleException e) {
            marcarError(evidenciaId, "El servicio de voz no respondió");
        } catch (RuntimeException e) {
            log.error("No se pudo procesar la evidencia {}", evidenciaId, e);
            marcarError(evidenciaId, "Error al procesar la lectura");
        }
    }

    // Si el backend se apagó a la mitad, al volver termina las que quedaron en "procesando"
    @EventListener(ApplicationReadyEvent.class)
    public void retomarPendientes() {
        try {
            for (PendienteVoz p : evidencias.pendientes()) {
                ejecutor.execute(() -> procesar(p.evidenciaId(), p.audioUrl()));
            }
        } catch (DataAccessException e) {
            log.warn("No se pudieron revisar las lecturas pendientes: {}", e.getMessage());
        }
    }

    private void marcarError(long evidenciaId, String mensaje) {
        log.warn("Evidencia {}: {}", evidenciaId, mensaje);
        evidencias.marcarError(evidenciaId, mensaje, LocalDateTime.now(reloj));
    }
}
