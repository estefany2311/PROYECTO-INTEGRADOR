import { useEffect, useState } from "react";
import { obtenerReporteAulas, obtenerReporteColegios } from "../api";

// US-47 · T-269: reporte consolidado por colegio y por aula (usa los endpoints de T-267 y T-268)

const COMPETENCIAS = [
  { clave: "lectura", texto: "Lectura" },
  { clave: "escritura", texto: "Escritura" },
  { clave: "oralidad", texto: "Oralidad" },
];

// Mismos rangos que el backend (reportes/consultas.py)
const colorDe = (promedio) => {
  if (promedio === null || promedio === undefined) return "sin_evaluar";
  if (promedio >= 75) return "verde";
  if (promedio >= 50) return "amarillo";
  return "rojo";
};

const ETIQUETAS_SEMAFORO = {
  verde: "Logrado (75 o más)",
  amarillo: "En proceso (50 a 74)",
  rojo: "En inicio (menos de 50)",
  sin_evaluar: "Sin evaluar",
};

function Promedio({ valor }) {
  if (valor === null || valor === undefined) return <span className="note">—</span>;
  return (
    <span className="promedio">
      <span className={`punto punto-${colorDe(valor)}`}></span>
      {valor.toFixed(1)}
    </span>
  );
}

function BarraSemaforo({ semaforo }) {
  const total = Object.values(semaforo).reduce((a, b) => a + b, 0);
  if (total === 0) return <span className="note">Sin alumnos</span>;
  const titulo = Object.entries(semaforo)
    .map(([color, n]) => `${ETIQUETAS_SEMAFORO[color]}: ${n}`)
    .join(" · ");
  return (
    <div className="semaforo" title={titulo}>
      <div className="semaforo-barra">
        {Object.entries(semaforo).map(([color, n]) =>
          n > 0 ? <span key={color} className={`tramo tramo-${color}`} style={{ width: `${(n / total) * 100}%` }}></span> : null
        )}
      </div>
      <div className="semaforo-numeros">
        <span className="t-verde">{semaforo.verde}</span>
        <span className="t-amarillo">{semaforo.amarillo}</span>
        <span className="t-rojo">{semaforo.rojo}</span>
        <span className="t-gris">{semaforo.sin_evaluar}</span>
      </div>
    </div>
  );
}

