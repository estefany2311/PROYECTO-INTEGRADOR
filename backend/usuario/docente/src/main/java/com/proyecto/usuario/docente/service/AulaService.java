package com.proyecto.usuario.docente.service;

// Importaciones de los DTOs, Entidades y Repositorios necesarios
import com.proyecto.usuario.docente.dto.AulaRequest;
import com.proyecto.usuario.docente.dto.AulaResponse;
import com.proyecto.usuario.docente.dto.GradoResponse;
import com.proyecto.usuario.docente.model.Aula;
import com.proyecto.usuario.docente.model.Grado;
import com.proyecto.usuario.docente.repository.AulaRepository;
import com.proyecto.usuario.docente.repository.GradoRepository;

// Annotations de Spring
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import org.springframework.http.HttpStatus;
import java.util.Set;

// Le indica a Spring que esta clase es un componente de servicio (capa de lógica de negocio)
@Service
public class AulaService {

    // Inyección de dependencias de los repositorios requeridos
    private final AulaRepository aulaRepository;
    private final GradoRepository gradoRepository;

    // Constructor para inyectar los repositorios
    public AulaService(AulaRepository aulaRepository, GradoRepository gradoRepository) {
        this.aulaRepository = aulaRepository;
        this.gradoRepository = gradoRepository;
    }

    // @Transactional garantiza que todo el método se ejecute en una sola transacción de BD.
    // Si algo falla, se hace un rollback automático sin guardar datos a medias.
    @Transactional
    public AulaResponse crear(Long docenteId, AulaRequest req) {
        // Elimina espacios al inicio y final del nombre recibido
        String nombre = req.nombre().trim();

        // Verifica si el docente ya tiene un aula registrada con el mismo nombre
        if (aulaRepository.existsByDocenteIdAndNombreIgnoreCase(docenteId, nombre)) {
            // Lanza excepción de conflicto (HTTP 409) si ya existe
            throw new ReglaNegocioException("Ya tienes un aula con ese nombre", HttpStatus.CONFLICT);
        }

        // Convierte la lista de IDs a un Set para eliminar posibles duplicados
        Set<Short> ids = new HashSet<>(req.gradoIds());

        // Consulta en la base de datos los grados existentes segun los IDs proporcionados
        List<Grado> grados = gradoRepository.findAllById(ids);

        // Valida si la cantidad de grados encontrados es distinta a los IDs solicitados
        if (grados.size() != ids.size()) {
            // Lanza excepción Bad Request (HTTP 400) si al menos un grado no existe
            throw new ReglaNegocioException("Alguno de los grados no existe", HttpStatus.BAD_REQUEST);
        }

        // Instancia una nueva entidad Aula y asigna sus propiedades
        Aula aula = new Aula();
        aula.setNombre(nombre);
        aula.setDocenteId(docenteId);
        aula.setGrados(new HashSet<>(grados));

        // Guarda el aula en la base de datos y mapea la respuesta a DTO
        return toResponse(aulaRepository.save(aula));
    }

    // Método auxiliar privado para mapear/convertir un objeto entidad "Aula" a su DTO de respuesta "AulaResponse"
    private AulaResponse toResponse(Aula a) {
        // Convierte la lista/Set de entidades Grado en una lista de DTOs GradoResponse ordenada por el atributo 'orden'
        List<GradoResponse> grados = a.getGrados().stream()
                .sorted(Comparator.comparing(Grado::getOrden))
                .map(g -> new GradoResponse(g.getId(), g.getNombre()))
                .toList();

        // Retorna el objeto DTO listo para enviar al cliente
        return new AulaResponse(a.getId(), a.getNombre(), grados);
    }
}