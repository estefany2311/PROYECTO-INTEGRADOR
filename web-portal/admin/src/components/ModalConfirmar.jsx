// Componente: ModalConfirmar
// Padre: Usuarios
// Recibe (props): usuario (el que se activa o desactiva), enviando, error
// Avisa al padre con: onConfirmar(), onCerrar()
import { useEffect } from "react";

export default function ModalConfirmar({ usuario, enviando, error, onConfirmar, onCerrar }) {
  const desactivar = usuario.is_active;

  useEffect(() => {
    const alPresionar = (e) => e.key === "Escape" && onCerrar();
    document.addEventListener("keydown", alPresionar);
    return () => document.removeEventListener("keydown", alPresionar);
  }, [onCerrar]);

  return (
    <>
      <div className="modal d-block" tabIndex="-1" role="dialog" aria-modal="true" onClick={onCerrar}>
        <div className="modal-dialog modal-dialog-centered modal-sm" onClick={(e) => e.stopPropagation()}>
          <div className="modal-content text-center p-3">
            <div className={`mx-auto mb-2 ${desactivar ? "bs-off" : "bs-ok"}`}
              style={{ width: 46, height: 46, borderRadius: 12, display: "grid", placeItems: "center", fontSize: 21 }}>
              <i className={`bi ${desactivar ? "bi-slash-circle" : "bi-arrow-counterclockwise"}`}></i>
            </div>
            <h5 className="fw-semibold">
              {desactivar ? "¿Desactivar" : "¿Reactivar"} a {usuario.username}?
            </h5>
            <p className="note">
              {desactivar
                ? "No podrá iniciar sesión hasta que la reactives. Su información se conserva."
                : "Podrá volver a iniciar sesión con su usuario y contraseña."}
            </p>
            {error && <div className="alert alert-danger py-2 small">{error}</div>}
            <div className="d-flex gap-2">
              <button type="button" className="btn btn-light w-50" onClick={onCerrar} disabled={enviando}>
                Cancelar
              </button>
              <button type="button" className={`btn w-50 ${desactivar ? "btn-danger" : "btn-success"}`}
                onClick={onConfirmar} disabled={enviando}>
                {enviando ? "Guardando..." : desactivar ? "Desactivar" : "Reactivar"}
              </button>
            </div>
          </div>
        </div>
      </div>
      <div className="modal-backdrop show"></div>
    </>
  );
}
