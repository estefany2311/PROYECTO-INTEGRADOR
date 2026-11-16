// Componente: ModalEliminar
// Padre: Catalogo
// Recibe (props): lectura (la que se va a desactivar)
// Avisa al padre con: onEliminada(mensaje), onCerrar(), onNoAutorizado()
import { useState } from "react";
import { eliminarLectura } from "../api";

// T-175: confirmación antes de eliminar (borrado lógico)
export default function ModalEliminar({ lectura, onCerrar, onEliminada, onNoAutorizado }) {
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState(lectura.asignada
    ? "La lectura está asignada a alumnos: no se puede eliminar, solo desactivar."
    : "");

  const confirmar = async () => {
    setEnviando(true);
    try {
      await eliminarLectura(lectura.id);
      onEliminada(`«${lectura.titulo}» se desactivó correctamente.`);
    } catch (err) {
      if (err.estado === 401) onNoAutorizado();
      else setError(err.datos?.detail ?? err.mensaje ?? "No se pudo eliminar la lectura.");
    } finally {
      setEnviando(false);
    }
  };

  return (
    <>
      <div className="modal d-block" tabIndex="-1" role="dialog" aria-modal="true" onClick={onCerrar}>
        <div className="modal-dialog modal-dialog-centered modal-sm" onClick={(e) => e.stopPropagation()}>
          <div className="modal-content text-center p-3">
            <div className="mx-auto mb-2 fs-2 text-danger"><i className={`bi ${error ? "bi-exclamation-triangle" : "bi-trash3"}`}></i></div>
            <h5 className="fw-semibold">{error ? "No se puede eliminar" : `¿Eliminar «${lectura.titulo}»?`}</h5>
            {error && <p className="note">{error}</p>}
            <div className="d-flex gap-2">
              <button type="button" className={`btn btn-light ${error ? "w-100" : "w-50"}`} onClick={onCerrar}>
                {error ? "Entendido" : "Cancelar"}
              </button>
              {!error && (
                <button type="button" className="btn btn-danger w-50" onClick={confirmar} disabled={enviando}>
                  {enviando ? "Eliminando..." : "Eliminar"}
                </button>
              )}
            </div>
          </div>
        </div>
      </div>
      <div className="modal-backdrop show"></div>
    </>
  );
}
