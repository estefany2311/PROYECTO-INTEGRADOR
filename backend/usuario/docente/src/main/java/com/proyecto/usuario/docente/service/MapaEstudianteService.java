package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.MapaResponse;
import com.proyecto.usuario.docente.dto.NivelMapaResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Estudiante;
import com.proyecto.usuario.docente.model.Nivel;
import com.proyecto.usuario.docente.model.Progreso;
import com.proyecto.usuario.docente.repository.EstudianteRepository;
import com.proyecto.usuario.docente.repository.NivelRepository;
import com.proyecto.usuario.docente.repository.ProgresoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * T-197 (US-05): arma el mapa de aventura del alumno: los niveles en orden
 * y el estado de cada uno según su progreso.
 *
 * Regla del mapa:
 *  - si el alumno ya tiene una fila de progreso, manda esa fila;
 *  - si no tiene fila: el primer nivel está desbloqueado y el resto bloqueado;
 *  - un nivel se desbloquea solo cuando el nivel anterior está completado.
 */
@Service
public class MapaEstudianteService {

    private final EstudianteRepository estudianteRepository;
    private final NivelRepository nivelRepository;
    private final ProgresoRepository progresoRepository;

    public MapaEstudianteService(EstudianteRepository estudianteRepository,
                                 NivelRepository nivelRepository,
                                 ProgresoRepository progresoRepository) {
        this.estudianteRepository = estudianteRepository;
        this.nivelRepository = nivelRepository;
        this.progresoRepository = progresoRepository;
    }

    @Transactional(readOnly = true)
    public MapaResponse obtenerMapa(Long estudianteId) {
        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new ReglaNegocioException("El alumno no existe", HttpStatus.NOT_FOUND));

        List<Nivel> niveles = nivelRepository.findByActivoTrueOrderByOrdenAsc();
        Map<Short, Progreso> guardados = progresoRepository.findByEstudianteId(estudianteId)
                .stream()
                .collect(Collectors.toMap(p -> p.getNivel().getId(), Function.identity(), (a, b) -> a));

        List<NivelMapaResponse> mapa = new ArrayList<>();
        boolean anteriorCompletado = false;
        int completados = 0;
        int puntos = 0;

        for (int i = 0; i < niveles.size(); i++) {
            Nivel nivel = niveles.get(i);
            Progreso progreso = guardados.get(nivel.getId());

            String estado;
            if (progreso != null) {
                estado = progreso.getEstado();          // el progreso guardado manda
            } else if (i == 0 || anteriorCompletado) {
                estado = Progreso.DESBLOQUEADO;         // primero del mapa o siguiente liberado
            } else {
                estado = Progreso.BLOQUEADO;
            }

            int puntosNivel = progreso != null ? progreso.getPuntos() : 0;
            if (Progreso.COMPLETADO.equals(estado)) completados++;
            puntos += puntosNivel;

            mapa.add(new NivelMapaResponse(nivel.getId(), nivel.getOrden(), nivel.getNombre(),
                    nivel.getTipoReto(), nivel.getDescripcion(), estado, puntosNivel));

            anteriorCompletado = Progreso.COMPLETADO.equals(estado);
        }

        return new MapaResponse(estudiante.getId(), estudiante.getNombres(), estudiante.getApellidos(),
                mapa.size(), completados, puntos, mapa);
    }
}
