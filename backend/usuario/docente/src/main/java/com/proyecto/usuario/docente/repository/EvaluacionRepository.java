package com.proyecto.usuario.docente.repository;

import com.proyecto.usuario.docente.model.Evaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * T-276 (US-27): promedio por competencia de los alumnos activos de un aula.
 */
public interface EvaluacionRepository extends JpaRepository<Evaluacion, Long> {

    @Query("""
            select e.estudiante.id as estudianteId,
                   e.competencia as competencia,
                   avg(e.puntaje) as promedio
            from Evaluacion e
            where e.estudiante.aula.id = :aulaId
              and e.estudiante.activo = true
            group by e.estudiante.id, e.competencia
            order by e.estudiante.id, e.competencia
            """)
    List<PromedioCompetenciaProjection> promediosPorAula(@Param("aulaId") Long aulaId);
}
