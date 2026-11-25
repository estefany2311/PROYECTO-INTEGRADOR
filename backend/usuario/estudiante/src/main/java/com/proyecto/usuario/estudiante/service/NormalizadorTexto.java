package com.proyecto.usuario.estudiante.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// T-421 (US-14): deja el texto de la lectura y lo que escuchó Whisper en la misma forma para compararlos:
// minúsculas, sin tildes ni puntuación (la ñ se queda) y los números escritos con letras
@Component
public class NormalizadorTexto {

    private static final Pattern NUMERO = Pattern.compile("\\d+");
    private static final Pattern PALABRA = Pattern.compile("[a-zñ]+");
    private static final char ENIE = '\u0001';

    public List<String> palabras(String texto) {
        if (texto == null || texto.isBlank()) return List.of();
        String s = numerosEnLetras(texto.toLowerCase(Locale.ROOT));
        s = s.replace('ñ', ENIE);
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");   // quita tildes y diéresis
        s = s.replace(ENIE, 'ñ');
        List<String> palabras = new ArrayList<>();
        Matcher m = PALABRA.matcher(s);
        while (m.find()) palabras.add(m.group());
        return palabras;
    }

    // Whisper a veces escribe "3 gatos" y el texto dice "tres gatos"
    private static String numerosEnLetras(String texto) {
        Matcher m = NUMERO.matcher(texto);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String digitos = m.group();
            String enLetras = digitos.length() <= 6 ? NumerosEnLetras.escribir(Integer.parseInt(digitos)) : digitos;
            m.appendReplacement(sb, " " + enLetras + " ");
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
