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
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

// T-333 (US-19): el corrector con el diccionario real y frases como las que escriben los niños
class CorrectorEspanolTest {

    // Grados según los valores semilla de la US-52 (Daniela)
    private static final ReglasOrtografia PRIMER_GRADO = new ReglasOrtografia("baja", true);
    private static final ReglasOrtografia TERCER_GRADO = new ReglasOrtografia("media", false);
    private static final ReglasOrtografia QUINTO_GRADO = new ReglasOrtografia("alta", false);

    // Las de palabras_permitidas (Daniela) y diccionario_grado (V5)
    private static final Set<String> PERMITIDAS = Set.of("chacra", "wawa", "choclo", "cuy", "pingüino", "kiko");

    private static VerificadorOrtografico corrector;

    @BeforeAll
    static void cargarDiccionario() {
        corrector = new VerificadorOrtografico(new DiccionarioEspanol());
    }

    private static List<ErrorOrtografico> revisar(String texto, ReglasOrtografia reglas) {
        return corrector.verificar(texto, reglas, PERMITIDAS);
    }

    private static List<String> palabras(List<ErrorOrtografico> errores) {
        return errores.stream().map(ErrorOrtografico::palabra).toList();
    }

    @Nested
    class Tildes {

        @Test
        void marcaLasTildesQueFaltanYSugiereLaPalabra() {
            List<ErrorOrtografico> e = revisar("El arbol del jardin tiene una cancion.", TERCER_GRADO);

            assertEquals(List.of("arbol", "jardin", "cancion"), palabras(e));
            assertEquals(List.of("árbol", "jardín", "canción"), e.stream().map(ErrorOrtografico::sugerencia).toList());
            assertTrue(e.stream().allMatch(x -> x.tipo().equals(ErrorOrtografico.TILDE)));
        }

        @Test
        void marcaTildesEnVerbosYAdverbios() {
            assertEquals(List.of("corrio", "rapido"), palabras(revisar("El niño corrio rapido.", TERCER_GRADO)));
            assertEquals(List.of("tambien"), palabras(revisar("Ella tambien vino.", TERCER_GRADO)));
        }

        @Test
        void marcaLaTildeDeMas() {
            ErrorOrtografico e = revisar("Ayer fuí al cine.", TERCER_GRADO).get(0);

            assertEquals("fuí", e.palabra());
            assertEquals("fui", e.sugerencia());
        }

        @Test
        void noMarcaPalabrasQueExistenConYSinTilde() {
            // "esta" y "está", "mama" y "mamá": las dos existen, depende de la oración
            assertTrue(revisar("Mi mamá está en la casa y esta es mi mesa.", TERCER_GRADO).isEmpty());
            assertTrue(revisar("Mi papa y mi mama.", TERCER_GRADO).isEmpty());
        }

        @Test
        void enPrimerGradoNoSeMarcanTildes() {
            assertTrue(revisar("El arbol del jardin tiene una cancion.", PRIMER_GRADO).isEmpty());
        }

        @Test
        void dieresisDeUnaPalabraPermitida() {
            ErrorOrtografico e = revisar("El pinguino nada.", TERCER_GRADO).get(0);

            assertEquals(ErrorOrtografico.TILDE, e.tipo());
            assertEquals("pingüino", e.sugerencia());
        }

        @Test
        void sinEnieSugiereConEnie() {
            ErrorOrtografico e = revisar("Mi nino juega.", PRIMER_GRADO).get(0);

            assertEquals(ErrorOrtografico.ORTOGRAFIA, e.tipo());
            assertEquals("niño", e.sugerencia());
        }
    }

    @Nested
    class Mayusculas {

        @Test
        void enQuintoGradoMarcaLaMinusculaAlEmpezarCadaOracion() {
            List<ErrorOrtografico> e = revisar("hoy fui al parque. después comí helado! luego dormí? sí.", QUINTO_GRADO);

            assertEquals(List.of("hoy", "después", "luego", "sí"), palabras(e));
            assertEquals(List.of("Hoy", "Después", "Luego", "Sí"), e.stream().map(ErrorOrtografico::sugerencia).toList());
        }

        @Test
        void tambienDespuesDeLosSignosDeAperturaDePreguntaYExclamacion() {
            assertEquals(List.of("donde", "que"), palabras(revisar("¿donde estás? ¡que bonito!", QUINTO_GRADO)));
        }

        @Test
        void enTercerGradoNoSeMarcanMayusculas() {
            assertTrue(revisar("hoy fui al parque. después comí.", TERCER_GRADO).isEmpty());
        }

        @Test
        void losNombresPropiosNoSonError() {
            assertTrue(revisar("Fui a Lima con Toby y mi hermano Jesús.", QUINTO_GRADO).isEmpty());
        }

        @Test
        void unTextoTodoEnMayusculasSeRevisaIgual() {
            assertTrue(revisar("EL PERRO JUEGA.", QUINTO_GRADO).isEmpty());
            assertEquals(List.of("JUGETON"), palabras(revisar("EL PERRO ES JUGETON.", QUINTO_GRADO)));
        }
    }

    @Nested
    class PalabrasPermitidas {

        @Test
        void lasPalabrasDeLaZonaNoSonError() {
            assertTrue(revisar("Comí cuy con choclo en la chacra.", QUINTO_GRADO).isEmpty());
        }

        @Test
        void seRespetanAunqueEmpiecenConMayuscula() {
            assertTrue(revisar("Chacra grande. Kiko lee.", QUINTO_GRADO).isEmpty());
        }

        @Test
        void sinLaListaSiSeMarcarian() {
            assertEquals(List.of("cuy"), palabras(corrector.verificar("Comí cuy.", TERCER_GRADO, Set.of())));
        }

        @Test
        void losDiminutivosSeAceptan() {
            assertTrue(revisar("Mi gatito, mi perrito, mi abuelita y un poquito con mi amiguito.", QUINTO_GRADO).isEmpty());
        }
    }

    @Nested
    class ErroresDeNinos {

        @Test
        void marcaLasPalabrasMalEscritas() {
            List<ErrorOrtografico> e = revisar("Voy a aser la tarea, boi llendo a la escuela y haci fue.", PRIMER_GRADO);

            assertEquals(List.of("aser", "boi", "llendo", "haci"), palabras(e));
            assertTrue(e.stream().allMatch(x -> x.tipo().equals(ErrorOrtografico.ORTOGRAFIA)));
            assertNull(e.get(0).sugerencia());
        }

        @Test
        void laPosicionPermiteSubrayarLaPalabraExacta() {
            String texto = "Mi perro es muy jugeton.";
            ErrorOrtografico e = revisar(texto, PRIMER_GRADO).get(0);

            assertEquals("jugeton", texto.substring(e.inicio(), e.fin()));
        }

        @Test
        void numerosSignosYEmojisNoSonError() {
            assertTrue(revisar("Tengo 2 perros 🐶!", QUINTO_GRADO).isEmpty());
        }

        @Test
        void unTextoLargoSeRevisaRapido() {
            String texto = "Mi perro juega en el jardín con su pelota roja. ".repeat(60);

            assertTimeout(java.time.Duration.ofMillis(500), () -> revisar(texto, QUINTO_GRADO));
        }
    }
}
