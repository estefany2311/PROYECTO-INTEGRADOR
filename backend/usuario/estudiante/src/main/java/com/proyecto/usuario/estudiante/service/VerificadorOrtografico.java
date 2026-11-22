package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.config.DiccionarioEspanol;
import com.proyecto.usuario.estudiante.dto.ErrorOrtografico;
import com.proyecto.usuario.estudiante.model.ReglasOrtografia;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// T-328 (US-19): revisa palabra por palabra el texto del alumno con las reglas de su grado
@Component
public class VerificadorOrtografico {

    private static final Pattern PALABRA = Pattern.compile("\\p{L}+");
    private static final Pattern FIN_DE_ORACION = Pattern.compile("[.!?]");
    private static final String[] DIMINUTIVOS = {"citos", "citas", "cito", "cita", "itos", "itas", "ito", "ita"};
    private static final String VOCALES = "aeiou";
    private static final String CON_TILDE = "áéíóú";

    // US-20 (T-364): sugerencias para corregir una palabra
    private static final String LETRAS = "abcdefghijklmnñopqrstuvwxyz";
    private static final int LARGO_MAXIMO_DISTANCIA_2 = 12;   // con palabras más largas solo se busca a distancia 1
    // Letras que los niños confunden porque suenan igual
    private static final String[][] PARECIDAS = {{"b", "v"}, {"s", "c"}, {"s", "z"}, {"c", "z"}, {"c", "k"},
            {"q", "k"}, {"g", "j"}, {"y", "i"}, {"y", "l"}, {"n", "ñ"}};

    private final DiccionarioEspanol diccionario;

    public VerificadorOrtografico(DiccionarioEspanol diccionario) {
        this.diccionario = diccionario;
    }

    public List<ErrorOrtografico> verificar(String texto, ReglasOrtografia reglas, Set<String> permitidas) {
        List<ErrorOrtografico> errores = new ArrayList<>();
        Map<String, List<String>> yaBuscadas = new HashMap<>();   // si repite la palabra, no se busca otra vez
        Matcher m = PALABRA.matcher(texto);
        int finAnterior = 0;
        boolean primera = true;
        while (m.find()) {
            String palabra = m.group();
            boolean inicioOracion = primera || FIN_DE_ORACION.matcher(texto.substring(finAnterior, m.start())).find();
            primera = false;
            finAnterior = m.end();

            String minuscula = palabra.toLowerCase(Locale.ROOT);
            if (permitidas.contains(minuscula)) continue;
            // Con mayúscula inicial en medio de la oración es un nombre propio (Toby, Lima): no se revisa
            if (!inicioOracion && esNombrePropio(palabra)) continue;

            boolean correcta = diccionario.contiene(minuscula) || esDiminutivo(minuscula);
            if (!correcta) {
                String conTilde = buscarConTilde(minuscula, permitidas);
                if (conTilde != null) {
                    if (reglas.marcaTildes()) {
                        errores.add(new ErrorOrtografico(palabra, m.start(), m.end(), ErrorOrtografico.TILDE,
                                comoLaOriginal(conTilde, palabra)));
                    }
                    continue;
                }
                List<String> opciones = yaBuscadas.computeIfAbsent(palabra, p -> sugerencias(p, permitidas));
                errores.add(ErrorOrtografico.conSugerencias(palabra, m.start(), m.end(),
                        ErrorOrtografico.ORTOGRAFIA, opciones));
                continue;
            }
            if (reglas.marcaMayusculas() && inicioOracion && Character.isLowerCase(palabra.charAt(0))) {
                errores.add(new ErrorOrtografico(palabra, m.start(), m.end(), ErrorOrtografico.MAYUSCULA,
                        Character.toUpperCase(palabra.charAt(0)) + palabra.substring(1)));
            }
        }
        return errores;
    }

