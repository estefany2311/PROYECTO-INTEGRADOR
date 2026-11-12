// Componente: Modal
// Padre: ModalLectura, ModalEliminar, ModalImportar, ModalColegio y ModalAsignar
// Recibe (props): titulo, tamano ("lg" para una ventana ancha), pie (botones de abajo), children (el contenido)
// Avisa al padre con: onCerrar() al presionar Esc, la X o fuera de la ventana
import { useEffect } from "react";

// Ventana emergente reutilizable (antes estaba repetida en Catálogo y Colegios)
export default function Modal({ titulo, onCerrar, children, pie, tamano = "" }) {
  useEffect(() => {
    const alPresionar = (e) => e.key === "Escape" && onCerrar();
    document.addEventListener("keydown", alPresionar);
    return () => document.removeEventListener("keydown", alPresionar);
  }, [onCerrar]);

  return (
    <>
      <div className="modal d-block" tabIndex="-1" role="dialog" aria-modal="true" onClick={onCerrar}>
        <div className={`modal-dialog modal-dialog-centered${tamano ? ` modal-${tamano}` : ""}`} onClick={(e) => e.stopPropagation()}>
          <div className="modal-content">
            <div className="modal-header">
              <h5 className="modal-title fw-semibold">{titulo}</h5>
              <button type="button" className="btn-close" aria-label="Cerrar" onClick={onCerrar}></button>
            </div>
            {children}
            <div className="modal-footer">{pie}</div>
          </div>
        </div>
      </div>
      <div className="modal-backdrop show"></div>
    </>
  );
}
