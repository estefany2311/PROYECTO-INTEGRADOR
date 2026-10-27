package com.proyecto.usuario.estudiante.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/*
 * Todas las respuestas de error tienen la forma {"mensaje": "..."}.
 * T-233 · Errores del PIN que lee la tablet (SesionRepository de la app):
 *   401 {"mensaje": "El PIN no es correcto", "intentosRestantes": 3}
 *   423 {"mensaje": "Demasiados intentos. Espera un momento", "segundosRestantes": 120}
 *       + cabecera Retry-After con los mismos segundos (estándar HTTP)
 */
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(CodigoAulaInvalidoException.class)
    public ResponseEntity<Map<String, Object>> codigoAula(CodigoAulaInvalidoException e) {
        return respuesta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(PinIncorrectoException.class)
    public ResponseEntity<Map<String, Object>> pinIncorrecto(PinIncorrectoException e) {
        Map<String, Object> cuerpo = cuerpo(e.getMessage());
        cuerpo.put("intentosRestantes", e.getIntentosRestantes());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(cuerpo);
    }

    // T-203 · 423 Locked: la cuenta está bloqueada temporalmente por muchos intentos fallidos
    // T-233 · Retry-After: cualquier cliente sabe cuántos segundos esperar sin leer el cuerpo
    @ExceptionHandler(CuentaBloqueadaException.class)
    public ResponseEntity<Map<String, Object>> cuentaBloqueada(CuentaBloqueadaException e) {
        Map<String, Object> cuerpo = cuerpo(e.getMessage());
        cuerpo.put("segundosRestantes", e.getSegundosRestantes());
        return ResponseEntity.status(HttpStatus.LOCKED)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(e.getSegundosRestantes()))
                .body(cuerpo);
    }

    // T-236 · 401: "Salir" sin la cabecera Authorization: Bearer <token>
    @ExceptionHandler(TokenFaltanteException.class)
    public ResponseEntity<Map<String, Object>> tokenFaltante(TokenFaltanteException e) {
        return respuesta(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException e) {
        String mensaje = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Datos inválidos");
        return respuesta(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> parametroFaltante(MissingServletRequestParameterException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "Falta el parámetro " + e.getParameterName());
    }

    // JSON mal formado o con tipos equivocados (por ejemplo "estudianteId": "abc")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> cuerpoIlegible(HttpMessageNotReadableException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "Datos inválidos");
    }

    static Map<String, Object> cuerpo(String mensaje) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("mensaje", mensaje);
        return cuerpo;
    }

    static ResponseEntity<Map<String, Object>> respuesta(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(cuerpo(mensaje));
    }
}