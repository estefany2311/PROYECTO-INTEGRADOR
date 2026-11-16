package com.proyecto.usuario.docente.config;

import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// T-197: sin token válido la tablet no ve el mapa de nadie
class InterceptorTokenEstudianteTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final InterceptorTokenEstudiante interceptor = new InterceptorTokenEstudiante(jdbc);

    private MockHttpServletRequest peticion(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/estudiante/mapa");
        if (authorization != null) request.addHeader("Authorization", authorization);
        return request;
    }

    @Test
    void tokenValidoDejaElIdDelAlumnoEnLaPeticion() {
        when(jdbc.queryForList(anyString(), eq(Long.class), eq(InterceptorTokenEstudiante.sha256("abc")), any()))
                .thenReturn(List.of(7L));
        MockHttpServletRequest request = peticion("Bearer abc");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertEquals(7L, request.getAttribute(InterceptorTokenEstudiante.ATRIBUTO_ESTUDIANTE));
    }

    @Test
    void sinTokenResponde401() {
        ReglaNegocioException e = assertThrows(ReglaNegocioException.class,
                () -> interceptor.preHandle(peticion(null), new MockHttpServletResponse(), new Object()));
        assertEquals(HttpStatus.UNAUTHORIZED, e.getEstado());
    }

    // Token vencido o borrado con "Salir" en la tablet
    @Test
    void tokenQueNoExisteResponde401() {
        when(jdbc.queryForList(anyString(), eq(Long.class), any(), any())).thenReturn(List.of());

        assertThrows(ReglaNegocioException.class,
                () -> interceptor.preHandle(peticion("Bearer viejo"), new MockHttpServletResponse(), new Object()));
    }

    @Test
    void otrasRutasNoPidenToken() {
        // solo /api/estudiante/mapa está protegida: el resto del portal docente no cambia
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/estudiante/mapa");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        verifyNoInteractions(jdbc);
    }
}
