package com.proyecto.usuario.docente.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * T-277 (US-27): colores del semáforo de rendimiento del dashboard docente.
 * La regla y los umbrales están documentados en {@code docs/regla-semaforo.md}.
 *
 * <p>El código es el valor que viaja al frontend; el texto y el ícono acompañan
 * al color para que no sea la única señal (T-280).</p>
 */
public enum ColorSemaforo {

    VERDE("verde", "#22C55E", "En nivel esperado", "✅"),
    AMARILLO("amarillo", "#F59E0B", "En proceso", "⚠️"),
    ROJO("rojo", "#F51E38", "Necesita refuerzo", "⛔"),
    GRIS("gris", "#9CA3AF", "Sin evaluaciones", "—");

    private final String codigo;
    private final String hex;
    private final String texto;
    private final String icono;

    ColorSemaforo(String codigo, String hex, String texto, String icono) {
        this.codigo = codigo;
        this.hex = hex;
        this.texto = texto;
        this.icono = icono;
    }

    /**
     * Código en minúsculas que consume el frontend: verde, amarillo, rojo o gris.
     */
    @JsonValue
    public String getCodigo() {
        return codigo;
    }

    public String getHex() {
        return hex;
    }

    public String getTexto() {
        return texto;
    }

    public String getIcono() {
        return icono;
    }
}
