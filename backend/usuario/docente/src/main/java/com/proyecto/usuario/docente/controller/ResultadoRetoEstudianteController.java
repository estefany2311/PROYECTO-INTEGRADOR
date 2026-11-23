package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.config.InterceptorTokenEstudiante;
import com.proyecto.usuario.docente.dto.ResultadoRetoResponse;
import com.proyecto.usuario.docente.service.ResultadoRetoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * La app de Karla cuenta una lectura u oralidad en los puntos y estrellas del
 * alumno cuando la evidencia pasa a "listo".
 * POST /api/estudiante/evidencias/{evidenciaId}/lectura con
 * Authorization: Bearer token. El id del alumno sale del token.
 *
 * <p>Es idempotente. Responde 409 si la lectura todavía se está procesando.</p>
 */
@RestController
@RequestMapping("/api/estudiante/evidencias")
@Tag(name = "Resultado de lectura", description = "Puntos y estrellas de una lectura u oralidad (token del alumno).")
@SecurityRequirement(name = "tokenEstudiante")
public class ResultadoRetoEstudianteController {

    private final ResultadoRetoService service;

    public ResultadoRetoEstudianteController(ResultadoRetoService service) {
        this.service = service;
    }

    @PostMapping("/{evidenciaId}/lectura")
    @Operation(summary = "Cuenta una lectura u oralidad del alumno autenticado en sus puntos y estrellas")
    public ResultadoRetoResponse registrar(
            @RequestAttribute(InterceptorTokenEstudiante.ATRIBUTO_ESTUDIANTE) Long estudianteId,
            @PathVariable Long evidenciaId) {
        return service.registrar(estudianteId, evidenciaId);
    }
}
