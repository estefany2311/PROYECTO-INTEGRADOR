package com.proyecto.usuario.docente.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * T-194 (US-37): grupo de alumnos de un aula.
 * Sirve para asignar una misma lectura a varios alumnos de una sola vez (T-195).
 * La relación con los alumnos se guarda en la tabla intermedia `grupo_alumno`.
 */
@Entity
@Table(name = "grupo")
public class Grupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // todo grupo pertenece a un aula del docente
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aula_id")
    private Aula aula;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(length = 200)
    private String descripcion;

    // la fecha la pone la base de datos (DEFAULT now())
    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    // alumnos del grupo; el Set evita que un alumno quede repetido (la BD también lo impide)
    @ManyToMany
    @JoinTable(name = "grupo_alumno",
            joinColumns = @JoinColumn(name = "grupo_id"),
            inverseJoinColumns = @JoinColumn(name = "estudiante_id"))
    private Set<Estudiante> alumnos = new HashSet<>();

    public Grupo() {
    }

    public Grupo(Aula aula, String nombre) {
        this.aula = aula;
        this.nombre = nombre;
    }

    public Long getId() { return id; }
    public Aula getAula() { return aula; }
    public void setAula(Aula aula) { this.aula = aula; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public Set<Estudiante> getAlumnos() { return alumnos; }

    // el Set ya ignora al alumno si vuelve a agregarse
    public void agregarAlumno(Estudiante alumno) {
        alumnos.add(alumno);
    }

    public void quitarAlumno(Estudiante alumno) {
        alumnos.remove(alumno);
    }

    public boolean estaVacio() {
        return alumnos.isEmpty();
    }
}
