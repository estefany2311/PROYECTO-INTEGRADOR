package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.SeguimientoTareasResponse;
import com.proyecto.usuario.docente.dto.TareaSeguimientoResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Asignacion;
import com.proyecto.usuario.docente.model.Aula;
import com.proyecto.usuario.docente.model.Lectura;
import com.proyecto.usuario.docente.repository.AsignacionRepository;
import com.proyecto.usuario.docente.repository.AulaRepository;
import com.proyecto.usuario.docente.repository.LecturaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * T-389 (US-38): seguimiento de las tareas asignadas del aula.
 *
 * <p>El estado que ve el docente se calcula con la fecha de hoy:</p>
 * <ul>
 *   <li>{@code entregada}: el alumno ya entregó;</li>
 *   <li>{@code vencida}: sigue pendiente y su fecha límite ya pasó (el mismo día
 *       de la fecha límite todavía cuenta como pendiente);</li>
 *   <li>{@code pendiente}: el resto, incluidas las tareas sin fecha límite.</li>
 * </ul>
 * <p>Se listan primero las vencidas, luego las pendientes y al final las
 * entregadas; dentro de cada grupo, la fecha límite más cercana primero y las
 * tareas sin fecha al final.</p>
 */
@Service
public class SeguimientoTareasService {

    public static final String PENDIENTE = "pendiente";
    public static final String ENTREGADA = "entregada";
    public static final String VENCIDA = "vencida";

    private static final Set<String> ESTADOS = Set.of(PENDIENTE, ENTREGADA, VENCIDA);
    private static final List<String> ORDEN_ESTADOS = List.of(VENCIDA, PENDIENTE, ENTREGADA);

    private final AsignacionRepository asignacionRepository;
    private final AulaRepository aulaRepository;
    private final LecturaRepository lecturaRepository;

    public SeguimientoTareasService(AsignacionRepository asignacionRepository,
                                    AulaRepository aulaRepository,
                                    LecturaRepository lecturaRepository) {
        this.asignacionRepository = asignacionRepository;
        this.aulaRepository = aulaRepository;
        this.lecturaRepository = lecturaRepository;
    }

    @Transactional(readOnly = true)
    public SeguimientoTareasResponse seguimiento(Long docenteId, Long aulaId, String estado) {
        return seguimiento(docenteId, aulaId, estado, LocalDate.now());
    }

    // "hoy" se recibe para poder probar las fechas sin depender del reloj
    @Transactional(readOnly = true)
    public SeguimientoTareasResponse seguimiento(Long docenteId, Long aulaId, String estado,
                                                 LocalDate hoy) {
        buscarAulaDelDocente(docenteId, aulaId);
        String filtro = normalizarFiltro(estado);

        List<Asignacion> asignaciones = asignacionRepository.findByEstudianteAulaId(aulaId);
        Map<Long, Lectura> lecturas = lecturaRepository
                .findAllById(asignaciones.stream().map(Asignacion::getLecturaId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Lectura::getId, Function.identity()));

        List<TareaSeguimientoResponse> todas = asignaciones.stream()
                .map(asignacion -> aTarea(asignacion, lecturas.get(asignacion.getLecturaId()), hoy))
                .sorted(Comparator
                        .comparingInt((TareaSeguimientoResponse t) -> ORDEN_ESTADOS.indexOf(t.estado()))
                        .thenComparing(TareaSeguimientoResponse::fechaLimite,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(TareaSeguimientoResponse::apellidos)
                        .thenComparing(TareaSeguimientoResponse::nombres))
                .toList();

        return new SeguimientoTareasResponse(
                contar(todas, PENDIENTE),
                contar(todas, ENTREGADA),
                contar(todas, VENCIDA),
                filtro == null ? todas : todas.stream().filter(t -> t.estado().equals(filtro)).toList());
    }

    // T-389: estado visible de una tarea según la fecha de hoy
    public static String estadoDe(Asignacion asignacion, LocalDate hoy) {
        if (Asignacion.ENTREGADA.equals(asignacion.getEstado())) {
            return ENTREGADA;
        }
        LocalDate limite = asignacion.getFechaLimite();
        return limite != null && limite.isBefore(hoy) ? VENCIDA : PENDIENTE;
    }

    private static Boolean entregadaATiempo(Asignacion asignacion) {
        if (!Asignacion.ENTREGADA.equals(asignacion.getEstado())) {
            return null;
        }
        LocalDate limite = asignacion.getFechaLimite();
        return limite == null || !asignacion.getEntregadaEn().toLocalDate().isAfter(limite);
    }

    private static TareaSeguimientoResponse aTarea(Asignacion asignacion, Lectura lectura,
                                                   LocalDate hoy) {
        var alumno = asignacion.getEstudiante();
        return new TareaSeguimientoResponse(
                asignacion.getId(),
                asignacion.getLecturaId(),
                lectura != null ? lectura.getTitulo() : "Lectura no disponible",
                alumno.getId(),
                alumno.getNombres(),
                alumno.getApellidos(),
                alumno.getGrado() != null ? alumno.getGrado().getNombre() : null,
                asignacion.getFechaAsignacion(),
                asignacion.getFechaLimite(),
                estadoDe(asignacion, hoy),
                asignacion.getEntregadaEn(),
                entregadaATiempo(asignacion));
    }

    private static int contar(List<TareaSeguimientoResponse> tareas, String estado) {
        return (int) tareas.stream().filter(t -> t.estado().equals(estado)).count();
    }

    private static String normalizarFiltro(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }
        String limpio = estado.trim().toLowerCase();
        if (!ESTADOS.contains(limpio)) {
            throw new ReglaNegocioException(
                    "El estado debe ser pendiente, entregada o vencida", HttpStatus.BAD_REQUEST);
        }
        return limpio;
    }

    // valida que el aula exista y pertenezca al docente autenticado
    private Aula buscarAulaDelDocente(Long docenteId, Long aulaId) {
        Aula aula = aulaRepository.findById(aulaId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "El aula no existe", HttpStatus.NOT_FOUND));
        if (!aula.getDocenteId().equals(docenteId)) {
            throw new ReglaNegocioException(
                    "El aula no pertenece al docente", HttpStatus.FORBIDDEN);
        }
        return aula;
    }
}
