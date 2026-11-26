package com.proyecto.usuario.docente.seguridad;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Frena la adivinación de contraseñas: tras {@value #MAXIMO_FALLOS} fallos
 * seguidos, ese usuario espera {@link #BLOQUEO}. Se guarda en memoria, así que
 * se reinicia al reiniciar el backend.
 */
@Component
public class FrenoIntentos {

    static final int MAXIMO_FALLOS = 5;
    static final Duration BLOQUEO = Duration.ofMinutes(5);
    private static final int MAXIMO_USUARIOS = 1000;

    private record Intentos(int fallos, Instant ultimo) {
    }

    private final Clock reloj;
    private final Map<String, Intentos> intentos = new ConcurrentHashMap<>();

    @Autowired
    public FrenoIntentos() {
        this(Clock.systemUTC());
    }

    FrenoIntentos(Clock reloj) {
        this.reloj = reloj;
    }

    public boolean estaBloqueado(String usuario) {
        Intentos registro = intentos.get(clave(usuario));
        if (registro == null) {
            return false;
        }
        if (!reloj.instant().isBefore(registro.ultimo().plus(BLOQUEO))) {
            intentos.remove(clave(usuario));
            return false;
        }
        return registro.fallos() >= MAXIMO_FALLOS;
    }

    public void registrarFallo(String usuario) {
        if (intentos.size() >= MAXIMO_USUARIOS) {
            Instant limite = reloj.instant().minus(BLOQUEO);
            intentos.values().removeIf(i -> i.ultimo().isBefore(limite));
        }
        intentos.merge(clave(usuario), new Intentos(1, reloj.instant()),
                (antes, nuevo) -> new Intentos(antes.fallos() + 1, nuevo.ultimo()));
    }

    public void limpiar(String usuario) {
        intentos.remove(clave(usuario));
    }

    private static String clave(String usuario) {
        return usuario.toLowerCase(Locale.ROOT);
    }
}
