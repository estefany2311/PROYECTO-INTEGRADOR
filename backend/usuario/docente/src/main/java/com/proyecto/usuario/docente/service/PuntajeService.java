package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.PuntajeEstudianteResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Estudiante;
import com.proyecto.usuario.docente.model.PuntajeEstudiante;
import com.proyecto.usuario.docente.model.ReglaPuntuacion;
import com.proyecto.usuario.docente.repository.EstudianteRepository;
import com.proyecto.usuario.docente.repository.PuntajeEstudianteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * T-302 (US-09): suma los puntos y las estrellas del alumno al completar un reto.
 *
 * <p>Aplica las reglas de puntuación de la US-53: cada tipo de reto entrega unos
 * puntos fijos y el puntaje obtenido (0 a 100) define cuántas estrellas se suman
 * (0 a 3). Mientras el módulo Admin no exponga sus reglas (T-260), los valores
 * iniciales viven aquí y se pueden cambiar en un solo lugar.</p>
 *
 * <p>T-379 (US-33): {@link #recalcularPorCambioDeNota} ajusta las estrellas del
 * acumulado cuando el docente aprueba o corrige la nota de una evidencia.</p>
 */
@Service
public class PuntajeService {

    public static final List<String> TIPOS_RETO =
            List.of("lectura", "escritura", "oralidad");

    private static final int PUNTAJE_MINIMO = 0;
    private static final int PUNTAJE_MAXIMO = 100;

    // Valores iniciales de la US-53: 100 puntos por reto y umbrales 60 / 80 / 95.
    private static final Map<String, ReglaPuntuacion> REGLAS = Map.of(
            "lectura", new ReglaPuntuacion("lectura", 100, 60, 80, 95),
            "escritura", new ReglaPuntuacion("escritura", 100, 60, 80, 95),
            "oralidad", new ReglaPuntuacion("oralidad", 100, 60, 80, 95));

    private final EstudianteRepository estudianteRepository;
    private final PuntajeEstudianteRepository puntajeEstudianteRepository;

    public PuntajeService(EstudianteRepository estudianteRepository,
                          PuntajeEstudianteRepository puntajeEstudianteRepository) {
        this.estudianteRepository = estudianteRepository;
        this.puntajeEstudianteRepository = puntajeEstudianteRepository;
    }

    /**
     * Suma al acumulado del alumno los puntos del reto y las estrellas ganadas.
     * Si el alumno todavía no tiene fila en {@code puntaje_estudiante}, se crea
     * en cero antes de sumar.
     */
    @Transactional
    public PuntajeEstudianteResponse sumarAlCompletarReto(Long estudianteId,
                                                          String tipoReto,
                                                          int puntajeObtenido) {
        String tipo = normalizarTipo(tipoReto);
        validarPuntaje(puntajeObtenido);

        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "El alumno no existe", HttpStatus.NOT_FOUND));

        ReglaPuntuacion regla = REGLAS.get(tipo);
        PuntajeEstudiante acumulado = puntajeEstudianteRepository
                .findByEstudianteId(estudianteId)
                .orElseGet(() -> PuntajeEstudiante.inicial(estudiante));

        acumulado.setPuntos(acumulado.getPuntos() + regla.puntos());
        acumulado.setEstrellas(acumulado.getEstrellas()
                + regla.estrellasPara(puntajeObtenido));

        PuntajeEstudiante guardado = puntajeEstudianteRepository.save(acumulado);

        return new PuntajeEstudianteResponse(
                guardado.getEstudiante().getId(),
                guardado.getPuntos(),
                guardado.getEstrellas());
    }

    /**
     * T-379 (US-33): ajusta las estrellas cuando el docente aprueba o corrige la
     * nota de la IA.
     *
     * <p>Los puntos no se tocan: se ganan al completar el reto (100 por reto) sin
     * importar el puntaje, así que una nota distinta solo cambia las estrellas.
     * El acumulado refleja la nota anterior (la de la IA o la última corrección),
     * por eso se aplica la diferencia de estrellas entre ambas notas.</p>
     *
     * <p>El acumulado nunca queda negativo. Si el alumno todavía no tiene fila en
     * {@code puntaje_estudiante} (el reto no se contó), no hay nada que ajustar y
     * el método no hace cambios.</p>
     */
    @Transactional
    public void recalcularPorCambioDeNota(Long estudianteId, String tipoReto,
                                          int notaAnterior, int notaNueva) {
        String tipo = normalizarTipo(tipoReto);
        validarPuntaje(notaAnterior);
        validarPuntaje(notaNueva);

        ReglaPuntuacion regla = REGLAS.get(tipo);
        int diferencia = regla.estrellasPara(notaNueva)
                - regla.estrellasPara(notaAnterior);
        if (diferencia == 0) {
            return;
        }

        puntajeEstudianteRepository.findByEstudianteId(estudianteId)
                .ifPresent(acumulado -> {
                    acumulado.setEstrellas(
                            Math.max(0, acumulado.getEstrellas() + diferencia));
                    puntajeEstudianteRepository.save(acumulado);
                });
    }

    /**
     * T-402 (US-22): estrellas (0 a 3) que gana un puntaje en un tipo de reto,
     * según los umbrales de la US-53.
     */
    public int estrellasPara(String tipoReto, int puntajeObtenido) {
        String tipo = normalizarTipo(tipoReto);
        validarPuntaje(puntajeObtenido);
        return REGLAS.get(tipo).estrellasPara(puntajeObtenido);
    }

    private String normalizarTipo(String tipoReto) {
        if (tipoReto == null || tipoReto.isBlank()) {
            throw tipoInvalido();
        }
        String normalizado = tipoReto.trim().toLowerCase(Locale.ROOT);
        if (!REGLAS.containsKey(normalizado)) {
            throw tipoInvalido();
        }
        return normalizado;
    }

    private ReglaNegocioException tipoInvalido() {
        return new ReglaNegocioException(
                "El tipo de reto no es válido", HttpStatus.BAD_REQUEST);
    }

    private void validarPuntaje(int puntajeObtenido) {
        if (puntajeObtenido < PUNTAJE_MINIMO || puntajeObtenido > PUNTAJE_MAXIMO) {
            throw new ReglaNegocioException(
                    "El puntaje del reto debe estar entre 0 y 100",
                    HttpStatus.BAD_REQUEST);
        }
    }
}
