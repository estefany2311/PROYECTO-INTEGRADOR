package com.proyecto.usuario.docente.service;

/**
 * T-392 (US-15): fórmula del puntaje de fluidez lectora (0 a 100).
 *
 * <p>La fluidez mide dos cosas: el ritmo (palabras por minuto) y las pausas
 * entre palabras. Los parámetros de cada grado salen de la configuración del
 * administrador (US-51: {@code palabras_por_minuto} y {@code tolerancia_pausas}).</p>
 *
 * <ul>
 *   <li><b>Ritmo</b> = palabras por minuto leídas / palabras por minuto esperadas
 *       del grado, a escala 0-100 y con tope 100: leer más rápido que lo esperado
 *       no se premia ni se castiga.</li>
 *   <li><b>Pausas</b> = 100 menos {@value #PENALIZACION_PAUSA_LARGA} por cada
 *       pausa larga (mayor a la tolerancia) y {@value #PENALIZACION_VACILACION}
 *       por cada vacilación (pausa entre la mitad de la tolerancia y la
 *       tolerancia), sin bajar de 0.</li>
 *   <li><b>Fluidez</b> = {@value #PESO_RITMO} del ritmo + {@value #PESO_PAUSAS}
 *       de las pausas, redondeada.</li>
 * </ul>
 */
public final class FormulaFluidez {

    public static final double PESO_RITMO = 0.6;
    public static final double PESO_PAUSAS = 0.4;
    public static final int PENALIZACION_PAUSA_LARGA = 10;
    public static final int PENALIZACION_VACILACION = 3;

    private FormulaFluidez() {
    }

    /** Puntaje de ritmo (0 a 100) según las palabras por minuto leídas y las esperadas. */
    public static int puntajeRitmo(double palabrasPorMinuto, int esperadas) {
        double puntaje = palabrasPorMinuto / esperadas * 100;
        return (int) Math.round(Math.min(100, Math.max(0, puntaje)));
    }

    /** Puntaje de pausas (0 a 100) según las pausas largas y las vacilaciones. */
    public static int puntajePausas(int pausasLargas, int vacilaciones) {
        int puntaje = 100
                - PENALIZACION_PAUSA_LARGA * pausasLargas
                - PENALIZACION_VACILACION * vacilaciones;
        return Math.max(0, puntaje);
    }

    /** Puntaje final de fluidez (0 a 100) a partir de los dos anteriores. */
    public static int puntajeFluidez(int puntajeRitmo, int puntajePausas) {
        long puntaje = Math.round(PESO_RITMO * puntajeRitmo + PESO_PAUSAS * puntajePausas);
        return (int) Math.min(100, Math.max(0, puntaje));
    }
}