    // US-20 · T-364: palabras parecidas que existen, para sugerirlas.
    // Busca a distancia de edición 1 y 2 (letra cambiada, de más, de menos o dos letras cambiadas
    // de lugar). Las tildes no cuentan al buscar: "aci" encuentra "así".
    List<String> sugerencias(String palabra, Set<String> permitidas) {
        String minuscula = palabra.toLowerCase(Locale.ROOT);
        String base = sinTildes(minuscula);
        Map<String, Double> encontradas = new HashMap<>();

        // "nino" → "niño" (la que ya se sugería antes) siempre va primero
        String conEnie = buscarConEnie(minuscula, permitidas);
        if (conEnie != null) encontradas.put(conEnie, -1.0);

        Set<String> distancia1 = ediciones(base);
        for (String opcion : distancia1) agregarSiExiste(opcion, 1, minuscula, permitidas, encontradas);

        // A distancia 2, cuando uno de los dos cambios es de letras que suenan igual:
        // "aser" → "hacer", "boi" → "voy", "llendo" → "yendo" (lo que el niño quiso escribir)
        if (base.length() <= LARGO_MAXIMO_DISTANCIA_2) {
            Set<String> revisadas = new HashSet<>(distancia1);
            Set<String> parecidas = edicionesQueSuenanIgual(base);
            for (String opcion : distancia1) {
                for (String opcion2 : edicionesQueSuenanIgual(opcion)) {
                    if (revisadas.add(opcion2)) agregarSiExiste(opcion2, 2, minuscula, permitidas, encontradas);
                }
            }
            for (String opcion : parecidas) {
                for (String opcion2 : ediciones(opcion)) {
                    if (revisadas.add(opcion2)) agregarSiExiste(opcion2, 2, minuscula, permitidas, encontradas);
                }
            }
        }

        // Primero las de menos cambios; a igual cambios, primero las palabras del grado
        // (diccionario_grado y las permitidas del admin)
        return encontradas.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Double>>comparingDouble(Map.Entry::getValue)
                        .thenComparing(e -> permitidas.contains(e.getKey()) ? 0 : 1)
                        .thenComparing(Map.Entry::getKey))
                .map(e -> comoLaOriginal(e.getKey(), palabra))
                .toList();
    }

    // Si la opción (sin tildes) existe tal cual o con una tilde o diéresis, se guarda con sus cambios
    private void agregarSiExiste(String opcion, int cambios, String original, Set<String> permitidas,
                                 Map<String, Double> encontradas) {
        for (String real : conTildesPosibles(opcion)) {
            if (!real.equals(original) && !encontradas.containsKey(real) && existe(real, permitidas)) {
                encontradas.put(real, cambios + (real.equals(opcion) ? 0 : 0.1));
            }
        }
    }

    // La palabra sin tilde, y con cada vocal tildada o con ü (en español va una sola tilde por palabra)
    private static List<String> conTildesPosibles(String palabra) {
        List<String> opciones = new ArrayList<>();
        opciones.add(palabra);
        for (int i = 0; i < palabra.length(); i++) {
            int vocal = VOCALES.indexOf(palabra.charAt(i));
            if (vocal >= 0) {
                opciones.add(palabra.substring(0, i) + CON_TILDE.charAt(vocal) + palabra.substring(i + 1));
            }
            if (palabra.charAt(i) == 'u') opciones.add(palabra.substring(0, i) + 'ü' + palabra.substring(i + 1));
        }
        return opciones;
    }

    // Todas las palabras a distancia 1: una letra de menos, dos cambiadas de lugar, una cambiada o una de más
    private static Set<String> ediciones(String palabra) {
        Set<String> resultado = new LinkedHashSet<>();
        int n = palabra.length();
        for (int i = 0; i <= n; i++) {
            String antes = palabra.substring(0, i);
            String despues = palabra.substring(i);
            if (i < n) resultado.add(antes + despues.substring(1));                                   // de menos
            if (i < n - 1) resultado.add(antes + despues.charAt(1) + despues.charAt(0) + despues.substring(2)); // de lugar
            for (char letra : LETRAS.toCharArray()) {
                if (i < n && letra != despues.charAt(0)) resultado.add(antes + letra + despues.substring(1)); // cambiada
                resultado.add(antes + letra + despues);                                               // de más
            }
        }
        resultado.remove(palabra);
        return resultado;
    }

    // Cambios de letras que suenan igual: poner o quitar una "h", b↔v, s↔c↔z, y↔ll... y dos letras de lugar
    private static Set<String> edicionesQueSuenanIgual(String palabra) {
        Set<String> resultado = new HashSet<>();
        int n = palabra.length();
        for (int i = 0; i <= n; i++) {
            resultado.add(palabra.substring(0, i) + 'h' + palabra.substring(i));                 // h de más
            if (i == n) break;
            char c = palabra.charAt(i);
            if (c == 'h') resultado.add(palabra.substring(0, i) + palabra.substring(i + 1));     // h de menos
            for (String[] par : PARECIDAS) {
                char otra = par[0].charAt(0) == c ? par[1].charAt(0) : par[1].charAt(0) == c ? par[0].charAt(0) : 0;
                if (otra != 0) resultado.add(palabra.substring(0, i) + otra + palabra.substring(i + 1));
            }
            if (i < n - 1) resultado.add(palabra.substring(0, i) + palabra.charAt(i + 1) + c + palabra.substring(i + 2));
        }
        resultado.remove(palabra);
        return resultado;
    }

    // "Toby" sí, "JUGETON" no: una palabra toda en mayúsculas se revisa igual
    private static boolean esNombrePropio(String palabra) {
        return Character.isUpperCase(palabra.charAt(0)) && !palabra.equals(palabra.toUpperCase(Locale.ROOT));
    }

    // "gatito", "abuelita", "poquito": el diccionario no trae todos los diminutivos
    private boolean esDiminutivo(String palabra) {
        for (String sufijo : DIMINUTIVOS) {
            if (palabra.endsWith(sufijo) && palabra.length() > sufijo.length() + 1) {
                String base = palabra.substring(0, palabra.length() - sufijo.length());
                if (base.endsWith("qu")) base = base.substring(0, base.length() - 2) + "c";
                else if (base.endsWith("gu")) base = base.substring(0, base.length() - 1);
                for (String fin : new String[]{"o", "a", "e", ""}) {
                    if (diccionario.contiene(base + fin)) return true;
                }
            }
        }
        return false;
    }

    // Existe en el diccionario o en las palabras permitidas ("pinguino" → "pingüino", que está en V5)
    private boolean existe(String palabra, Set<String> permitidas) {
        return diccionario.contiene(palabra) || permitidas.contains(palabra);
    }

    // La misma palabra con la tilde bien puesta: "arbol" → "árbol", "pérro" → "perro"
    private String buscarConTilde(String palabra, Set<String> permitidas) {
        String base = sinTildes(palabra);
        if (!base.equals(palabra) && existe(base, permitidas)) return base;
        for (int i = 0; i < base.length(); i++) {
            char c = base.charAt(i);
            int vocal = VOCALES.indexOf(c);
            if (vocal >= 0) {
                String opcion = base.substring(0, i) + CON_TILDE.charAt(vocal) + base.substring(i + 1);
                if (!opcion.equals(palabra) && existe(opcion, permitidas)) return opcion;
            }
            if (c == 'u') {
                String opcion = base.substring(0, i) + 'ü' + base.substring(i + 1);
                if (existe(opcion, permitidas)) return opcion;
            }
        }
        return null;
    }

    // "nino" → "niño"
    private String buscarConEnie(String palabra, Set<String> permitidas) {
        for (int i = 0; i < palabra.length(); i++) {
            if (palabra.charAt(i) == 'n') {
                String opcion = palabra.substring(0, i) + 'ñ' + palabra.substring(i + 1);
                if (existe(opcion, permitidas)) return opcion;
            }
        }
        return null;
    }

    private static String sinTildes(String palabra) {
        StringBuilder sb = new StringBuilder();
        for (char c : palabra.toCharArray()) {
            int i = CON_TILDE.indexOf(c);
            sb.append(i >= 0 ? VOCALES.charAt(i) : (c == 'ü' ? 'u' : c));
        }
        return Normalizer.normalize(sb.toString(), Normalizer.Form.NFC);
    }

    // Si el niño escribió "Arbol" al inicio, la sugerencia es "Árbol"
    private static String comoLaOriginal(String sugerencia, String original) {
        if (original.length() > 1 && original.equals(original.toUpperCase(Locale.ROOT))) {
            return sugerencia.toUpperCase(Locale.ROOT);     // "JUGETON" → "JUGUETÓN"
        }
        if (Character.isUpperCase(original.charAt(0))) {
            return Character.toUpperCase(sugerencia.charAt(0)) + sugerencia.substring(1);
        }
        return sugerencia;
    }
}