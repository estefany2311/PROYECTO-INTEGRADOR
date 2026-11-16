package com.proyecto.usuario.estudiante.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
