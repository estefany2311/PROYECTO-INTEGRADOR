package com.proyecto.usuario.docente.seguridad;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Login docente: lee las cuentas del módulo Admin (Daniela).
 *
 * <p>Las tablas {@code usuarios} y {@code roles} son suyas: aquí <b>solo se
 * leen</b>, con SQL directo. Las cuentas se siguen creando y desactivando desde
 * el portal de administración.</p>
 */
@Repository
public class CuentaRepository {

    private final JdbcTemplate jdbc;

    public CuentaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<CuentaDocente> porUsuario(String usuario) {
        return jdbc.query(
                        "SELECT u.id, u.username, u.password, u.is_active, u.first_name, "
                                + "u.last_name, r.nombre AS rol "
                                + "FROM usuarios u LEFT JOIN roles r ON r.id = u.rol_id "
                                + "WHERE u.username = ?",
                        (rs, i) -> new CuentaDocente(
                                rs.getLong("id"), rs.getString("username"), rs.getString("password"),
                                rs.getBoolean("is_active"), rs.getString("first_name"),
                                rs.getString("last_name"), rs.getString("rol")),
                        usuario)
                .stream()
                .findFirst();
    }
}
