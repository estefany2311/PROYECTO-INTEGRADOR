package com.proyecto.usuario.docente.seguridad;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Compara una contraseña con la que guarda Django en la tabla {@code usuarios}.
 * Admite los dos formatos de la configuración de Admin
 * ({@code PASSWORD_HASHERS}):
 *
 * <ul>
 *   <li>{@code bcrypt$$2b$12$...} (BCryptPasswordHasher);</li>
 *   <li>{@code pbkdf2_sha256$iteraciones$sal$resumen} (cuentas anteriores).</li>
 * </ul>
 *
 * Cualquier otro formato se rechaza.
 */
public final class ContrasenaDjango {

    private static final String BCRYPT = "bcrypt$";
    private static final String PBKDF2 = "pbkdf2_sha256$";
    // BCrypt solo usa los primeros 72 bytes; Django rechaza las más largas
    private static final int MAXIMO_BYTES_BCRYPT = 72;
    private static final int MAXIMO_ITERACIONES = 10_000_000;
    private static final BCryptPasswordEncoder CODIFICADOR = new BCryptPasswordEncoder();

    private ContrasenaDjango() {
    }

    public static boolean coincide(String contrasena, String guardada) {
        if (contrasena == null || guardada == null) {
            return false;
        }
        if (guardada.startsWith(BCRYPT)) {
            return contrasena.getBytes(StandardCharsets.UTF_8).length <= MAXIMO_BYTES_BCRYPT
                    && CODIFICADOR.matches(contrasena, guardada.substring(BCRYPT.length()));
        }
        if (guardada.startsWith(PBKDF2)) {
            return coincidePbkdf2(contrasena, guardada.split("\\$"));
        }
        return false;
    }

    private static boolean coincidePbkdf2(String contrasena, String[] partes) {
        if (partes.length != 4) {
            return false;
        }
        try {
            int iteraciones = Integer.parseInt(partes[1]);
            if (iteraciones <= 0 || iteraciones > MAXIMO_ITERACIONES) {
                return false;
            }
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            PBEKeySpec spec = new PBEKeySpec(contrasena.toCharArray(),
                    partes[2].getBytes(StandardCharsets.UTF_8), iteraciones, esperado.length * 8);
            byte[] calculado = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
            return MessageDigest.isEqual(esperado, calculado);
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            return false;
        }
    }
}
