package com.proyecto.usuario.docente.seguridad;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.proyecto.usuario.docente.dto.PinGeneradoResponse;
import com.proyecto.usuario.docente.model.Aula;
import com.proyecto.usuario.docente.model.Estudiante;
import com.proyecto.usuario.docente.model.Grado;
import com.proyecto.usuario.docente.repository.AulaRepository;
import com.proyecto.usuario.docente.repository.AvatarRepository;
import com.proyecto.usuario.docente.repository.EstudianteRepository;
import com.proyecto.usuario.docente.service.AccesoEstudianteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * T-188: el PIN no debe aparecer en los logs.
 * Se captura el log real de la aplicación (Logback) y se busca el PIN generado.
 */
class PinNoEnLogsTest {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(PinNoEnLogsTest.class);

    private AccesoEstudianteService service;
    private Estudiante alumno;

    @BeforeEach
    void preparar() {
        EstudianteRepository estudianteRepository = mock(EstudianteRepository.class);
        AulaRepository aulaRepository = mock(AulaRepository.class);
        AvatarRepository avatarRepository = mock(AvatarRepository.class);
        PasswordEncoder codificador = new BCryptPasswordEncoder();

        service = new AccesoEstudianteService(
                estudianteRepository, aulaRepository, avatarRepository, codificador);

        Aula aula = new Aula();
        aula.setDocenteId(1L);
        alumno = new Estudiante();
        alumno.setNombres("Ana");
        alumno.setApellidos("Quispe");
        alumno.setGrado(new Grado((short) 1, "1.º", (short) 1));

        lenient().when(aulaRepository.findById(10L)).thenReturn(Optional.of(aula));
        lenient().when(estudianteRepository.findByIdAndAulaId(5L, 10L)).thenReturn(Optional.of(alumno));
    }

    @Test
    void generarPin_noEscribeElPinNiElHashEnLosLogs() {
        // capturamos TODO lo que se registre mientras se genera el PIN
        Logger raiz = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        ListAppender<ILoggingEvent> capturador = new ListAppender<>();
        capturador.start();
        raiz.addAppender(capturador);

        try {
            PinGeneradoResponse respuesta = service.generarPin(1L, 10L, 5L);
            String pin = respuesta.pin();
            String hash = (String) ReflectionTestUtils.getField(alumno, "pinHash");

            // simulamos los logs que un controlador podría escribir
            log.info("PIN generado para el alumno {}", respuesta);
            log.debug("Respuesta completa: {}", List.of(respuesta));
            log.warn("Alumno listo: {}", alumno); // el toString de la entidad tampoco lleva el hash

            List<String> textos = capturador.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .toList();

            assertFalse(textos.isEmpty(), "la prueba debe capturar al menos un log");
            for (String texto : textos) {
                assertFalse(texto.contains(pin), "el PIN apareció en un log: " + texto);
                assertFalse(texto.contains(hash), "el hash apareció en un log: " + texto);
            }
            assertTrue(capturador.list.stream()
                    .anyMatch(e -> e.getFormattedMessage().contains("****")));
        } finally {
            raiz.detachAppender(capturador);
        }
    }
}
