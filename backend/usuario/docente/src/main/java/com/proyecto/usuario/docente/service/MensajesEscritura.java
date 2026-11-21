package com.proyecto.usuario.docente.service;

/**
 * T-401 (US-22): retroalimentación que ve el alumno según su puntaje de
 * escritura.
 *
 * <p>El mensaje tiene dos partes: una frase según el puntaje final y, si hay algo
 * que mejorar, un consejo sobre la parte más débil (ortografía, longitud o
 * oraciones completas), en ese orden de prioridad cuando empatan.</p>
 */
public final class MensajesEscritura {

    private static final int PUNTAJE_EXCELENTE = 90;
    private static final int PUNTAJE_BUENO = 70;
    private static final int PUNTAJE_REGULAR = 50;
    // una parte con menos de esto merece un consejo
    private static final int PARTE_A_MEJORAR = 80;

    private MensajesEscritura() {
    }

    public static String para(int puntaje, int ortografia, int longitud, int consigna,
                              int errores, int palabras, int palabrasMinimas) {
        String frase = fraseSegunPuntaje(puntaje);
        String consejo = consejo(ortografia, longitud, consigna, errores, palabras, palabrasMinimas);
        return consejo == null ? frase : frase + " " + consejo;
    }

    private static String fraseSegunPuntaje(int puntaje) {
        if (puntaje >= PUNTAJE_EXCELENTE) {
            return "¡Excelente! Escribiste muy bien.";
        }
        if (puntaje >= PUNTAJE_BUENO) {
            return "¡Muy bien! Tu escritura va por buen camino.";
        }
        if (puntaje >= PUNTAJE_REGULAR) {
            return "Buen intento. Con un poco más de práctica vas a mejorar.";
        }
        return "Sigue practicando, tú puedes.";
    }

    // consejo sobre la parte más débil; null si todas están bien
    private static String consejo(int ortografia, int longitud, int consigna,
                                  int errores, int palabras, int palabrasMinimas) {
        int menor = Math.min(ortografia, Math.min(longitud, consigna));
        if (menor >= PARTE_A_MEJORAR) {
            return null;
        }
        if (ortografia == menor) {
            return errores == 1
                    ? "Revisa la palabra que quedó subrayada."
                    : "Revisa las " + errores + " palabras que quedaron subrayadas.";
        }
        if (longitud == menor) {
            int faltan = Math.max(1, palabrasMinimas - palabras);
            return faltan == 1
                    ? "Escribe un poco más: te faltó 1 palabra."
                    : "Escribe un poco más: te faltaron " + faltan + " palabras.";
        }
        return "Recuerda empezar cada oración con mayúscula y terminarla con punto.";
    }
}
