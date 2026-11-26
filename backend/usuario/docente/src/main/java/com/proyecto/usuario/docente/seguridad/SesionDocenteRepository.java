package com.proyecto.usuario.docente.seguridad;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Login docente: las sesiones abiertas (tabla {@code sesion_docente}, V22).
 *
 * <p>Una sesión solo sirve mientras la cuenta siga activa y con rol DOCENTE:
 * si el administrador la desactiva, deja de entrar en la siguiente petición.</p>
 */
@Repository
public class SesionDocenteRepository {

    // la cuenta de la sesión sigue siendo una cuenta docente activa
    private static final String CUENTA_VIGENTE =
            "EXISTS (SELECT 1 FROM usuarios u JOIN roles r ON r.id = u.rol_id "
                    + "WHERE u.id = s.docente_id AND u.is_active AND r.nombre = 'DOCENTE')";

    private final JdbcTemplate jdbc;

    public SesionDocenteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void crear(Long docenteId, String accesoHash, LocalDateTime accesoExpira,
                      String refreshHash, LocalDateTime refreshExpira) {
        jdbc.update("INSERT INTO sesion_docente (docente_id, acceso_hash, acceso_expira, "
                        + "refresh_hash, refresh_expira) VALUES (?, ?, ?, ?, ?)",
                docenteId, accesoHash, accesoExpira, refreshHash, refreshExpira);
    }

    public Optional<Long> docentePorAcceso(String accesoHash, LocalDateTime ahora) {
        List<Long> ids = jdbc.queryForList(
                "SELECT s.docente_id FROM sesion_docente s "
                        + "WHERE s.acceso_hash = ? AND s.acceso_expira > ? AND " + CUENTA_VIGENTE,
                Long.class, accesoHash, ahora);
        return ids.stream().findFirst();
    }

    /** Cambia el token de acceso. @return false si la sesión ya no sirve */
    public boolean renovarAcceso(String refreshHash, String nuevoAccesoHash,
                                 LocalDateTime accesoExpira, LocalDateTime ahora) {
        return jdbc.update(
                "UPDATE sesion_docente s SET acceso_hash = ?, acceso_expira = ? "
                        + "WHERE s.refresh_hash = ? AND s.refresh_expira > ? AND " + CUENTA_VIGENTE,
                nuevoAccesoHash, accesoExpira, refreshHash, ahora) == 1;
    }

    public void cerrar(String refreshHash) {
        jdbc.update("DELETE FROM sesion_docente WHERE refresh_hash = ?", refreshHash);
    }

    public void eliminarVencidas(LocalDateTime ahora) {
        jdbc.update("DELETE FROM sesion_docente WHERE refresh_expira <= ?", ahora);
    }
}
