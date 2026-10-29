package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.dto.NivelDetalleResponse;
import com.proyecto.usuario.estudiante.exception.NivelBloqueadoException;
import com.proyecto.usuario.estudiante.exception.NivelNoEncontradoException;
import com.proyecto.usuario.estudiante.model.Nivel;
import com.proyecto.usuario.estudiante.repository.NivelRepository;
import org.springframework.stereotype.Service;

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

    public NivelService(NivelRepository niveles) {
        this.niveles = niveles;
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
