package com.proyecto.usuario.docente.repository;

import com.proyecto.usuario.docente.model.Aula;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

// Hereda de JpaRepository especificando la entidad (Aula) y el tipo de dato de su ID (Long)
public interface AulaRepository extends JpaRepository<Aula, Long> {

    // Consulta personalizada generada automáticamente por Spring:
    // Busca todas las aulas de un docente específico ordenadas de la más reciente a la más antigua
    List<Aula> findByDocenteIdOrderByCreadoEnDesc(Long docenteId);

    // Consulta personalizada generada automáticamente por Spring:
    // Verifica si ya existe un aula con el mismo nombre para dicho docente (ignorando mayúsculas/minúsculas)
    boolean existsByDocenteIdAndNombreIgnoreCase(Long docenteId, String nombre);
}