package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.LecturaResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Aula;
import com.proyecto.usuario.docente.model.Grado;
import com.proyecto.usuario.docente.model.Lectura;
import com.proyecto.usuario.docente.repository.AulaRepository;
import com.proyecto.usuario.docente.repository.LecturaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// T-189: solo lecturas activas de los grados del aula + filtros de la pantalla
@ExtendWith(MockitoExtension.class)
class CatalogoServiceTest {

    @Mock AulaRepository aulaRepository;
    @Mock LecturaRepository lecturaRepository;

    private CatalogoService service;
    private Lectura basica1;
    private Lectura intermedia2;

    @BeforeEach
    void preparar() {
        service = new CatalogoService(aulaRepository, lecturaRepository);

        // aula 10 del docente 1, atiende 1.º y 2.º
        Aula aula = new Aula();
        aula.setNombre("Aula Multigrado");
        aula.setDocenteId(1L);
        aula.setGrados(Set.of(new Grado((short) 1, "1.º", (short) 1),
                new Grado((short) 2, "2.º", (short) 2)));

        basica1 = lectura(10L, "El cóndor y el zorro", "basica", "lectura", (short) 1);
        intermedia2 = lectura(11L, "La llama viajera", "intermedia", "escritura", (short) 2);

        lenient().when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        lenient().when(lecturaRepository.findByActivoTrueAndGradoInOrderByGradoAscTituloAsc(any()))
                .thenReturn(List.of(basica1, intermedia2));
    }

    private Lectura lectura(Long id, String titulo, String dificultad, String tipoReto, Short grado) {
        Lectura l = new Lectura();
        ReflectionTestUtils.setField(l, "id", id);
        ReflectionTestUtils.setField(l, "titulo", titulo);
        ReflectionTestUtils.setField(l, "texto", "Había una vez...");
        ReflectionTestUtils.setField(l, "dificultad", dificultad);
        ReflectionTestUtils.setField(l, "tipoReto", tipoReto);
        ReflectionTestUtils.setField(l, "grado", grado);
        ReflectionTestUtils.setField(l, "activo", true);
        return l;
    }

    @Test
    void pideSoloLasLecturasActivasDeLosGradosDelAula() {
        List<LecturaResponse> catalogo = service.listar(1L, 10L, null, null, null);

        assertEquals(2, catalogo.size());
        // la consulta recibe exactamente los grados del aula
        verify(lecturaRepository)
                .findByActivoTrueAndGradoInOrderByGradoAscTituloAsc(Set.of((short) 1, (short) 2));
        assertEquals("1.º", catalogo.get(0).gradoNombre());
        assertEquals("basica", catalogo.get(0).dificultad());
    }

    @Test
    void filtraPorGrado() {
        List<LecturaResponse> catalogo = service.listar(1L, 10L, (short) 2, null, null);

        assertEquals(1, catalogo.size());
        assertEquals(11L, catalogo.get(0).id());
        assertEquals("2.º", catalogo.get(0).gradoNombre());
    }

    @Test
    void filtraPorDificultadSinImportarMayusculas() {
        List<LecturaResponse> catalogo = service.listar(1L, 10L, null, "BASICA", null);

        assertEquals(1, catalogo.size());
        assertEquals(10L, catalogo.get(0).id());
    }

    @Test
    void filtraPorTipoDeReto() {
        List<LecturaResponse> catalogo = service.listar(1L, 10L, null, null, "escritura");

        assertEquals(1, catalogo.size());
        assertEquals("La llama viajera", catalogo.get(0).titulo());
    }

    @Test
    void aulaDeOtroDocente_lanza403SinConsultarElCatalogo() {
        ReflectionTestUtils.setField(aulaRepository.findById(10L).get(), "docenteId", 99L);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.listar(1L, 10L, null, null, null));

        assertEquals(HttpStatus.FORBIDDEN, ex.getEstado());
        verifyNoInteractions(lecturaRepository);
    }

    @Test
    void aulaInexistente_lanza404() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.listar(1L, 99L, null, null, null));

        assertEquals(HttpStatus.NOT_FOUND, ex.getEstado());
        verifyNoInteractions(lecturaRepository);
    }

    // T-230: un aula sin grados no tiene catálogo posible
    @Test
    void aulaSinGrados_devuelveVacioSinConsultarElCatalogo() {
        Aula sinGrados = new Aula();
        sinGrados.setNombre("Aula sin grados");
        sinGrados.setDocenteId(1L);
        sinGrados.setGrados(Set.of());
        when(aulaRepository.findById(20L)).thenReturn(Optional.of(sinGrados));

        List<LecturaResponse> catalogo = service.listar(1L, 20L, null, null, null);

        assertTrue(catalogo.isEmpty());
        verifyNoInteractions(lecturaRepository);
    }

    // T-230: pedir un grado que el aula no atiende no inventa lecturas
    @Test
    void gradoQueNoAtiendeElAula_devuelveVacio() {
        List<LecturaResponse> catalogo = service.listar(1L, 10L, (short) 5, null, null);

        assertTrue(catalogo.isEmpty());
    }

    // T-230: los filtros vacíos o en blanco se comportan como "sin filtro"
    @Test
    void filtrosEnBlancoNoFiltran() {
        List<LecturaResponse> catalogo = service.listar(1L, 10L, null, "  ", "");

        assertEquals(2, catalogo.size());
    }

    @Test
    void combinaGradoDificultadYTipoDeReto() {
        List<LecturaResponse> catalogo = service.listar(1L, 10L, (short) 2, "intermedia", "escritura");

        assertEquals(1, catalogo.size());
        assertEquals("La llama viajera", catalogo.get(0).titulo());
    }

    @Test
    void tipoDeRetoIgnoraMayusculas() {
        List<LecturaResponse> catalogo = service.listar(1L, 10L, null, null, "ESCRITURA");

        assertEquals(1, catalogo.size());
        assertEquals("La llama viajera", catalogo.get(0).titulo());
    }
}
