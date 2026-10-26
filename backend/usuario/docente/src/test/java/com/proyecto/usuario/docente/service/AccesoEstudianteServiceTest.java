package com.proyecto.usuario.docente.service;

import com.proyecto.usuario.docente.dto.EstudianteResponse;
import com.proyecto.usuario.docente.dto.PinGeneradoResponse;
import com.proyecto.usuario.docente.exception.ReglaNegocioException;
import com.proyecto.usuario.docente.model.Aula;
import com.proyecto.usuario.docente.model.Avatar;
import com.proyecto.usuario.docente.model.Estudiante;
import com.proyecto.usuario.docente.model.Grado;
import com.proyecto.usuario.docente.repository.AulaRepository;
import com.proyecto.usuario.docente.repository.AvatarRepository;
import com.proyecto.usuario.docente.repository.EstudianteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// T-186: el PIN se guarda cifrado (BCrypt) y solo se devuelve una vez
@ExtendWith(MockitoExtension.class)
class AccesoEstudianteServiceTest {

    @Mock EstudianteRepository estudianteRepository;
    @Mock AulaRepository aulaRepository;
    @Mock AvatarRepository avatarRepository;

    // BCrypt real: así comprobamos que "matches" funciona igual que en la tablet
    private final PasswordEncoder codificador = new BCryptPasswordEncoder();
    private AccesoEstudianteService service;
    private Estudiante alumno;

    @BeforeEach
    void preparar() {
        service = new AccesoEstudianteService(
                estudianteRepository, aulaRepository, avatarRepository, codificador);

        Aula aula = new Aula();
        aula.setDocenteId(1L); // el aula 10 es del docente 1
        alumno = new Estudiante();
        alumno.setNombres("Ana");
        alumno.setApellidos("Quispe");
        alumno.setGrado(new Grado((short) 1, "1.º", (short) 1));

        lenient().when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        lenient().when(estudianteRepository.findByIdAndAulaId(5L, 10L)).thenReturn(Optional.of(alumno));
    }

    // el pinHash no tiene getter a propósito; lo leemos con reflexión solo en la prueba
    private String hashGuardado() {
        return (String) ReflectionTestUtils.getField(alumno, "pinHash");
    }

    @Test
    void generarPin_guardaSoloElHashYDevuelveUnPinDe4Numeros() {
        PinGeneradoResponse r = service.generarPin(1L, 10L, 5L);

        assertTrue(r.pin().matches("\\d{4}"));
        assertNotEquals(r.pin(), hashGuardado());          // lo guardado NO es el PIN
        assertTrue(hashGuardado().startsWith("$2"));       // formato BCrypt
        assertTrue(codificador.matches(r.pin(), hashGuardado())); // la tablet podrá validarlo
        assertTrue(alumno.tienePin());
        verify(estudianteRepository).save(alumno);
    }

    @Test
    void restablecerPin_reemplazaElPinAnterior() {
        service.generarPin(1L, 10L, 5L);
        PinGeneradoResponse nuevo = service.generarPin(1L, 10L, 5L);

        assertTrue(codificador.matches(nuevo.pin(), hashGuardado())); // vale el PIN nuevo
    }

    @Test
    void generarPin_aulaDeOtroDocente_lanza403SinTocarAlumnos() {
        ReflectionTestUtils.setField(aulaRepository.findById(10L).get(), "docenteId", 99L);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.generarPin(1L, 10L, 5L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getEstado());
        assertFalse(alumno.tienePin());
        verify(estudianteRepository, never()).save(any());
    }

    @Test
    void generarPin_alumnoQueNoEsDeEsaAula_lanza404() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.generarPin(1L, 10L, 777L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getEstado());
        verify(estudianteRepository, never()).save(any());
    }

    @Test
    void asignarAvatar_guardaElAvatarYLoDevuelveEnLaRespuesta() {
        Avatar avatar = mock(Avatar.class);
        when(avatar.getId()).thenReturn((short) 2);
        when(avatar.getUrl()).thenReturn("https://img/buho.png");
        when(avatarRepository.findById((short) 2)).thenReturn(Optional.of(avatar));

        EstudianteResponse r = service.asignarAvatar(1L, 10L, 5L, (short) 2);

        assertEquals((short) 2, r.avatarId());
        assertEquals("https://img/buho.png", r.avatarUrl());
        assertSame(avatar, alumno.getAvatar());
        verify(estudianteRepository).save(alumno);
    }

    @Test
    void asignarAvatar_avatarInexistente_lanza400() {
        when(avatarRepository.findById((short) 99)).thenReturn(Optional.empty());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.asignarAvatar(1L, 10L, 5L, (short) 99));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getEstado());
        assertNull(alumno.getAvatar());
    }

    @Test
    void pinGeneradoResponse_toStringNoMuestraElPin() {
        String texto = new PinGeneradoResponse(5L, "4821").toString();

        assertFalse(texto.contains("4821"));
    }
}