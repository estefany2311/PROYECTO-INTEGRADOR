package com.proyecto.usuario.docente.config;

import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.seguridad.SesionDocenteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// login docente: el id del docente sale del token y nunca de lo que envíe el cliente
class FiltroSesionDocenteTest {

    @RestController
    static class RutasDePrueba {
        @GetMapping("/api/docente/aulas")
        String aulas(@RequestHeader("X-Docente-Id") Long docenteId) {
            return "docente " + docenteId;
        }

        @GetMapping({"/api/docente/grados", "/api/docente/avatares", "/api/estudiante/mapa"})
        String publica(@RequestHeader(value = "X-Docente-Id", required = false) Long docenteId) {
            return "docente " + docenteId;
        }
    }

    private final SesionDocenteService sesiones = mock(SesionDocenteService.class);
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new RutasDePrueba())
                .addFilters(new FiltroSesionDocente(sesiones,
                        new ConfiguracionCors("http://localhost:5173")))
                .build();
    }

    @Test
    void conTokenValidoElControladorRecibeElIdDeLaCuenta() throws Exception {
        when(sesiones.docenteDe("abc")).thenReturn(15L);

        mvc.perform(get("/api/docente/aulas").header("Authorization", "Bearer abc"))
                .andExpect(status().isOk())
                .andExpect(content().string("docente 15"));
    }

    @Test
    void elIdQueEnvieElClienteSeIgnora() throws Exception {
        when(sesiones.docenteDe("abc")).thenReturn(15L);

        mvc.perform(get("/api/docente/aulas")
                        .header("Authorization", "Bearer abc")
                        .header("X-Docente-Id", 1))
                .andExpect(content().string("docente 15"));
    }

    @Test
    void sinTokenNoSeEntraAunqueEnvieElIdDeUnDocente() throws Exception {
        mvc.perform(get("/api/docente/aulas").header("X-Docente-Id", 1))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Inicia sesión para continuar"));
        verifyNoInteractions(sesiones);
    }

    @Test
    void unaBarraDobleNoSaltaLaProteccion() throws Exception {
        mvc.perform(get("//api/docente/aulas").header("X-Docente-Id", 1))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void elErrorLlevaLasCabecerasCorsParaQueElPortalLoLea() throws Exception {
        when(sesiones.docenteDe("viejo")).thenThrow(new ReglaNegocioException(
                "Tu sesión terminó. Vuelve a iniciar sesión", HttpStatus.UNAUTHORIZED));

        mvc.perform(get("/api/docente/aulas")
                        .header("Authorization", "Bearer viejo")
                        .header("Origin", "http://localhost:5173"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void unOrigenDesconocidoNoRecibeCabeceraCors() throws Exception {
        mvc.perform(get("/api/docente/aulas").header("Origin", "http://otro.example"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void gradosYAvataresSonPublicosYNoAceptanElIdDelCliente() throws Exception {
        for (String ruta : new String[]{"/api/docente/grados", "/api/docente/avatares"}) {
            mvc.perform(get(ruta).header("X-Docente-Id", 1))
                    .andExpect(status().isOk())
                    .andExpect(content().string("docente null"));
        }
        verifyNoInteractions(sesiones);
    }

    @Test
    void lasRutasDeLaTabletNoPidenSesionDocente() throws Exception {
        mvc.perform(get("/api/estudiante/mapa")).andExpect(status().isOk());
        verifyNoInteractions(sesiones);
    }

    @Test
    void elPreflightDeCorsNoPideToken() throws Exception {
        mvc.perform(options("/api/docente/aulas"));

        verifyNoInteractions(sesiones);
    }
}
