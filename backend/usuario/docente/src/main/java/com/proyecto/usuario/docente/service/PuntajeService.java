package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.PuntajeEstudianteResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Estudiante;
import com.proyecto.usuario.docente.model.Evidencia;
import com.proyecto.usuario.docente.model.Nivel;
import com.proyecto.usuario.docente.model.PuntajeEstudiante;
import com.proyecto.usuario.docente.model.PuntajeNivelEstudiante;
import com.proyecto.usuario.docente.model.ReglaPuntuacion;
import com.proyecto.usuario.docente.repository.EstudianteRepository;
import com.proyecto.usuario.docente.repository.EvidenciaRepository;
import com.proyecto.usuario.docente.repository.PuntajeEstudianteRepository;
import com.proyecto.usuario.docente.repository.PuntajeNivelEstudianteRepository;
import com.proyecto.usuario.docente.repository.ReglaPuntuacionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * US-09: puntos y estrellas del alumno. Es el único lugar que escribe en
 * {@code puntaje_estudiante}; la app del alumno solo guarda su progreso.
 *
 * <p>Regla única acordada entre Daniela, Karla y Noemi:</p>
 * <ul>
 *   <li>Puntos máximos y umbrales de estrellas salen de {@code reglas_puntuacion}
 *       (US-53). Sin fila: 100 puntos y umbrales 60 / 80 / 95.</li>
 *   <li>Nota de 0 a 100 de un intento: la calificación final del docente si la
 *       hay; si no, lectura = precisión de la IA ({@code puntaje_ia}), escritura =
 *       puntaje de escritura ({@code puntaje_ia}) y oralidad = fluidez
 *       ({@code puntaje_fluidez}).</li>
 *   <li>Puntos = puntos máximos × nota / 100, redondeado.</li>
 *   <li>Cuenta el <b>mejor intento por alumno y nivel</b>: si mejora, se suma solo
 *       la diferencia; si el docente baja la nota, se resta.</li>
 * </ul>
 */
@Service
public class PuntajeService {

    public static final List<String> TIPOS_RETO =
            List.of("lectura", "escritura", "oralidad");

    private static final int PUNTAJE_MINIMO = 0;
    private static final int PUNTAJE_MAXIMO = 100;

    private final EstudianteRepository estudianteRepository;
    private final PuntajeEstudianteRepository puntajeEstudianteRepository;
    private final PuntajeNivelEstudianteRepository puntajeNivelRepository;
    private final EvidenciaRepository evidenciaRepository;
    private final ReglaPuntuacionRepository reglaRepository;

    public PuntajeService(EstudianteRepository estudianteRepository,
                          PuntajeEstudianteRepository puntajeEstudianteRepository,
                          PuntajeNivelEstudianteRepository puntajeNivelRepository,
                          EvidenciaRepository evidenciaRepository,
                          ReglaPuntuacionRepository reglaRepository) {
        this.estudianteRepository = estudianteRepository;
        this.puntajeEstudianteRepository = puntajeEstudianteRepository;
        this.puntajeNivelRepository = puntajeNivelRepository;
        this.evidenciaRepository = evidenciaRepository;
        this.reglaRepository = reglaRepository;
    }

    /**
     * Nota de 0 a 100 de un intento, o {@code null} si todavía no tiene (la IA aún
     * no la calculó).
     */
    public static Integer notaDe(Evidencia evidencia, String tipoReto) {
        if (evidencia.getCalificacionFinal() != null) {
            return evidencia.getCalificacionFinal().intValue();
        }
        Short base = "oralidad".equals(tipoReto)
                ? evidencia.getPuntajeFluidez()
                : evidencia.getPuntajeIa();
        return base == null ? null : base.intValue();
    }

    /** Estrellas (0 a 3) que gana una nota en un tipo de reto, con las reglas vigentes. */
    public int estrellasPara(String tipoReto, int nota) {
        String tipo = normalizarTipo(tipoReto);
        validarNota(nota);
        return reglaRepository.porTipo(tipo).estrellasPara(nota);
    }

    /** Puntos que gana una nota en un tipo de reto: puntos máximos × nota / 100. */
    public int puntosPara(String tipoReto, int nota) {
        String tipo = normalizarTipo(tipoReto);
        validarNota(nota);
        return reglaRepository.porTipo(tipo).puntosPara(nota);
    }

