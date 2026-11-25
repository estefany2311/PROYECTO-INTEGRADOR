package com.proyecto.usuario.docente.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Mejor intento de un alumno en un nivel: la nota, y los puntos y estrellas que
 * esa nota dio con las reglas de puntuación (US-53) vigentes al calcularlos.
 *
 * <p>Hay una sola fila por alumno y nivel (lo garantiza el {@code UNIQUE} de la
 * migración V21). Sirve para que {@code puntaje_estudiante} sume solo la
 * diferencia cuando el intento mejora o el docente corrige la nota.</p>
 */
@Entity
@Table(name = "puntaje_nivel_estudiante")
public class PuntajeNivelEstudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "estudiante_id", nullable = false)
    private Long estudianteId;

    @Column(name = "nivel_id", nullable = false)
    private Short nivelId;

    @Column(nullable = false)
    private Short nota;

    @Column(nullable = false)
    private int puntos;

    @Column(nullable = false)
    private int estrellas;

    @Column(name = "actualizado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime actualizadoEn;

    public PuntajeNivelEstudiante() {
    }

    public PuntajeNivelEstudiante(Long estudianteId, Short nivelId) {
        this.estudianteId = estudianteId;
        this.nivelId = nivelId;
    }

    public Long getId() { return id; }
    public Long getEstudianteId() { return estudianteId; }
    public Short getNivelId() { return nivelId; }
    public Short getNota() { return nota; }
    public int getPuntos() { return puntos; }
    public int getEstrellas() { return estrellas; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }

    public void registrar(int nota, int puntos, int estrellas) {
        this.nota = (short) nota;
        this.puntos = puntos;
        this.estrellas = estrellas;
    }
}
