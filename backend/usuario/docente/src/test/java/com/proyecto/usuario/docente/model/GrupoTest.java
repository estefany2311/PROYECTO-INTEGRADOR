package com.proyecto.usuario.docente.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T-194: comportamiento del grupo antes de exponerlo por API (T-195).
 * No toca base de datos: solo la lógica de la lista de alumnos.
 */
class GrupoTest {

    private final Grupo grupo = new Grupo(new Aula(), "Los exploradores");

    @Test
    void unGrupoNuevoNaceVacio() {
        assertTrue(grupo.estaVacio());
        assertEquals(0, grupo.getAlumnos().size());
    }

    @Test
    void guardaElNombreDelGrupo() {
        assertEquals("Los exploradores", grupo.getNombre());
    }

    @Test
    void noRepiteAlMismoAlumnoAunqueSeAgregueDosVeces() {
        Estudiante ana = new Estudiante();

        grupo.agregarAlumno(ana);
        grupo.agregarAlumno(ana);

        assertEquals(1, grupo.getAlumnos().size());
    }

    @Test
    void quitarUnAlumnoLoDejaFueraDelGrupo() {
        Estudiante ana = new Estudiante();
        Estudiante luis = new Estudiante();
        grupo.agregarAlumno(ana);
        grupo.agregarAlumno(luis);

        grupo.quitarAlumno(ana);

        assertEquals(1, grupo.getAlumnos().size());
        assertFalse(grupo.getAlumnos().contains(ana));
        assertTrue(grupo.getAlumnos().contains(luis));
    }
}
