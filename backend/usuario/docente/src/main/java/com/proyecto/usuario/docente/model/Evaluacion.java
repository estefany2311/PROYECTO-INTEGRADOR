package com.proyecto.usuario.docente.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * T-275/T-276 (US-27): evaluacion de una competencia.
 * El promedio por competencia alimenta el semaforo del dashboard docente.
 */
@Entity
@Table(name = "evaluacion")
public class Evaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id")
    private Estudiante estudiante;

    // 'lectura', 'escritura' u 'oralidad' (la base lo valida con un CHECK)
    @Column(nullable = false)
    private String competencia;

    // 0 a 100 (la base lo valida con un CHECK); SMALLINT para coincidir con la migracion
    @Column(nullable = false)
    private short puntaje;

    @Column(name = "evaluado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime evaluadoEn;

    public Evaluacion() {
    }

    public Evaluacion(Estudiante estudiante, String competencia, short puntaje) {
        this.estudiante = estudiante;
        this.competencia = competencia;
        this.puntaje = puntaje;
    }

    public Long getId() { return id; }
    public Estudiante getEstudiante() { return estudiante; }
    public void setEstudiante(Estudiante estudiante) { this.estudiante = estudiante; }
    public String getCompetencia() { return competencia; }
    public void setCompetencia(String competencia) { this.competencia = competencia; }
    public short getPuntaje() { return puntaje; }
    public void setPuntaje(short puntaje) { this.puntaje = puntaje; }
    public LocalDateTime getEvaluadoEn() { return evaluadoEn; }
}
