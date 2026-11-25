package com.proyecto.usuario.estudiante.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.LinkedHashMap;
import java.util.Map;

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

    // T-214 · 401: el token no existe, venció o se cerró con "Salir"
    @ExceptionHandler(SesionVencidaException.class)
    public ResponseEntity<Map<String, Object>> sesionVencida(SesionVencidaException e) {
        return respuesta(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    // T-214 · 403: nivel bloqueado. Incluye qué nivel terminar antes, para el mensaje de la tablet
    @ExceptionHandler(NivelBloqueadoException.class)
    public ResponseEntity<Map<String, Object>> nivelBloqueado(NivelBloqueadoException e) {
        Map<String, Object> cuerpo = cuerpo(e.getMessage());
        if (e.getNivelAnterior() != null) {
            cuerpo.put("nivelAnterior", e.getNivelAnterior());
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(cuerpo);
    }

    // T-214 · 404: el nivel no existe o está desactivado
    @ExceptionHandler(NivelNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> nivelNoEncontrado(NivelNoEncontradoException e) {
        return respuesta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // T-218 · 404: el nivel está abierto pero todavía no hay lectura para el grado del alumno
    @ExceptionHandler(LecturaNoDisponibleException.class)
    public ResponseEntity<Map<String, Object>> lecturaNoDisponible(LecturaNoDisponibleException e) {
        return respuesta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // T-320 · 404: el nivel está abierto pero no es de escritura o todavía no tiene consigna
    @ExceptionHandler(ConsignaNoDisponibleException.class)
    public ResponseEntity<Map<String, Object>> consignaNoDisponible(ConsignaNoDisponibleException e) {
        return respuesta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // T-413 · 404: la lectura que se grabó no existe
    @ExceptionHandler(LecturaNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> lecturaNoEncontrada(LecturaNoEncontradaException e) {
        return respuesta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // T-413 · 400: audio vacío, muy corto, muy largo o nivel de escritura
    @ExceptionHandler(AudioInvalidoException.class)
    public ResponseEntity<Map<String, Object>> audioInvalido(AudioInvalidoException e) {
        return respuesta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    // T-434 · 400: el nivel no es de escritura o al texto le faltan palabras
    @ExceptionHandler(TextoInvalidoException.class)
    public ResponseEntity<Map<String, Object>> textoInvalido(TextoInvalidoException e) {
        return respuesta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    // T-413 · 415: no es un WAV mono de 16 kHz y 16 bits
    @ExceptionHandler(FormatoAudioNoSoportadoException.class)
    public ResponseEntity<Map<String, Object>> formatoAudio(FormatoAudioNoSoportadoException e) {
        return respuesta(HttpStatus.UNSUPPORTED_MEDIA_TYPE, e.getMessage());
    }

    // T-413 · 413: el archivo pasa el límite de spring.servlet.multipart.max-file-size
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> audioMuyGrande(MaxUploadSizeExceededException e) {
        return respuesta(HttpStatus.CONTENT_TOO_LARGE, "La grabación es demasiado grande");
    }

    // T-416 · 404: la grabación no existe o es de otro alumno
    @ExceptionHandler(EvidenciaNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> evidenciaNoEncontrada(EvidenciaNoEncontradaException e) {
        return respuesta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // T-415 · 503: el servicio de voz a texto no responde
    @ExceptionHandler(ServicioVozNoDisponibleException.class)
    public ResponseEntity<Map<String, Object>> servicioVoz(ServicioVozNoDisponibleException e) {
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, "No pudimos escuchar tu lectura ahora. Intenta en un momento");
    }

    // T-413 · 400: falta la parte "audio" del formulario
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> parteFaltante(MissingServletRequestPartException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "Falta el " + e.getRequestPartName());
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