package com.proyecto.usuario.docente.service;

import java.util.regex.Pattern;

/**
 * T-399 (US-22): fórmula del puntaje de escritura (0 a 100).
 *
 * <p>Se compone de tres partes:</p>
 * <ul>
 *   <li><b>Ortografía</b> ({@value #PESO_ORTOGRAFIA}): baja con la proporción de
 *       palabras con error. Cada error por palabra escrita resta
 *       {@value #CASTIGO_POR_PROPORCION_DE_ERRORES} puntos por cada 100; con la
 *       mitad de las palabras mal escritas ya queda en 0.</li>
 *   <li><b>Longitud</b> ({@value #PESO_LONGITUD}): palabras escritas sobre las
 *       palabras mínimas que pide la consigna del nivel, con tope 100.</li>
 *   <li><b>Cumplimiento de la consigna</b> ({@value #PESO_CONSIGNA}): la consigna
 *       pide escribir oraciones, así que se mide la proporción de oraciones
 *       completas (empiezan con mayúscula y terminan con punto, ! o ?).</li>
 * </ul>
 *
 * <p>El cumplimiento de la consigna es una aproximación por estructura: no
 * interpreta el contenido del texto. Los pesos son valores iniciales que se
 * pueden ajustar aquí, en un solo lugar.</p>
 */
public final class FormulaEscritura {

    public static final double PESO_ORTOGRAFIA = 0.5;
    public static final double PESO_LONGITUD = 0.3;
    public static final double PESO_CONSIGNA = 0.2;
    public static final int CASTIGO_POR_PROPORCION_DE_ERRORES = 200;

    private static final Pattern ESPACIOS = Pattern.compile("\\s+");
    // corta después de . ! ? seguidos de espacio
    private static final Pattern FIN_DE_ORACION = Pattern.compile("(?<=[.!?])\\s+");
    private static final Pattern TERMINA_BIEN = Pattern.compile(".*[.!?]$", Pattern.DOTALL);

    private FormulaEscritura() {
    }

    /** Oraciones del texto y cuántas están completas. */
    public record Oraciones(int total, int completas) {
    }

    /** Palabras del texto: lo que queda al separar por espacios, sin contar signos sueltos. */
    public static int contarPalabras(String texto) {
        if (texto == null || texto.isBlank()) {
            return 0;
        }
        int palabras = 0;
        for (String parte : ESPACIOS.split(texto.trim())) {
            if (parte.chars().anyMatch(Character::isLetterOrDigit)) {
                palabras++;
            }
        }
        return palabras;
    }

    /**
     * Cuenta las oraciones y cuáles están completas: empiezan con letra
     * mayúscula (se admiten ¿ y ¡ al inicio) y terminan en . ! o ?.
     */
    public static Oraciones contarOraciones(String texto) {
        if (texto == null || texto.isBlank()) {
            return new Oraciones(0, 0);
        }
        int total = 0;
        int completas = 0;
        for (String oracion : FIN_DE_ORACION.split(texto.trim())) {
            String limpia = oracion.trim();
            if (limpia.isEmpty()) {
                continue;
            }
            total++;
            if (empiezaConMayuscula(limpia) && TERMINA_BIEN.matcher(limpia).matches()) {
                completas++;
            }
        }
        return new Oraciones(total, completas);
    }

    private static boolean empiezaConMayuscula(String oracion) {
        for (char c : oracion.toCharArray()) {
            if (c == '¿' || c == '¡' || c == '"' || c == '«') {
                continue;
            }
            return Character.isUpperCase(c);
        }
        return false;
    }

    /** Puntaje de ortografía (0 a 100) según los errores y las palabras escritas. */
    public static int puntajeOrtografia(int errores, int palabras) {
        if (palabras <= 0) {
            return 0;
        }
        double proporcion = Math.min(1.0, (double) errores / palabras);
        double puntaje = 100 - proporcion * CASTIGO_POR_PROPORCION_DE_ERRORES;
        return (int) Math.round(Math.max(0, puntaje));
    }

    /** Puntaje de longitud (0 a 100): palabras escritas sobre las mínimas de la consigna. */
    public static int puntajeLongitud(int palabras, int palabrasMinimas) {
        if (palabrasMinimas <= 0) {
            return 100;
        }
        double puntaje = (double) palabras / palabrasMinimas * 100;
        return (int) Math.round(Math.min(100, Math.max(0, puntaje)));
    }

    /** Puntaje de cumplimiento (0 a 100): proporción de oraciones completas. */
    public static int puntajeConsigna(Oraciones oraciones) {
        if (oraciones.total() == 0) {
            return 0;
        }
        return (int) Math.round((double) oraciones.completas() / oraciones.total() * 100);
    }

    /** Puntaje final de escritura (0 a 100) con los pesos de la fórmula. */
    public static int puntajeEscritura(int ortografia, int longitud, int consigna) {
        long puntaje = Math.round(PESO_ORTOGRAFIA * ortografia
                + PESO_LONGITUD * longitud
                + PESO_CONSIGNA * consigna);
        return (int) Math.min(100, Math.max(0, puntaje));
    }
}
