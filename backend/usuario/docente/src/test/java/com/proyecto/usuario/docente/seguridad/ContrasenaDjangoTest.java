package com.proyecto.usuario.docente.seguridad;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// login docente: los dos formatos con los que Django guarda las contraseñas
class ContrasenaDjangoTest {

    // generado con hashlib de Python (mismo algoritmo que PBKDF2PasswordHasher)
    private static final String PBKDF2 =
            "pbkdf2_sha256$1000$salDePrueba$gzlFddWEi2SA7OPs3Eb6ll3vm2zvWeXSSgEfnREoUqM=";

    // Django escribe el prefijo $2b$; el algoritmo es el mismo que $2a$
    private static String bcryptDeDjango(String contrasena) {
        return "bcrypt$" + new BCryptPasswordEncoder(4).encode(contrasena).replace("$2a$", "$2b$");
    }

    @Test
    void reconoceUnaContrasenaBcryptDeDjango() {
        String guardada = bcryptDeDjango("clave-de-prueba");

        assertTrue(guardada.startsWith("bcrypt$$2b$"));
        assertTrue(ContrasenaDjango.coincide("clave-de-prueba", guardada));
        assertFalse(ContrasenaDjango.coincide("otra-clave", guardada));
    }

    @Test
    void reconoceUnaContrasenaPbkdf2DeDjango() {
        assertTrue(ContrasenaDjango.coincide("clave-de-prueba", PBKDF2));
        assertFalse(ContrasenaDjango.coincide("otra-clave", PBKDF2));
    }

    @Test
    void rechazaContrasenasDemasiadoLargasParaBcrypt() {
        String guardada = bcryptDeDjango("a".repeat(72));

        assertFalse(ContrasenaDjango.coincide("a".repeat(73), guardada));
    }

    @Test
    void rechazaFormatosDesconocidosOIncompletos() {
        for (String guardada : new String[]{null, "", "clave-de-prueba", "!sinContrasena",
                "md5$sal$abc", "pbkdf2_sha256$abc$sal$xx", "pbkdf2_sha256$0$sal$eHg=",
                "pbkdf2_sha256$1000$sal", "bcrypt$no-es-un-hash"}) {
            assertFalse(ContrasenaDjango.coincide("clave-de-prueba", guardada), String.valueOf(guardada));
        }
    }
}
