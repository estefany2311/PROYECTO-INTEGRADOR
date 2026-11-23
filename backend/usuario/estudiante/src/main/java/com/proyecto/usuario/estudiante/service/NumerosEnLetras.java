package com.proyecto.usuario.estudiante.service;

// T-421: 0 a 999 999 escritos con letras, sin tildes (el normalizador las quita igual)
final class NumerosEnLetras {

    private static final String[] UNIDADES = {"cero", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete",
            "ocho", "nueve", "diez", "once", "doce", "trece", "catorce", "quince", "dieciseis", "diecisiete",
            "dieciocho", "diecinueve", "veinte", "veintiuno", "veintidos", "veintitres", "veinticuatro",
            "veinticinco", "veintiseis", "veintisiete", "veintiocho", "veintinueve"};
    private static final String[] DECENAS = {"", "", "", "treinta", "cuarenta", "cincuenta", "sesenta", "setenta",
            "ochenta", "noventa"};
    private static final String[] CENTENAS = {"", "ciento", "doscientos", "trescientos", "cuatrocientos",
            "quinientos", "seiscientos", "setecientos", "ochocientos", "novecientos"};

    private NumerosEnLetras() {}

    static String escribir(int n) {
        if (n < 1000) return hastaMil(n);
        int miles = n / 1000;
        int resto = n % 1000;
        String primera = miles == 1 ? "mil" : hastaMil(miles).replaceAll("uno$", "un") + " mil";
        return resto == 0 ? primera : primera + " " + hastaMil(resto);
    }

    private static String hastaMil(int n) {
        if (n < 30) return UNIDADES[n];
        if (n < 100) return DECENAS[n / 10] + (n % 10 == 0 ? "" : " y " + UNIDADES[n % 10]);
        if (n == 100) return "cien";
        return CENTENAS[n / 100] + (n % 100 == 0 ? "" : " " + hastaMil(n % 100));
    }
}
