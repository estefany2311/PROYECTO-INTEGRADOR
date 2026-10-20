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

        // el grado debe ser uno de los que atiende el aula
        Grado grado = aula.getGrados().stream()
                .filter(g -> g.getId().equals(req.gradoId()))
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException(
                        "El grado no corresponde a esta aula", HttpStatus.BAD_REQUEST));

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

    private EstudianteResponse toResponse(Estudiante e) {
        return new EstudianteResponse(e.getId(), e.getNombres(), e.getApellidos(),
                e.getGrado().getId(), e.getGrado().getNombre(), e.isActivo());
    }
}