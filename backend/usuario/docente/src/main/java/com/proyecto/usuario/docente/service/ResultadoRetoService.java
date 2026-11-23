package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.PuntajeEstudianteResponse;
import com.proyecto.usuario.docente.dto.ResultadoRetoResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Evidencia;
import com.proyecto.usuario.docente.repository.EvidenciaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cuenta el resultado de un reto de voz del alumno en sus puntos y estrellas.
 *
 * <p>La app lo pide cuando la evidencia pasa a {@code listo}. La nota depende del
 * tipo de reto del nivel: en lectura es la precisión de la IA y en oralidad es la
 * fluidez (que se calcula aquí si todavía no se había calculado). Después se
 * recalculan todos los niveles del alumno, así que también se cuentan las
 * lecturas que quedaron sin contar (por ejemplo, las enviadas sin internet).</p>
 *
 * <p>Es idempotente: el total solo cuenta el mejor intento por nivel, de modo que
 * pedirlo varias veces no suma de más.</p>
 */
@Service
public class ResultadoRetoService {

    private final EvidenciaRepository evidenciaRepository;
    private final FluidezService fluidezService;
    private final PuntajeService puntajeService;

    public ResultadoRetoService(EvidenciaRepository evidenciaRepository,
                                FluidezService fluidezService,
                                PuntajeService puntajeService) {
        this.evidenciaRepository = evidenciaRepository;
        this.fluidezService = fluidezService;
        this.puntajeService = puntajeService;
    }

    @Transactional
    public ResultadoRetoResponse registrar(Long estudianteId, Long evidenciaId) {
        Evidencia evidencia = evidenciaRepository
                .findByIdAndEstudianteId(evidenciaId, estudianteId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "La evidencia no existe", HttpStatus.NOT_FOUND));

        if (!Evidencia.AUDIO.equals(evidencia.getTipo())) {
            throw new ReglaNegocioException(
                    "Los textos escritos usan el resultado de escritura", HttpStatus.BAD_REQUEST);
        }

        String tipo = evidencia.getNivel().getTipoReto();
        if ("oralidad".equals(tipo)) {
            // la nota de oralidad es la fluidez; calcular la deja guardada en la evidencia
            fluidezService.calcular(estudianteId, evidenciaId);
        } else if ("lectura".equals(tipo)) {
            if (evidencia.getPuntajeIa() == null) {
                throw new ReglaNegocioException(
                        "Tu lectura todavía se está procesando", HttpStatus.CONFLICT);
            }
        } else {
            throw new ReglaNegocioException(
                    "Este nivel no es de lectura ni de oralidad", HttpStatus.BAD_REQUEST);
        }

        puntajeService.recalcularTodo(estudianteId);

        int nota = PuntajeService.notaDe(evidencia, tipo);
        PuntajeEstudianteResponse total = puntajeService.acumuladoDe(estudianteId);
        return new ResultadoRetoResponse(evidencia.getId(), tipo, nota,
                puntajeService.puntosPara(tipo, nota),
                puntajeService.estrellasPara(tipo, nota),
                total.puntos(), total.estrellas());
    }
}
