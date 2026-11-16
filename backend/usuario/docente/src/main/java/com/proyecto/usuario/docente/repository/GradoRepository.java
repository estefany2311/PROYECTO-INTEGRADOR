package com.proyecto.usuario.docente.repository;

import com.proyecto.usuario.docente.model.Grado;
import org.springframework.data.jpa.repository.JpaRepository;

// Hereda de JpaRepository especificando la entidad (Grado) y el tipo de dato de su ID (Short)
public interface GradoRepository extends JpaRepository<Grado, Short> {
    // Al heredar de JpaRepository ya incluye métodos como findAllById, findById, save, etc
}