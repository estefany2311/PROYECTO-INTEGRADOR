"""T-415: pruebas del servicio sin cargar Whisper (usa un transcriptor falso)."""
from fastapi.testclient import TestClient

import app as servicio


class TranscriptorFalso:
    nombre_modelo = "falso"
    cargado = True

    def __init__(self, falla=False):
        self.falla = falla
        self.recibido = None

    def transcribir(self, audio):
        if self.falla:
            raise ValueError("archivo dañado")
        self.recibido = audio
        return {
            "texto": "Había una vez un cóndor",
            "idioma": "es",
            "duracion": 3.0,
            "palabras": [{"palabra": "Había", "inicio": 0.0, "fin": 0.4, "probabilidad": 0.95}],
            "modelo": "falso",
        }


def cliente(falso):
    servicio.app.dependency_overrides[servicio.transcriptor] = lambda: falso
    return TestClient(servicio.app)


def test_salud_dice_el_modelo():
    r = cliente(TranscriptorFalso()).get("/salud")
    assert r.status_code == 200
    assert r.json() == {"estado": "ok", "modelo": "falso", "cargado": True}


def test_transcribe_el_audio_y_devuelve_las_palabras():
    falso = TranscriptorFalso()
    r = cliente(falso).post("/transcribir", files={"audio": ("lectura.wav", b"RIFF-audio", "audio/wav")})
    assert r.status_code == 200
    assert r.json()["texto"] == "Había una vez un cóndor"
    assert r.json()["palabras"][0]["palabra"] == "Había"
    assert falso.recibido == b"RIFF-audio"


def test_sin_audio_responde_422_de_fastapi():
    r = cliente(TranscriptorFalso()).post("/transcribir")
    assert r.status_code == 422


def test_audio_vacio_responde_400():
    r = cliente(TranscriptorFalso()).post("/transcribir", files={"audio": ("vacio.wav", b"", "audio/wav")})
    assert r.status_code == 400


def test_audio_muy_grande_responde_413():
    grande = b"0" * (servicio.TAMANO_MAXIMO + 1)
    r = cliente(TranscriptorFalso()).post("/transcribir", files={"audio": ("grande.wav", grande, "audio/wav")})
    assert r.status_code == 413


def test_audio_que_no_se_puede_leer_responde_422():
    r = cliente(TranscriptorFalso(falla=True)).post("/transcribir", files={"audio": ("malo.wav", b"xx", "audio/wav")})
    assert r.status_code == 422
    assert "No se pudo leer" in r.json()["detail"]
