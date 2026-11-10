package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.DetalleEstudianteResponse;
import com.proyecto.usuario.docente.dto.EvidenciaEstudianteResponse;
import com.proyecto.usuario.docente.dto.NivelMapaResponse;
import com.proyecto.usuario.docente.dto.PuntajeCompetenciaResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Aula;
import com.proyecto.usuario.docente.model.Estudiante;
import com.proyecto.usuario.docente.model.Evaluacion;
import com.proyecto.usuario.docente.repository.AulaRepository;
import com.proyecto.usuario.docente.repository.EstudianteRepository;
import com.proyecto.usuario.docente.repository.EvaluacionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * T-290 (US-29): detalle de progreso de un estudiante para el docente.
 *
 * <p>Devuelve en una sola llamada los niveles del mapa, el promedio y el color
 * de cada competencia y las últimas evaluaciones. La pantalla de detalle
 * (T-292) y el gráfico de evolución (T-293) consumen esta respuesta.</p>
 */
@Service
public class DetalleEstudianteService {

    private static final List<String> COMPETENCIAS =
            List.of("lectura", "escritura", "oralidad");
    private static final int LIMITE_EVIDENCIAS = 10;

    private final AulaRepository aulaRepository;
    private final EstudianteRepository estudianteRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final MapaEstudianteService mapaEstudianteService;
    private final SemaforoService semaforoService;

    public DetalleEstudianteService(AulaRepository aulaRepository,
                                    EstudianteRepository estudianteRepository,
                                    EvaluacionRepository evaluacionRepository,
                                    MapaEstudianteService mapaEstudianteService,
                                    SemaforoService semaforoService) {
        this.aulaRepository = aulaRepository;
        this.estudianteRepository = estudianteRepository;
        this.evaluacionRepository = evaluacionRepository;
        this.mapaEstudianteService = mapaEstudianteService;
        this.semaforoService = semaforoService;
    }

    @Transactional(readOnly = true)
    public DetalleEstudianteResponse detalle(Long docenteId, Long aulaId, Long estudianteId) {
        buscarAulaDelDocente(docenteId, aulaId);
        // T-291 completará la validación de que el alumno pertenece a esta aula.
        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "El alumno no existe", HttpStatus.NOT_FOUND));

        List<Evaluacion> evaluaciones = evaluacionRepository
                .findByEstudianteIdOrderByEvaluadoEnDescIdDesc(estudianteId);

        Map<String, List<Evaluacion>> porCompetencia = evaluaciones.stream()
                .collect(Collectors.groupingBy(Evaluacion::getCompetencia));

        List<PuntajeCompetenciaResponse> puntajes = COMPETENCIAS.stream()
                .filter(porCompetencia::containsKey)
                .map(competencia -> resumirCompetencia(
                        competencia, porCompetencia.get(competencia)))
                .toList();

        Double promedioGeneral = puntajes.isEmpty()
                ? null
                : puntajes.stream()
                        .mapToDouble(PuntajeCompetenciaResponse::promedio)
                        .average()
                        .orElseThrow();

        List<EvidenciaEstudianteResponse> evidencias = evaluaciones.stream()
                .limit(LIMITE_EVIDENCIAS)
                .map(evaluacion -> new EvidenciaEstudianteResponse(
                        evaluacion.getId(),
                        evaluacion.getCompetencia(),
                        evaluacion.getPuntaje(),
                        evaluacion.getEvaluadoEn()))
                .toList();

        List<NivelMapaResponse> niveles = mapaEstudianteService.nivelesDe(estudianteId);

        return new DetalleEstudianteResponse(
                estudiante.getId(),
                estudiante.getNombres(),
                estudiante.getApellidos(),
                estudiante.getGrado().getId(),
                estudiante.getGrado().getNombre(),
                promedioGeneral == null ? null : (int) Math.round(promedioGeneral),
                semaforoService.calcularColor(promedioGeneral),
                niveles,
                puntajes,
                evidencias);
    }

    private PuntajeCompetenciaResponse resumirCompetencia(String competencia,
                                                          List<Evaluacion> evaluaciones) {
        double promedio = evaluaciones.stream()
                .mapToInt(Evaluacion::getPuntaje)
                .average()
                .orElseThrow();
        return new PuntajeCompetenciaResponse(
                competencia,
                promedio,
                evaluaciones.size(),
                evaluaciones.get(0).getPuntaje(),
                semaforoService.calcularColor(promedio));
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
}
