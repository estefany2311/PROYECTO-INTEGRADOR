package com.proyecto.usuario.docente.model;

import jakarta.persistence.*;

/**
 * T-189: lectura del catálogo. La tabla `lecturas` es del módulo Admin (Daniela):
 * aquí solo se LEE (no se crean ni se modifican lecturas desde el portal docente).
 * Las columnas creado_en, actualizado_en y creado_por_id no se mapean porque no se usan.
 */
@Entity
@Table(name = "lecturas")
public class Lectura {

    @Id
    private Long id; // sin @GeneratedValue: el id lo genera el módulo Admin

    @Column(nullable = false)
    private String titulo;

    // columnDefinition evita que Hibernate valide `text` como si fuera varchar
    @Column(nullable = false, columnDefinition = "text")
    private String texto;

    @Column(name = "tipo_reto", nullable = false)
    private String tipoReto;

    @Column(nullable = false)
    private String dificultad;

    // grado 1..6: coincide con grado.id
    @Column(nullable = false)
    private Short grado;

    @Column(nullable = false)
    private boolean activo;

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getTexto() { return texto; }
    public String getTipoReto() { return tipoReto; }
    public String getDificultad() { return dificultad; }
    public Short getGrado() { return grado; }
    public boolean isActivo() { return activo; }
}
