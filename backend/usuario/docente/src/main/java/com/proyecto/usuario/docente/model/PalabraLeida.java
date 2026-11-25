package com.proyecto.usuario.docente.model;

/**
 * T-393 (US-15): una palabra que el motor de voz escuchó en la lectura, con el
 * segundo en que empezó y terminó y qué tan seguro estuvo de haberla entendido.
 */
public record PalabraLeida(String palabra, double inicio, double fin, double probabilidad) {
}
