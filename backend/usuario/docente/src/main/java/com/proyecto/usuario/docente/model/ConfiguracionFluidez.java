package com.proyecto.usuario.docente.model;

/**
 * T-392 (US-15): parámetros con los que se mide la fluidez de un grado. Vienen
 * de la configuración de voz del administrador (US-51, tabla
 * {@code configuracion_evaluacion}).
 *
 * @param grado               grado del alumno (1 a 6)
 * @param palabrasPorMinuto   velocidad de lectura esperada para el grado
 * @param toleranciaPausas    segundos de pausa permitidos entre palabras
 */
public record ConfiguracionFluidez(
        int grado,
        int palabrasPorMinuto,
        double toleranciaPausas
) {

    public ConfiguracionFluidez {
        if (palabrasPorMinuto <= 0) {
            throw new IllegalArgumentException("Las palabras por minuto deben ser mayores a 0");
        }
        if (toleranciaPausas < 0) {
            throw new IllegalArgumentException("La tolerancia de pausas no puede ser negativa");
        }
    }

    /**
     * Valores iniciales de la US-51 (los mismos que carga el módulo Admin). Se
     * usan cuando el administrador todavía no configuró el grado o su tabla no
     * está disponible, para que la fluidez siempre se pueda calcular.
     */
    public static ConfiguracionFluidez porDefecto(int grado) {
        return switch (grado) {
            case 1 -> new ConfiguracionFluidez(1, 30, 3.0);
            case 2 -> new ConfiguracionFluidez(2, 45, 2.5);
            case 4 -> new ConfiguracionFluidez(4, 75, 2.0);
            case 5 -> new ConfiguracionFluidez(5, 85, 1.5);
            case 6 -> new ConfiguracionFluidez(6, 95, 1.5);
            default -> new ConfiguracionFluidez(3, 60, 2.0);
        };
    }
}
