package com.proyecto.usuario.docente.repository;

import com.proyecto.usuario.docente.model.Nivel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// T-197: niveles del mapa, en el orden del recorrido
public interface NivelRepository extends JpaRepository<Nivel, Short> {

    List<Nivel> findByActivoTrueOrderByOrdenAsc();
}
