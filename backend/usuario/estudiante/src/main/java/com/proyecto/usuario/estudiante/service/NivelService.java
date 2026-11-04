package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.config.ReglasReto;
import com.proyecto.usuario.estudiante.dto.CompletarNivelResponse;
import com.proyecto.usuario.estudiante.dto.NivelDetalleResponse;
import com.proyecto.usuario.estudiante.exception.NivelBloqueadoException;
import com.proyecto.usuario.estudiante.exception.NivelNoEncontradoException;
import com.proyecto.usuario.estudiante.model.Nivel;
import com.proyecto.usuario.estudiante.repository.IntentoRetoRepository;
import com.proyecto.usuario.estudiante.repository.NivelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * T-214 (US-07): detalle de un nivel para el alumno que tiene la sesión.
 *
 * Misma regla de estado que el mapa del backend docente (T-197):
 *  - si el alumno tiene una fila de progreso en el nivel, manda esa fila;
 *  - si no: el primer nivel está desbloqueado y los demás solo si el anterior está completado.
 * Un nivel bloqueado NO se abre (403), aunque alguien escriba su id a mano.
 */
@Service
public class NivelService {

    public static final String BLOQUEADO = "bloqueado";
    public static final String DESBLOQUEADO = "desbloqueado";
    public static final String COMPLETADO = "completado";

    private final NivelRepository niveles;
    private final IntentoRetoRepository intentos;
    private final ReglasReto reglas;
    private final Clock reloj;

    public NivelService(NivelRepository niveles, IntentoRetoRepository intentos, ReglasReto reglas, Clock reloj) {
        this.niveles = niveles;
        this.intentos = intentos;
        this.reglas = reglas;
        this.reloj = reloj;
    }

    public NivelDetalleResponse detalle(long estudianteId, int nivelId) {
        Nivel nivel = niveles.buscarActivo(nivelId).orElseThrow(NivelNoEncontradoException::new);
        Optional<NivelRepository.Progreso> progreso = niveles.progreso(estudianteId, nivelId);
        Optional<Nivel> anterior = niveles.anterior(nivel.orden());

        String estado = progreso.map(NivelRepository.Progreso::estado)
                .orElseGet(() -> estadoSinProgreso(estudianteId, anterior));

        if (BLOQUEADO.equals(estado)) {
            throw new NivelBloqueadoException(anterior.map(Nivel::nombre).orElse(null));
        }

        int puntos = progreso.map(NivelRepository.Progreso::puntos).orElse(0);
        return new NivelDetalleResponse(nivel.id(), nivel.orden(), nivel.nombre(), nivel.tipoReto(),
                nivel.descripcion(), estado, puntos);
    }

    /**
     * T-198 (US-08): el alumno terminó el reto del nivel.
     * Solo se puede terminar un nivel abierto para él (el mismo control que el detalle: 403 si está bloqueado).
     * T-308: primero registra el intento (fecha, puntos y estrellas), siempre.
     * T-309: si sacó las estrellas mínimas (por defecto 1), el nivel queda completado y se desbloquea
     * el siguiente. Si no, solo queda el intento: puede volver a intentarlo.
     * Todo en una sola transacción. Repetir un reto sin perder lo ganado llega en T-310.
     */
    @Transactional
    public CompletarNivelResponse completar(long estudianteId, int nivelId, int puntos, int estrellas) {
        NivelDetalleResponse nivel = detalle(estudianteId, nivelId);    // 404 si no existe, 403 si está bloqueado
        LocalDateTime ahora = LocalDateTime.now(reloj);

        intentos.registrar(estudianteId, nivelId, puntos, estrellas, ahora);

        // T-309: sin las estrellas mínimas no se completa ni se desbloquea nada
        if (!reglas.aprueba(estrellas)) {
            return new CompletarNivelResponse(nivelId, nivel.estado(), puntos, estrellas, false,
                    reglas.estrellasMinimas(), null, null);
        }

        niveles.marcarCompletado(estudianteId, nivelId, puntos, ahora);

        Optional<Nivel> siguiente = niveles.siguiente(nivel.orden());
        siguiente.ifPresent(s -> niveles.desbloquear(estudianteId, s.id(), ahora));

        return new CompletarNivelResponse(nivelId, COMPLETADO, puntos, estrellas, true, reglas.estrellasMinimas(),
                siguiente.map(Nivel::id).orElse(null),
                siguiente.map(Nivel::nombre).orElse(null));
    }

    private String estadoSinProgreso(long estudianteId, Optional<Nivel> anterior) {
        if (anterior.isEmpty()) {
            return DESBLOQUEADO;                          // primer nivel del mapa
        }
        boolean anteriorCompletado = niveles.progreso(estudianteId, anterior.get().id())
                .map(p -> COMPLETADO.equals(p.estado()))
                .orElse(false);
        return anteriorCompletado ? DESBLOQUEADO : BLOQUEADO;
    }
}
