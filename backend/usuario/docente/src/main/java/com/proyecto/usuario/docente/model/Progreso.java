package com.proyecto.usuario.docente.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * T-197: avance de un alumno en un nivel concreto.
 * Ojo: si no existe fila para un nivel, el mapa lo calcula (nivel 1 desbloqueado, resto bloqueado).
 */
@Entity
@Table(name = "progreso")
public class Progreso {

    // valores permitidos; coinciden con el CHECK de la migración V7
    public static final String BLOQUEADO = "bloqueado";
    public static final String DESBLOQUEADO = "desbloqueado";
    public static final String COMPLETADO = "completado";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id")
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nivel_id")
    private Nivel nivel;

    @Column(nullable = false)
    private String estado = BLOQUEADO;

    @Column(nullable = false)
    private int puntos;

    @Column(name = "actualizado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime actualizadoEn;

    public Progreso() {
    }

    public Progreso(Estudiante estudiante, Nivel nivel, String estado, int puntos) {
        this.estudiante = estudiante;
        this.nivel = nivel;
        this.estado = estado;
        this.puntos = puntos;
    }

    public Long getId() { return id; }
    public Estudiante getEstudiante() { return estudiante; }
    public void setEstudiante(Estudiante estudiante) { this.estudiante = estudiante; }
    public Nivel getNivel() { return nivel; }
    public void setNivel(Nivel nivel) { this.nivel = nivel; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public int getPuntos() { return puntos; }
    public void setPuntos(int puntos) { this.puntos = puntos; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }

    public boolean estaCompletado() { return COMPLETADO.equals(estado); }
}
