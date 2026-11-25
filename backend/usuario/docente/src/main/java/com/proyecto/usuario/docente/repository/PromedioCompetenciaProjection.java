package com.proyecto.usuario.docente.repository;

/**
 * T-276 (US-27): proyeccion de la consulta de promedios por competencia.
 */
public interface PromedioCompetenciaProjection {

    Long getEstudianteId();

    String getCompetencia();

    Double getPromedio();
}
