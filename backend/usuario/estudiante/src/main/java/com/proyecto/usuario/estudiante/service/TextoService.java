package com.proyecto.usuario.estudiante.service;

import com.proyecto.usuario.estudiante.dto.ErrorOrtografico;
import com.proyecto.usuario.estudiante.dto.NivelDetalleResponse;
import com.proyecto.usuario.estudiante.dto.TextoRecibidoResponse;
import com.proyecto.usuario.estudiante.exception.ConsignaNoDisponibleException;
import com.proyecto.usuario.estudiante.exception.TextoInvalidoException;
import com.proyecto.usuario.estudiante.model.Consigna;
import com.proyecto.usuario.estudiante.repository.ConsignaRepository;
import com.proyecto.usuario.estudiante.repository.EvidenciaTextoRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

// T-434 (US-21): recibe el texto final, lo revisa con el corrector (US-19) y crea la evidencia.
// Las palabras con error van a palabras_marcadas: de ahí el backend docente cuenta los errores (US-22)
@Service
public class TextoService {

    static final String ESCRITURA = "escritura";
    private static final Pattern PALABRA = Pattern.compile("[\\p{L}\\p{N}]+");   // igual que el contador de la app

    private final NivelService niveles;
    private final ConsignaRepository consignas;
    private final OrtografiaService ortografia;
    private final EvidenciaTextoRepository evidencias;
    private final Clock reloj;

    public TextoService(NivelService niveles, ConsignaRepository consignas, OrtografiaService ortografia,
                        EvidenciaTextoRepository evidencias, Clock reloj) {
        this.niveles = niveles;
        this.consignas = consignas;
        this.ortografia = ortografia;
        this.evidencias = evidencias;
        this.reloj = reloj;
    }

    public TextoRecibidoResponse recibir(long estudianteId, int nivelId, String texto) {
        NivelDetalleResponse nivel = niveles.detalle(estudianteId, nivelId);   // 404 o 403 si no puede entrar
        if (!ESCRITURA.equals(nivel.tipoReto())) {
            throw new TextoInvalidoException("Este nivel no es de escritura");
        }
        Consigna consigna = consignas.delNivel(nivelId).orElseThrow(ConsignaNoDisponibleException::sinConsigna);
        String limpio = texto.strip();
        int palabras = contarPalabras(limpio);
        int faltan = consigna.palabrasMinimas() - palabras;
        if (faltan > 0) {
            throw new TextoInvalidoException(faltan == 1
                    ? "Te falta 1 palabra para enviar tu texto"
                    : "Te faltan " + faltan + " palabras para enviar tu texto");
        }

        // Cada aparición, en minúsculas y sin espacios: el mismo formato que en las lecturas
        List<String> marcadas = ortografia.verificar(estudianteId, limpio).errores().stream()
                .map(ErrorOrtografico::palabra)
                .map(p -> p.toLowerCase(Locale.ROOT))
                .toList();
        long evidenciaId = evidencias.crear(estudianteId, nivelId, limpio, String.join(",", marcadas),
                LocalDateTime.now(reloj));
        return new TextoRecibidoResponse(evidenciaId, nivelId, palabras, marcadas);
    }

    static int contarPalabras(String texto) {
        return (int) PALABRA.matcher(texto).results().count();
    }
}
