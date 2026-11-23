package com.proyecto.usuario.estudiante.model;

// T-422: una palabra del texto comparada con lo que leyó el niño
public record PalabraAlineada(String esperada, String leida, Tipo tipo) {

    public enum Tipo {
        CORRECTA,   // la leyó bien
        CAMBIADA,   // leyó otra palabra en su lugar
        OMITIDA,    // se la saltó
        AGREGADA    // dijo una palabra que no está en el texto
    }
}
