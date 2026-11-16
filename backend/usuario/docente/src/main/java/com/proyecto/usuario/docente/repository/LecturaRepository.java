package com.proyecto.usuario.docente.repository;

import com.proyecto.usuario.docente.model.Lectura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * T-189: catálogo de lecturas (solo lectura).
 * La consulta ya trae únicamente las activas de los grados del aula; los demás
 * filtros de la pantalla (grado, dificultad, tipo de reto) se aplican en el servicio.
 */
public interface LecturaRepository extends JpaRepository<Lectura, Long> {

    List<Lectura> findByActivoTrueAndGradoInOrderByGradoAscTituloAsc(Collection<Short> grados);
}
