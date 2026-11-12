package com.proyecto.usuario.estudiante.model;

// US-52 (Daniela): qué tan estricto es el corrector en el grado del alumno
public record ReglasOrtografia(String sensibilidad, boolean ignorarTildes) {

    public static final String BAJA = "baja";
    public static final String MEDIA = "media";
    public static final String ALTA = "alta";

    // Si el admin todavía no configuró el grado
    public static final ReglasOrtografia POR_DEFECTO = new ReglasOrtografia(MEDIA, false);

    public boolean marcaTildes() {
        return !ignorarTildes && !BAJA.equals(sensibilidad);
    }

    public boolean marcaMayusculas() {
        return ALTA.equals(sensibilidad);
    }
}
