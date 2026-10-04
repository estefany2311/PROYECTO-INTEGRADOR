package com.proyecto.usuario.docente.model;

// Importa todas las anotaciones de JPA para mapear la base de datos (Entity, Table, Id, etc.)
import jakarta.persistence.*;

// Le indica a Spring/Hibernate que esta clase representa una tabla de la base de datos
@Entity
// Especifica la tabla exacta en PostgreSQL con la que se conecta
@Table(name = "grado")
public class Grado {

    // Marca esta variable como la Clave Primaria (Primary Key / PK) de la tabla
    @Id
    private Short id;

    // Almacena el nombre del grado (ejemplo: "1°", "2°", "3°")
    private String nombre;

    // Almacena el orden para poder ordenar los grados secuencialmente (1, 2, 3...)
    private Short orden;

    // Constructor vacío requerido obligatoriamente por JPA/Hibernate para instanciar la clase
    protected Grado() {}

    // Constructor con parámetros para crear objetos de tipo Grado con sus datos
    public Grado(Short id, String nombre, Short orden) {
        this.id = id;
        this.nombre = nombre;
        this.orden = orden;
    }

    // --- Métodos Getter: Permiten consultar/leer los valores de los atributos privados ---

    // Retorna el ID del grado
    public Short getId() {
        return id;
    }

    // Retorna el nombre del grado
    public String getNombre() {
        return nombre;
    }

    // Retorna el orden del grado
    public Short getOrden() {
        return orden;
    }
}