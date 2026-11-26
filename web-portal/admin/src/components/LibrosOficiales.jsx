// Componente: LibrosOficiales
// Padre: Catalogo
// Recibe (props): recursos (los libros), cargando, puedeCambiar, cambiando (id del libro que se está guardando)
// Avisa al padre con: onCambiarEstado(libro) al presionar «Activar» o «Desactivar»
import { FUENTE_OFICIAL } from "../utils/catalogo";

// El admin entrega los libros: solo los activos le aparecen al docente
export default function LibrosOficiales({ recursos, cargando, puedeCambiar, cambiando, onCambiarEstado }) {
  const activos = recursos.filter((r) => r.activo).length;
  return (
    <div className="card mb-4">
      <div className="card-header d-flex flex-wrap align-items-center gap-2">
        <h2>Libros para los docentes</h2>
        {!cargando && recursos.length > 0 && (
          <span className="badge-soft bs-p">{activos} de {recursos.length} activos</span>
        )}
      </div>
      <div className="card-body">
        {cargando ? (
          <div className="text-center text-secondary py-3">Cargando…</div>
        ) : recursos.length === 0 ? (
          <div className="text-center text-secondary py-3">
            Todavía no hay libros oficiales. Presiona «Actualizar catálogo» para traerlos.
          </div>
        ) : (
          <div className="libros-oficiales">
            {recursos.map((r) => (
              <div key={r.id} className={`libro libro-grado-${r.grado ?? 0} ${r.activo ? "" : "libro-inactivo"}`}>
                <div className="note">{r.titulo.split(":")[0].replace(/\s*\d+\s*$/, "").trim() || "Libro"}</div>
                <div className="libro-grado">{r.grado ? `${r.grado}.° grado` : "Sin grado"}</div>
                <div className="note">
                  {r.lecturas_importadas === 1 ? "1 lectura sacada" : `${r.lecturas_importadas} lecturas sacadas`}
                </div>
                <span className={`badge-soft ${r.activo ? "bs-ok" : "bs-off"} mt-1 d-inline-block`}>
                  {r.activo ? "Activo" : "Desactivado"}
                </span>
                <div className="d-flex gap-1 mt-2">
                  {puedeCambiar && (
                    <button type="button" className="btn btn-sm btn-light flex-grow-1" disabled={cambiando === r.id}
                      onClick={() => onCambiarEstado(r)}>
                      {cambiando === r.id ? (
                        <span className="spinner-border spinner-border-sm"></span>
                      ) : r.activo ? (
                        <><i className="bi bi-eye-slash me-1"></i>Desactivar</>
                      ) : (
                        <><i className="bi bi-eye me-1"></i>Activar</>
                      )}
                    </button>
                  )}
                  {r.url_pdf && (
                    <a className="btn btn-sm btn-light" href={r.url_pdf} target="_blank" rel="noreferrer" title="Ver el PDF">
                      <i className="bi bi-file-earmark-pdf"></i>
                    </a>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
        <div className="note mt-3">Fuente: {FUENTE_OFICIAL}</div>
      </div>
    </div>
  );
}
