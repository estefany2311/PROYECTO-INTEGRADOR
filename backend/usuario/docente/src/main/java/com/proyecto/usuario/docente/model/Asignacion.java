package com.proyecto.usuario.docente.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * T-191: una lectura del catálogo asignada a un alumno concreto (tarea individual).
 * La lectura vive en la tabla `lecturas`, que pertenece al módulo Admin (Daniela):
 * aquí solo guardamos su id para no acoplar los dos modelos.
 *
 * T-388 (US-38): la tarea puede tener una fecha límite y un estado de seguimiento
 * (pendiente o entregada). "Vencida" se calcula, no se guarda.
 */
@Entity
@Table(name = "asignacion")
public class Asignacion {

    public static final String PENDIENTE = "pendiente";
    public static final String ENTREGADA = "entregada";

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

    // T-388: opcional; sin fecha, la tarea nunca vence
    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    @Column(nullable = false)
    private String estado = PENDIENTE;

    @Column(name = "entregada_en")
    private LocalDateTime entregadaEn;

    public Asignacion() {
    }

    public Asignacion(Long lecturaId, Estudiante estudiante) {
        this(lecturaId, estudiante, null);
    }

    public Asignacion(Long lecturaId, Estudiante estudiante, LocalDate fechaLimite) {
        this.lecturaId = lecturaId;
        this.estudiante = estudiante;
        this.fechaAsignacion = LocalDateTime.now();
        this.fechaLimite = fechaLimite;
    }

    // T-388: el alumno entregó la tarea; queda registrado el momento
    public void marcarEntregada(LocalDateTime momento) {
        this.estado = ENTREGADA;
        this.entregadaEn = momento;
    }

    public Long getId() { return id; }
    public Long getLecturaId() { return lecturaId; }
    public void setLecturaId(Long lecturaId) { this.lecturaId = lecturaId; }
    public Estudiante getEstudiante() { return estudiante; }
    public void setEstudiante(Estudiante estudiante) { this.estudiante = estudiante; }
    public LocalDateTime getFechaAsignacion() { return fechaAsignacion; }
    public LocalDate getFechaLimite() { return fechaLimite; }
    public void setFechaLimite(LocalDate fechaLimite) { this.fechaLimite = fechaLimite; }
    public String getEstado() { return estado; }
    public LocalDateTime getEntregadaEn() { return entregadaEn; }
}
