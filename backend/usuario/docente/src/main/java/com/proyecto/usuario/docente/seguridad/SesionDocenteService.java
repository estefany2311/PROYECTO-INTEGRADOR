package com.proyecto.usuario.docente.seguridad;

import com.proyecto.usuario.docente.dto.SesionDocenteResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Login docente con las cuentas del módulo Admin.
 *
 * <p>Solo entran cuentas activas con rol DOCENTE. Cada inicio de sesión entrega
 * un token de acceso ({@link #VIGENCIA_ACCESO}) y uno de renovación
 * ({@link #VIGENCIA_REFRESH}). Son valores aleatorios: en la base solo se guarda
 * su huella, no hace falta ninguna clave secreta y cerrar sesión los anula.</p>
 */
@Service
public class SesionDocenteService {

    static final String ROL_DOCENTE = "DOCENTE";
    static final Duration VIGENCIA_ACCESO = Duration.ofHours(1);
    static final Duration VIGENCIA_REFRESH = Duration.ofDays(1);

    // se compara contra esto cuando el usuario no existe, para tardar lo mismo
    // (Django cifra con BCrypt de 12 rondas)
    private static final String CONTRASENA_SENUELO =
            "bcrypt$" + new BCryptPasswordEncoder(12).encode("usuario-que-no-existe");

    private final CuentaRepository cuentas;
    private final SesionDocenteRepository sesiones;
    private final FrenoIntentos freno;
    private final Clock reloj;
    private final SecureRandom aleatorio = new SecureRandom();

    @Autowired
    public SesionDocenteService(CuentaRepository cuentas, SesionDocenteRepository sesiones,
                                FrenoIntentos freno) {
        this(cuentas, sesiones, freno, Clock.systemDefaultZone());
    }

    SesionDocenteService(CuentaRepository cuentas, SesionDocenteRepository sesiones,
                         FrenoIntentos freno, Clock reloj) {
        this.cuentas = cuentas;
        this.sesiones = sesiones;
        this.freno = freno;
        this.reloj = reloj;
    }

    @Transactional
    public SesionDocenteResponse iniciar(String usuario, String contrasena) {
        String nombreUsuario = usuario.trim();
        if (freno.estaBloqueado(nombreUsuario)) {
            throw new ReglaNegocioException(
                    "Demasiados intentos. Espera unos minutos y vuelve a probar",
                    HttpStatus.TOO_MANY_REQUESTS);
        }

        Optional<CuentaDocente> encontrada = cuentas.porUsuario(nombreUsuario);
        boolean correcta = ContrasenaDjango.coincide(contrasena,
                encontrada.map(CuentaDocente::contrasenaCifrada).orElse(CONTRASENA_SENUELO));
        if (encontrada.isEmpty() || !correcta) {
            freno.registrarFallo(nombreUsuario);
            throw new ReglaNegocioException(
                    "Usuario o contraseña incorrectos", HttpStatus.UNAUTHORIZED);
        }
        freno.limpiar(nombreUsuario);

        // estos avisos solo los ve quien sabe la contraseña
        CuentaDocente cuenta = encontrada.get();
        if (!cuenta.activa()) {
            throw new ReglaNegocioException(
                    "Tu cuenta está desactivada. Contacta al administrador",
                    HttpStatus.UNAUTHORIZED);
        }
        if (!ROL_DOCENTE.equals(cuenta.rol())) {
            throw new ReglaNegocioException("Esta cuenta no es de docente", HttpStatus.FORBIDDEN);
        }

        LocalDateTime ahora = LocalDateTime.now(reloj);
        String acceso = nuevoToken();
        String refresh = nuevoToken();
        sesiones.eliminarVencidas(ahora);
        sesiones.crear(cuenta.id(), huellaDe(acceso), ahora.plus(VIGENCIA_ACCESO),
                huellaDe(refresh), ahora.plus(VIGENCIA_REFRESH));

        return new SesionDocenteResponse(acceso, refresh, new SesionDocenteResponse.Docente(
                cuenta.id(), cuenta.usuario(), cuenta.nombreCompleto()));
    }

    /** @return el id del docente dueño del token de acceso; 401 si no sirve */
    public Long docenteDe(String acceso) {
        return sesiones.docentePorAcceso(huellaDe(acceso), LocalDateTime.now(reloj))
                .orElseThrow(SesionDocenteService::sinSesion);
    }

    /** Nuevo token de acceso. El de renovación no se alarga: al día hay que volver a entrar. */
    @Transactional
    public String renovar(String refresh) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        String acceso = nuevoToken();
        if (!sesiones.renovarAcceso(huellaDe(refresh), huellaDe(acceso),
                ahora.plus(VIGENCIA_ACCESO), ahora)) {
            throw sinSesion();
        }
        return acceso;
    }

    @Transactional
    public void cerrar(String refresh) {
        sesiones.cerrar(huellaDe(refresh));
    }

    static ReglaNegocioException sinSesion() {
        return new ReglaNegocioException(
                "Tu sesión terminó. Vuelve a iniciar sesión", HttpStatus.UNAUTHORIZED);
    }

    private String nuevoToken() {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String huellaDe(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
