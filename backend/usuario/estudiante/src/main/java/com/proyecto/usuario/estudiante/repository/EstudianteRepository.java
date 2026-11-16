package com.proyecto.usuario.estudiante.repository;

import com.proyecto.usuario.estudiante.model.Estudiante;

import java.util.List;
import java.util.Optional;

// Interfaz: hoy la implementa el repositorio de prueba; cuando Noemí
// agregue pin_hash y avatar se crea la versión que lee la tabla estudiante.
public interface EstudianteRepository {

    boolean existeAula(String codigoAula);

    List<Estudiante> listarActivosPorCodigoAula(String codigoAula);

    Optional<Estudiante> buscarPorId(long id);
}