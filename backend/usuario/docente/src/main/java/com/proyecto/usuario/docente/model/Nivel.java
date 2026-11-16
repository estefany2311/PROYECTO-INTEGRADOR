package com.proyecto.usuario.docente.model;

import jakarta.persistence.*;

/**
 * T-197: un nivel del mapa de aventura (US-05).
 * El tipo de reto define qué actividad se hace al entrar al nivel (US-07).
 */
@Entity
@Table(name = "nivel")
public class Nivel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;

    @Column(nullable = false, unique = true)
    private Short orden;

    @Column(nullable = false)
    private String nombre;

    // 'lectura', 'escritura' u 'oralidad' (la base lo valida con un CHECK)
    @Column(name = "tipo_reto", nullable = false)
    private String tipoReto;

    private String descripcion;

    @Column(nullable = false)
    private boolean activo = true;

    public Nivel() {
    }

    public Nivel(Short id, Short orden, String nombre, String tipoReto, String descripcion) {
        this.id = id;
        this.orden = orden;
        this.nombre = nombre;
        this.tipoReto = tipoReto;
        this.descripcion = descripcion;
    }

    public Short getId() { return id; }
    public Short getOrden() { return orden; }
    public void setOrden(Short orden) { this.orden = orden; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTipoReto() { return tipoReto; }
    public void setTipoReto(String tipoReto) { this.tipoReto = tipoReto; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
