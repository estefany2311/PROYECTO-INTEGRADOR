package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.dto.ConsignaNivelResponse;
import com.proyecto.usuario.estudiante.dto.NivelDetalleResponse;
import com.proyecto.usuario.estudiante.exception.ConsignaNoDisponibleException;
import com.proyecto.usuario.estudiante.repository.ConsignaRepository;
import org.springframework.stereotype.Service;

/**
 * T-320 (US-17): la consigna que el alumno tiene que escribir en un nivel.
 *  1. El nivel tiene que estar abierto para él (mismo control que el detalle: 403 bloqueado, 404 no existe).
 *  2. Solo los niveles de escritura tienen consigna: si no lo es, 404.
 *  3. Si todavía no se cargó su consigna, 404 con un mensaje amable.
 */
@Service
public class ConsignaService {

    private static final String ESCRITURA = "escritura";

    private final NivelService niveles;
    private final ConsignaRepository consignas;

    public ConsignaService(NivelService niveles, ConsignaRepository consignas) {
        this.niveles = niveles;
        this.consignas = consignas;
    }

    public ConsignaNivelResponse consignaDelNivel(long estudianteId, int nivelId) {
        NivelDetalleResponse nivel = niveles.detalle(estudianteId, nivelId);
        if (!ESCRITURA.equals(nivel.tipoReto())) {
            throw ConsignaNoDisponibleException.noEsDeEscritura();
        }
        return consignas.delNivel(nivelId)
                .map(c -> new ConsignaNivelResponse(
                        nivelId, nivel.nombre(), c.consigna(), c.pista(), c.ilustracion(), c.palabrasMinimas()))
                .orElseThrow(ConsignaNoDisponibleException::sinConsigna);
    }
}
