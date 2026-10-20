package com.proyecto.usuario.docente.repository;

import com.proyecto.usuario.docente.model.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {
    List<Estudiante> findByAulaIdOrderByApellidosAscNombresAsc(Long aulaId);
    boolean existsByAulaIdAndNombresIgnoreCaseAndApellidosIgnoreCase(
            Long aulaId, String nombres, String apellidos);
}