"""T-420: prueba con Whisper de verdad y una lectura grabada por una persona.

Se salta sola si falta el audio. Para correrla:
  1. Graba tu voz leyendo un texto corto y guárdalo como tests/audios/lectura_real.wav (o .m4a)
  2. Escribe el mismo texto en tests/audios/lectura_real.txt
  3. pytest tests/test_whisper_real.py -s   (la primera vez descarga el modelo)
"""
import re
import unicodedata
from pathlib import Path

import pytest

AUDIOS = Path(__file__).parent / "audios"


def _palabras(texto):
    # Minúsculas y sin tildes (la ñ se queda), para comparar sin importar la puntuación
    texto = texto.lower().replace("ñ", "\0")
    texto = "".join(c for c in unicodedata.normalize("NFD", texto) if unicodedata.category(c) != "Mn")
    return [p.replace("\0", "ñ") for p in re.findall(r"[a-z\0]+", texto)]


def _audio_real():
    for extension in ("wav", "m4a", "mp3"):
        ruta = AUDIOS / f"lectura_real.{extension}"
        if ruta.exists():
            return ruta
    return None


@pytest.mark.skipif(_audio_real() is None or not (AUDIOS / "lectura_real.txt").exists(),
                    reason="falta tests/audios/lectura_real.wav y su .txt")
def test_whisper_entiende_la_mayoria_de_palabras():
    pytest.importorskip("faster_whisper")
    from transcriptor import Transcriptor

    resultado = Transcriptor().transcribir(_audio_real().read_bytes())
    esperadas = _palabras((AUDIOS / "lectura_real.txt").read_text(encoding="utf-8"))
    escuchadas = set(_palabras(resultado["texto"]))
    aciertos = sum(1 for p in esperadas if p in escuchadas)

    print(f"\nWhisper escuchó: {resultado['texto']}\nAciertos: {aciertos}/{len(esperadas)}")
    assert resultado["palabras"], "no devolvió palabras con sus tiempos"
    assert aciertos / len(esperadas) >= 0.7
