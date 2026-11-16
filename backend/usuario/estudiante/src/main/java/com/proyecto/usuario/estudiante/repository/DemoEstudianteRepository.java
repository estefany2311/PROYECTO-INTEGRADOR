package com.proyecto.usuario.estudiante.repository;

import com.proyecto.usuario.estudiante.model.Estudiante;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// Alumnos de prueba mientras la tabla estudiante no tenga PIN. Aula DEMO123, PIN 1234.
@Repository
public class DemoEstudianteRepository implements EstudianteRepository {

    public static final String CODIGO_AULA_DEMO = "DEMO123";
    public static final String PIN_DEMO = "1234";
    private static final String AVATAR = "https://api.dicebear.com/9.x/adventurer/png?seed=%s&backgroundColor=%s";

    private final List<Estudiante> estudiantes;

    public DemoEstudianteRepository(PasswordEncoder codificadorPin) {
        String hash = codificadorPin.encode(PIN_DEMO);   // así se guarda el PIN: cifrado
        estudiantes = List.of(
                new Estudiante(1, "Ana", AVATAR.formatted("Ana", "b6e3f4"), hash, true),
                new Estudiante(2, "Iker", AVATAR.formatted("Iker", "b6e3f4"), hash, true),
                new Estudiante(3, "Luis", AVATAR.formatted("Luis", "ffd5dc"), hash, true),
                new Estudiante(4, "Mía", AVATAR.formatted("Mia", "c0f5d8"), hash, true),
                new Estudiante(5, "Pedro", AVATAR.formatted("Pedro", "ffe8b6"), hash, true),
                new Estudiante(6, "Sofía", AVATAR.formatted("Sofia", "e0c3fc"), hash, true));
    }

    @Override
    public boolean existeAula(String codigoAula) {
        return CODIGO_AULA_DEMO.equalsIgnoreCase(codigoAula);
    }

    @Override
    public List<Estudiante> listarActivosPorCodigoAula(String codigoAula) {
        return existeAula(codigoAula) ? estudiantes : List.of();
    }

    @Override
    public Optional<Estudiante> buscarPorId(long id) {
        return estudiantes.stream().filter(e -> e.id() == id).findFirst();
    }
}