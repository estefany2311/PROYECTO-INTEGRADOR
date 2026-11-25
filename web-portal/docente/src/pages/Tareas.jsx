import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { listarAulas, listarSeguimientoTareas } from "../api";
import { obtenerAulaSeleccionada, seleccionarAula } from "../aulaSeleccionada";
import { formatearFechaLimite } from "../fechaLimite";

// T-390 (US-38): seguimiento de las tareas asignadas del aula. Consume el
// endpoint de T-389, que ya calcula el estado (pendiente, entregada o vencida),
// así que la pantalla solo lo pinta con su color.
const ESTADOS = [
  { clave: "vencida", texto: "Vencida", plural: "Vencidas", icono: "bi-exclamation-octagon-fill" },
  { clave: "pendiente", texto: "Pendiente", plural: "Pendientes", icono: "bi-hourglass-split" },
  { clave: "entregada", texto: "Entregada", plural: "Entregadas", icono: "bi-check-circle-fill" },
];

const COLORES_AVATAR = ["#8B5CF6", "#2FC5F6", "#F59E0B", "#EC4899", "#10B981", "#6366F1"];

const estadoDe = (clave) =>
  ESTADOS.find((estado) => estado.clave === clave) || ESTADOS[1];

const iniciales = (nombres = "", apellidos = "") =>
  `${nombres.trim().charAt(0)}${apellidos.trim().charAt(0)}`.toUpperCase() || "?";

const colorAvatar = (id) =>
  COLORES_AVATAR[Number(id || 0) % COLORES_AVATAR.length];

const formatearEntrega = (valor) => {
  if (!valor) return "";
  const fecha = new Date(valor);
  if (Number.isNaN(fecha.getTime())) return "";
  return new Intl.DateTimeFormat("es-PE", {
    day: "2-digit",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  }).format(fecha);
};

const textoVacio = (estado) =>
  estado
    ? `No hay tareas ${estadoDe(estado).plural.toLowerCase()}.`
    : "Todavía no asignaste tareas en esta aula.";

