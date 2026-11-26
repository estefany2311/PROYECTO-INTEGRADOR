package com.proyecto.usuario.docente.repository;

import com.proyecto.usuario.docente.dto.LibroResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Biblioteca: lee los libros oficiales que sincroniza el módulo Admin (Daniela).
 *
 * <p>La tabla {@code recursos_oficiales} es suya: aquí <b>solo se lee</b>, con
 * SQL directo. Si la tabla todavía no existe en la base (o no tiene las columnas
 * esperadas, por ejemplo en el CI) la biblioteca sale vacía. Cualquier otro
 * fallo de la base sí se propaga como error.</p>
 */
@Repository
public class LibroRepository {

    private static final Logger log = LoggerFactory.getLogger(LibroRepository.class);

    private static final String CONSULTA =
            "SELECT id, titulo, descripcion, grado, competencia, url_pdf FROM recursos_oficiales";
    private static final String ORDEN = " ORDER BY grado NULLS LAST, titulo, id";

    private static final RowMapper<LibroResponse> LIBRO = (rs, i) -> {
        int grado = rs.getInt("grado");
        Integer gradoONulo = rs.wasNull() ? null : grado;
        String urlPdf = rs.getString("url_pdf");
        return new LibroResponse(rs.getLong("id"), rs.getString("titulo"),
                rs.getString("descripcion"), gradoONulo, rs.getString("competencia"),
                urlPdf != null && !urlPdf.isBlank());
    };

    private final JdbcTemplate jdbc;

    public LibroRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** @param grado 1 a 6, o nulo para todos */
    public List<LibroResponse> listar(Integer grado) {
        try {
            return grado == null
                    ? jdbc.query(CONSULTA + ORDEN, LIBRO)
                    : jdbc.query(CONSULTA + " WHERE grado = ?" + ORDEN, LIBRO, grado);
        } catch (BadSqlGrammarException e) {
            log.warn("No se pudo leer la tabla de libros oficiales; la biblioteca sale vacía", e);
            return List.of();
        }
    }
}
