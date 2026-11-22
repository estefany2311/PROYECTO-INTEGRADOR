package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.dto.ErrorOrtografico;
import com.proyecto.usuario.estudiante.dto.VerificarTextoResponse;
import com.proyecto.usuario.estudiante.model.ReglasOrtografia;
import com.proyecto.usuario.estudiante.repository.OrtografiaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

// T-328 (US-19): verifica el texto con la configuración del grado del alumno (US-52)
@Service
public class OrtografiaService {

    private final OrtografiaRepository repositorio;
    private final VerificadorOrtografico verificador;

    public OrtografiaService(OrtografiaRepository repositorio, VerificadorOrtografico verificador) {
        this.repositorio = repositorio;
        this.verificador = verificador;
    }

    public VerificarTextoResponse verificar(long estudianteId, String texto) {
        ReglasOrtografia reglas = repositorio.reglasDe(estudianteId).orElse(ReglasOrtografia.POR_DEFECTO);
        List<ErrorOrtografico> errores = verificador.verificar(texto, reglas, repositorio.palabrasPermitidas(estudianteId));
        return new VerificarTextoResponse(errores, reglas.sensibilidad(), reglas.ignorarTildes());
    }
}
