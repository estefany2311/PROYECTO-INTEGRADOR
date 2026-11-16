package com.proyecto.usuario.docente.model;

import jakarta.persistence.*;

// T-185: avatar del catálogo (solo lectura: los 12 vienen de la migración)
@Entity
@Table(name = "avatar")
public class Avatar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id; // SMALLSERIAL en BD = Short en Java

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(nullable = false)
    private String url;

    public Short getId() { return id; }
    public String getNombre() { return nombre; }
    public String getUrl() { return url; }
}