package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.dto.LecturaNivelResponse;
import com.proyecto.usuario.estudiante.dto.NivelDetalleResponse;
import com.proyecto.usuario.estudiante.exception.LecturaNoDisponibleException;
import com.proyecto.usuario.estudiante.model.Lectura;
import com.proyecto.usuario.estudiante.repository.LecturaRepository;
import org.springframework.stereotype.Service;

/**
 * T-218 (US-10): qué texto lee el alumno en un nivel.
 *  1. El nivel tiene que estar abierto para él (mismo control que el detalle: 403 bloqueado, 404 no existe).
 *  2. Si su docente le asignó una lectura de ese tipo de reto, se usa la más reciente.
 *  3. Si no, una lectura activa del catálogo para su grado, empezando por las básicas.
 *  4. Si no hay ninguna: 404 con un mensaje amable.
 */
@Service
public class LecturaService {

    private final NivelService niveles;
    private final LecturaRepository lecturas;

    public LecturaService(NivelService niveles, LecturaRepository lecturas) {
        this.niveles = niveles;
        this.lecturas = lecturas;
    }

    public LecturaNivelResponse lecturaDelNivel(long estudianteId, int nivelId) {
        NivelDetalleResponse nivel = niveles.detalle(estudianteId, nivelId);
        String tipo = nivel.tipoReto();

        return lecturas.asignada(estudianteId, tipo)
                .map(l -> respuesta(nivelId, l, true))
                .or(() -> lecturas.delCatalogo(estudianteId, tipo).map(l -> respuesta(nivelId, l, false)))
                .orElseThrow(LecturaNoDisponibleException::new);
    }

    private static LecturaNivelResponse respuesta(int nivelId, Lectura l, boolean asignada) {
        return new LecturaNivelResponse(nivelId, l.id(), l.titulo(), l.texto(), l.grado(), l.dificultad(), asignada);
    }
}
