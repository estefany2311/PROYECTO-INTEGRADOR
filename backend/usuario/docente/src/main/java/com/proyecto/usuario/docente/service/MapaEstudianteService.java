package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.MapaResponse;
import com.proyecto.usuario.docente.dto.NivelMapaResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Estudiante;
import com.proyecto.usuario.docente.model.Nivel;
import com.proyecto.usuario.docente.model.PuntajeEstudiante;
import com.proyecto.usuario.docente.model.Progreso;
import com.proyecto.usuario.docente.repository.EstudianteRepository;
import com.proyecto.usuario.docente.repository.NivelRepository;
import com.proyecto.usuario.docente.repository.ProgresoRepository;
import com.proyecto.usuario.docente.repository.PuntajeEstudianteRepository;
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
    private final PuntajeEstudianteRepository puntajeEstudianteRepository;

    public MapaEstudianteService(EstudianteRepository estudianteRepository,
                                 NivelRepository nivelRepository,
                                 ProgresoRepository progresoRepository,
                                 PuntajeEstudianteRepository puntajeEstudianteRepository) {
        this.estudianteRepository = estudianteRepository;
        this.nivelRepository = nivelRepository;
        this.progresoRepository = progresoRepository;
        this.puntajeEstudianteRepository = puntajeEstudianteRepository;
    }

    @Transactional(readOnly = true)
    public MapaResponse obtenerMapa(Long estudianteId) {
        Estudiante estudiante = buscarEstudiante(estudianteId);
        List<NivelMapaResponse> mapa = nivelesDe(estudianteId);

        int completados = (int) mapa.stream()
                .filter(nivel -> Progreso.COMPLETADO.equals(nivel.estado()))
                .count();
        int puntosProgreso = mapa.stream().mapToInt(NivelMapaResponse::puntos).sum();

        // T-303 (US-09): la cabecera del mapa muestra el acumulado de la tabla
        // puntaje_estudiante. Si el alumno todavía no tiene fila (aún no completó
        // un reto desde T-302), se mantiene la suma de puntos del progreso.
        PuntajeEstudiante acumulado = puntajeEstudianteRepository
                .findByEstudianteId(estudianteId)
                .orElse(null);
        int puntos = acumulado != null ? acumulado.getPuntos() : puntosProgreso;
        int estrellas = acumulado != null ? acumulado.getEstrellas() : 0;

        return new MapaResponse(estudiante.getId(), estudiante.getNombres(), estudiante.getApellidos(),
                mapa.size(), completados, puntos, estrellas, mapa);
    }

    /**
     * T-290 (US-29): reutiliza la regla del mapa para el detalle del estudiante.
     * Devuelve los niveles activos en orden con su estado y puntos.
     */
    @Transactional(readOnly = true)
    public List<NivelMapaResponse> nivelesDe(Long estudianteId) {
        buscarEstudiante(estudianteId);
        List<Nivel> niveles = nivelRepository.findByActivoTrueOrderByOrdenAsc();
        Map<Short, Progreso> guardados = progresoRepository.findByEstudianteId(estudianteId)
                .stream()
                .collect(Collectors.toMap(p -> p.getNivel().getId(), Function.identity(), (a, b) -> a));

        List<NivelMapaResponse> mapa = new ArrayList<>();
        boolean anteriorCompletado = false;

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

            mapa.add(new NivelMapaResponse(nivel.getId(), nivel.getOrden(), nivel.getNombre(),
                    nivel.getTipoReto(), nivel.getDescripcion(), estado, puntosNivel));

            anteriorCompletado = Progreso.COMPLETADO.equals(estado);
        }

        return mapa;
    }

    private Estudiante buscarEstudiante(Long estudianteId) {
        return estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new ReglaNegocioException("El alumno no existe", HttpStatus.NOT_FOUND));
    }
}
