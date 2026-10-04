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
        // 1. Instanciar una nueva entidad Aula
        Aula aula = new Aula();

        // 2. Asignar el nombre limpiando espacios en blanco al inicio y final
        aula.setNombre(req.nombre().trim());

        // 3. Vincular el ID del docente correspondiente
        aula.setDocenteId(docenteId);

        // 4. Buscar en la BD los objetos Grado que coinciden con la lista de IDs enviados en la petición (req.gradoIds())
        // y asignarlos al Set de grados del aula
        aula.setGrados(new HashSet<>(gradoRepository.findAllById(req.gradoIds())));

        // 5. Guardar el aula en la base de datos (aulaRepository.save) y convertir la entidad guardada a un DTO de respuesta
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