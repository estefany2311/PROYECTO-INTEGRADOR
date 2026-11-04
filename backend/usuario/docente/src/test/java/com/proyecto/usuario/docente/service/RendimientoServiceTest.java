package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.DashboardAulaResponse;
import com.proyecto.usuario.docente.model.Aula;
import com.proyecto.usuario.docente.model.ColorSemaforo;
import com.proyecto.usuario.docente.model.Estudiante;
import com.proyecto.usuario.docente.model.Grado;
import com.proyecto.usuario.docente.repository.AulaRepository;
import com.proyecto.usuario.docente.repository.EstudianteRepository;
import com.proyecto.usuario.docente.repository.EvaluacionRepository;
import com.proyecto.usuario.docente.repository.PromedioCompetenciaProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T-282 (US-30): el filtro soloRojo limita las tarjetas, no el resumen del aula.
 */
@ExtendWith(MockitoExtension.class)
class RendimientoServiceTest {

    @Mock
    EvaluacionRepository evaluacionRepository;

    @Mock
    AulaRepository aulaRepository;

    @Mock
    EstudianteRepository estudianteRepository;

    RendimientoService service;
    Grado grado;

    @BeforeEach
    void preparar() {
        service = new RendimientoService(
                evaluacionRepository, aulaRepository, estudianteRepository, new SemaforoService());
        grado = new Grado((short) 5, "5.º", (short) 5);
    }

    @Test
    void soloRojoFiltraLasTarjetasPeroMantieneElResumenDelAula() {
        Aula aula = mock(Aula.class);
        when(aula.getId()).thenReturn(10L);
        when(aula.getNombre()).thenReturn("Aula Rural");
        when(aula.getDocenteId()).thenReturn(1L);
        when(aula.getGrados()).thenReturn(Set.of(grado));
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));

        Estudiante minji = estudiante(7L, "Minji", "Prueba");
        Estudiante breysi = estudiante(8L, "Breysi", "Prueba");
        Estudiante ester = estudiante(9L, "Ester", "Prueba");
        when(estudianteRepository.findByAulaIdAndActivoTrueOrderByApellidosAscNombresAsc(10L))
                .thenReturn(List.of(minji, breysi, ester));

        PromedioCompetenciaProjection promedioMinji = promedio(7L, "lectura", 85.0);
        PromedioCompetenciaProjection promedioBreysi = promedio(8L, "lectura", 68.0);
        PromedioCompetenciaProjection promedioEster = promedio(9L, "lectura", 50.0);
        when(evaluacionRepository.promediosPorAula(10L)).thenReturn(
                List.of(promedioMinji, promedioBreysi, promedioEster));

        DashboardAulaResponse completo = service.dashboardAula(1L, 10L, false);
        DashboardAulaResponse soloRojo = service.dashboardAula(1L, 10L, true);

        assertEquals(3, completo.estudiantes().size());
        assertEquals(1, soloRojo.estudiantes().size());
        assertEquals("Ester", soloRojo.estudiantes().get(0).nombres());
        assertEquals(ColorSemaforo.ROJO, soloRojo.estudiantes().get(0).color());
        assertEquals(3, soloRojo.resumen().totalEstudiantes());
        assertEquals(1, soloRojo.resumen().necesitanRefuerzo());
        assertEquals(68, soloRojo.promedioGeneral().intValue());
    }

    private Estudiante estudiante(Long id, String nombres, String apellidos) {
        Estudiante estudiante = mock(Estudiante.class);
        when(estudiante.getId()).thenReturn(id);
        when(estudiante.getNombres()).thenReturn(nombres);
        when(estudiante.getApellidos()).thenReturn(apellidos);
        when(estudiante.getGrado()).thenReturn(grado);
        return estudiante;
    }

    private PromedioCompetenciaProjection promedio(Long estudianteId, String competencia, Double valor) {
        PromedioCompetenciaProjection promedio = mock(PromedioCompetenciaProjection.class);
        when(promedio.getEstudianteId()).thenReturn(estudianteId);
        when(promedio.getCompetencia()).thenReturn(competencia);
        when(promedio.getPromedio()).thenReturn(valor);
        return promedio;
    }
}
