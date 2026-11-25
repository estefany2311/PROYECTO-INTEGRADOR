package com.proyecto.usuario.docente.model;

/**
 * T-302 (US-09): regla de puntuación de un tipo de reto, alineada con la US-53.
 *
 * <p>Define los puntos máximos de un reto y los umbrales de nota para ganar 1, 2
 * o 3 estrellas. Salen de la tabla {@code reglas_puntuacion} del administrador
 * (ver {@code ReglaPuntuacionRepository}); si no hay fila se usa
 * {@link #porDefecto(String)}.</p>
 */
public record ReglaPuntuacion(
        String tipoReto,
        int puntos,
        int umbral1,
        int umbral2,
        int umbral3
) {

    /** Regla de respaldo cuando el administrador no configuró el tipo de reto. */
    public static ReglaPuntuacion porDefecto(String tipoReto) {
        return new ReglaPuntuacion(tipoReto, 100, 60, 80, 95);
    }

    public ReglaPuntuacion {
        if (puntos < 0) {
            throw new IllegalArgumentException("Los puntos del reto no pueden ser negativos");
        }
        if (!(umbral1 < umbral2 && umbral2 < umbral3)) {
            throw new IllegalArgumentException("Los umbrales de estrellas deben ser crecientes");
        }
    }

    /**
     * Puntos ganados con una nota de 0 a 100: los puntos máximos del reto por la
     * nota, redondeado.
     */
    public int puntosPara(int nota) {
        return (int) Math.round(puntos * nota / 100.0);
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
