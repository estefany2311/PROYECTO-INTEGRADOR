package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.AulaRequest;
import com.proyecto.usuario.docente.dto.AulaResponse;
import com.proyecto.usuario.docente.dto.GradoResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Aula;
import com.proyecto.usuario.docente.model.Grado;
import com.proyecto.usuario.docente.repository.AulaRepository;
import com.proyecto.usuario.docente.repository.GradoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AulaService {

    private final AulaRepository aulaRepository;
    private final GradoRepository gradoRepository;

    // Inyección de dependencias de los repositorios
    public AulaService(AulaRepository aulaRepository, GradoRepository gradoRepository) {
        this.aulaRepository = aulaRepository;
        this.gradoRepository = gradoRepository;
    }

    // Método para crear una nueva aula con sus validaciones correspondientes
    @Transactional
    public AulaResponse crear(Long docenteId, AulaRequest req) {
        // Elimina espacios en blanco al inicio y al final del nombre
        String nombre = req.nombre().trim();

        // Validar que el docente no tenga un aula con el mismo nombre (ignora mayúsculas y minúsculas)
        if (aulaRepository.existsByDocenteIdAndNombreIgnoreCase(docenteId, nombre)) {
            throw new ReglaNegocioException("Ya tienes un aula con ese nombre", HttpStatus.CONFLICT);
        }

        // Convertir la lista de IDs recibida a un Set para eliminar duplicados
        Set<Short> ids = new HashSet<>(req.gradoIds());

        // Buscar los grados existentes en la base de datos según los IDs enviados
        List<Grado> grados = gradoRepository.findAllById(ids);

        // Verificar que todos los grados enviados realmente existan en la base de datos
        if (grados.size() != ids.size()) {
            throw new ReglaNegocioException("Alguno de los grados no existe", HttpStatus.BAD_REQUEST);
        }

        // Crear la entidad Aula y mapear sus campos
        Aula aula = new Aula();
        aula.setNombre(nombre);
        aula.setDocenteId(docenteId);
        aula.setGrados(new HashSet<>(grados));

        // Guardar el aula registrada y retornar el DTO de respuesta
        return toResponse(aulaRepository.save(aula));
    }

    // Listar las aulas pertenecientes a un docente en específico, ordenadas por fecha de creación descendente
    @Transactional(readOnly = true)
    public List<AulaResponse> listar(Long docenteId) {
        return aulaRepository.findByDocenteIdOrderByCreadoEnDesc(docenteId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Listar todos los grados del sistema ordenados por el campo 'orden'
    @Transactional(readOnly = true)
    public List<GradoResponse> listarGrados() {
        return gradoRepository.findAll(Sort.by("orden"))
                .stream()
                .map(g -> new GradoResponse(g.getId(), g.getNombre()))
                .toList();
    }

    // Método auxiliar para convertir la entidad Aula al DTO AulaResponse
    private AulaResponse toResponse(Aula a) {
        // Mapear y ordenar los grados asociados al aula por su propiedad 'orden'
        List<GradoResponse> grados = a.getGrados().stream()
                .sorted(Comparator.comparing(Grado::getOrden))
                .map(g -> new GradoResponse(g.getId(), g.getNombre()))
                .toList();

        return new AulaResponse(a.getId(), a.getNombre(), grados);
    }
}