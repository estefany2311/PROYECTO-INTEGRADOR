package com.proyecto.usuario.docente.controller;

import com.proyecto.usuario.docente.config.InterceptorTokenEstudiante;
import com.proyecto.usuario.docente.dto.ResultadoEscrituraResponse;
import com.proyecto.usuario.docente.service.EscrituraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * T-403 (US-22): endpoint que consume la app de Karla en la pantalla de resultado
 * de escritura.
 * POST /api/estudiante/evidencias/{evidenciaId}/escritura con
 * Authorization: Bearer token. El id del alumno sale del token.
 *
 * <p>Calcula y guarda el puntaje la primera vez (y suma las estrellas); si se
 * vuelve a pedir devuelve el mismo resultado sin sumar de nuevo.</p>
 */
@RestController
@RequestMapping("/api/estudiante/evidencias")
@Tag(name = "Resultado de escritura", description = "Puntaje y retroalimentación de escritura que lee la tablet (token del alumno).")
@SecurityRequirement(name = "tokenEstudiante")
public class EscrituraEstudianteController {

    private final EscrituraService service;

    public EscrituraEstudianteController(EscrituraService service) {
        this.service = service;
    }

    @PostMapping("/{evidenciaId}/escritura")
    @Operation(summary = "Puntaje, retroalimentación y estrellas de un texto del alumno autenticado")
    public ResultadoEscrituraResponse evaluar(
            @RequestAttribute(InterceptorTokenEstudiante.ATRIBUTO_ESTUDIANTE) Long estudianteId,
            @PathVariable Long evidenciaId) {
        return service.evaluar(estudianteId, evidenciaId);
    }
}
