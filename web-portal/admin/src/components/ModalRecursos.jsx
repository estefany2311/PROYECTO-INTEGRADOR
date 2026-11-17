// Componente: ModalRecursos
// Padre: Catalogo
// Recibe (props): lectura (a la que se le asocian los recursos), puedeSubir (permiso catalogo.crear)
// Avisa al padre con: onCerrar(), onNoAutorizado()
// Hijos: Modal
import { useEffect, useRef, useState } from "react";
import { listarRecursosLectura, subirRecursoLectura } from "../api";
import Modal from "./Modal";

const MB = 1024 * 1024;
// Las mismas reglas del backend (T-343)
const FORMATOS = {
  jpg: { tipo: "imagen", maximo: 2 * MB },
  jpeg: { tipo: "imagen", maximo: 2 * MB },
  png: { tipo: "imagen", maximo: 2 * MB },
  mp3: { tipo: "audio", maximo: 5 * MB },
};
const MAX_RECURSOS = 6;

const megas = (bytes) => (bytes / MB).toFixed(1).replace(".", ",");
const tamanoLegible = (bytes) => (bytes >= MB ? `${megas(bytes)} MB` : `${Math.round(bytes / 1024)} KB`);

// Validación en el navegador, antes de subir (el backend vuelve a validar)
function errorDeArchivo(archivo) {
  const extension = archivo.name.split(".").pop().toLowerCase();
  const formato = FORMATOS[extension];
  if (!formato) return `«${archivo.name}»: tipo de archivo no permitido. Solo imágenes (JPG, PNG) o audios (MP3).`;
  if (archivo.size > formato.maximo) {
    const articulo = formato.tipo === "imagen" ? "una imagen" : "un audio";
    return `«${archivo.name}» pesa ${megas(archivo.size)} MB: el máximo para ${articulo} es ${formato.maximo / MB} MB.`;
  }
  return "";
}

export default function ModalRecursos({ lectura, puedeSubir, onCerrar, onNoAutorizado }) {
  const [recursos, setRecursos] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [subiendo, setSubiendo] = useState(false);
  const [descripcion, setDescripcion] = useState("");
  const [arrastrando, setArrastrando] = useState(false);
  const selector = useRef(null);

  useEffect(() => {
    listarRecursosLectura(lectura.id)
      .then((datos) => setRecursos(Array.isArray(datos) ? datos : datos?.results ?? []))
      .catch((err) => {
        if (err.estado === 401) onNoAutorizado();
        else setError(err.datos?.detail ?? err.mensaje ?? "No se pudieron cargar los recursos.");
      })
      .finally(() => setCargando(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [lectura.id]);

  const lleno = recursos.length >= MAX_RECURSOS;

  const subir = async (archivo) => {
    if (!archivo) return;
    const problema = lleno
      ? `La lectura ya tiene ${MAX_RECURSOS} recursos: elimina uno antes de subir otro.`
      : errorDeArchivo(archivo);
    if (problema) {
      setError(problema);
      return;
    }
    setError("");
    setSubiendo(true);
    try {
      const nuevo = await subirRecursoLectura(lectura.id, archivo, descripcion.trim());
      setRecursos([...recursos, nuevo]);   // arreglo nuevo, sin push
      setDescripcion("");
    } catch (err) {
      if (err.estado === 401) return onNoAutorizado();
      const mensaje = err.datos?.archivo ?? err.datos?.detail ?? err.mensaje;
      setError(Array.isArray(mensaje) ? mensaje.join(" ") : mensaje ?? "No se pudo subir el archivo.");
    } finally {
      setSubiendo(false);
      if (selector.current) selector.current.value = "";   // permite volver a elegir el mismo archivo
    }
  };

  const alSoltar = (e) => {
    e.preventDefault();
    setArrastrando(false);
    if (puedeSubir && !subiendo) subir(e.dataTransfer.files[0]);
  };

  return (
    <Modal
      titulo={`Recursos · ${lectura.titulo}`}
      tamano="lg"
      onCerrar={onCerrar}
      pie={
        <>
          <span className="note me-auto">{recursos.length} de {MAX_RECURSOS} recursos</span>
          <button type="button" className="btn btn-light" onClick={onCerrar}>Cerrar</button>
        </>
      }
    >
      <div className="modal-body">
        {puedeSubir && (
          <>
            <div
              className={`zona-subir${arrastrando ? " arrastrando" : ""}${lleno || subiendo ? " deshabilitada" : ""}`}
              onDragOver={(e) => { e.preventDefault(); setArrastrando(true); }}
              onDragLeave={() => setArrastrando(false)}
              onDrop={alSoltar}
            >
              <i className="bi bi-cloud-arrow-up"></i>
              <div className="fw-semibold mt-1">
                {subiendo ? "Subiendo…" : (
                  <>Arrastra una imagen o un audio, o{" "}
                    <button type="button" className="btn btn-link p-0 align-baseline"
                      disabled={lleno} onClick={() => selector.current?.click()}>elige un archivo</button>
                  </>
                )}
              </div>
              <div className="note">Imágenes JPG o PNG hasta 2 MB · Audios MP3 hasta 5 MB · Máximo {MAX_RECURSOS} recursos por lectura</div>
              <input ref={selector} type="file" accept=".jpg,.jpeg,.png,.mp3" className="d-none"
                onChange={(e) => subir(e.target.files[0])} />
            </div>
            <input type="text" className="form-control form-control-sm mt-2" maxLength={120}
              placeholder="Descripción opcional (por ejemplo: portada, ilustración, narración)"
              value={descripcion} disabled={lleno || subiendo} onChange={(e) => setDescripcion(e.target.value)} />
          </>
        )}

        {error && (
          <div className="alert alert-danger py-2 small mt-3 mb-0">
            <i className="bi bi-exclamation-circle me-1"></i>{error}
          </div>
        )}

        {cargando ? (
          <div className="text-center text-secondary py-4">Cargando…</div>
        ) : recursos.length === 0 ? (
          <div className="text-center note py-4">Esta lectura todavía no tiene imágenes ni audios.</div>
        ) : (
          <div className="grilla-recursos mt-3">
            {recursos.map((r) => (
              <div key={r.id} className="tarjeta-recurso">
                <div className="vista">
                  {r.tipo === "imagen"
                    ? <img src={r.archivo} alt={r.descripcion || r.nombre_original} loading="lazy" />
                    : <audio controls preload="none" src={r.archivo}>Tu navegador no puede reproducir el audio.</audio>}
                </div>
                <div className="datos">
                  <b title={r.nombre_original}>{r.nombre_original}</b>
                  <span className="note">{r.tipo === "imagen" ? "Imagen" : "Audio"} · {tamanoLegible(r.tamano)}</span>
                  {r.descripcion && <span className="note d-block">{r.descripcion}</span>}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </Modal>
  );
}