package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.exception.ServicioVozNoDisponibleException;
import com.proyecto.usuario.estudiante.model.Transcripcion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;

// T-415 (US-13): llama al servicio Whisper de backend/ia/voz (POST /transcribir)
@Component
public class ClienteWhisper implements ClienteVoz {

    private final RestClient http;

    @Autowired
    public ClienteWhisper(@Value("${estudiante.ia.voz.url}") String url,
                          @Value("${estudiante.ia.voz.espera-segundos}") int esperaSegundos) {
        this(RestClient.builder().requestFactory(fabrica(esperaSegundos)), url);
    }

    // Para las pruebas: recibe un builder ya conectado a un servidor simulado
    ClienteWhisper(RestClient.Builder builder, String url) {
        this.http = builder.baseUrl(url).build();
    }

    private static JdkClientHttpRequestFactory fabrica(int esperaSegundos) {
        HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(cliente);
        fabrica.setReadTimeout(Duration.ofSeconds(esperaSegundos));   // en CPU, 2 minutos de audio tardan
        return fabrica;
    }

    @Override
    public Transcripcion transcribir(byte[] audioWav) {
        MultiValueMap<String, Object> partes = new LinkedMultiValueMap<>();
        partes.add("audio", new ByteArrayResource(audioWav) {
            @Override
            public String getFilename() {
                return "lectura.wav";
            }
        });
        try {
            Transcripcion transcripcion = http.post()
                    .uri("/transcribir")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(partes)
                    .retrieve()
                    .body(Transcripcion.class);
            if (transcripcion == null || transcripcion.texto() == null) {
                throw new ServicioVozNoDisponibleException("respuesta vacía", null);
            }
            return transcripcion;
        } catch (RestClientException e) {
            throw new ServicioVozNoDisponibleException(e.getMessage(), e);
        }
    }
}
