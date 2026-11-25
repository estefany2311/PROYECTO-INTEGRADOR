package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.exception.ServicioVozNoDisponibleException;
import com.proyecto.usuario.estudiante.model.Transcripcion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static com.proyecto.usuario.estudiante.service.AudiosDePrueba.wav;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

// T-415 (US-13): el backend manda el WAV al servicio de voz y lee el texto con sus palabras
class ClienteWhisperTest {

    private MockRestServiceServer servidor;
    private ClienteWhisper cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new ClienteWhisper(builder, "http://voz");
    }

    @Test
    void mandaElAudioYLeeElTextoConSusPalabras() {
        servidor.expect(requestTo("http://voz/transcribir"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                .andRespond(withSuccess("""
                        {"texto": "Había una vez un cóndor", "idioma": "es", "duracion": 3.0,
                         "palabras": [{"palabra": "Había", "inicio": 0.0, "fin": 0.42, "probabilidad": 0.95},
                                      {"palabra": "una", "inicio": 0.42, "fin": 0.6, "probabilidad": 0.99}],
                         "modelo": "small"}
                        """, MediaType.APPLICATION_JSON));

        Transcripcion t = cliente.transcribir(wav(3_000));

        assertEquals("Había una vez un cóndor", t.texto());
        assertEquals(2, t.palabras().size());
        assertEquals(new Transcripcion.Palabra("Había", 0.0, 0.42, 0.95), t.palabras().get(0));
        servidor.verify();
    }

    @Test
    void siElServicioFallaAvisaConSuExcepcion() {
        servidor.expect(requestTo("http://voz/transcribir")).andRespond(withServerError());

        assertThrows(ServicioVozNoDisponibleException.class, () -> cliente.transcribir(wav(3_000)));
    }

    @Test
    void siElAudioNoSePuedeLeerTambienAvisa() {
        servidor.expect(requestTo("http://voz/transcribir"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT)
                        .body("{\"detail\": \"No se pudo leer el audio\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThrows(ServicioVozNoDisponibleException.class, () -> cliente.transcribir(wav(3_000)));
    }

    @Test
    void respuestaSinTextoSeTrataComoError() {
        servidor.expect(requestTo("http://voz/transcribir")).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThrows(ServicioVozNoDisponibleException.class, () -> cliente.transcribir(wav(3_000)));
    }
}
