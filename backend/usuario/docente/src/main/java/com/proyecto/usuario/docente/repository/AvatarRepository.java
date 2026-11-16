package com.proyecto.usuario.docente.repository;

import com.proyecto.usuario.docente.model.Avatar;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvatarRepository extends JpaRepository<Avatar, Short> {
}