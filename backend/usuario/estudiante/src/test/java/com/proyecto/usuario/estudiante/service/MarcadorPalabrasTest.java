package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.model.PalabraTexto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// T-428 (US-14): las palabras por mejorar se marcan en el texto tal como está escrito
class MarcadorPalabrasTest {

    private final MarcadorPalabras marcador = new MarcadorPalabras(new NormalizadorTexto());

    @Test
    void marcaLasCambiadasYOmitidasConSuTildeYPuntuacion() {
        List<PalabraTexto> palabras = marcador.marcar("Había una vez un cóndor.",
                List.of("correcta", "cambiada", "correcta", "correcta", "omitida"), 5);

        assertEquals(List.of(
                new PalabraTexto("Había", false),
                new PalabraTexto("una", true),
                new PalabraTexto("vez", false),
                new PalabraTexto("un", false),
                new PalabraTexto("cóndor.", true)), palabras);
    }

    @Test
    void siTodoEstaBienNoMarcaNada() {
        List<PalabraTexto> palabras = marcador.marcar("El gato duerme",
                List.of("correcta", "correcta", "correcta"), 3);

        assertTrue(palabras.stream().noneMatch(PalabraTexto::mejorar));
    }

    @Test
    void unNumeroQueSonVariasPalabrasSeMarcaEntero() {
        // "31" se califica como "treinta y uno" (3 palabras); basta que una esté mal
        List<PalabraTexto> palabras = marcador.marcar("Tengo 31 lápices",
                List.of("correcta", "correcta", "cambiada", "correcta", "correcta"), 5);

        assertEquals(List.of(
                new PalabraTexto("Tengo", false),
                new PalabraTexto("31", true),
                new PalabraTexto("lápices", false)), palabras);
    }

    @Test
    void siSeAcaboElTiempoNoMarcaLoQueNoAlcanzoALeer() {
        // Leyó 2 de 4 palabras antes de que se cortara la grabación: las otras 2 no cuentan
        List<PalabraTexto> palabras = marcador.marcar("El sol sale temprano",
                List.of("correcta", "cambiada", "omitida", "omitida"), 2);

        assertEquals(List.of(false, true, false, false), palabras.stream().map(PalabraTexto::mejorar).toList());
    }

    @Test
    void unTextoVacioNoTienePalabras() {
        assertTrue(marcador.marcar("  ", List.of(), 0).isEmpty());
    }
}
