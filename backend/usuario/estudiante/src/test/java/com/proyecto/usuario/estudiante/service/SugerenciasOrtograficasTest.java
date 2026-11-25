package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.config.DiccionarioEspanol;
import com.proyecto.usuario.estudiante.dto.ErrorOrtografico;
import com.proyecto.usuario.estudiante.model.ReglasOrtografia;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// US-20 · T-368: sugerencias de corrección (T-364 y T-365)
class SugerenciasOrtograficasTest {

    private static final ReglasOrtografia MEDIA = new ReglasOrtografia("media", false);

    // El diccionario real se carga una sola vez para todas las pruebas
    private static VerificadorOrtografico corrector;

    @BeforeAll
    static void cargarDiccionarioReal() {
        corrector = new VerificadorOrtografico(new DiccionarioEspanol());
    }

    // Con un diccionario pequeño se sabe exactamente qué palabras puede sugerir
    private static VerificadorOrtografico con(String... palabras) {
        return new VerificadorOrtografico(new DiccionarioEspanol(Set.of(palabras)));
    }

    @Nested
    class MaximoYOrden {

        @Test
        void nuncaDevuelveMasDeTres() {
            List<String> s = con("casa", "caza", "masa", "pasa", "tasa", "cosa").sugerencias("kasa", Set.of());

            assertEquals(VerificadorOrtografico.MAX_SUGERENCIAS, s.size());
            assertEquals(3, s.size());
        }

        @Test
        void primeroLasQueSuenanIgual() {
            // "kasa" suena igual que "casa" y "caza"; "masa" cambia una letra que no suena igual
            List<String> s = con("casa", "caza", "masa", "pasa", "tasa", "cosa").sugerencias("kasa", Set.of());

            assertEquals(List.of("casa", "caza", "masa"), s);
        }

        @Test
        void laHNoSuenaYPesaMenos() {
            // "aser" y "hacer" suenan igual: va antes que "asar", que solo cambia una letra
            assertEquals(List.of("hacer", "asar"), con("hacer", "asar").sugerencias("aser", Set.of()));
        }

        @Test
        void bYVSeConfundenPorqueSuenanIgual() {
            assertEquals(List.of("voy", "boa"), con("voy", "boa").sugerencias("boi", Set.of()));
        }

        @Test
        void aIgualParecidoPrimeroLasPalabrasDelGrado() {
            // "cuy" (diccionario_grado) y "cuyo" están igual de cerca de "cuyy": gana la del grado
            assertEquals(List.of("cuy", "cuyo"), con("cuyo").sugerencias("cuyy", Set.of("cuy")));
        }

        @Test
        void laSugerenciaConEnieSigueSiendoLaPrimera() {
            assertEquals("niño", con("niño", "nido").sugerencias("nino", Set.of()).get(0));
        }
    }

    @Nested
    class Mayusculas {

        @Test
        void respetaLaMayusculaInicialDelNino() {
            assertEquals(List.of("Árbol"), con("árbol").sugerencias("Arvol", Set.of()));
        }

        @Test
        void unaPalabraTodaEnMayusculasSeSugiereEnMayusculas() {
            assertEquals(List.of("JUGUETÓN"), con("juguetón").sugerencias("JUGETON", Set.of()));
        }

        @Test
        void laTildeTambienRespetaLaMayuscula() {
            ErrorOrtografico e = con("árbol", "es", "grande").verificar("Arbol es grande.", MEDIA, Set.of()).get(0);

            assertEquals(ErrorOrtografico.TILDE, e.tipo());
            assertEquals(List.of("Árbol"), e.sugerencias());
        }
    }

    @Nested
    class EnElError {

        @Test
        void sugerenciaEsLaPrimeraDeLaLista() {
            ErrorOrtografico e = con("hacer", "asar", "voy", "a").verificar("Voy a aser.", MEDIA, Set.of()).get(0);

            assertEquals("aser", e.palabra());
            assertEquals(List.of("hacer", "asar"), e.sugerencias());
            assertEquals("hacer", e.sugerencia());
        }

        @Test
        void palabraSinNingunaSugerencia() {
            ErrorOrtografico e = con("mi", "perro").verificar("Mi xqzw.", MEDIA, Set.of()).get(0);

            assertTrue(e.sugerencias().isEmpty());
            assertNull(e.sugerencia());
        }

        @Test
        void laMismaPalabraRepetidaTieneLasMismasSugerencias() {
            List<ErrorOrtografico> e = con("hacer", "y").verificar("aser y aser", MEDIA, Set.of());

            assertEquals(2, e.size());
            assertEquals(e.get(0).sugerencias(), e.get(1).sugerencias());
        }

        @Test
        void losErroresDeMayusculaTraenUnaSolaSugerencia() {
            ErrorOrtografico e = con("mi", "perro").verificar("mi perro.", new ReglasOrtografia("alta", false), Set.of()).get(0);

            assertEquals(List.of("Mi"), e.sugerencias());
        }
    }

    // Con el diccionario real (LibreOffice es_ES), como escriben los niños
    @Nested
    class ConElDiccionarioReal {

        @Test
        void laPrimeraEsLaQueQuisoEscribir() {
            assertEquals("hacer", corrector.sugerencias("aser", Set.of()).get(0));
            assertEquals("voy", corrector.sugerencias("boi", Set.of()).get(0));
            assertEquals("yendo", corrector.sugerencias("llendo", Set.of()).get(0));
            assertEquals("hermano", corrector.sugerencias("ermano", Set.of()).get(0));
            assertEquals("bicicleta", corrector.sugerencias("vicicleta", Set.of()).get(0));
            assertEquals("azul", corrector.sugerencias("asul", Set.of()).get(0));
        }

        @Test
        void siempreHastaTres() {
            for (String palabra : List.of("aser", "boi", "llendo", "haci", "canpo", "yerva")) {
                assertTrue(corrector.sugerencias(palabra, Set.of()).size() <= 3);
            }
        }
    }
}