package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.FluidezResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.ConfiguracionFluidez;
import com.proyecto.usuario.docente.model.Evidencia;
import com.proyecto.usuario.docente.model.PalabraLeida;
import com.proyecto.usuario.docente.repository.ConfiguracionFluidezRepository;
import com.proyecto.usuario.docente.repository.EvidenciaRepository;
import com.proyecto.usuario.docente.repository.PalabraLeidaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * T-396 (US-15): calcula y guarda la fluidez lectora de una lectura en voz alta.
 *
 * <p>Usa las marcas de tiempo por palabra que dejó el motor de voz, la
 * configuración del grado (US-51) y la fórmula de {@link FormulaFluidez}. El
 * puntaje queda guardado en la evidencia; se recalcula cada vez que se pide, de
 * modo que si el administrador cambia la configuración el resultado se
 * actualiza.</p>
 */
@Service
public class FluidezService {

    // grado que se usa si el alumno no lo tiene registrado
    private static final int GRADO_POR_DEFECTO = 3;

    private final EvidenciaRepository evidenciaRepository;
    private final PalabraLeidaRepository palabraRepository;
    private final ConfiguracionFluidezRepository configuracionRepository;

    public FluidezService(EvidenciaRepository evidenciaRepository,
                          PalabraLeidaRepository palabraRepository,
                          ConfiguracionFluidezRepository configuracionRepository) {
        this.evidenciaRepository = evidenciaRepository;
        this.palabraRepository = palabraRepository;
        this.configuracionRepository = configuracionRepository;
    }

    @Transactional
    public FluidezResponse calcular(Long estudianteId, Long evidenciaId) {
        Evidencia evidencia = evidenciaRepository
                .findByIdAndEstudianteId(evidenciaId, estudianteId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "La evidencia no existe", HttpStatus.NOT_FOUND));

        if (!Evidencia.AUDIO.equals(evidencia.getTipo())) {
            throw new ReglaNegocioException(
                    "Solo las lecturas en voz alta tienen fluidez", HttpStatus.BAD_REQUEST);
        }

        List<PalabraLeida> palabras = palabraRepository.porEvidencia(evidenciaId);
        if (palabras.size() < 2) {
            // el audio todavía se está transcribiendo, o no se entendió nada
            throw new ReglaNegocioException(
                    "Tu lectura todavía se está procesando", HttpStatus.CONFLICT);
        }

        int grado = evidencia.getEstudiante().getGrado() != null
                ? evidencia.getEstudiante().getGrado().getId()
                : GRADO_POR_DEFECTO;
        ConfiguracionFluidez config = configuracionRepository.porGrado(grado);

        double ppm = FormulaFluidez.palabrasPorMinuto(palabras);
        var pausas = FormulaFluidez.analizarPausas(palabras, config.toleranciaPausas());
        int puntajeRitmo = FormulaFluidez.puntajeRitmo(ppm, config.palabrasPorMinuto());
        int puntajePausas = FormulaFluidez.puntajePausas(pausas.pausasLargas(), pausas.vacilaciones());
        int puntaje = FormulaFluidez.puntajeFluidez(puntajeRitmo, puntajePausas);

        evidencia.setPuntajeFluidez((short) puntaje);

        return new FluidezResponse(evidencia.getId(), puntaje, (int) Math.round(ppm),
                config.palabrasPorMinuto(), pausas.pausasLargas(), pausas.vacilaciones(),
                mensajePara(puntaje));
    }

    // retroalimentación simple para el alumno según su puntaje
    static String mensajePara(int puntaje) {
        if (puntaje >= 90) {
            return "¡Excelente! Lees con un ritmo muy fluido.";
        }
        if (puntaje >= 70) {
            return "¡Vas muy bien! Intenta leer sin detenerte tanto entre palabras.";
        }
        if (puntaje >= 50) {
            return "Buen intento. Practica leyendo palabra tras palabra con más seguridad.";
        }
        return "Sigue practicando: lee despacio y trata de no detenerte entre palabras.";
    }
}
