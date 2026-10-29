package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.AsignacionGrupoResponse;
import com.proyecto.usuario.docente.dto.AsignarGrupoRequest;
import com.proyecto.usuario.docente.dto.GrupoAlumnoResponse;
import com.proyecto.usuario.docente.dto.GrupoRequest;
import com.proyecto.usuario.docente.dto.GrupoResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Asignacion;
import com.proyecto.usuario.docente.model.Aula;
import com.proyecto.usuario.docente.model.Estudiante;
import com.proyecto.usuario.docente.model.Grupo;
import com.proyecto.usuario.docente.model.Lectura;
import com.proyecto.usuario.docente.repository.AsignacionRepository;
import com.proyecto.usuario.docente.repository.AulaRepository;
import com.proyecto.usuario.docente.repository.EstudianteRepository;
import com.proyecto.usuario.docente.repository.GrupoRepository;
import com.proyecto.usuario.docente.repository.LecturaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * T-195 (US-37): grupos de alumnos y asignación de una lectura a todo el grupo.
 *
 * Reglas:
 * - el aula debe existir (404) y ser del docente (403);
 * - el grupo debe pertenecer a esa aula (404);
 * - los alumnos del grupo deben ser del aula (404) y no se repiten;
 * - la lectura debe existir (404) y estar activa (400);
 * - asignar a un grupo vacío se rechaza (400);
 * - si un alumno ya tenía la lectura, se omite (no falla ni duplica).
 */
@Service
public class GrupoService {

    private final GrupoRepository grupoRepository;
    private final AulaRepository aulaRepository;
    private final EstudianteRepository estudianteRepository;
    private final LecturaRepository lecturaRepository;
    private final AsignacionRepository asignacionRepository;

    public GrupoService(GrupoRepository grupoRepository,
                        AulaRepository aulaRepository,
                        EstudianteRepository estudianteRepository,
                        LecturaRepository lecturaRepository,
                        AsignacionRepository asignacionRepository) {
        this.grupoRepository = grupoRepository;
        this.aulaRepository = aulaRepository;
        this.estudianteRepository = estudianteRepository;
        this.lecturaRepository = lecturaRepository;
        this.asignacionRepository = asignacionRepository;
    }

    @Transactional
    public GrupoResponse crear(Long docenteId, Long aulaId, GrupoRequest req) {
        Aula aula = buscarAulaDelDocente(docenteId, aulaId);
        String nombre = req.nombre().trim();

        if (grupoRepository.existsByAulaIdAndNombreIgnoreCase(aulaId, nombre)) {
            throw new ReglaNegocioException(
                    "Ya existe un grupo con ese nombre en esta aula", HttpStatus.CONFLICT);
        }

        Grupo grupo = new Grupo(aula, nombre);
        grupo.setDescripcion(limpiar(req.descripcion()));
        buscarAlumnosDelAula(aulaId, req.estudiantesIds()).forEach(grupo::agregarAlumno);

        return toResponse(grupoRepository.save(grupo));
    }

    @Transactional(readOnly = true)
    public List<GrupoResponse> listar(Long docenteId, Long aulaId) {
        buscarAulaDelDocente(docenteId, aulaId);
        return grupoRepository.findByAulaIdOrderByNombreAsc(aulaId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AsignacionGrupoResponse asignar(Long docenteId, Long aulaId, Long grupoId,
                                           AsignarGrupoRequest req) {
        buscarAulaDelDocente(docenteId, aulaId);

        Grupo grupo = grupoRepository.findByIdAndAulaId(grupoId, aulaId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "El grupo no existe en esta aula", HttpStatus.NOT_FOUND));

        Lectura lectura = lecturaRepository.findById(req.lecturaId())
                .orElseThrow(() -> new ReglaNegocioException(
                        "La lectura no existe", HttpStatus.NOT_FOUND));

        if (!lectura.isActivo()) {
            throw new ReglaNegocioException(
                    "La lectura no está activa", HttpStatus.BAD_REQUEST);
        }

        if (grupo.estaVacio()) {
            throw new ReglaNegocioException(
                    "El grupo no tiene alumnos", HttpStatus.BAD_REQUEST);
        }

        List<Asignacion> nuevas = new ArrayList<>();
        int yaTenían = 0;
        for (Estudiante alumno : grupo.getAlumnos()) {
            if (asignacionRepository.existsByEstudianteIdAndLecturaId(
                    alumno.getId(), req.lecturaId())) {
                yaTenían++;
            } else {
                nuevas.add(new Asignacion(req.lecturaId(), alumno));
            }
        }

        if (!nuevas.isEmpty()) {
            try {
                // flush: si dos peticiones simultáneas chocan, el índice único decide
                asignacionRepository.saveAllAndFlush(nuevas);
            } catch (DataIntegrityViolationException e) {
                throw new ReglaNegocioException(
                        "Algún alumno del grupo ya tenía esta tarea", HttpStatus.CONFLICT);
            }
        }

        return new AsignacionGrupoResponse(
                grupo.getId(), req.lecturaId(), nuevas.size(), yaTenían);
    }

    // valida que el aula exista y pertenezca al docente autenticado
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

    // todos los ids deben existir y ser alumnos de esa aula; los repetidos cuentan una vez
    private List<Estudiante> buscarAlumnosDelAula(Long aulaId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Set<Long> distintos = new LinkedHashSet<>(ids);
        List<Estudiante> encontrados =
                estudianteRepository.findByIdInAndAulaId(distintos, aulaId);
        if (encontrados.size() != distintos.size()) {
            throw new ReglaNegocioException(
                    "Algún alumno no pertenece a esta aula", HttpStatus.NOT_FOUND);
        }
        return encontrados;
    }

    private String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private GrupoResponse toResponse(Grupo grupo) {
        List<GrupoAlumnoResponse> alumnos = grupo.getAlumnos().stream()
                .sorted(Comparator
                        .comparing(Estudiante::getApellidos, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Estudiante::getNombres, String.CASE_INSENSITIVE_ORDER))
                .map(e -> new GrupoAlumnoResponse(
                        e.getId(), e.getNombres(), e.getApellidos(), e.getGrado().getNombre()))
                .toList();
        return new GrupoResponse(
                grupo.getId(), grupo.getNombre(), grupo.getDescripcion(), alumnos);
    }
}
