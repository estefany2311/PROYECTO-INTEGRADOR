package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.DashboardAulaResponse;
import com.proyecto.usuario.docente.dto.ResumenDashboardResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
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
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T-282 (US-30): el filtro soloRojo limita las tarjetas, no el resumen del aula.
 * T-289 (US-28): combinaciones de grado, competencia y soloRojo.
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
    Grado gradoUno;

    @BeforeEach
    void preparar() {
        service = new RendimientoService(
                evaluacionRepository, aulaRepository, estudianteRepository, new SemaforoService());
        grado = new Grado((short) 5, "5.º", (short) 5);
        gradoUno = new Grado((short) 1, "1.º", (short) 1);
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

    @Test
    void filtraPorGradoYMantieneLosGradosDelAulaParaElSelector() {
        Aula aula = mock(Aula.class);
        when(aula.getId()).thenReturn(10L);
        when(aula.getNombre()).thenReturn("Aula Rural");
        when(aula.getDocenteId()).thenReturn(1L);
        when(aula.getGrados()).thenReturn(Set.of(grado, gradoUno));
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));

        Estudiante minji = mock(Estudiante.class);
        when(minji.getGrado()).thenReturn(grado);
        Estudiante ester = estudiante(9L, "Ester", "Prueba", gradoUno);
        when(estudianteRepository.findByAulaIdAndActivoTrueOrderByApellidosAscNombresAsc(10L))
                .thenReturn(List.of(minji, ester));
        PromedioCompetenciaProjection promedioMinji = promedio(7L, "lectura", 85.0);
        PromedioCompetenciaProjection promedioEster = promedio(9L, "lectura", 50.0);
        when(evaluacionRepository.promediosPorAula(10L))
                .thenReturn(List.of(promedioMinji, promedioEster));

        DashboardAulaResponse filtrado = service.dashboardAula(
                1L, 10L, (short) 1, null, false);

        assertEquals(1, filtrado.estudiantes().size());
        assertEquals("Ester", filtrado.estudiantes().get(0).nombres());
        assertEquals((short) 1, filtrado.estudiantes().get(0).gradoId());
        assertEquals(1, filtrado.resumen().totalEstudiantes());
        assertEquals(1, filtrado.resumen().necesitanRefuerzo());
        assertEquals(50, filtrado.promedioGeneral().intValue());
        assertEquals(2, filtrado.grados().size());
        assertEquals(1, filtrado.promediosPorCompetencia().size());
        assertEquals("lectura", filtrado.promediosPorCompetencia().get(0).competencia());
        assertEquals(50.0, filtrado.promediosPorCompetencia().get(0).promedio(), 0.001);
    }

    @Test
    void filtraPorCompetenciaYRecalculaPromedioYColor() {
        Aula aula = mock(Aula.class);
        when(aula.getId()).thenReturn(10L);
        when(aula.getNombre()).thenReturn("Aula Rural");
        when(aula.getDocenteId()).thenReturn(1L);
        when(aula.getGrados()).thenReturn(Set.of(grado));
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));

        Estudiante minji = estudiante(7L, "Minji", "Prueba");
        when(estudianteRepository.findByAulaIdAndActivoTrueOrderByApellidosAscNombresAsc(10L))
                .thenReturn(List.of(minji));
        PromedioCompetenciaProjection promedioLectura = promedio(7L, "lectura", 50.0);
        PromedioCompetenciaProjection promedioEscritura = promedio(7L, "escritura", 90.0);
        when(evaluacionRepository.promediosPorAula(10L))
                .thenReturn(List.of(promedioLectura, promedioEscritura));

        DashboardAulaResponse filtrado = service.dashboardAula(
                1L, 10L, null, "lectura", false);

        assertEquals(1, filtrado.estudiantes().size());
        assertEquals(50, filtrado.estudiantes().get(0).promedio());
        assertEquals(ColorSemaforo.ROJO, filtrado.estudiantes().get(0).color());
        assertEquals(1, filtrado.estudiantes().get(0).competencias().size());
        assertEquals("lectura",
                filtrado.estudiantes().get(0).competencias().get(0).competencia());
        assertEquals(1, filtrado.promediosPorCompetencia().size());
        assertEquals("lectura", filtrado.promediosPorCompetencia().get(0).competencia());
        assertEquals(50, filtrado.promedioGeneral().intValue());
        assertEquals(ColorSemaforo.ROJO, filtrado.colorGeneral());
    }

    @Test
    void combinaGradoYCompetenciaParaFiltrarAlumnosYRecalcularPromedios() {
        Aula aula = mock(Aula.class);
        when(aula.getId()).thenReturn(10L);
        when(aula.getNombre()).thenReturn("Aula Rural");
        when(aula.getDocenteId()).thenReturn(1L);
        when(aula.getGrados()).thenReturn(Set.of(grado, gradoUno));
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));

        Estudiante minji = mock(Estudiante.class);
        when(minji.getGrado()).thenReturn(grado);
        Estudiante ester = estudiante(9L, "Ester", "Prueba", gradoUno);
        when(estudianteRepository.findByAulaIdAndActivoTrueOrderByApellidosAscNombresAsc(10L))
                .thenReturn(List.of(minji, ester));

        PromedioCompetenciaProjection promedioMinjiLectura = promedio(7L, "lectura", 85.0);
        PromedioCompetenciaProjection promedioMinjiEscritura = promedio(7L, "escritura", 95.0);
        PromedioCompetenciaProjection promedioEsterLectura = promedio(9L, "lectura", 50.0);
        PromedioCompetenciaProjection promedioEsterEscritura = promedio(9L, "escritura", 90.0);
        when(evaluacionRepository.promediosPorAula(10L)).thenReturn(List.of(
                promedioMinjiLectura,
                promedioMinjiEscritura,
                promedioEsterLectura,
                promedioEsterEscritura));

        DashboardAulaResponse filtrado = service.dashboardAula(
                1L, 10L, (short) 1, "lectura", false);

        assertEquals(1, filtrado.estudiantes().size());
        assertEquals("Ester", filtrado.estudiantes().get(0).nombres());
        assertEquals((short) 1, filtrado.estudiantes().get(0).gradoId());
        assertEquals(50, filtrado.estudiantes().get(0).promedio());
        assertEquals(ColorSemaforo.ROJO, filtrado.estudiantes().get(0).color());
        assertEquals(1, filtrado.estudiantes().get(0).competencias().size());
        assertEquals("lectura",
                filtrado.estudiantes().get(0).competencias().get(0).competencia());
        assertEquals(new ResumenDashboardResponse(1, 0, 0, 1, 0), filtrado.resumen());
        assertEquals(1, filtrado.promediosPorCompetencia().size());
        assertEquals("lectura", filtrado.promediosPorCompetencia().get(0).competencia());
        assertEquals(50.0, filtrado.promediosPorCompetencia().get(0).promedio(), 0.001);
        assertEquals(50, filtrado.promedioGeneral().intValue());
        assertEquals(ColorSemaforo.ROJO, filtrado.colorGeneral());
        assertEquals(2, filtrado.grados().size());
        assertEquals("1.º", filtrado.grados().get(0).nombre());
    }

    @Test
    void combinaGradoCompetenciaYSoloRojoSinAlterarElResumen() {
        Aula aula = mock(Aula.class);
        when(aula.getId()).thenReturn(10L);
        when(aula.getNombre()).thenReturn("Aula Rural");
        when(aula.getDocenteId()).thenReturn(1L);
        when(aula.getGrados()).thenReturn(Set.of(grado, gradoUno));
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));

        Estudiante minji = mock(Estudiante.class);
        when(minji.getGrado()).thenReturn(grado);
        Estudiante ester = estudiante(9L, "Ester", "Prueba", gradoUno);
        Estudiante ana = estudiante(10L, "Ana", "Prueba", gradoUno);
        Estudiante luis = estudiante(11L, "Luis", "Prueba", gradoUno);
        when(estudianteRepository.findByAulaIdAndActivoTrueOrderByApellidosAscNombresAsc(10L))
                .thenReturn(List.of(minji, ester, ana, luis));

        PromedioCompetenciaProjection promedioMinji = promedio(7L, "lectura", 40.0);
        PromedioCompetenciaProjection promedioEster = promedio(9L, "lectura", 50.0);
        PromedioCompetenciaProjection promedioAna = promedio(10L, "lectura", 65.0);
        PromedioCompetenciaProjection promedioLuis = promedio(11L, "lectura", 90.0);
        when(evaluacionRepository.promediosPorAula(10L)).thenReturn(List.of(
                promedioMinji,
                promedioEster,
                promedioAna,
                promedioLuis));

        DashboardAulaResponse filtrado = service.dashboardAula(
                1L, 10L, (short) 1, "lectura", true);

        assertEquals(1, filtrado.estudiantes().size());
        assertEquals("Ester", filtrado.estudiantes().get(0).nombres());
        assertEquals(ColorSemaforo.ROJO, filtrado.estudiantes().get(0).color());
        assertEquals(3, filtrado.resumen().totalEstudiantes());
        assertEquals(1, filtrado.resumen().enNivelEsperado());
        assertEquals(1, filtrado.resumen().enProceso());
        assertEquals(1, filtrado.resumen().necesitanRefuerzo());
        assertEquals(0, filtrado.resumen().sinEvaluaciones());
        assertEquals(1, filtrado.promediosPorCompetencia().size());
        assertEquals("lectura", filtrado.promediosPorCompetencia().get(0).competencia());
        assertEquals(68.333, filtrado.promediosPorCompetencia().get(0).promedio(), 0.001);
        assertEquals(68, filtrado.promedioGeneral().intValue());
        assertEquals(ColorSemaforo.AMARILLO, filtrado.colorGeneral());
    }

    @Test
    void gradoFueraDelAulaDevuelve400() {
        Aula aula = mock(Aula.class);
        when(aula.getDocenteId()).thenReturn(1L);
        when(aula.getGrados()).thenReturn(Set.of(grado));
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.dashboardAula(1L, 10L, (short) 1, null, false));

        assertEquals(HttpStatus.BAD_REQUEST, error.getEstado());
        assertEquals("El grado no corresponde a esta aula", error.getMessage());
    }

    @Test
    void competenciaInvalidaDevuelve400() {
        Aula aula = mock(Aula.class);
        when(aula.getDocenteId()).thenReturn(1L);
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.dashboardAula(1L, 10L, null, "matematica", false));

        assertEquals(HttpStatus.BAD_REQUEST, error.getEstado());
        assertEquals("La competencia no es válida", error.getMessage());
    }

    private Estudiante estudiante(Long id, String nombres, String apellidos) {
        return estudiante(id, nombres, apellidos, grado);
    }

    private Estudiante estudiante(Long id, String nombres, String apellidos, Grado grado) {
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
