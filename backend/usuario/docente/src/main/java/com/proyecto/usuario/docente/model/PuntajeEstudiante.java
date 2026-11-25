package com.proyecto.usuario.docente.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * T-301 (US-09): puntos y estrellas acumulados por el estudiante en el mapa.
 *
 * <p>Hay una sola fila por alumno (lo garantiza el {@code UNIQUE} de la
 * migración V12). El servicio de T-302 suma aquí los puntos y las estrellas de
 * cada reto completado usando las reglas de puntuación de la US-53.</p>
 */
@Entity
@Table(name = "puntaje_estudiante")
public class PuntajeEstudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false, unique = true)
    private Estudiante estudiante;

    @Column(nullable = false)
    private int puntos;

    @Column(nullable = false)
    private int estrellas;

    @Column(name = "actualizado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime actualizadoEn;

    public PuntajeEstudiante() {
    }

    /**
     * Acumulado en cero para un alumno que todavía no completó ningún reto.
     */
    public static PuntajeEstudiante inicial(Estudiante estudiante) {
        PuntajeEstudiante puntaje = new PuntajeEstudiante();
        puntaje.estudiante = estudiante;
        return puntaje;
    }

    public Long getId() { return id; }
    public Estudiante getEstudiante() { return estudiante; }
    public void setEstudiante(Estudiante estudiante) { this.estudiante = estudiante; }
    public int getPuntos() { return puntos; }
    public void setPuntos(int puntos) { this.puntos = puntos; }
    public int getEstrellas() { return estrellas; }
    public void setEstrellas(int estrellas) { this.estrellas = estrellas; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
}
