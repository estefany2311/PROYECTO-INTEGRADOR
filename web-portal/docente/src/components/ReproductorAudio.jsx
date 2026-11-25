import { useEffect, useRef, useState } from "react";
import { descargarAudioEvidencia } from "../api";

// T-373 (US-32): reproductor del audio de una evidencia. El archivo se pide al
// backend con el header del docente (T-372) y se reproduce desde un blob, así
// no se expone una URL pública. La descarga empieza cuando el docente pulsa
// "Escuchar audio", para no bajar todos los audios de la lista.
export default function ReproductorAudio({ aulaId, evidenciaId, nombre }) {
  const [src, setSrc] = useState(null);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState(null);
  const urlBlob = useRef(null);

  // al desmontar (o cambiar de evidencia) se libera el blob
  useEffect(
    () => () => {
      if (urlBlob.current) URL.revokeObjectURL(urlBlob.current);
    },
    []
  );

  const cargar = async () => {
    setCargando(true);
    setError(null);
    try {
      const audio = await descargarAudioEvidencia(aulaId, evidenciaId);
      if (urlBlob.current) URL.revokeObjectURL(urlBlob.current);
      urlBlob.current = URL.createObjectURL(audio);
      setSrc(urlBlob.current);
    } catch (e) {
      setError(e.message);
    } finally {
      setCargando(false);
    }
  };

  if (src) {
    return (
      <div className="evidencia-audio-caja">
        <audio
          className="evidencia-reproductor"
          src={src}
          controls
          autoPlay
          preload="metadata"
          aria-label={`Audio de ${nombre || "la evidencia"}`}
        >
          Tu navegador no puede reproducir este audio.
        </audio>
      </div>
    );
  }

  return (
    <div className="evidencia-audio-caja">
      <button
        type="button"
        className="evidencia-boton-audio"
        onClick={cargar}
        disabled={cargando}
      >
        {cargando ? (
          <span className="spinner-border spinner-border-sm" aria-hidden="true" />
        ) : (
          <i className="bi bi-play-circle-fill" aria-hidden="true" />
        )}
        {cargando ? "Cargando audio…" : error ? "Reintentar audio" : "Escuchar audio"}
      </button>

      {error && (
        <p className="evidencia-audio-error" role="alert">
          <i className="bi bi-exclamation-triangle-fill" aria-hidden="true" />
          {error}
        </p>
      )}
    </div>
  );
}
