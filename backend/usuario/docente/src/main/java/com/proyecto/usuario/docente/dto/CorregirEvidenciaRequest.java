package com.proyecto.usuario.docente.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * T-378 (US-33): datos para corregir la calificación que emitió la IA.
 *
 * <p>La nota final es obligatoria y va de 0 a 100. El motivo también es
 * obligatorio para que el alumno y el docente sepan por qué cambió la nota;
 * se guarda junto con la evidencia.</p>
 */
public record CorregirEvidenciaRequest(
        @NotNull(message = "Debes indicar la calificación final")
        @Min(value = 0, message = "La calificación final no puede ser menor que 0")
        @Max(value = 100, message = "La calificación final no puede ser mayor que 100")
        Short calificacionFinal,

        @NotBlank(message = "Debes indicar el motivo de la corrección")
        @Size(max = 500, message = "El motivo no puede superar los 500 caracteres")
        String motivo
) {
}
