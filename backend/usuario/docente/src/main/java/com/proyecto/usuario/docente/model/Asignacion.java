package com.proyecto.usuario.docente.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * T-191: una lectura del catálogo asignada a un alumno concreto (tarea individual).
 * La lectura vive en la tabla `lecturas`, que pertenece al módulo Admin (Daniela):
 * aquí solo guardamos su id para no acoplar los dos modelos.
 */
@Entity
@Table(name = "asignacion")
public class Asignacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lectura_id", nullable = false)
    private Long lecturaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id")
    private Estudiante estudiante;

    // T-192: se fija al crear la asignación; la BD conserva DEFAULT now() como respaldo
    @Column(name = "fecha_asignacion", nullable = false, updatable = false)
    private LocalDateTime fechaAsignacion;

    public Asignacion() {
    }

    public Asignacion(Long lecturaId, Estudiante estudiante) {
        this.lecturaId = lecturaId;
        this.estudiante = estudiante;
        this.fechaAsignacion = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getLecturaId() { return lecturaId; }
    public void setLecturaId(Long lecturaId) { this.lecturaId = lecturaId; }
    public Estudiante getEstudiante() { return estudiante; }
    public void setEstudiante(Estudiante estudiante) { this.estudiante = estudiante; }
    public LocalDateTime getFechaAsignacion() { return fechaAsignacion; }
}
