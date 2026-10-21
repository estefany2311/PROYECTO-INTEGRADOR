package com.proyecto.usuario.docente.repository;

import com.proyecto.usuario.docente.model.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {
    List<Estudiante> findByAulaIdOrderByApellidosAscNombresAsc(Long aulaId);

    Optional<Estudiante> findByIdAndAulaId(Long id, Long aulaId);

    boolean existsByAulaIdAndNombresIgnoreCaseAndApellidosIgnoreCase(
            Long aulaId, String nombres, String apellidos);

    // igual que la anterior, pero ignorando al propio alumno que se edita
    boolean existsByAulaIdAndNombresIgnoreCaseAndApellidosIgnoreCaseAndIdNot(
            Long aulaId, String nombres, String apellidos, Long id);
}