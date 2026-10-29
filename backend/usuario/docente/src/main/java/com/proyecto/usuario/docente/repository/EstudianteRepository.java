package com.proyecto.usuario.docente.repository;

import com.proyecto.usuario.docente.model.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {
    List<Estudiante> findByAulaIdOrderByApellidosAscNombresAsc(Long aulaId);

    Optional<Estudiante> findByIdAndAulaId(Long id, Long aulaId);

    // T-195: valida de una sola consulta que todos los alumnos del grupo son del aula
    List<Estudiante> findByIdInAndAulaId(Collection<Long> ids, Long aulaId);

    boolean existsByAulaIdAndNombresIgnoreCaseAndApellidosIgnoreCase(
            Long aulaId, String nombres, String apellidos);

    // igual que la anterior, pero ignorando al propio alumno que se edita
    boolean existsByAulaIdAndNombresIgnoreCaseAndApellidosIgnoreCaseAndIdNot(
            Long aulaId, String nombres, String apellidos, Long id);
    // T-184: solo alumnos activos (los que ve la tablet), mismo orden que el listado normal
    List<Estudiante> findByAulaIdAndActivoTrueOrderByApellidosAscNombresAsc(Long aulaId);
}