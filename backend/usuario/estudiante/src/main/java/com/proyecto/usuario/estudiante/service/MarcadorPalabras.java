package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.model.PalabraTexto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

// T-428 (US-14): marca en el texto original (con tildes y puntuación) las palabras que leyó mal o se saltó.
// La calificación se hizo con el texto normalizado; aquí se vuelve a cada palabra como está escrita.
@Component
public class MarcadorPalabras {

    private static final String CORRECTA = "correcta";

    private final NormalizadorTexto normalizador;

    public MarcadorPalabras(NormalizadorTexto normalizador) {
        this.normalizador = normalizador;
    }

    // tipos: uno por palabra normalizada del texto; evaluadas: solo esas cuentan (si se acabó el tiempo)
    public List<PalabraTexto> marcar(String texto, List<String> tipos, int evaluadas) {
        List<PalabraTexto> resultado = new ArrayList<>();
        if (texto == null || texto.isBlank()) return resultado;
        int posicion = 0;
        for (String escrita : texto.trim().split("\\s+")) {
            // "vez," da una palabra; "1990" da varias ("mil novecientos noventa")
            int cuantas = normalizador.palabras(escrita).size();
            boolean mejorar = false;
            for (int k = 0; k < cuantas; k++, posicion++) {
                if (posicion < evaluadas && posicion < tipos.size() && !CORRECTA.equals(tipos.get(posicion))) {
                    mejorar = true;
                }
            }
            resultado.add(new PalabraTexto(escrita, mejorar));
        }
        return resultado;
    }
}
