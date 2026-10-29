package com.proyecto.usuario.docente.config;

import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

/**
 * T-197: valida el token que la tablet obtuvo al entrar con su PIN (backend estudiante, T-202).
 * La sesión vive en la tabla sesion_estudiante de la base compartida; aquí solo se lee.
 * Si es válida, deja el id del alumno en la petición: el id nunca viene de la URL.
 */
@Component
public class InterceptorTokenEstudiante implements HandlerInterceptor {

    public static final String ATRIBUTO_ESTUDIANTE = "estudianteId";
    private static final String PREFIJO = "Bearer ";

    private final JdbcTemplate jdbc;

    public InterceptorTokenEstudiante(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // las peticiones OPTIONS (CORS) no llevan token
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String cabecera = request.getHeader("Authorization");
        if (cabecera == null || !cabecera.startsWith(PREFIJO) || cabecera.length() == PREFIJO.length()) {
            throw sinSesion();
        }

        String hash = sha256(cabecera.substring(PREFIJO.length()).trim());
        // LocalDateTime.now() (hora de Lima del servidor), NO now() de PostgreSQL (UTC)
        List<Long> ids = jdbc.queryForList(
                "SELECT estudiante_id FROM sesion_estudiante WHERE token_hash = ? AND expira_en > ?",
                Long.class, hash, LocalDateTime.now());
        if (ids.isEmpty()) {
            throw sinSesion();
        }

        request.setAttribute(ATRIBUTO_ESTUDIANTE, ids.get(0));
        return true;
    }

    private static ReglaNegocioException sinSesion() {
        return new ReglaNegocioException("Tu sesión terminó. Vuelve a escribir tu PIN", HttpStatus.UNAUTHORIZED);
    }

    // Mismo cálculo que el backend estudiante (GeneradorToken.hash): SHA-256 en hexadecimal
    static String sha256(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
