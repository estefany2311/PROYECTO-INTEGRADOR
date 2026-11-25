package com.proyecto.usuario.estudiante.repository;

import com.proyecto.usuario.estudiante.model.Estudiante;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// Alumnos reales del módulo docente (tablas aula, estudiante y avatar de Noemí).
// Reemplaza a la lista de prueba del Sprint 2 (aula DEMO123): el PIN lo pone el docente en su portal.
@Repository
public class JdbcEstudianteRepository implements EstudianteRepository {

    // Si el docente no le eligió avatar, uno con la inicial de su nombre
    private static final String AVATAR_POR_DEFECTO = "https://api.dicebear.com/9.x/adventurer/png?seed=%s&backgroundColor=b6e3f4";

    private static final RowMapper<Estudiante> ESTUDIANTE = (rs, n) -> {
        String nombre = rs.getString("nombres");
        String avatar = rs.getString("avatar_url");
        return new Estudiante(rs.getLong("id"), nombre,
                avatar != null ? avatar : AVATAR_POR_DEFECTO.formatted(nombre.replace(' ', '_')),
                rs.getString("pin_hash"), rs.getBoolean("activo"));
    };

    private static final String SELECT = """
            SELECT e.id, e.nombres, av.url AS avatar_url, e.pin_hash, e.activo
            FROM estudiante e
            JOIN aula a ON a.id = e.aula_id
            LEFT JOIN avatar av ON av.id = e.avatar_id
            """;

    private final JdbcClient jdbc;

    public JdbcEstudianteRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean existeAula(String codigoAula) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM aula WHERE upper(codigo) = upper(:codigo))")
                .param("codigo", codigoAula.trim())
                .query(Boolean.class)
                .single();
    }

    // Solo los que ya tienen PIN: sin PIN no podrían entrar, así que no aparecen en la tablet
    @Override
    public List<Estudiante> listarActivosPorCodigoAula(String codigoAula) {
        return jdbc.sql(SELECT + """
                        WHERE upper(a.codigo) = upper(:codigo) AND e.activo AND e.pin_hash IS NOT NULL
                        ORDER BY lower(e.nombres)
                        """)
                .param("codigo", codigoAula.trim())
                .query(ESTUDIANTE)
                .list();
    }

    @Override
    public Optional<Estudiante> buscarPorId(long id) {
        return jdbc.sql(SELECT + "WHERE e.id = :id")
                .param("id", id)
                .query(ESTUDIANTE)
                .optional();
    }
}
