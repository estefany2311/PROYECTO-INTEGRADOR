"""T-415 (US-13): servicio de voz a texto. Solo lo llama el backend estudiante.

Cómo levantarlo (en backend/ia/voz):
    python -m venv .venv
    .venv\\Scripts\\activate            (en Windows)
    pip install -r requirements.txt
    uvicorn app:app --port 8090
"""
from fastapi import Depends, FastAPI, File, HTTPException, UploadFile

from transcriptor import Transcriptor

TAMANO_MAXIMO = 6 * 1024 * 1024          # igual que el límite del backend estudiante

app = FastAPI(title="Servicio de voz a texto", version="1.0")
_transcriptor = Transcriptor()


def transcriptor() -> Transcriptor:
    return _transcriptor


@app.get("/salud")
def salud(t: Transcriptor = Depends(transcriptor)):
    return {"estado": "ok", "modelo": t.nombre_modelo, "cargado": t.cargado}


# def (no async) para que FastAPI lo corra en otro hilo y no bloquee el servicio
@app.post("/transcribir")
def transcribir(audio: UploadFile = File(...), t: Transcriptor = Depends(transcriptor)):
    contenido = audio.file.read(TAMANO_MAXIMO + 1)
    if not contenido:
        raise HTTPException(status_code=400, detail="No llegó ningún audio")
    if len(contenido) > TAMANO_MAXIMO:
        raise HTTPException(status_code=413, detail="El audio es demasiado grande")
    try:
        return t.transcribir(contenido)
    except Exception as e:  # audio dañado o formato que no se puede leer
        raise HTTPException(status_code=422, detail=f"No se pudo leer el audio: {e}")
