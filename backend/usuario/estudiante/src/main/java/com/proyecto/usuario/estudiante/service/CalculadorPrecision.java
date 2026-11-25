package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.model.PalabraAlineada;
import com.proyecto.usuario.estudiante.model.PalabraAlineada.Tipo;
import com.proyecto.usuario.estudiante.model.ResultadoPrecision;
import org.springframework.stereotype.Component;

import java.util.List;

// T-423 (US-14): % de palabras bien leídas y si llega al umbral de precisión del grado (US-51).
// Si se acabó el tiempo de grabación, solo se califica hasta donde alcanzó a leer;
// si paró antes por su cuenta, las palabras que faltaron cuentan como omitidas.
@Component
public class CalculadorPrecision {

    public ResultadoPrecision calcular(List<PalabraAlineada> alineacion, int umbral, boolean seAcaboElTiempo) {
        int total = 0;
        int ultimaLeida = -1;   // posición (entre las palabras del texto) de la última que sí leyó
        for (PalabraAlineada p : alineacion) {
            if (p.tipo() == Tipo.AGREGADA) continue;
            if (p.tipo() != Tipo.OMITIDA) ultimaLeida = total;
            total++;
        }
        int evaluadas = seAcaboElTiempo ? ultimaLeida + 1 : total;

        int correctas = 0;
        int agregadas = 0;
        int posicion = 0;
        for (PalabraAlineada p : alineacion) {
            if (p.tipo() == Tipo.AGREGADA) {
                agregadas++;
                continue;
            }
            if (posicion < evaluadas && p.tipo() == Tipo.CORRECTA) correctas++;
            posicion++;
        }

        int precision = evaluadas == 0 ? 0 : (int) Math.round(correctas * 100.0 / evaluadas);
        int cobertura = total == 0 ? 0 : (int) Math.round(evaluadas * 100.0 / total);
        return new ResultadoPrecision(precision, correctas, evaluadas, total, cobertura, agregadas, umbral,
                evaluadas > 0 && precision >= umbral);
    }
}
