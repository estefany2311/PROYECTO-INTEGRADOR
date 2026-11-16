package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.AulaRequest;
import com.proyecto.usuario.docente.dto.AulaResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Aula;
import com.proyecto.usuario.docente.model.Grado;
import com.proyecto.usuario.docente.repository.AulaRepository;
import com.proyecto.usuario.docente.repository.GradoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AulaServiceTest {

    @Mock
    AulaRepository aulaRepository;

    @Mock
    GradoRepository gradoRepository;

    @InjectMocks
    AulaService service;

    // Prueba el flujo correcto al crear un aula: limpia espacios en blanco y asigna grados correctamente
    @Test
    void creaAulaConGradosYLimpiaElNombre() {
        Grado g1 = new Grado((short) 1, "1.º", (short) 1);
        when(aulaRepository.existsByDocenteIdAndNombreIgnoreCase(1L, "Aula Rural")).thenReturn(false);
        when(gradoRepository.findAllById(any())).thenReturn(List.of(g1));
        when(aulaRepository.save(any(Aula.class))).thenAnswer(i -> i.getArgument(0));

        AulaResponse resp = service.crear(1L, new AulaRequest("  Aula Rural ", List.of((short) 1)));

        assertEquals("Aula Rural", resp.nombre());
        assertEquals(1, resp.grados().size());
        assertEquals("1.º", resp.grados().get(0).nombre());
    }

    // Verifica que se lance un error 409 CONFLICT cuando el docente intenta registrar un nombre repetido
    @Test
    void rechazaNombreRepetido() {
        when(aulaRepository.existsByDocenteIdAndNombreIgnoreCase(1L, "Aula Rural")).thenReturn(true);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.crear(1L, new AulaRequest("Aula Rural", List.of((short) 1))));

        assertEquals(HttpStatus.CONFLICT, ex.getEstado());
        verify(aulaRepository, never()).save(any());
    }

    // Verifica que se lance un error 400 BAD_REQUEST al intentar asociar un ID de grado que no existe en la BD
    @Test
    void rechazaGradoInexistente() {
        Grado g1 = new Grado((short) 1, "1.º", (short) 1);
        when(gradoRepository.findAllById(any())).thenReturn(List.of(g1));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.crear(1L, new AulaRequest("Aula Nueva", List.of((short) 1, (short) 99))));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getEstado());
        verify(aulaRepository, never()).save(any());
    }

    // Prueba la consulta exitosa de las aulas pertenecientes a un docente específico
    @Test
    void listaLasAulasDelDocente() {
        Grado g1 = new Grado((short) 1, "1.º", (short) 1);
        Aula aula = new Aula();
        aula.setNombre("Aula Norte");
        aula.setDocenteId(1L);
        Set<Grado> grados = new HashSet<>();
        grados.add(g1);
        aula.setGrados(grados);

        when(aulaRepository.findByDocenteIdOrderByCreadoEnDesc(1L)).thenReturn(List.of(aula));

        List<AulaResponse> resultado = service.listar(1L);

        assertEquals(1, resultado.size());
        assertEquals("Aula Norte", resultado.get(0).nombre());
    }

    // Verifica que devuelva una lista vacía si el docente no tiene ninguna aula registrada
    @Test
    void docenteSinAulasObtieneListaVacia() {
        when(aulaRepository.findByDocenteIdOrderByCreadoEnDesc(2L)).thenReturn(List.of());

        assertEquals(0, service.listar(2L).size());
    }
}