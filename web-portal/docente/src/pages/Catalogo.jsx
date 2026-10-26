import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { listarAulas, listarLecturas } from "../api";

// T-190 (US-35): catálogo de lecturas del aula con filtros.
// El grado y la dificultad los filtra el backend (T-189); la búsqueda por título se hace aquí.
const DIFICULTADES = [
  { valor: "basica", texto: "Básica" },
  { valor: "intermedia", texto: "Intermedia" },
  { valor: "avanzada", texto: "Avanzada" },
];

const textoDificultad = (valor) => {
  const encontrada = DIFICULTADES.find((d) => d.valor === (valor || "").toLowerCase());
  return encontrada ? encontrada.texto : valor;
};

// color del distintivo según la dificultad, para que se lea de un vistazo
const claseDificultad = (valor) => {
  switch ((valor || "").toLowerCase()) {
    case "basica":
      return "tag-basica";
    case "intermedia":
      return "tag-intermedia";
    case "avanzada":
      return "tag-avanzada";
    default:
      return "tag-neutro";
  }
};

export default function Catalogo() {
  const [aulas, setAulas] = useState([]);
  const [aulaId, setAulaId] = useState("");
  const [lecturas, setLecturas] = useState([]);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState(null);
  const [gradoId, setGradoId] = useState("");
  const [dificultad, setDificultad] = useState("");
  const [busqueda, setBusqueda] = useState("");
  const [detalle, setDetalle] = useState(null); // lectura abierta en el modal

  const aula = aulas.find((a) => String(a.id) === String(aulaId));

  // al abrir la pantalla: las aulas del docente
  useEffect(() => {
    listarAulas()
      .then((data) => {
        setAulas(data);
        if (data.length > 0) {
          setAulaId(String(data[0].id));
          setCargando(true); // la primera carga del catálogo
        }
      })
      .catch((e) => setError(e.message));
  }, []);

  // cada cambio de aula o de filtro vuelve a pedir el catálogo
  useEffect(() => {
    if (!aulaId) return;
    let vigente = true; // si el docente cambia de filtro rápido, no pisa la respuesta nueva

    listarLecturas(aulaId, {
      gradoId: gradoId ? Number(gradoId) : undefined,
      dificultad: dificultad || undefined,
    })
      .then((data) => {
        if (vigente) {
          setLecturas(data);
          setError(null);
        }
      })
      .catch((e) => {
        if (vigente) {
          setLecturas([]);
          setError(e.message);
        }
      })
      .finally(() => {
        if (vigente) setCargando(false);
      });

    return () => {
      vigente = false;
    };
  }, [aulaId, gradoId, dificultad]);

  // los filtros se cambian desde los controles: así el "Cargando…" sale al instante
  const cambiarFiltro = (cambiar) => (valor) => {
    setCargando(true);
    setError(null);
    cambiar(valor);
  };

  // la búsqueda solo filtra lo que ya llegó del servidor
  const visibles = lecturas.filter((l) =>
    l.titulo.toLowerCase().includes(busqueda.trim().toLowerCase())
  );

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center flex-wrap gap-3 mb-4">
        <div>
          <h1 className="h3 fw-bold mb-1">Catálogo de lecturas</h1>
          <div className="text-secondary">
            Lecturas disponibles para los grados de tu aula
          </div>
        </div>
        {aulas.length > 0 && (
          <div>
            <label htmlFor="aula" className="visually-hidden">Aula</label>
            <select
              id="aula"
              className="form-select rounded-pill"
              value={aulaId}
              onChange={(ev) => {
                // al cambiar de aula el grado elegido ya no sirve
                setCargando(true);
                setError(null);
                setAulaId(ev.target.value);
                setGradoId("");
              }}
            >
              {aulas.map((a) => (
                <option key={a.id} value={a.id}>{a.nombre}</option>
              ))}
            </select>
          </div>
        )}
      </div>

      {error && <div className="alert alert-danger">{error}</div>}

      {aulas.length === 0 && !error ? (
        <div className="panel text-center py-5">
          <p className="mb-3">Aún no tienes aulas registradas.</p>
          <Link to="/aulas" className="btn btn-primary">Registrar mi primera aula</Link>
        </div>
      ) : (
        <>
          <div className="panel mb-4">
            <div className="row g-3 align-items-end">
              <div className="col-md-4">
                <label htmlFor="filtro-grado" className="form-label fw-semibold">Grado</label>
                <select
                  id="filtro-grado"
                  className="form-select"
                  value={gradoId}
                  onChange={(ev) => cambiarFiltro(setGradoId)(ev.target.value)}
                >
                  <option value="">Todos los grados</option>
                  {aula?.grados.map((g) => (
                    <option key={g.id} value={g.id}>{g.nombre}</option>
                  ))}
                </select>
              </div>

              <div className="col-md-4">
                <label htmlFor="filtro-dificultad" className="form-label fw-semibold">Dificultad</label>
                <select
                  id="filtro-dificultad"
                  className="form-select"
                  value={dificultad}
                  onChange={(ev) => cambiarFiltro(setDificultad)(ev.target.value)}
                >
                  <option value="">Todas las dificultades</option>
                  {DIFICULTADES.map((d) => (
                    <option key={d.valor} value={d.valor}>{d.texto}</option>
                  ))}
                </select>
              </div>

              <div className="col-md-4">
                <label htmlFor="buscar" className="form-label fw-semibold">Buscar por título</label>
                <input
                  id="buscar"
                  className="form-control"
                  placeholder="Escribe el título…"
                  value={busqueda}
                  onChange={(ev) => setBusqueda(ev.target.value)}
                />
              </div>
            </div>
          </div>

          <div className="d-flex justify-content-between align-items-center mb-3">
            <span className="text-secondary">
              {aula?.nombre} · {aula?.grados.map((g) => g.nombre).join(", ")}
            </span>
            {!cargando && !error && (
              <span className="tag tag-neutro">{visibles.length} lecturas</span>
            )}
          </div>

          {cargando ? (
            <p className="text-center text-secondary py-4">Cargando lecturas…</p>
          ) : visibles.length === 0 ? (
            <div className="panel text-center py-5">
              <p className="mb-1 fw-semibold">No hay lecturas con estos filtros.</p>
              <p className="text-secondary mb-0">
                Prueba con otro grado o dificultad. El catálogo lo carga el equipo de Admin.
              </p>
            </div>
          ) : (
            <div className="row g-3">
              {visibles.map((l) => (
                <div className="col-md-6 col-xl-4" key={l.id}>
                  <article className="panel h-100 d-flex flex-column">
                    <div className="d-flex justify-content-between align-items-start gap-2 mb-2">
                      <h2 className="h6 fw-bold mb-0">{l.titulo}</h2>
                      <span className={`tag ${claseDificultad(l.dificultad)}`}>
                        {textoDificultad(l.dificultad)}
                      </span>
                    </div>

                    <div className="d-flex flex-wrap gap-2 mb-2">
                      <span className="insignia">{l.gradoNombre || `Grado ${l.gradoId}`}</span>
                      {l.tipoReto && <span className="tag tag-neutro">{l.tipoReto}</span>}
                    </div>

                    <p className="text-secondary lectura-texto mb-3">{l.texto}</p>

                    <button
                      type="button"
                      className="btn btn-sm btn-outline-primary rounded-pill mt-auto align-self-start"
                      aria-label={`Ver lectura ${l.titulo}`}
                      onClick={() => setDetalle(l)}
                    >
                      <i className="bi bi-eye me-2" />
                      Ver lectura
                    </button>
                  </article>
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {/* Modal con el texto completo: sirve para decidir si la lectura se asigna */}
      {detalle && (
        <div className="modal d-block" tabIndex="-1" role="dialog" aria-label="Lectura completa">
          <div className="modal-dialog modal-dialog-centered modal-lg">
            <div className="modal-content border-0 rounded-4">
              <div className="modal-header border-0">
                <h2 className="modal-title h5 fw-bold">{detalle.titulo}</h2>
                <button
                  type="button"
                  className="btn-close"
                  aria-label="Cerrar"
                  onClick={() => setDetalle(null)}
                />
              </div>
              <div className="modal-body">
                <div className="d-flex flex-wrap gap-2 mb-3">
                  <span className="insignia">{detalle.gradoNombre || `Grado ${detalle.gradoId}`}</span>
                  <span className={`tag ${claseDificultad(detalle.dificultad)}`}>
                    {textoDificultad(detalle.dificultad)}
                  </span>
                  {detalle.tipoReto && <span className="tag tag-neutro">{detalle.tipoReto}</span>}
                </div>
                <p className="lectura-completa mb-0">{detalle.texto}</p>
              </div>
              <div className="modal-footer border-0">
                <button
                  type="button"
                  className="btn btn-outline-secondary rounded-pill"
                  onClick={() => setDetalle(null)}
                >
                  Cerrar
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {detalle && <div className="modal-backdrop fade show" />}
    </div>
  );
}
