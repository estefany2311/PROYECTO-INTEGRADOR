package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.repository.ConfiguracionEvaluacionRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// T-427 (US-14): textos de 1.º, 3.º y 6.º grado con el umbral de precisión de cada grado (US-51)
class EvaluacionPorGradoTest {

    // Textos como los del catálogo: cortos y simples en 1.º, más largos y con signos en 6.º
    private static final String PRIMERO = "Mi mamá me mima. Yo amo a mi mamá.";
    private static final String TERCERO = "El zorro y el cóndor eran amigos. Un día el cóndor llevó al zorro "
            + "a una fiesta en el cielo, pero el zorro comió tanto que se quedó dormido.";
    private static final String SEXTO = "En 1911, Hiram Bingham llegó a Machu Picchu guiado por campesinos de la zona. "
            + "La ciudadela, construida por los incas en el siglo XV, había permanecido oculta entre las montañas; "
            + "hoy es una de las maravillas del mundo y recibe a miles de visitantes cada año.";

    // Umbrales por grado (como en configuracion_evaluacion): más exigentes en los grados mayores
    private EvaluadorLectura evaluadorConUmbral(int umbral) {
        ConfiguracionEvaluacionRepository configuracion = mock(ConfiguracionEvaluacionRepository.class);
        when(configuracion.umbralPrecision(7L)).thenReturn(Optional.of(umbral));
        return new EvaluadorLectura(new NormalizadorTexto(), new AlineadorPalabras(), new CalculadorPrecision(), configuracion);
    }

    @Test
    void primerGradoLecturaPerfectaAunqueWhisperNoPongaPuntuacion() {
        var r = evaluadorConUmbral(60).evaluar(7L, PRIMERO, "mi mama me mima yo amo a mi mama", 8_000).precision();

        assertEquals(100, r.precision());
        assertEquals(9, r.total());
        assertTrue(r.aprobado());
    }

    @Test
    void primerGradoUnErrorEnUnTextoCortoPesaMas() {
        var r = evaluadorConUmbral(60).evaluar(7L, PRIMERO, "mi mama me mira yo amo a mi mama", 8_000).precision();

        assertEquals(89, r.precision());     // 8 de 9
    }

    @Test
    void tercerGradoSeSaltaUnaFraseYRepiteUnaPalabra() {
        String leido = "el zorro y el condor eran amigos un dia el el condor llevo al zorro "
                + "a una fiesta pero el zorro comio tanto que se quedo dormido";   // se saltó "en el cielo"

        var r = evaluadorConUmbral(70).evaluar(7L, TERCERO, leido, 30_000).precision();

        assertEquals(29, r.total());
        assertEquals(26, r.correctas());
        assertEquals(90, r.precision());
        assertEquals(1, r.agregadas());     // el "el" repetido no baja la nota
        assertTrue(r.aprobado());
    }

    @Test
    void sextoGradoNumerosYNombresPropiosSeComparanBien() {
        String leido = "en mil novecientos once hiram bingham llego a machu picchu guiado por campesinos de la zona "
                + "la ciudadela construida por los incas en el siglo quince habia permanecido oculta entre las montañas "
                + "hoy es una de las maravillas del mundo y recibe a miles de visitantes cada año";

        var r = evaluadorConUmbral(85).evaluar(7L, SEXTO, leido, 60_000).precision();

        // "1911" y "mil novecientos once" son lo mismo; "XV" (romano) y "quince" no, y cuenta como cambiada
        assertEquals(1, r.total() - r.correctas());
        assertTrue(r.precision() >= 97);
        assertTrue(r.aprobado());
    }

    @Test
    void sextoGradoConVariosErroresNoLlegaAlUmbral() {
        String leido = "en mil novecientos once hiram bingan llego a machu pichu guiado por campesinos de la sona "
                + "la ciudad construida por los incas en el siglo quince habia permanecido escondida entre las montañas";

        var r = evaluadorConUmbral(85).evaluar(7L, SEXTO, leido, 60_000).precision();

        assertFalse(r.aprobado());     // además paró antes y faltó la última oración
    }

    // Cada grado puede pedir un umbral distinto: la misma lectura aprueba en 1.º y 3.º, pero no en 6.º
    @Test
    void elMismoResultadoSegunElUmbralDelGrado() {
        String leido = "el zorro y el condor eran amigos un dio el condor lleva el zorro "
                + "a una fiesta en el cielo pero el zorro come tanto y se durmio";   // 79 %

        assertTrue(evaluadorConUmbral(60).evaluar(7L, TERCERO, leido, 30_000).precision().aprobado());
        assertTrue(evaluadorConUmbral(70).evaluar(7L, TERCERO, leido, 30_000).precision().aprobado());
        assertFalse(evaluadorConUmbral(85).evaluar(7L, TERCERO, leido, 30_000).precision().aprobado());
    }
}