export default function Reportes({ onNoAutorizado }) {
  const [vista, setVista] = useState("colegios"); // "colegios" | "aulas"
  const [colegios, setColegios] = useState([]);   // para el selector (siempre todos)
  const [colegioId, setColegioId] = useState("");
  const [reporte, setReporte] = useState(null);   // { resumen, colegios }
  const [aulas, setAulas] = useState(null);       // { colegio, aulas }
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");

  const manejarError = (err) => {
    if (err.estado === 401) return onNoAutorizado();
    if (err.estado === 403) return setError("No tienes permiso para ver los reportes.");
    setError(err.datos?.detail ?? err.mensaje ?? "No se pudo cargar el reporte.");
  };

  // Reporte por colegio (todos o uno)
  useEffect(() => {
    setCargando(true);
    setError("");
    obtenerReporteColegios(colegioId)
      .then((datos) => {
        setReporte(datos);
        if (!colegioId) setColegios(datos.colegios);
      })
      .catch(manejarError)
      .finally(() => setCargando(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [colegioId]);

  // Aulas del colegio elegido
  useEffect(() => {
    if (vista !== "aulas" || !colegioId) {
      setAulas(null);
      return;
    }
    obtenerReporteAulas(colegioId).then(setAulas).catch(manejarError);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [vista, colegioId]);

  const verAulas = (id) => {
    setColegioId(String(id));
    setVista("aulas");
  };

  const resumen = reporte?.resumen;
  const filas = reporte?.colegios ?? [];
  // Promedio general: promedio de las competencias del resumen (los mismos alumnos que las otras tarjetas)
  const promediosResumen = Object.values(resumen?.promedios ?? {}).filter((p) => p !== null && p !== undefined);
  const promedioGeneral = promediosResumen.length
    ? promediosResumen.reduce((s, p) => s + p, 0) / promediosResumen.length
    : null;

  const tarjetas = [
    { titulo: "Alumnos activos", valor: resumen?.alumnos ?? 0, icono: "bi-people", tono: "verde-marca" },
    { titulo: "Evaluaciones", valor: resumen?.evaluaciones ?? 0, icono: "bi-clipboard-check", tono: "azul" },
    { titulo: "Promedio general", valor: promedioGeneral === null ? "—" : `${promedioGeneral.toFixed(1)}`, icono: "bi-graph-up", tono: "naranja" },
    { titulo: "Necesitan refuerzo", valor: resumen?.semaforo?.rojo ?? 0, icono: "bi-life-preserver", tono: "rojo" },
  ];

  return (
    <section>
      <div className="page-title d-flex flex-wrap justify-content-between align-items-end gap-2 mb-4">
        <div>
          <h1>Reportes consolidados</h1>
          <div className="breadcrumb text-secondary">Análisis / Reportes</div>
        </div>
        <select
          className="form-select selector-colegio"
          value={colegioId}
          onChange={(e) => setColegioId(e.target.value)}
          aria-label="Elegir colegio"
        >
          <option value="">Todos los colegios</option>
          {colegios.map((c) => (
            <option key={c.colegio_id} value={c.colegio_id}>{c.nombre}</option>
          ))}
        </select>
      </div>

      {error && <div className="alert alert-danger py-2 small">{error}</div>}

      <div className="row g-3 mb-4">
        {tarjetas.map((t) => (
          <div className="col-6 col-xl-3" key={t.titulo}>
            <div className={`card tarjeta-reporte tono-${t.tono}`}>
              <div className="card-body d-flex align-items-center gap-3">
                <span className="tarjeta-icono"><i className={`bi ${t.icono}`}></i></span>
                <div>
                  <div className="note fw-bold">{t.titulo}</div>
                  <div className="tarjeta-valor">{cargando ? "…" : t.valor}</div>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="card">
        <div className="card-header d-flex flex-wrap align-items-center gap-2">
          <div className="filtro-vista" role="group" aria-label="Ver por">
            <button type="button" className={vista === "colegios" ? "activo" : ""} onClick={() => setVista("colegios")}>
              Por colegio
            </button>
            <button type="button" className={vista === "aulas" ? "activo" : ""} onClick={() => setVista("aulas")}>
              Por aula
            </button>
          </div>
          {vista === "aulas" && aulas && (
            <span className="note ms-auto">
              {aulas.colegio.nombre} · código modular {aulas.colegio.codigo_modular}
            </span>
          )}
        </div>

        {vista === "colegios" && (
          cargando ? (
            <div className="text-center text-secondary py-5">Cargando…</div>
          ) : filas.length === 0 ? (
            <div className="text-center text-secondary py-5">No hay colegios activos.</div>
          ) : (
            <div className="table-responsive">
              <table className="table table-hover">
                <thead>
                  <tr>
                    <th>Colegio</th>
                    <th>Alumnos</th>
                    {COMPETENCIAS.map((c) => <th key={c.clave}>{c.texto}</th>)}
                    <th>General</th>
                    <th>Semáforo</th>
                    <th className="text-end">Detalle</th>
                  </tr>
                </thead>
                <tbody>
                  {filas.map((c) => (
                    <tr key={c.colegio_id}>
                      <td>
                        <div className="fw-bold">{c.nombre}</div>
                        <div className="note">{c.codigo_modular}{c.region ? ` · ${c.region}` : ""}</div>
                      </td>
                      <td>{c.alumnos}</td>
                      {COMPETENCIAS.map((k) => <td key={k.clave}><Promedio valor={c.promedios[k.clave]} /></td>)}
                      <td><Promedio valor={c.promedio_general} /></td>
                      <td><BarraSemaforo semaforo={c.semaforo} /></td>
                      <td className="text-end">
                        <button type="button" className="btn btn-sm btn-light" onClick={() => verAulas(c.colegio_id)}>
                          Ver aulas <i className="bi bi-chevron-right"></i>
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )
        )}

        {vista === "aulas" && (
          !colegioId ? (
            <div className="text-center text-secondary py-5">Elige un colegio arriba para ver sus aulas.</div>
          ) : !aulas ? (
            <div className="text-center text-secondary py-5">Cargando…</div>
          ) : aulas.aulas.length === 0 ? (
            <div className="text-center text-secondary py-5">Este colegio todavía no tiene aulas registradas.</div>
          ) : (
            <div className="table-responsive">
              <table className="table table-hover">
                <thead>
                  <tr>
                    <th>Aula</th>
                    <th>Docente</th>
                    <th>Alumnos</th>
                    {COMPETENCIAS.map((c) => <th key={c.clave}>{c.texto}</th>)}
                    <th>General</th>
                    <th>Semáforo</th>
                  </tr>
                </thead>
                <tbody>
                  {aulas.aulas.map((a) => (
                    <tr key={a.aula_id}>
                      <td className="fw-bold">{a.nombre}</td>
                      <td>{a.docente ?? <span className="note">—</span>}</td>
                      <td>{a.alumnos}</td>
                      {COMPETENCIAS.map((k) => <td key={k.clave}><Promedio valor={a.promedios[k.clave]} /></td>)}
                      <td><Promedio valor={a.promedio_general} /></td>
                      <td><BarraSemaforo semaforo={a.semaforo} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )
        )}

        <div className="card-footer note leyenda">
          <span><span className="punto punto-verde"></span>Logrado: 75 o más</span>
          <span><span className="punto punto-amarillo"></span>En proceso: 50 a 74</span>
          <span><span className="punto punto-rojo"></span>En inicio: menos de 50</span>
          <span><span className="punto punto-sin_evaluar"></span>Sin evaluar</span>
        </div>
      </div>
    </section>
  );
}
