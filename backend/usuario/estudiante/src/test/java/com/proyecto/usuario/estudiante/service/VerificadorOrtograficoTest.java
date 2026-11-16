package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.config.DiccionarioEspanol;
import com.proyecto.usuario.estudiante.dto.ErrorOrtografico;
import com.proyecto.usuario.estudiante.model.ReglasOrtografia;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// T-328 (US-19): reglas del corrector con un diccionario pequeño
class VerificadorOrtograficoTest {

    private static final ReglasOrtografia BAJA = new ReglasOrtografia("baja", true);
    private static final ReglasOrtografia MEDIA = new ReglasOrtografia("media", false);
    private static final ReglasOrtografia ALTA = new ReglasOrtografia("alta", false);

    private final VerificadorOrtografico verificador = new VerificadorOrtografico(new DiccionarioEspanol(Set.of(
            "mi", "perro", "se", "llama", "come", "árbol", "es", "grande", "canción", "niño", "juega", "gato",
            "abuela", "poco", "amigo", "un", "el", "la", "y", "con", "su", "pingüino", "corre")));

    private List<ErrorOrtografico> errores(String texto, ReglasOrtografia reglas) {
        return verificador.verificar(texto, reglas, Set.of());
    }

    @Test
    void textoCorrectoNoTieneErrores() {
        assertTrue(errores("Mi perro se llama Toby.", ALTA).isEmpty());
    }

    @Test
    void palabraQueNoExisteEsErrorDeOrtografiaConSuPosicion() {
        List<ErrorOrtografico> e = errores("Mi pero corre.", MEDIA);

        assertEquals(1, e.size());
        assertEquals("pero", e.get(0).palabra());
        assertEquals(3, e.get(0).inicio());
        assertEquals(7, e.get(0).fin());
        assertEquals(ErrorOrtografico.ORTOGRAFIA, e.get(0).tipo());
    }

    @Test
    void faltaLaTildeYSugiereLaPalabraCorrecta() {
        List<ErrorOrtografico> e = errores("El arbol es grande.", MEDIA);

        assertEquals(1, e.size());
        assertEquals(ErrorOrtografico.TILDE, e.get(0).tipo());
        assertEquals("árbol", e.get(0).sugerencia());
    }

    @Test
    void tildeDeMasTambienSeMarca() {
        List<ErrorOrtografico> e = errores("Mi pérro.", MEDIA);

        assertEquals(ErrorOrtografico.TILDE, e.get(0).tipo());
        assertEquals("perro", e.get(0).sugerencia());
    }

    @Test
    void conIgnorarTildesLasTildesNoSonError() {
        assertTrue(errores("El arbol es grande y la cancion.", new ReglasOrtografia("media", true)).isEmpty());
    }

    @Test
    void sensibilidadBajaNoMarcaTildesNiMayusculas() {
        assertTrue(errores("el arbol es grande.", new ReglasOrtografia("baja", false)).isEmpty());
    }

    @Test
    void sensibilidadBajaSiMarcaLasPalabrasQueNoExisten() {
        assertEquals(1, errores("Mi perro jugeton.", BAJA).size());
    }

    @Test
    void sinEnieSugiereLaPalabraConEnie() {
        List<ErrorOrtografico> e = errores("El nino juega.", BAJA);

        assertEquals(ErrorOrtografico.ORTOGRAFIA, e.get(0).tipo());
        assertEquals("niño", e.get(0).sugerencia());
    }

    @Test
    void sinDieresisSeMarcaComoTilde() {
        assertEquals("pingüino", errores("El pinguino.", MEDIA).get(0).sugerencia());
    }

    @Test
    void sensibilidadAltaMarcaMinusculaAlEmpezarLaOracion() {
        List<ErrorOrtografico> e = errores("mi perro come. el gato juega.", ALTA);

        assertEquals(2, e.size());
        assertEquals(ErrorOrtografico.MAYUSCULA, e.get(0).tipo());
        assertEquals("Mi", e.get(0).sugerencia());
        assertEquals("El", e.get(1).sugerencia());
    }

    @Test
    void sensibilidadMediaNoMarcaMayusculas() {
        assertTrue(errores("mi perro come.", MEDIA).isEmpty());
    }

    @Test
    void nombresPropiosEnMedioDeLaOracionNoSeRevisan() {
        assertTrue(errores("Mi perro se llama Toby y mi gato Misifú.", ALTA).isEmpty());
    }

    @Test
    void laSugerenciaRespetaLaMayusculaDelNino() {
        assertEquals("Árbol", errores("Arbol grande.", MEDIA).get(0).sugerencia());
    }

    @Test
    void diminutivosSeAceptan() {
        assertTrue(errores("Mi gatito juega con mi abuelita, un poquito con su amiguito.", MEDIA).isEmpty());
    }

    @Test
    void palabrasPermitidasNuncaSonError() {
        assertTrue(verificador.verificar("Mi chacra y mi cuy.", ALTA, Set.of("chacra", "cuy")).isEmpty());
    }

    @Test
    void lasMayusculasNoCambianLaRevision() {
        ErrorOrtografico e = errores("PERRO JUGETON.", MEDIA).get(0);

        assertEquals("JUGETON", e.palabra());
        assertNull(e.sugerencia());
    }

    @Test
    void textoVacioNoTieneErrores() {
        assertTrue(errores("", ALTA).isEmpty());
        assertTrue(errores("  ... ¡! 123 ", ALTA).isEmpty());
    }
}
