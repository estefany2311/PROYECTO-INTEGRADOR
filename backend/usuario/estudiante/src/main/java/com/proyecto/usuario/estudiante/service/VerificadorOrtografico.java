package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.config.DiccionarioEspanol;
import com.proyecto.usuario.estudiante.dto.ErrorOrtografico;
import com.proyecto.usuario.estudiante.model.ReglasOrtografia;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

    private final DiccionarioEspanol diccionario;

    public VerificadorOrtografico(DiccionarioEspanol diccionario) {
        this.diccionario = diccionario;
    }

    public List<ErrorOrtografico> verificar(String texto, ReglasOrtografia reglas, Set<String> permitidas) {
        List<ErrorOrtografico> errores = new ArrayList<>();
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
                String conTilde = buscarConTilde(minuscula);
                if (conTilde != null) {
                    if (reglas.marcaTildes()) {
                        errores.add(new ErrorOrtografico(palabra, m.start(), m.end(), ErrorOrtografico.TILDE,
                                comoLaOriginal(conTilde, palabra)));
                    }
                    continue;
                }
                String conEnie = buscarConEnie(minuscula);
                errores.add(new ErrorOrtografico(palabra, m.start(), m.end(), ErrorOrtografico.ORTOGRAFIA,
                        conEnie == null ? null : comoLaOriginal(conEnie, palabra)));
                continue;
            }
            if (reglas.marcaMayusculas() && inicioOracion && Character.isLowerCase(palabra.charAt(0))) {
                errores.add(new ErrorOrtografico(palabra, m.start(), m.end(), ErrorOrtografico.MAYUSCULA,
                        Character.toUpperCase(palabra.charAt(0)) + palabra.substring(1)));
            }
        }
        return errores;
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

    // La misma palabra con la tilde bien puesta: "arbol" → "árbol", "pérro" → "perro"
    private String buscarConTilde(String palabra) {
        String base = sinTildes(palabra);
        if (!base.equals(palabra) && diccionario.contiene(base)) return base;
        for (int i = 0; i < base.length(); i++) {
            char c = base.charAt(i);
            int vocal = VOCALES.indexOf(c);
            if (vocal >= 0) {
                String opcion = base.substring(0, i) + CON_TILDE.charAt(vocal) + base.substring(i + 1);
                if (!opcion.equals(palabra) && diccionario.contiene(opcion)) return opcion;
            }
            if (c == 'u') {
                String opcion = base.substring(0, i) + 'ü' + base.substring(i + 1);
                if (diccionario.contiene(opcion)) return opcion;
            }
        }
        return null;
    }

    // "nino" → "niño"
    private String buscarConEnie(String palabra) {
        for (int i = 0; i < palabra.length(); i++) {
            if (palabra.charAt(i) == 'n') {
                String opcion = palabra.substring(0, i) + 'ñ' + palabra.substring(i + 1);
                if (diccionario.contiene(opcion)) return opcion;
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
        if (Character.isUpperCase(original.charAt(0))) {
            return Character.toUpperCase(sugerencia.charAt(0)) + sugerencia.substring(1);
        }
        return sugerencia;
    }
}
