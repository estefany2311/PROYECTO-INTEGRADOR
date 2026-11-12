package com.proyecto.usuario.docente.model;

/**
 * T-302 (US-09): regla de puntuación de un tipo de reto, alineada con la US-53.
 *
 * <p>Define cuántos puntos entrega completar un reto y los umbrales de puntaje
 * para ganar 1, 2 o 3 estrellas. Los valores iniciales viven en
 * {@code PuntajeService} y se podrán leer del módulo Admin cuando la US-53
 * exponga su endpoint (T-260).</p>
 */
public record ReglaPuntuacion(
        String tipoReto,
        int puntos,
        int umbral1,
        int umbral2,
        int umbral3
) {

    public ReglaPuntuacion {
        if (puntos < 0) {
            throw new IllegalArgumentException("Los puntos del reto no pueden ser negativos");
        }
        if (!(umbral1 < umbral2 && umbral2 < umbral3)) {
            throw new IllegalArgumentException("Los umbrales de estrellas deben ser crecientes");
        }
    }

    /**
     * Estrellas ganadas según el puntaje obtenido en el reto (0 a 3).
     */
    public int estrellasPara(int puntajeObtenido) {
        if (puntajeObtenido >= umbral3) {
            return 3;
        }
        if (puntajeObtenido >= umbral2) {
            return 2;
        }
        if (puntajeObtenido >= umbral1) {
            return 1;
        }
        return 0;
    }
}
