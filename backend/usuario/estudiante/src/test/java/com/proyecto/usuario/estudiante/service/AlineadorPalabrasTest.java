package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.model.PalabraAlineada;
import com.proyecto.usuario.estudiante.model.PalabraAlineada.Tipo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// T-422 (US-14): emparejar lo que leyó el niño con el texto
class AlineadorPalabrasTest {

    private final AlineadorPalabras alineador = new AlineadorPalabras();

    private List<Tipo> tipos(String texto, String leido) {
        return alineador.alinear(List.of(texto.split(" ")), leido.isEmpty() ? List.of() : List.of(leido.split(" ")))
                .stream().map(PalabraAlineada::tipo).toList();
    }

    @Test
    void lecturaPerfectaEsTodoCorrecto() {
        assertEquals(List.of(Tipo.CORRECTA, Tipo.CORRECTA, Tipo.CORRECTA), tipos("el gato duerme", "el gato duerme"));
    }

    // Lo importante: saltarse una palabra no vuelve error todas las siguientes
    @Test
    void unaPalabraSaltadaSoloMarcaEsaComoOmitida() {
        assertEquals(List.of(Tipo.CORRECTA, Tipo.OMITIDA, Tipo.CORRECTA, Tipo.CORRECTA),
                tipos("el gato negro duerme", "el negro duerme"));
    }

    @Test
    void otraPalabraEnSuLugarEsCambiada() {
        List<PalabraAlineada> a = alineador.alinear(List.of("el", "gato", "duerme"), List.of("el", "pato", "duerme"));

        assertEquals(new PalabraAlineada("gato", "pato", Tipo.CAMBIADA), a.get(1));
    }

    @Test
    void repetirOAgregarUnaPalabraEsAgregada() {
        assertEquals(List.of(Tipo.CORRECTA, Tipo.AGREGADA, Tipo.CORRECTA, Tipo.CORRECTA),
                tipos("el gato duerme", "el el gato duerme"));
    }

    @Test
    void siNoLeyoNadaTodoEsOmitido() {
        assertEquals(List.of(Tipo.OMITIDA, Tipo.OMITIDA), tipos("el gato", ""));
    }

    @Test
    void enPalabrasLargasSeToleraUnaLetra() {
        assertTrue(AlineadorPalabras.iguales("bicicleta", "bisicleta"));
        assertTrue(AlineadorPalabras.iguales("montañas", "montaña"));
        assertFalse(AlineadorPalabras.iguales("gato", "pato"));         // en palabras cortas, no
        assertFalse(AlineadorPalabras.iguales("vivia", "bebia"));       // dos letras distintas, no
    }
}
