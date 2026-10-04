package com.proyecto.usuario.docente.model;

// Importa las anotaciones necesarias para mapear la entidad con la base de datos JPA
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

// Define que esta clase es una entidad JPA (se convertirá en una tabla en la BD)
@Entity
// Especifica el nombre de la tabla exacta en PostgreSQL
@Table(name = "aula")
public class Aula {

    // Marca esta propiedad como la Clave Primaria (Primary Key)
    @Id
    // Configura el ID para que sea autoincrementable según la estrategia de la BD
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Columna obligatorio (NOT NULL) para guardar el nombre del aula
    @Column(nullable = false)
    private String nombre;

    // Columna obligatoria para vincular el aula con un docente específico
    @Column(name = "docente_id", nullable = false)
    private Long docenteId;

    // Mapea la fecha de creación. insertable/updatable = false permite que la BD gestione la fecha por defecto
    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    // Define una relación de muchos a muchos (Un aula puede tener varios grados y un grado estar en varias aulas)
    @ManyToMany
    // Configura la tabla intermedia (aula_grado) y las llaves foráneas para unir ambas entidades
    @JoinTable(name = "aula_grado",
            joinColumns = @JoinColumn(name = "aula_id"),
            inverseJoinColumns = @JoinColumn(name = "grado_id"))
    private Set<Grado> grados = new HashSet<>();

    // --- Métodos Getters y Setters para acceder y modificar las propiedades ---

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Long getDocenteId() {
        return docenteId;
    }

    public void setDocenteId(Long docenteId) {
        this.docenteId = docenteId;
    }

    public Set<Grado> getGrados() {
        return grados;
    }

    public void setGrados(Set<Grado> grados) {
        this.grados = grados;
    }
}