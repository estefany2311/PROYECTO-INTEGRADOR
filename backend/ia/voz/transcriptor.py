"""T-415 (US-13): convierte la lectura del niño en texto con Whisper (faster-whisper), en español."""
import io
import os
import threading


class Transcriptor:
    """Carga el modelo la primera vez que se usa, porque tarda unos segundos."""

    def __init__(self, modelo=None, dispositivo=None, tipo=None):
        self.nombre_modelo = modelo or os.getenv("WHISPER_MODELO", "small")
        self.dispositivo = dispositivo or os.getenv("WHISPER_DISPOSITIVO", "cpu")
        self.tipo = tipo or os.getenv("WHISPER_TIPO", "int8")
        self._modelo = None
        self._candado = threading.Lock()

    @property
    def cargado(self):
        return self._modelo is not None

    def _cargar(self):
        with self._candado:
            if self._modelo is None:
                from faster_whisper import WhisperModel
                self._modelo = WhisperModel(self.nombre_modelo, device=self.dispositivo, compute_type=self.tipo)
        return self._modelo

    def transcribir(self, audio: bytes) -> dict:
        segmentos, info = self._cargar().transcribe(
            io.BytesIO(audio),
            language="es",
            beam_size=5,
            temperature=0.0,
            vad_filter=True,                    # quita silencios y evita que invente texto
            word_timestamps=True,               # T-422 alinea palabra por palabra
            condition_on_previous_text=False,
        )
        # Sin initial_prompt con el texto de la lectura: si no, Whisper "corregiría" los errores del niño
        palabras, partes = [], []
        for segmento in segmentos:
            partes.append(segmento.text.strip())
            for p in segmento.words or []:
                palabras.append({
                    "palabra": p.word.strip(),
                    "inicio": round(p.start, 2),
                    "fin": round(p.end, 2),
                    "probabilidad": round(p.probability, 3),
                })
        return {
            "texto": " ".join(t for t in partes if t),
            "idioma": info.language,
            "duracion": round(info.duration, 2),
            "palabras": palabras,
            "modelo": self.nombre_modelo,
        }
