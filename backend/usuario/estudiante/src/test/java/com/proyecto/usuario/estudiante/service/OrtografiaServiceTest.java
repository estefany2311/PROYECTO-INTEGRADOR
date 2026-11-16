package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.config.DiccionarioEspanol;
import com.proyecto.usuario.estudiante.dto.VerificarTextoResponse;
import com.proyecto.usuario.estudiante.model.ReglasOrtografia;
import com.proyecto.usuario.estudiante.repository.OrtografiaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// T-328/T-329: el servicio usa la configuración del grado del alumno y sus palabras permitidas
class OrtografiaServiceTest {

    private OrtografiaRepository repositorio;
    private OrtografiaService service;

    @BeforeEach
    void preparar() {
        repositorio = mock(OrtografiaRepository.class);
        var verificador = new VerificadorOrtografico(new DiccionarioEspanol(Set.of("el", "árbol", "es", "grande", "mi")));
        service = new OrtografiaService(repositorio, verificador);
        when(repositorio.palabrasPermitidas(1L)).thenReturn(Set.of());
    }

    @Test
    void usaLaConfiguracionDelGradoDelAlumno() {
        when(repositorio.reglasDe(1L)).thenReturn(Optional.of(new ReglasOrtografia("baja", true)));

        VerificarTextoResponse r = service.verificar(1L, "El arbol es grande.");

        assertTrue(r.errores().isEmpty());
        assertEquals("baja", r.sensibilidad());
        assertTrue(r.ignorarTildes());
    }

    @Test
    void sinConfiguracionUsaLaSensibilidadMedia() {
        when(repositorio.reglasDe(1L)).thenReturn(Optional.empty());

        VerificarTextoResponse r = service.verificar(1L, "El arbol es grande.");

        assertEquals("media", r.sensibilidad());
        assertFalse(r.ignorarTildes());
        assertEquals("árbol", r.errores().get(0).sugerencia());
    }

    @Test
    void lasPalabrasDelGradoNoSeMarcan() {
        when(repositorio.reglasDe(1L)).thenReturn(Optional.empty());
        when(repositorio.palabrasPermitidas(1L)).thenReturn(Set.of("cuy"));

        assertTrue(service.verificar(1L, "Mi cuy es grande.").errores().isEmpty());
    }
}
