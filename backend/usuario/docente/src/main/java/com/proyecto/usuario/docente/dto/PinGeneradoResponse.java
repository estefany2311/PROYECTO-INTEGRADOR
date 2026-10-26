package com.proyecto.usuario.docente.dto;

// Único lugar donde el PIN viaja en claro: para que el docente se lo diga al alumno
public record PinGeneradoResponse(Long estudianteId, String pin) {

    // Si alguien lo imprime en un log, el PIN no se ve
    @Override
    public String toString() {
        return "PinGeneradoResponse[estudianteId=" + estudianteId + ", pin=****]";
    }
}