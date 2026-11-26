package com.proyecto.usuario.docente.seguridad;

/** Cuenta de la tabla {@code usuarios} del módulo Admin, con el nombre de su rol. */
public record CuentaDocente(
        Long id,
        String usuario,
        String contrasenaCifrada,
        boolean activa,
        String nombres,
        String apellidos,
        String rol
) {
    public String nombreCompleto() {
        String nombre = ((nombres == null ? "" : nombres) + " "
                + (apellidos == null ? "" : apellidos)).trim();
        return nombre.isEmpty() ? usuario : nombre;
    }
}
