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
