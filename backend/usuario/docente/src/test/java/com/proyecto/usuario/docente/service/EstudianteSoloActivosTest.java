package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.EstudianteResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Aula;
import com.proyecto.usuario.docente.model.Estudiante;
import com.proyecto.usuario.docente.model.Grado;
import com.proyecto.usuario.docente.repository.AulaRepository;
import com.proyecto.usuario.docente.repository.EstudianteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

// T-184: el listado para la tablet solo trae alumnos activos
@ExtendWith(MockitoExtension.class)
class EstudianteSoloActivosTest {

    @Mock EstudianteRepository estudianteRepository;
    @Mock AulaRepository aulaRepository;
    @InjectMocks EstudianteService service;

    @Test
    void listarActivos_usaLaConsultaDeActivosYDevuelveSoloEsos() {
        Aula aula = mock(Aula.class);
        when(aula.getDocenteId()).thenReturn(1L);
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));

        Grado grado = mock(Grado.class);
        when(grado.getId()).thenReturn((short) 1);
        when(grado.getNombre()).thenReturn("1.º");

        Estudiante ana = mock(Estudiante.class);
        when(ana.getId()).thenReturn(5L);
        when(ana.getNombres()).thenReturn("Ana");
        when(ana.getApellidos()).thenReturn("Quispe");
        when(ana.getGrado()).thenReturn(grado);
        when(ana.isActivo()).thenReturn(true);

        // el repositorio ya filtra: solo devuelve a Ana (activa)
        when(estudianteRepository.findByAulaIdAndActivoTrueOrderByApellidosAscNombresAsc(10L))
                .thenReturn(List.of(ana));

        List<EstudianteResponse> resultado = service.listarActivos(1L, 10L);

        assertEquals(1, resultado.size());
        assertEquals("Ana", resultado.get(0).nombres());
        assertEquals(true, resultado.get(0).activo());
        // no debe usar la consulta que trae a todos
        verify(estudianteRepository, never()).findByAulaIdOrderByApellidosAscNombresAsc(anyLong());
    }

    @Test
    void listarActivos_aulaDeOtroDocente_lanza403SinConsultarAlumnos() {
        Aula aula = mock(Aula.class);
        when(aula.getDocenteId()).thenReturn(99L); // otro docente
        when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.listarActivos(1L, 10L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getEstado());
        verifyNoInteractions(estudianteRepository);
    }
}