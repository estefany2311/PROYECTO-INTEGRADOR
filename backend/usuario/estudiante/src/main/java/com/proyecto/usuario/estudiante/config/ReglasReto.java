package com.proyecto.usuario.estudiante.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * T-309 (US-08): regla de desbloqueo por defecto. El siguiente nivel se abre si el alumno
 * saca al menos esta cantidad de estrellas (de 0 a 3) en el reto.
 * Es configurable en application.properties (estudiante.reto.estrellas-minimas) hasta que
 * exista la US-54, donde el admin definirá la regla.
 */
@Component
public class ReglasReto {

    private final int estrellasMinimas;

    public ReglasReto(@Value("${estudiante.reto.estrellas-minimas:1}") int estrellasMinimas) {
        this.estrellasMinimas = estrellasMinimas;
    }

    public int estrellasMinimas() {
        return estrellasMinimas;
    }

    public boolean aprueba(int estrellas) {
        return estrellas >= estrellasMinimas;
    }
}
