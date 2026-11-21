package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.ResultadoEscrituraResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Evidencia;
import com.proyecto.usuario.docente.repository.ConsignaEscrituraRepository;
import com.proyecto.usuario.docente.repository.EvidenciaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

/**
 * T-400 (US-22): calcula el puntaje de escritura de un texto que el alumno envió
 * (US-21).
 *
 * <p>Las palabras con error ortográfico son las que el corrector del alumno
 * (US-19) dejó marcadas en la evidencia, separadas por comas; las palabras
 * mínimas salen de la consigna del nivel. Con eso se aplica la fórmula de
 * {@link FormulaEscritura}.</p>
 */
@Service
public class EscrituraService {

    private final EvidenciaRepository evidenciaRepository;
    private final ConsignaEscrituraRepository consignaRepository;

    public EscrituraService(EvidenciaRepository evidenciaRepository,
                            ConsignaEscrituraRepository consignaRepository) {
        this.evidenciaRepository = evidenciaRepository;
        this.consignaRepository = consignaRepository;
    }

    @Transactional(readOnly = true)
    public ResultadoEscrituraResponse calcular(Long estudianteId, Long evidenciaId) {
        return resultadoDe(buscarTexto(estudianteId, evidenciaId));
    }

    // la evidencia debe ser del alumno y de tipo texto
    Evidencia buscarTexto(Long estudianteId, Long evidenciaId) {
        Evidencia evidencia = evidenciaRepository
                .findByIdAndEstudianteId(evidenciaId, estudianteId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "La evidencia no existe", HttpStatus.NOT_FOUND));

        if (!Evidencia.TEXTO.equals(evidencia.getTipo())) {
            throw new ReglaNegocioException(
                    "Solo los textos escritos tienen puntaje de escritura", HttpStatus.BAD_REQUEST);
        }
        return evidencia;
    }

    ResultadoEscrituraResponse resultadoDe(Evidencia evidencia) {
        int palabras = FormulaEscritura.contarPalabras(evidencia.getTexto());
        int errores = contarErrores(evidencia.getPalabrasMarcadas());
        int minimas = consignaRepository.palabrasMinimas(evidencia.getNivel().getId());
        var oraciones = FormulaEscritura.contarOraciones(evidencia.getTexto());

        int ortografia = FormulaEscritura.puntajeOrtografia(errores, palabras);
        int longitud = FormulaEscritura.puntajeLongitud(palabras, minimas);
        int consigna = FormulaEscritura.puntajeConsigna(oraciones);
        int puntaje = FormulaEscritura.puntajeEscritura(ortografia, longitud, consigna);

        return new ResultadoEscrituraResponse(evidencia.getId(), puntaje, ortografia, longitud,
                consigna, palabras, minimas, errores, oraciones.total(), oraciones.completas());
    }

    // palabras marcadas: "kasa, perro" -> 2
    private static int contarErrores(String palabrasMarcadas) {
        if (palabrasMarcadas == null || palabrasMarcadas.isBlank()) {
            return 0;
        }
        return (int) Arrays.stream(palabrasMarcadas.split(","))
                .filter(palabra -> !palabra.isBlank())
                .count();
    }
}
