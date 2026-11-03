package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.dto.CompletarNivelResponse;
import com.proyecto.usuario.estudiante.dto.NivelDetalleResponse;
import com.proyecto.usuario.estudiante.exception.NivelBloqueadoException;
import com.proyecto.usuario.estudiante.exception.NivelNoEncontradoException;
import com.proyecto.usuario.estudiante.model.Nivel;
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
    private final Clock reloj;

    public NivelService(NivelRepository niveles, Clock reloj) {
        this.niveles = niveles;
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
     * Solo se puede completar un nivel abierto para él (el mismo control que el detalle: 403 si está bloqueado).
     * Deja el nivel completado y desbloquea el siguiente del recorrido, todo en una sola transacción.
     * Las reglas finas llegan después: mínimo de estrellas (T-309), registro del intento (T-308)
     * y repetir un reto sin perder lo ganado (T-310).
     */
    @Transactional
    public CompletarNivelResponse completar(long estudianteId, int nivelId, int puntos) {
        NivelDetalleResponse nivel = detalle(estudianteId, nivelId);    // 404 si no existe, 403 si está bloqueado
        LocalDateTime ahora = LocalDateTime.now(reloj);

        niveles.marcarCompletado(estudianteId, nivelId, puntos, ahora);

        Optional<Nivel> siguiente = niveles.siguiente(nivel.orden());
        siguiente.ifPresent(s -> niveles.desbloquear(estudianteId, s.id(), ahora));

        return new CompletarNivelResponse(nivelId, COMPLETADO, puntos,
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
