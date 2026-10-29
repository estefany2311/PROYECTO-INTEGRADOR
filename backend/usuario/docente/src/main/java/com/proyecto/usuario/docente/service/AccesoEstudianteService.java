package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.EstudianteResponse;
import com.proyecto.usuario.docente.dto.PinGeneradoResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Aula;
import com.proyecto.usuario.docente.model.Avatar;
import com.proyecto.usuario.docente.model.Estudiante;
import com.proyecto.usuario.docente.repository.AulaRepository;
import com.proyecto.usuario.docente.repository.AvatarRepository;
import com.proyecto.usuario.docente.repository.EstudianteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

// T-186: avatar y PIN con los que el alumno entra en la tablet
@Service
public class AccesoEstudianteService {

    private final EstudianteRepository estudianteRepository;
    private final AulaRepository aulaRepository;
    private final AvatarRepository avatarRepository;
    private final PasswordEncoder codificadorPin;
    private final SecureRandom aleatorio = new SecureRandom();

    public AccesoEstudianteService(EstudianteRepository estudianteRepository,
                                   AulaRepository aulaRepository,
                                   AvatarRepository avatarRepository,
                                   PasswordEncoder codificadorPin) {
        this.estudianteRepository = estudianteRepository;
        this.aulaRepository = aulaRepository;
        this.avatarRepository = avatarRepository;
        this.codificadorPin = codificadorPin;
    }

    @Transactional
    public EstudianteResponse asignarAvatar(Long docenteId, Long aulaId, Long estudianteId, Short avatarId) {
        Estudiante e = buscarAlumnoDelDocente(docenteId, aulaId, estudianteId);
        Avatar avatar = avatarRepository.findById(avatarId)
                .orElseThrow(() -> new ReglaNegocioException("El avatar no existe", HttpStatus.BAD_REQUEST));
        e.setAvatar(avatar);
        estudianteRepository.save(e);
        return toResponse(e);
    }

    /**
     * Genera un PIN nuevo de 4 números (sirve para "Generar" y para "Restablecer").
     * Se guarda solo el hash BCrypt; el PIN en claro se devuelve una única vez al docente.
     */
    @Transactional
    public PinGeneradoResponse generarPin(Long docenteId, Long aulaId, Long estudianteId) {
        Estudiante e = buscarAlumnoDelDocente(docenteId, aulaId, estudianteId);
        String pin = String.format("%04d", aleatorio.nextInt(10_000)); // "0007", "4821"...
        e.setPinHash(codificadorPin.encode(pin));                      // aquí se cifra
        estudianteRepository.save(e);
        return new PinGeneradoResponse(e.getId(), pin);
    }

    // aula existe (404) -> es del docente (403) -> el alumno es de esa aula (404)
    private Estudiante buscarAlumnoDelDocente(Long docenteId, Long aulaId, Long estudianteId) {
        Aula aula = aulaRepository.findById(aulaId)
                .orElseThrow(() -> new ReglaNegocioException("El aula no existe", HttpStatus.NOT_FOUND));
        if (!aula.getDocenteId().equals(docenteId)) {
            throw new ReglaNegocioException("El aula no pertenece al docente", HttpStatus.FORBIDDEN);
        }
        return estudianteRepository.findByIdAndAulaId(estudianteId, aulaId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "El alumno no existe en esta aula", HttpStatus.NOT_FOUND));
    }

    private EstudianteResponse toResponse(Estudiante e) {
        Avatar av = e.getAvatar();
        return new EstudianteResponse(e.getId(), e.getNombres(), e.getApellidos(),
                e.getGrado().getId(), e.getGrado().getNombre(), e.isActivo(),
                av != null ? av.getId() : null, av != null ? av.getUrl() : null, e.tienePin());
    }
}