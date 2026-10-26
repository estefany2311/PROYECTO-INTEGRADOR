package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.LecturaResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Aula;
import com.proyecto.usuario.docente.model.Grado;
import com.proyecto.usuario.docente.model.Lectura;
import com.proyecto.usuario.docente.repository.AulaRepository;
import com.proyecto.usuario.docente.repository.LecturaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * T-189 (US-35): catálogo de lecturas filtrado por los grados del aula.
 * Solo se ofrecen lecturas ACTIVAS de los grados que atiende el aula;
 * además el docente puede filtrar por grado, dificultad o tipo de reto.
 */
@Service
public class CatalogoService {

    private final AulaRepository aulaRepository;
    private final LecturaRepository lecturaRepository;

    public CatalogoService(AulaRepository aulaRepository, LecturaRepository lecturaRepository) {
        this.aulaRepository = aulaRepository;
        this.lecturaRepository = lecturaRepository;
    }

    @Transactional(readOnly = true)
    public List<LecturaResponse> listar(Long docenteId, Long aulaId,
                                        Short gradoId, String dificultad, String tipoReto) {
        Aula aula = aulaRepository.findById(aulaId)
                .orElseThrow(() -> new ReglaNegocioException("El aula no existe", HttpStatus.NOT_FOUND));

        if (!aula.getDocenteId().equals(docenteId)) {
            throw new ReglaNegocioException("El aula no pertenece al docente", HttpStatus.FORBIDDEN);
        }

        // nombres de los grados del aula: sirven para mostrar "1.º" en la pantalla
        Map<Short, String> nombresGrado = aula.getGrados().stream()
                .collect(Collectors.toMap(Grado::getId, Grado::getNombre));

        if (nombresGrado.isEmpty()) {
            return List.of(); // el aula no tiene grados: no hay catálogo posible
        }

        return lecturaRepository.findByActivoTrueAndGradoInOrderByGradoAscTituloAsc(nombresGrado.keySet())
                .stream()
                .filter(l -> gradoId == null || gradoId.equals(l.getGrado()))
                .filter(l -> sinFiltro(dificultad) || dificultad.equalsIgnoreCase(l.getDificultad()))
                .filter(l -> sinFiltro(tipoReto) || tipoReto.equalsIgnoreCase(l.getTipoReto()))
                .map(l -> toResponse(l, nombresGrado))
                .toList();
    }

    private boolean sinFiltro(String valor) {
        return valor == null || valor.isBlank();
    }

    private LecturaResponse toResponse(Lectura l, Map<Short, String> nombresGrado) {
        return new LecturaResponse(l.getId(), l.getTitulo(), l.getTexto(), l.getTipoReto(),
                l.getDificultad(), l.getGrado(), nombresGrado.get(l.getGrado()), l.isActivo());
    }
}
