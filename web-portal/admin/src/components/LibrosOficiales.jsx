// Componente: LibrosOficiales
// Padre: Catalogo
// Recibe (props): recursos (los libros), cargando, puedeImportar
// Avisa al padre con: onImportar(libro) al presionar «Importar»
import { FUENTE_OFICIAL } from "../utils/catalogo";

// T-444: libros oficiales disponibles para importar lecturas
export default function LibrosOficiales({ recursos, cargando, puedeImportar, onImportar }) {
  return (
    <div className="card mb-4">
      <div className="card-header d-flex flex-wrap align-items-center gap-2">
        <h2>Libros oficiales disponibles</h2>
        {!cargando && recursos.length > 0 && (
          <span className="badge-soft bs-p">{recursos.length} {recursos.length === 1 ? "libro" : "libros"}</span>
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
              <div key={r.id} className={`libro libro-grado-${r.grado ?? 0}`}>
                <div className="note">{r.titulo.split(":")[0].replace(/\s*\d+\s*$/, "").trim() || "Libro"}</div>
                <div className="libro-grado">{r.grado ? `${r.grado}.° grado` : "Sin grado"}</div>
                <div className="note">
                  {r.lecturas_importadas === 1 ? "1 lectura importada" : `${r.lecturas_importadas} lecturas importadas`}
                </div>
                <div className="d-flex gap-1 mt-2">
                  {puedeImportar && (
                    <button type="button" className="btn btn-sm btn-light flex-grow-1" onClick={() => onImportar(r)}>
                      <i className="bi bi-download me-1"></i>Importar
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
