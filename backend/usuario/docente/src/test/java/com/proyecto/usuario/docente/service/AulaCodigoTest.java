package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.AulaRequest;
import com.proyecto.usuario.docente.dto.AulaResponse;
import com.proyecto.usuario.docente.model.Aula;
import com.proyecto.usuario.docente.model.Grado;
import com.proyecto.usuario.docente.repository.AulaRepository;
import com.proyecto.usuario.docente.repository.GradoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Código de aula: 6 caracteres sin O/0/I/1, no repetido
@ExtendWith(MockitoExtension.class)
class AulaCodigoTest {

    @Mock AulaRepository aulaRepository;
    @Mock GradoRepository gradoRepository;
    @InjectMocks AulaService service;

    private void prepararCreacion() {
        Grado g1 = new Grado((short) 1, "1.º", (short) 1);
        when(aulaRepository.existsByDocenteIdAndNombreIgnoreCase(anyLong(), anyString())).thenReturn(false);
        when(gradoRepository.findAllById(any())).thenReturn(List.of(g1));
        when(aulaRepository.save(any(Aula.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void alCrearAula_generaCodigoDe6CaracteresSinConfusiones() {
        prepararCreacion();
        when(aulaRepository.existsByCodigo(anyString())).thenReturn(false);

        AulaResponse resp = service.crear(1L, new AulaRequest("Aula Rural", List.of((short) 1)));

        // A-Z sin I ni O, y dígitos del 2 al 9
        assertTrue(resp.codigo().matches("[A-HJ-NP-Z2-9]{6}"), "código inválido: " + resp.codigo());
    }

    @Test
    void siElCodigoYaExiste_generaOtro() {
        prepararCreacion();
        // primer intento: ocupado; segundo: libre
        when(aulaRepository.existsByCodigo(anyString())).thenReturn(true, false);

        AulaResponse resp = service.crear(1L, new AulaRequest("Aula Rural", List.of((short) 1)));

        verify(aulaRepository, times(2)).existsByCodigo(anyString());
        assertEquals(6, resp.codigo().length());
    }

    @Test
    void listar_incluyeElCodigoDelAula() {
        Aula aula = new Aula();
        aula.setNombre("Aula Norte");
        aula.setDocenteId(1L);
        aula.setCodigo("K7M2QX");
        Set<Grado> grados = new HashSet<>();
        grados.add(new Grado((short) 1, "1.º", (short) 1));
        aula.setGrados(grados);
        when(aulaRepository.findByDocenteIdOrderByCreadoEnDesc(1L)).thenReturn(List.of(aula));

        assertEquals("K7M2QX", service.listar(1L).get(0).codigo());
    }
}