package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.model.ColorSemaforo;
import org.springframework.stereotype.Service;

/**
 * T-277 (US-27): aplica la regla del semáforo de rendimiento.
 *
 * <p>El promedio se redondea al entero más cercano (0.5 sube) antes de comparar:
 * 80-100 verde, 60-79 amarillo y 0-59 rojo. Si no hay evaluaciones
 * (promedio nulo o no finito) el resultado es gris.</p>
 *
 * <p>La regla vive en {@code docs/regla-semaforo.md} y se reutiliza en el
 * endpoint del dashboard (T-278) y en la pantalla (T-279).</p>
 */
@Service
public class SemaforoService {

    /**
     * Calcula el color del semáforo para un promedio de 0 a 100.
     *
     * @param promedio promedio sin redondear; {@code null} o no finito significa
     *                 que el estudiante no tiene evaluaciones
     * @return color del semáforo que corresponde al promedio redondeado
     */
    public ColorSemaforo calcularColor(Double promedio) {
        if (promedio == null || !Double.isFinite(promedio)) {
            return ColorSemaforo.GRIS;
        }

        long redondeado = Math.round(promedio);

        if (redondeado >= 80) {
            return ColorSemaforo.VERDE;
        }
        if (redondeado >= 60) {
            return ColorSemaforo.AMARILLO;
        }
        return ColorSemaforo.ROJO;
    }
}
