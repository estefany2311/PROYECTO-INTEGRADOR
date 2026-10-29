package com.proyecto.usuario.docente.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice // Intercepta las excepciones lanzadas en los controladores REST
public class ManejadorErrores {

    // Maneja las excepciones personalizadas de regla de negocio
    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<Map<String, String>> regla(ReglaNegocioException e) {
        // Retorna la respuesta con el estado HTTP de la excepción y el mensaje JSON
        return ResponseEntity.status(e.getEstado()).body(Map.of("mensaje", e.getMessage()));
    }

    // Maneja los errores de validación causados por @Valid en el DTO
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validacion(MethodArgumentNotValidException e) {
        // Recoge todos los mensajes de error de los campos y los junta separados por un punto
        String mensaje = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getDefaultMessage())
                .collect(Collectors.joining(". "));

        // Retorna una respuesta 400 Bad Request con el mensaje compilado
        return ResponseEntity.badRequest().body(Map.of("mensaje", mensaje));
    }
}