export default function Tareas() {
  const [aulas, setAulas] = useState([]);
  const [aulaId, setAulaId] = useState("");
  const [estado, setEstado] = useState("");
  const [seguimiento, setSeguimiento] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [recarga, setRecarga] = useState(0);

  const tareas = seguimiento?.tareas || [];

  // El aula elegida se comparte con el menú y con el resto de pantallas.
  useEffect(() => {
    if (aulaId) seleccionarAula(aulaId);
  }, [aulaId]);

  // Al abrir la pantalla se cargan las aulas y se restaura la última elegida.
  useEffect(() => {
    let vigente = true;

    listarAulas()
      .then((data) => {
        if (!vigente) return;
        setAulas(data);
        if (data.length > 0) {
          const guardada = obtenerAulaSeleccionada();
          const inicial =
            data.find((a) => String(a.id) === String(guardada)) || data[0];
          setAulaId(String(inicial.id));
        } else {
          setCargando(false);
        }
      })
      .catch((e) => {
        if (vigente) {
          setError(e.message);
          setCargando(false);
        }
      });

    return () => {
      vigente = false;
    };
  }, []);

  // Cada cambio de aula, filtro o recarga vuelve a pedir el seguimiento.
  useEffect(() => {
    if (!aulaId) return;
    let vigente = true;

    listarSeguimientoTareas(aulaId, estado ? { estado } : {})
      .then((data) => {
        if (vigente) setSeguimiento(data);
      })
      .catch((e) => {
        if (vigente) setError(e.message);
      })
      .finally(() => {
        if (vigente) setCargando(false);
      });

    return () => {
      vigente = false;
    };
  }, [aulaId, estado, recarga]);

  const reiniciar = () => {
    setSeguimiento(null);
    setCargando(true);
    setError(null);
  };

  const actualizar = () => {
    reiniciar();
    setRecarga((n) => n + 1);
  };

  const cambiarAula = (id) => {
    setAulaId(id);
    reiniciar();
  };

  const cambiarEstado = (valor) => {
    setEstado(valor);
    reiniciar();
  };

  const conteo = {
    vencida: seguimiento?.vencidas ?? 0,
    pendiente: seguimiento?.pendientes ?? 0,
    entregada: seguimiento?.entregadas ?? 0,
  };

  return (
    <div className="tareas-page">
      <section className="dashboard-hero" aria-labelledby="titulo-tareas">
        <div>
          <div className="dashboard-eyebrow">Mi aula</div>
          <h1 id="titulo-tareas" className="dashboard-titulo">
            Seguimiento de tareas
          </h1>
          <p className="dashboard-sub">
            Revisa qué tareas siguen pendientes, cuáles ya se entregaron y cuáles
            vencieron.
          </p>
        </div>

        <div className="dashboard-controles">
          {aulas.length > 0 && (
            <>
              <div>
                <label htmlFor="aula-tareas" className="visually-hidden">
                  Aula
                </label>
                <select
                  id="aula-tareas"
                  className="form-select rounded-pill"
                  value={aulaId}
                  onChange={(ev) => cambiarAula(ev.target.value)}
                >
                  {aulas.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.nombre}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label htmlFor="estado-tareas" className="visually-hidden">
                  Estado de la tarea
                </label>
                <select
                  id="estado-tareas"
                  className="form-select rounded-pill"
                  value={estado}
                  onChange={(ev) => cambiarEstado(ev.target.value)}
                >
                  <option value="">Todos los estados</option>
                  {ESTADOS.map(({ clave, plural }) => (
                    <option key={clave} value={clave}>
                      {plural}
                    </option>
                  ))}
                </select>
              </div>
            </>
          )}
          <button
            type="button"
            className="btn btn-outline-secondary rounded-pill dashboard-refresh"
            onClick={actualizar}
            disabled={cargando || aulas.length === 0}
          >
            {cargando ? (
              <span className="spinner-border spinner-border-sm" aria-hidden="true" />
            ) : (
              <i className="bi bi-arrow-clockwise" aria-hidden="true" />
            )}
            {cargando ? "Actualizando…" : "Actualizar"}
          </button>
        </div>
      </section>

      {error && (
        <div
          className="alert alert-danger d-flex justify-content-between align-items-center gap-2 flex-wrap"
          role="alert"
        >
          <span>{error}</span>
          {aulaId && (
            <button
              type="button"
              className="btn btn-sm btn-outline-danger rounded-pill"
              onClick={actualizar}
            >
              Reintentar
            </button>
          )}
        </div>
      )}

      {aulas.length === 0 && !cargando && !error ? (
        <div className="panel text-center py-5">
          <p className="mb-3">Aún no tienes aulas registradas.</p>
          <Link to="/aulas" className="btn btn-primary">
            Registrar mi primera aula
          </Link>
        </div>
      ) : cargando && !seguimiento ? (
        <div className="panel text-center py-5">
          <span className="spinner-border text-warning" role="status" aria-hidden="true" />
          <p className="text-secondary mt-3 mb-0">Cargando las tareas…</p>
        </div>
      ) : seguimiento ? (
        <>
          <section className="tareas-resumen" aria-label="Resumen de tareas">
            {ESTADOS.map(({ clave, plural, icono }) => (
              <button
                type="button"
                key={clave}
                className={`tareas-resumen-item ${clave} ${
                  estado === clave ? "activo" : ""
                }`}
                aria-pressed={estado === clave}
                onClick={() => cambiarEstado(estado === clave ? "" : clave)}
              >
                <i className={`bi ${icono}`} aria-hidden="true" />
                <b>{conteo[clave]}</b>
                <span>{plural}</span>
              </button>
            ))}
          </section>

          <section className="panel" aria-label="Tareas asignadas">
            {tareas.length === 0 ? (
              <p className="detalle-vacio">
                <i className="bi bi-calendar-x" aria-hidden="true" />
                {textoVacio(estado)}
              </p>
            ) : (
              <ul className="tareas-lista">
                {tareas.map((tarea) => (
                  <TarjetaTarea key={tarea.id} tarea={tarea} />
                ))}
              </ul>
            )}
          </section>
        </>
      ) : null}
    </div>
  );
}

function TarjetaTarea({ tarea }) {
  const estado = estadoDe(tarea.estado);
  const nombre = [tarea.nombres, tarea.apellidos].filter(Boolean).join(" ");

  return (
    <li>
      <article
        className={`tarea-card tarea-${estado.clave}`}
        aria-label={`Tarea de ${nombre || "estudiante"}: ${tarea.lecturaTitulo}`}
      >
        <span
          className="evidencia-avatar"
          style={{ background: colorAvatar(tarea.estudianteId) }}
          aria-hidden="true"
        >
          {iniciales(tarea.nombres, tarea.apellidos)}
        </span>

        <div className="tarea-datos">
          <h3 className="tarea-titulo">{tarea.lecturaTitulo}</h3>
          <span className="tarea-alumno">
            {nombre || "Estudiante"}
            {tarea.gradoNombre ? ` · ${tarea.gradoNombre}` : ""}
          </span>
          <span className="tarea-fecha">
            <i className="bi bi-calendar-event" aria-hidden="true" />
            {formatearFechaLimite(tarea.fechaLimite)}
            {tarea.estado === "entregada" && tarea.entregadaEn && (
              <>
                {" · entregada "}
                {formatearEntrega(tarea.entregadaEn)}
                {tarea.entregadaATiempo === false ? " (fuera de plazo)" : ""}
              </>
            )}
          </span>
        </div>

        <span className={`tarea-estado ${estado.clave}`}>
          <i className={`bi ${estado.icono}`} aria-hidden="true" />
          {estado.texto}
        </span>
      </article>
    </li>
  );
}