    /** Puntos y estrellas totales del alumno (en cero si todavía no tiene). */
    @Transactional(readOnly = true)
    public PuntajeEstudianteResponse acumuladoDe(Long estudianteId) {
        return puntajeEstudianteRepository.findByEstudianteId(estudianteId)
                .map(p -> new PuntajeEstudianteResponse(estudianteId, p.getPuntos(), p.getEstrellas()))
                .orElseGet(() -> new PuntajeEstudianteResponse(estudianteId, 0, 0));
    }

    /**
     * Recalcula lo que aporta un nivel al total del alumno a partir de su mejor
     * intento, y ajusta el total por la diferencia. Es idempotente: llamarlo dos
     * veces seguidas no suma dos veces.
     */
    @Transactional
    public void recalcularNivel(Long estudianteId, Nivel nivel) {
        recalcular(estudianteId, nivel,
                evidenciaRepository.findByEstudianteIdAndNivelId(estudianteId, nivel.getId()));
    }

    /**
     * Recalcula todos los niveles en los que el alumno tiene intentos. Cuenta
     * también los que quedaron sin contar (por ejemplo, lecturas enviadas sin
     * internet que se procesaron después).
     */
    @Transactional
    public void recalcularTodo(Long estudianteId) {
        Map<Short, List<Evidencia>> porNivel = evidenciaRepository.findByEstudianteId(estudianteId)
                .stream()
                .collect(Collectors.groupingBy(e -> e.getNivel().getId()));
        porNivel.values().forEach(intentos ->
                recalcular(estudianteId, intentos.get(0).getNivel(), intentos));
    }

    private void recalcular(Long estudianteId, Nivel nivel, List<Evidencia> intentos) {
        String tipo = normalizarTipo(nivel.getTipoReto());
        Optional<Integer> mejor = intentos.stream()
                .map(intento -> notaDe(intento, tipo))
                .filter(nota -> nota != null)
                .max(Integer::compare);
        if (mejor.isEmpty()) {
            return; // ningún intento tiene nota todavía
        }

        int nota = mejor.get();
        ReglaPuntuacion regla = reglaRepository.porTipo(tipo);
        int puntos = regla.puntosPara(nota);
        int estrellas = regla.estrellasPara(nota);

        PuntajeNivelEstudiante fila = puntajeNivelRepository
                .findByEstudianteIdAndNivelId(estudianteId, nivel.getId())
                .orElseGet(() -> new PuntajeNivelEstudiante(estudianteId, nivel.getId()));
        int puntosAntes = fila.getId() == null ? 0 : fila.getPuntos();
        int estrellasAntes = fila.getId() == null ? 0 : fila.getEstrellas();

        fila.registrar(nota, puntos, estrellas);
        puntajeNivelRepository.save(fila);

        int diferenciaPuntos = puntos - puntosAntes;
        int diferenciaEstrellas = estrellas - estrellasAntes;
        if (diferenciaPuntos != 0 || diferenciaEstrellas != 0) {
            sumarAlTotal(estudianteId, diferenciaPuntos, diferenciaEstrellas);
        }
    }

    // el total nunca queda negativo
    private void sumarAlTotal(Long estudianteId, int puntos, int estrellas) {
        PuntajeEstudiante total = puntajeEstudianteRepository.findByEstudianteId(estudianteId)
                .orElseGet(() -> PuntajeEstudiante.inicial(buscarEstudiante(estudianteId)));
        total.setPuntos(Math.max(0, total.getPuntos() + puntos));
        total.setEstrellas(Math.max(0, total.getEstrellas() + estrellas));
        puntajeEstudianteRepository.save(total);
    }

    private Estudiante buscarEstudiante(Long estudianteId) {
        return estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "El alumno no existe", HttpStatus.NOT_FOUND));
    }

    private String normalizarTipo(String tipoReto) {
        if (tipoReto == null || tipoReto.isBlank()) {
            throw tipoInvalido();
        }
        String normalizado = tipoReto.trim().toLowerCase(Locale.ROOT);
        if (!TIPOS_RETO.contains(normalizado)) {
            throw tipoInvalido();
        }
        return normalizado;
    }

    private ReglaNegocioException tipoInvalido() {
        return new ReglaNegocioException(
                "El tipo de reto no es válido", HttpStatus.BAD_REQUEST);
    }

    private void validarNota(int nota) {
        if (nota < PUNTAJE_MINIMO || nota > PUNTAJE_MAXIMO) {
            throw new ReglaNegocioException(
                    "El puntaje del reto debe estar entre 0 y 100",
                    HttpStatus.BAD_REQUEST);
        }
    }
}
