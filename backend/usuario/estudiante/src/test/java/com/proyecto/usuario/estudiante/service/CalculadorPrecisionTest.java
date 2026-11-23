package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.model.PalabraAlineada;
import com.proyecto.usuario.estudiante.model.PalabraAlineada.Tipo;
import com.proyecto.usuario.estudiante.model.ResultadoPrecision;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// T-423 (US-14): precisión de la lectura y umbral del grado (US-51)
class CalculadorPrecisionTest {

    private final CalculadorPrecision calculador = new CalculadorPrecision();

    private static List<PalabraAlineada> palabras(Tipo... tipos) {
        List<PalabraAlineada> lista = new ArrayList<>();
        for (Tipo t : tipos) lista.add(new PalabraAlineada(t == Tipo.AGREGADA ? null : "x", t == Tipo.OMITIDA ? null : "x", t));
        return lista;
    }

    @Test
    void ochoDeDiezBienEsOchentaPorCiento() {
        ResultadoPrecision r = calculador.calcular(palabras(Tipo.CORRECTA, Tipo.CORRECTA, Tipo.CORRECTA, Tipo.CORRECTA,
                Tipo.CORRECTA, Tipo.CORRECTA, Tipo.CORRECTA, Tipo.CORRECTA, Tipo.CAMBIADA, Tipo.OMITIDA), 70, false);

        assertEquals(80, r.precision());
        assertEquals(8, r.correctas());
        assertEquals(10, r.evaluadas());
        assertTrue(r.aprobado());
    }

    @Test
    void debajoDelUmbralDelGradoNoAprueba() {
        ResultadoPrecision r = calculador.calcular(palabras(Tipo.CORRECTA, Tipo.CAMBIADA, Tipo.CORRECTA, Tipo.CAMBIADA), 70, false);

        assertEquals(50, r.precision());
        assertFalse(r.aprobado());
    }

    @Test
    void lasPalabrasAgregadasNoBajanLaPrecision() {
        ResultadoPrecision r = calculador.calcular(palabras(Tipo.AGREGADA, Tipo.CORRECTA, Tipo.CORRECTA), 70, false);

        assertEquals(100, r.precision());
        assertEquals(1, r.agregadas());
        assertEquals(2, r.total());
    }

    // Si paró antes por su cuenta, lo que faltó cuenta como omitido
    @Test
    void siParoAntesLoQueFaltoCuentaComoOmitido() {
        ResultadoPrecision r = calculador.calcular(palabras(Tipo.CORRECTA, Tipo.CORRECTA, Tipo.OMITIDA, Tipo.OMITIDA), 70, false);

        assertEquals(50, r.precision());
        assertEquals(100, r.cobertura());
    }

    // Si se le acabaron los 2 minutos, se califica solo hasta donde alcanzó a leer
    @Test
    void siSeAcaboElTiempoSoloSeCalificaLoQueAlcanzoALeer() {
        ResultadoPrecision r = calculador.calcular(palabras(Tipo.CORRECTA, Tipo.CAMBIADA, Tipo.CORRECTA, Tipo.OMITIDA, Tipo.OMITIDA), 70, true);

        assertEquals(67, r.precision());     // 2 de 3
        assertEquals(3, r.evaluadas());
        assertEquals(60, r.cobertura());     // leyó 3 de 5
    }

    @Test
    void sinLeerNadaEsCeroYNoAprueba() {
        ResultadoPrecision r = calculador.calcular(palabras(Tipo.OMITIDA, Tipo.OMITIDA), 0, true);

        assertEquals(0, r.precision());
        assertFalse(r.aprobado());
    }
}
