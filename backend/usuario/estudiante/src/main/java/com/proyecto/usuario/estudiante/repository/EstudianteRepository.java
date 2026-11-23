package com.proyecto.usuario.estudiante.repository;

import com.proyecto.usuario.estudiante.model.Estudiante;

import java.util.List;
import java.util.Optional;

// Interfaz: la implementa JdbcEstudianteRepository con los alumnos reales; en las pruebas se usa un mock
public interface EstudianteRepository {

    boolean existeAula(String codigoAula);

    List<Estudiante> listarActivosPorCodigoAula(String codigoAula);

    Optional<Estudiante> buscarPorId(long id);
}