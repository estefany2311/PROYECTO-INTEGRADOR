package com.proyecto.usuario.estudiante.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

// T-328: diccionario español gratuito (LibreOffice es_ES, ver resources/ortografia/LICENCIA.txt), en minúsculas
@Component
public class DiccionarioEspanol {

    private static final String ARCHIVO = "ortografia/palabras_es.txt";

    private final Set<String> palabras;

    @Autowired
    public DiccionarioEspanol() {
        this(cargar());
    }

    // Para las pruebas: un diccionario pequeño
    public DiccionarioEspanol(Set<String> palabras) {
        this.palabras = palabras;
    }

    public boolean contiene(String palabra) {
        return palabras.contains(palabra.toLowerCase(Locale.ROOT));
    }

    private static Set<String> cargar() {
        Set<String> palabras = new HashSet<>(700_000);
        try (var entrada = new ClassPathResource(ARCHIVO).getInputStream();
             var lector = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                if (!linea.isBlank()) palabras.add(linea.strip());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el diccionario " + ARCHIVO, e);
        }
        return palabras;
    }
}
