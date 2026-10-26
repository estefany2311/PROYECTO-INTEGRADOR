package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.EstudianteRequest;
import com.proyecto.usuario.docente.dto.EstudianteResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Aula;
import com.proyecto.usuario.docente.model.Estudiante;
import com.proyecto.usuario.docente.model.Grado;
import com.proyecto.usuario.docente.repository.AulaRepository;
import com.proyecto.usuario.docente.repository.EstudianteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EstudianteService {

    private final EstudianteRepository estudianteRepository;
    private final AulaRepository aulaRepository;

    public EstudianteService(EstudianteRepository estudianteRepository,
                             AulaRepository aulaRepository) {
        this.estudianteRepository = estudianteRepository;
        this.aulaRepository = aulaRepository;
    }

    @Transactional
    public EstudianteResponse registrar(Long docenteId, Long aulaId, EstudianteRequest req) {
        Aula aula = buscarAulaDelDocente(docenteId, aulaId);
        Grado grado = buscarGradoDelAula(aula, req.gradoId());

        String nombres = req.nombres().trim();
        String apellidos = req.apellidos().trim();

        if (estudianteRepository.existsByAulaIdAndNombresIgnoreCaseAndApellidosIgnoreCase(
                aulaId, nombres, apellidos)) {
            throw new ReglaNegocioException(
                    "Ya existe un alumno con ese nombre en esta aula", HttpStatus.CONFLICT);
        }

        Estudiante e = new Estudiante();
        e.setNombres(nombres);
        e.setApellidos(apellidos);
        e.setAula(aula);
        e.setGrado(grado);
        return toResponse(estudianteRepository.save(e));
    }

    @Transactional(readOnly = true)
    public List<EstudianteResponse> listar(Long docenteId, Long aulaId) {
        buscarAulaDelDocente(docenteId, aulaId);
        return estudianteRepository.findByAulaIdOrderByApellidosAscNombresAsc(aulaId)
                .stream()
                .map(this::toResponse)
                .toList();
    }
    // T-184: igual que listar, pero oculta a los alumnos inactivos
    @Transactional(readOnly = true)
    public List<EstudianteResponse> listarActivos(Long docenteId, Long aulaId) {
        buscarAulaDelDocente(docenteId, aulaId); // mismas validaciones: 404 y 403
        return estudianteRepository.findByAulaIdAndActivoTrueOrderByApellidosAscNombresAsc(aulaId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public EstudianteResponse actualizar(Long docenteId, Long aulaId, Long estudianteId,
                                         EstudianteRequest req) {
        Aula aula = buscarAulaDelDocente(docenteId, aulaId);
        Estudiante e = buscarEstudianteDelAula(aulaId, estudianteId);
        Grado grado = buscarGradoDelAula(aula, req.gradoId());

        String nombres = req.nombres().trim();
        String apellidos = req.apellidos().trim();

        if (estudianteRepository.existsByAulaIdAndNombresIgnoreCaseAndApellidosIgnoreCaseAndIdNot(
                aulaId, nombres, apellidos, estudianteId)) {
            throw new ReglaNegocioException(
                    "Ya existe un alumno con ese nombre en esta aula", HttpStatus.CONFLICT);
        }

        e.setNombres(nombres);
        e.setApellidos(apellidos);
        e.setGrado(grado);
        return toResponse(estudianteRepository.save(e));
    }

    // desactiva (activo = false) o reactiva (activo = true) al alumno
    @Transactional
    public EstudianteResponse cambiarEstado(Long docenteId, Long aulaId, Long estudianteId,
                                            boolean activo) {
        buscarAulaDelDocente(docenteId, aulaId);
        Estudiante e = buscarEstudianteDelAula(aulaId, estudianteId);
        e.setActivo(activo);
        return toResponse(estudianteRepository.save(e));
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

    // valida que el alumno exista dentro de esa aula
    private Estudiante buscarEstudianteDelAula(Long aulaId, Long estudianteId) {
        return estudianteRepository.findByIdAndAulaId(estudianteId, aulaId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "El alumno no existe en esta aula", HttpStatus.NOT_FOUND));
    }

    // el grado debe ser uno de los que atiende el aula
    private Grado buscarGradoDelAula(Aula aula, Short gradoId) {
        return aula.getGrados().stream()
                .filter(g -> g.getId().equals(gradoId))
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException(
                        "El grado no corresponde a esta aula", HttpStatus.BAD_REQUEST));
    }

    private EstudianteResponse toResponse(Estudiante e) {
        Short avatarId = e.getAvatar() != null ? e.getAvatar().getId() : null;
        String avatarUrl = e.getAvatar() != null ? e.getAvatar().getUrl() : null;
        return new EstudianteResponse(e.getId(), e.getNombres(), e.getApellidos(),
                e.getGrado().getId(), e.getGrado().getNombre(), e.isActivo(),
                avatarId, avatarUrl, e.tienePin());
    }
}