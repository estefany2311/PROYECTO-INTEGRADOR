package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.CompetenciaPromedioResponse;
import com.proyecto.usuario.docente.dto.DashboardAulaResponse;
import com.proyecto.usuario.docente.dto.EstudianteDashboardResponse;
import com.proyecto.usuario.docente.dto.GradoResponse;
import com.proyecto.usuario.docente.dto.PromedioCompetenciaResponse;
import com.proyecto.usuario.docente.dto.ResumenDashboardResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Aula;
import com.proyecto.usuario.docente.model.ColorSemaforo;
import com.proyecto.usuario.docente.model.Estudiante;
import com.proyecto.usuario.docente.model.Grado;
import com.proyecto.usuario.docente.repository.AulaRepository;
import com.proyecto.usuario.docente.repository.EstudianteRepository;
import com.proyecto.usuario.docente.repository.EvaluacionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * T-276/T-278 (US-27): consulta del rendimiento del aula para el dashboard docente.
 * T-276 devuelve el promedio de cada alumno activo en cada competencia;
 * T-278 arma el dashboard completo con el semáforo y los promedios del aula.
 */
@Service
public class RendimientoService {

    private final EvaluacionRepository evaluacionRepository;
    private final AulaRepository aulaRepository;
    private final EstudianteRepository estudianteRepository;
    private final SemaforoService semaforoService;

    public RendimientoService(EvaluacionRepository evaluacionRepository,
                              AulaRepository aulaRepository,
                              EstudianteRepository estudianteRepository,
                              SemaforoService semaforoService) {
        this.evaluacionRepository = evaluacionRepository;
        this.aulaRepository = aulaRepository;
        this.estudianteRepository = estudianteRepository;
        this.semaforoService = semaforoService;
    }

    @Transactional(readOnly = true)
    public List<PromedioCompetenciaResponse> promediosPorAula(Long docenteId, Long aulaId) {
        buscarAulaDelDocente(docenteId, aulaId);
        return promediosDelAula(aulaId);
    }

    /**
     * T-278: dashboard del aula con una tarjeta por alumno activo.
     * Los alumnos sin evaluaciones se incluyen en gris y no entran en los
     * promedios del aula.
     */
    @Transactional(readOnly = true)
    public DashboardAulaResponse dashboardAula(Long docenteId, Long aulaId) {
        Aula aula = buscarAulaDelDocente(docenteId, aulaId);

        List<Estudiante> estudiantes = estudianteRepository
                .findByAulaIdAndActivoTrueOrderByApellidosAscNombresAsc(aulaId);

        Map<Long, List<PromedioCompetenciaResponse>> promediosPorEstudiante =
                promediosDelAula(aulaId).stream()
                        .collect(Collectors.groupingBy(PromedioCompetenciaResponse::estudianteId));

        int enNivelEsperado = 0;
        int enProceso = 0;
        int necesitanRefuerzo = 0;
        int sinEvaluaciones = 0;

        List<EstudianteDashboardResponse> estudiantesResponse = new ArrayList<>();
        Map<String, List<Double>> acumuladosPorCompetencia = new HashMap<>();
        List<Double> promediosGenerales = new ArrayList<>();

        for (Estudiante estudiante : estudiantes) {
            List<CompetenciaPromedioResponse> competencias =
                    promediosPorEstudiante.getOrDefault(estudiante.getId(), List.of()).stream()
                            .sorted(Comparator.comparingInt(p -> ordenCompetencia(p.competencia())))
                            .map(p -> new CompetenciaPromedioResponse(p.competencia(), p.promedio()))
                            .toList();

            Double promedio = calcularPromedioCompetencias(competencias);
            ColorSemaforo color = semaforoService.calcularColor(promedio);

            switch (color) {
                case VERDE -> enNivelEsperado++;
                case AMARILLO -> enProceso++;
                case ROJO -> necesitanRefuerzo++;
                case GRIS -> sinEvaluaciones++;
            }

            if (promedio != null) {
                promediosGenerales.add(promedio);
            }
            for (CompetenciaPromedioResponse competencia : competencias) {
                acumuladosPorCompetencia
                        .computeIfAbsent(competencia.competencia(), clave -> new ArrayList<>())
                        .add(competencia.promedio());
            }

            estudiantesResponse.add(new EstudianteDashboardResponse(
                    estudiante.getId(),
                    estudiante.getNombres(),
                    estudiante.getApellidos(),
                    estudiante.getGrado().getId(),
                    estudiante.getGrado().getNombre(),
                    promedio == null ? null : (int) Math.round(promedio),
                    color,
                    competencias));
        }

        // El mockup muestra primero a los alumnos con mejor promedio; los grises van al final.
        estudiantesResponse.sort(
                Comparator.comparing(EstudianteDashboardResponse::promedio,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(EstudianteDashboardResponse::apellidos)
                        .thenComparing(EstudianteDashboardResponse::nombres));

        List<CompetenciaPromedioResponse> promediosAula = acumuladosPorCompetencia.entrySet().stream()
                .map(entry -> new CompetenciaPromedioResponse(
                        entry.getKey(), calcularPromedioValores(entry.getValue())))
                .sorted(Comparator.comparingInt(c -> ordenCompetencia(c.competencia())))
                .toList();

        Double promedioGeneral = calcularPromedioValores(promediosGenerales);
        ColorSemaforo colorGeneral = semaforoService.calcularColor(promedioGeneral);

        List<GradoResponse> grados = aula.getGrados().stream()
                .sorted(Comparator.comparing(Grado::getOrden))
                .map(g -> new GradoResponse(g.getId(), g.getNombre()))
                .toList();

        return new DashboardAulaResponse(
                aula.getId(),
                aula.getNombre(),
                grados,
                new ResumenDashboardResponse(estudiantes.size(), enNivelEsperado, enProceso,
                        necesitanRefuerzo, sinEvaluaciones),
                estudiantesResponse,
                promediosAula,
                promedioGeneral == null ? null : (int) Math.round(promedioGeneral),
                colorGeneral);
    }

    private List<PromedioCompetenciaResponse> promediosDelAula(Long aulaId) {
        return evaluacionRepository.promediosPorAula(aulaId).stream()
                .map(p -> new PromedioCompetenciaResponse(
                        p.getEstudianteId(), p.getCompetencia(), p.getPromedio()))
                .toList();
    }

    private Aula buscarAulaDelDocente(Long docenteId, Long aulaId) {
        Aula aula = aulaRepository.findById(aulaId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "El aula no existe", HttpStatus.NOT_FOUND));
        if (!aula.getDocenteId().equals(docenteId)) {
            throw new ReglaNegocioException(
                    "El aula no pertenece al docente", HttpStatus.FORBIDDEN);
        }
        return aula;
    }

    private Double calcularPromedioCompetencias(List<CompetenciaPromedioResponse> competencias) {
        if (competencias.isEmpty()) {
            return null;
        }
        return competencias.stream()
                .mapToDouble(CompetenciaPromedioResponse::promedio)
                .average()
                .orElseThrow();
    }

    private Double calcularPromedioValores(List<Double> promedios) {
        if (promedios.isEmpty()) {
            return null;
        }
        return promedios.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElseThrow();
    }

    /**
     * Orden visual del mockup: lectura, escritura y oralidad.
     */
    private int ordenCompetencia(String competencia) {
        return switch (competencia) {
            case "lectura" -> 0;
            case "escritura" -> 1;
            case "oralidad" -> 2;
            default -> 3;
        };
    }
}
