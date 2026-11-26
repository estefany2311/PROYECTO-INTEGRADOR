package com.proyecto.usuario.docente.config;

import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.seguridad.SesionDocenteService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

/**
 * Login docente: protege las rutas {@code /api/docente/**}.
 *
 * <p>El header {@code X-Docente-Id} que envíe el cliente <b>se descarta
 * siempre</b>. Los controladores solo lo reciben cuando este filtro comprobó el
 * token ({@code Authorization: Bearer ...}) y lo escribe con el id de la cuenta.
 * Así nadie puede hacerse pasar por otro docente, y una ruta que el filtro no
 * reconociera se quedaría sin id en lugar de aceptar el del cliente.</p>
 *
 * <p>Los catálogos de grados y avatares siguen siendo públicos. Las rutas de la
 * tablet ({@code /api/estudiante/**}) no cambian: las protege
 * {@link InterceptorTokenEstudiante}.</p>
 */
@Component
public class FiltroSesionDocente extends OncePerRequestFilter {

    static final String CABECERA_DOCENTE = "X-Docente-Id";
    private static final String PREFIJO_TOKEN = "Bearer ";
    private static final String RUTAS_DOCENTE = "/api/docente/";
    private static final Set<String> PUBLICAS = Set.of("/api/docente/grados", "/api/docente/avatares");

    private final SesionDocenteService sesiones;
    private final List<String> origenesPermitidos;

    public FiltroSesionDocente(SesionDocenteService sesiones, ConfiguracionCors cors) {
        this.sesiones = sesiones;
        this.origenesPermitidos = Arrays.asList(cors.origenes());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain cadena) throws ServletException, IOException {
        Long docenteId = null;
        if (requiereSesion(request)) {
            try {
                docenteId = sesiones.docenteDe(tokenDe(request));
            } catch (ReglaNegocioException e) {
                responderError(request, response, e);
                return;
            }
        }
        cadena.doFilter(new PeticionConDocente(request, docenteId), response);
    }

    private static boolean requiereSesion(HttpServletRequest request) {
        // las peticiones OPTIONS (CORS) no llevan token
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        // ruta decodificada, igual que la que usa Spring para elegir el controlador
        String ruta = new UrlPathHelper().getPathWithinApplication(request).replaceAll("/{2,}", "/");
        String sinBarraFinal = ruta.endsWith("/") ? ruta.substring(0, ruta.length() - 1) : ruta;
        return ruta.startsWith(RUTAS_DOCENTE) && !PUBLICAS.contains(sinBarraFinal);
    }

    private static String tokenDe(HttpServletRequest request) {
        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecera == null || !cabecera.startsWith(PREFIJO_TOKEN)
                || cabecera.substring(PREFIJO_TOKEN.length()).isBlank()) {
            throw new ReglaNegocioException("Inicia sesión para continuar",
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
        return cabecera.substring(PREFIJO_TOKEN.length()).trim();
    }

    // el error sale antes de llegar a Spring MVC, así que las cabeceras CORS se ponen aquí
    private void responderError(HttpServletRequest request, HttpServletResponse response,
                                ReglaNegocioException error) throws IOException {
        String origen = request.getHeader(HttpHeaders.ORIGIN);
        if (origen != null && origenesPermitidos.contains(origen)) {
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origen);
            response.setHeader(HttpHeaders.VARY, HttpHeaders.ORIGIN);
        }
        response.setStatus(error.getEstado().value());
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // los mensajes son textos fijos del backend, sin comillas
        response.getWriter().write("{\"mensaje\":\"" + error.getMessage() + "\"}");
    }

    // la petición que ven los controladores: X-Docente-Id solo existe si se comprobó el token
    private static final class PeticionConDocente extends HttpServletRequestWrapper {

        private final String docenteId;

        private PeticionConDocente(HttpServletRequest request, Long docenteId) {
            super(request);
            this.docenteId = docenteId == null ? null : docenteId.toString();
        }

        @Override
        public String getHeader(String nombre) {
            return CABECERA_DOCENTE.equalsIgnoreCase(nombre) ? docenteId : super.getHeader(nombre);
        }

        @Override
        public Enumeration<String> getHeaders(String nombre) {
            if (!CABECERA_DOCENTE.equalsIgnoreCase(nombre)) {
                return super.getHeaders(nombre);
            }
            return docenteId == null
                    ? Collections.emptyEnumeration()
                    : Collections.enumeration(List.of(docenteId));
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> nombres = new ArrayList<>(Collections.list(super.getHeaderNames()));
            nombres.removeIf(CABECERA_DOCENTE::equalsIgnoreCase);
            if (docenteId != null) {
                nombres.add(CABECERA_DOCENTE);
            }
            return Collections.enumeration(nombres);
        }

        @Override
        public int getIntHeader(String nombre) {
            if (!CABECERA_DOCENTE.equalsIgnoreCase(nombre)) {
                return super.getIntHeader(nombre);
            }
            return docenteId == null ? -1 : Integer.parseInt(docenteId);
        }
    }
}
