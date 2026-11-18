package com.proyecto.usuario.estudiante.controller;

import com.proyecto.usuario.estudiante.dto.AudioRecibidoResponse;
import com.proyecto.usuario.estudiante.service.AudioService;
import com.proyecto.usuario.estudiante.service.AutenticadorTablet;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

// T-413 (US-13): la tablet sube la grabación de la lectura (multipart: audio + lecturaId)
@RestController
@RequestMapping("/api/estudiante/niveles")
public class AudioController {

    private final AutenticadorTablet autenticador;
    private final AudioService audioService;

    public AudioController(AutenticadorTablet autenticador, AudioService audioService) {
        this.autenticador = autenticador;
        this.audioService = audioService;
    }

    @PostMapping(value = "/{nivelId}/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AudioRecibidoResponse subir(
            @PathVariable int nivelId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam long lecturaId,
            @RequestPart("audio") MultipartFile audio) throws IOException {
        long estudianteId = autenticador.estudianteDe(authorization);
        return audioService.recibir(estudianteId, nivelId, lecturaId, audio.getBytes());
    }
}
