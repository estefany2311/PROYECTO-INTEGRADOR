package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.model.PalabraAlineada;
import com.proyecto.usuario.estudiante.model.PalabraAlineada.Tipo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

// T-422 (US-14): empareja las palabras del texto con las que leyó el niño (distancia de edición por palabras).
// Así una palabra saltada no hace que todas las siguientes cuenten como error.
@Component
public class AlineadorPalabras {

    static final int LARGO_PARA_TOLERAR = 5;   // desde 5 letras se acepta una letra distinta ("bisicleta")

    public List<PalabraAlineada> alinear(List<String> esperadas, List<String> leidas) {
        int n = esperadas.size();
        int m = leidas.size();
        // costo[i][j]: cuánto cuesta emparejar el resto del texto (desde i) con el resto de lo leído (desde j)
        int[][] costo = new int[n + 1][m + 1];
        for (int i = n; i >= 0; i--) costo[i][m] = n - i;
        for (int j = m; j >= 0; j--) costo[n][j] = m - j;
        for (int i = n - 1; i >= 0; i--) {
            for (int j = m - 1; j >= 0; j--) {
                int cambio = costo[i + 1][j + 1] + (iguales(esperadas.get(i), leidas.get(j)) ? 0 : 1);
                costo[i][j] = Math.min(cambio, Math.min(costo[i + 1][j] + 1, costo[i][j + 1] + 1));
            }
        }

        // Se recorre de adelante hacia atrás; ante un empate se empareja lo primero que leyó.
        // Así en "el el gato" la palabra repetida es la segunda, como lo diría el niño.
        List<PalabraAlineada> resultado = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < n || j < m) {
            if (i < n && j < m) {
                String esperada = esperadas.get(i);
                String leida = leidas.get(j);
                boolean igual = iguales(esperada, leida);
                if (costo[i][j] == costo[i + 1][j + 1] + (igual ? 0 : 1)) {
                    resultado.add(new PalabraAlineada(esperada, leida, igual ? Tipo.CORRECTA : Tipo.CAMBIADA));
                    i++;
                    j++;
                    continue;
                }
            }
            if (i < n && costo[i][j] == costo[i + 1][j] + 1) {
                resultado.add(new PalabraAlineada(esperadas.get(i), null, Tipo.OMITIDA));
                i++;
            } else {
                resultado.add(new PalabraAlineada(null, leidas.get(j), Tipo.AGREGADA));
                j++;
            }
        }
        return resultado;
    }

    // Igual, o casi igual en palabras largas: Whisper y la pronunciación de los niños varían un poco
    static boolean iguales(String esperada, String leida) {
        if (esperada.equals(leida)) return true;
        return esperada.length() >= LARGO_PARA_TOLERAR && distanciaLetras(esperada, leida) <= 1;
    }

    private static int distanciaLetras(String a, String b) {
        if (Math.abs(a.length() - b.length()) > 1) return 2;
        int[] anterior = new int[b.length() + 1];
        int[] actual = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) anterior[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            actual[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cambio = anterior[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);
                actual[j] = Math.min(cambio, Math.min(anterior[j] + 1, actual[j - 1] + 1));
            }
            int[] t = anterior;
            anterior = actual;
            actual = t;
        }
        return anterior[b.length()];
    }
}
