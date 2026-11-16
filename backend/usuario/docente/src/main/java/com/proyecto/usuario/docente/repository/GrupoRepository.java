package com.proyecto.usuario.docente.repository;

import com.proyecto.usuario.docente.model.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * T-194 (US-37): acceso a los grupos de alumnos.
 * T-195 (endpoints) usará estas consultas para crear grupos y asignarles una lectura.
 */
public interface GrupoRepository extends JpaRepository<Grupo, Long> {

    // pantalla de grupos: los de un aula, en orden alfabético
    List<Grupo> findByAulaIdOrderByNombreAsc(Long aulaId);

    // evita dos grupos con el mismo nombre en la misma aula
    boolean existsByAulaIdAndNombreIgnoreCase(Long aulaId, String nombre);

    // un grupo solo se puede tocar si pertenece al aula indicada (aula ajena -> vacío)
    Optional<Grupo> findByIdAndAulaId(Long id, Long aulaId);
}
