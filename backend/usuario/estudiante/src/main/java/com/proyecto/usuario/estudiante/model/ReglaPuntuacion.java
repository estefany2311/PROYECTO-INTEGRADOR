package com.proyecto.usuario.estudiante.model;

// T-425: regla única de puntos del equipo (reglas_puntuacion de Daniela, US-53)
public record ReglaPuntuacion(int puntosMaximos, int umbral1, int umbral2, int umbral3) {

    // Solo si no hay fila en reglas_puntuacion
    public static final ReglaPuntuacion POR_DEFECTO = new ReglaPuntuacion(100, 60, 80, 95);

    public int estrellas(int nota) {
        if (nota >= umbral3) return 3;
        if (nota >= umbral2) return 2;
        if (nota >= umbral1) return 1;
        return 0;
    }

    // Puntos proporcionales a la nota: máximo × nota / 100, redondeado
    public int puntos(int nota) {
        return (int) Math.round(puntosMaximos * nota / 100.0);
    }
}
