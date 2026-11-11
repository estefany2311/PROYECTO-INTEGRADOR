import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { listarAulas, listarEvidenciasPendientes } from "../api";
import { obtenerAulaSeleccionada, seleccionarAula } from "../aulaSeleccionada";

// T-299 (US-31): lista de evidencias pendientes de revisión con filtro por tipo.
// Consume el endpoint de T-298, que ya devuelve el alumno, su grado, el nivel y
// el resultado de la IA en una sola llamada.
const TIPOS = [
  { clave: "audio", texto: "Audio", icono: "bi-mic-fill" },
  { clave: "texto", texto: "Texto", icono: "bi-pencil-fill" },
];

const COLORES_AVATAR = ["#8B5CF6", "#2FC5F6", "#F59E0B", "#EC4899", "#10B981", "#6366F1"];

const tipoDe = (clave) =>
  TIPOS.find(({ clave: valor }) => valor === clave) || {
    clave: clave || "texto",
    texto: clave || "Evidencia",
    icono: "bi-file-earmark-text-fill",
  };

const iniciales = (nombres = "", apellidos = "") => {
  const primera = nombres.trim().charAt(0);
  const segunda = apellidos.trim().charAt(0);
  return `${primera}${segunda}`.toUpperCase() || "?";
};

const colorAvatar = (id) =>
  COLORES_AVATAR[Number(id || 0) % COLORES_AVATAR.length];

const nombreCompleto = (evidencia) =>
  [evidencia?.nombres, evidencia?.apellidos].filter(Boolean).join(" ").trim();

const textoEvidencias = (n) =>
  `${n} ${n === 1 ? "evidencia pendiente" : "evidencias pendientes"}`;

const formatearFecha = (valor) => {
  if (!valor) return "—";
  const fecha = new Date(valor);
  if (Number.isNaN(fecha.getTime())) return "—";
  return new Intl.DateTimeFormat("es-PE", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(fecha);
};

const textoVacio = (tipo) => {
  if (tipo === "audio") {
    return "No hay evidencias de audio pendientes de revisión.";
  }
  if (tipo === "texto") {
    return "No hay evidencias de texto pendientes de revisión.";
  }
  return "No hay evidencias pendientes de revisión.";
};

export default function Evidencias() {
  const [aulas, setAulas] = useState([]);
  const [aulaId, setAulaId] = useState("");
  const [tipo, setTipo] = useState("");
  const [evidencias, setEvidencias] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [recarga, setRecarga] = useState(0);

  const aula = aulas.find((a) => String(a.id) === String(aulaId));
  const lista = evidencias || [];
  const audio = lista.filter((e) => e.tipo === "audio").length;
  const texto = lista.filter((e) => e.tipo === "texto").length;

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

  // Cada cambio de aula, filtro o recarga vuelve a pedir las pendientes.
  useEffect(() => {
    if (!aulaId) return;
    let vigente = true;

    listarEvidenciasPendientes(aulaId, tipo ? { tipo } : {})
      .then((data) => {
        if (vigente) setEvidencias(data || []);
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
  }, [aulaId, tipo, recarga]);

  const actualizar = () => {
    setEvidencias(null);
    setCargando(true);
    setError(null);
    setRecarga((n) => n + 1);
  };

  const cambiarAula = (id) => {
    setAulaId(id);
    setEvidencias(null);
    setCargando(true);
    setError(null);
  };

  const cambiarTipo = (valor) => {
    setTipo(valor);
    setEvidencias(null);
    setCargando(true);
    setError(null);
  };

  const resumenAula = [
    aula?.nombre,
    lista.length > 0 ? `${audio} audio · ${texto} texto` : null,
  ]
    .filter(Boolean)
    .join(" · ");

  return (
    <div className="evidencias-page">
      <section className="dashboard-hero" aria-labelledby="titulo-evidencias">
        <div>
          <div className="dashboard-eyebrow">Evaluación</div>
          <h1 id="titulo-evidencias" className="dashboard-titulo">
            Evidencias pendientes
            <span className="evidencias-icono-titulo" aria-hidden="true">
              <i className="bi bi-inbox-fill" />
            </span>
          </h1>
          <p className="dashboard-sub">
            Audios y textos que esperan tu revisión
            {resumenAula ? ` · ${resumenAula}` : ""}.
          </p>
        </div>

        <div className="dashboard-controles">
          {aulas.length > 0 && (
            <>
              <div>
                <label htmlFor="aula-evidencias" className="visually-hidden">
                  Aula
                </label>
                <select
                  id="aula-evidencias"
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
                <label htmlFor="tipo-evidencias" className="visually-hidden">
                  Tipo de evidencia
                </label>
                <select
                  id="tipo-evidencias"
                  className="form-select rounded-pill"
                  value={tipo}
                  onChange={(ev) => cambiarTipo(ev.target.value)}
                >
                  <option value="">Todos los tipos</option>
                  {TIPOS.map(({ clave, texto: etiqueta }) => (
                    <option key={clave} value={clave}>
                      {etiqueta}
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
      ) : cargando && !evidencias ? (
        <div className="panel text-center py-5">
          <span
            className="spinner-border text-warning"
            role="status"
            aria-hidden="true"
          />
          <p className="text-secondary mt-3 mb-0">
            Cargando evidencias pendientes…
          </p>
        </div>
      ) : evidencias ? (
        <section className="panel evidencias-panel" aria-label="Evidencias pendientes">
          <div className="detalle-seccion-head">
            <div>
              <h2 className="h5 fw-bold mb-1">Por revisar</h2>
              <p className="text-secondary mb-0 small">
                {textoEvidencias(lista.length)}
                {tipo ? ` de tipo ${tipo}` : ""} · de la más reciente a la más
                antigua.
              </p>
            </div>
          </div>

          {lista.length === 0 ? (
            <p className="detalle-vacio">
              <i className="bi bi-inbox" aria-hidden="true" />
              {textoVacio(tipo)}
            </p>
          ) : (
            <ul className="evidencias-lista">
              {lista.map((evidencia) => (
                <TarjetaEvidencia
                  key={evidencia.id}
                  evidencia={evidencia}
                  aulaId={aulaId}
                />
              ))}
            </ul>
          )}
        </section>
      ) : null}
    </div>
  );
}

function TarjetaEvidencia({ evidencia, aulaId }) {
  const tipo = tipoDe(evidencia.tipo);
  const nombre = nombreCompleto(evidencia);
  const meta = [
    evidencia.gradoNombre,
    evidencia.nivelNombre
      ? `${evidencia.nivelOrden}. ${evidencia.nivelNombre}`
      : null,
  ]
    .filter(Boolean)
    .join(" · ");

  return (
    <li>
      <article
        className={`evidencia-card evidencia-${tipo.clave}`}
        aria-label={`Evidencia de ${nombre || "estudiante"}`}
      >
        <div className="evidencia-head">
          <span
            className="evidencia-avatar"
            style={{ background: colorAvatar(evidencia.estudianteId) }}
            aria-hidden="true"
          >
            {iniciales(evidencia.nombres, evidencia.apellidos)}
          </span>
          <div className="evidencia-datos">
            <h3 className="evidencia-nombre">{nombre || "Estudiante"}</h3>
            <span className="evidencia-meta">{meta || "Sin nivel"}</span>
          </div>
          <span className={`evidencia-tipo ${tipo.clave}`}>
            <i className={`bi ${tipo.icono}`} aria-hidden="true" />
            {tipo.texto}
          </span>
        </div>

        {tipo.clave === "audio" ? (
          <a
            className="evidencia-audio"
            href={evidencia.audioUrl || "#"}
            target="_blank"
            rel="noreferrer"
          >
            <i className="bi bi-play-circle-fill" aria-hidden="true" />
            Escuchar audio
          </a>
        ) : (
          <blockquote className="evidencia-texto">
            {evidencia.texto || "Sin texto registrado."}
          </blockquote>
        )}

        <p className="evidencia-ia">
          <i className="bi bi-stars" aria-hidden="true" />
          <span>
            <b>Resultado de la IA:</b>{" "}
            {evidencia.resultadoIa || "todavía no generó un resultado."}
          </span>
        </p>

        <div className="evidencia-pie">
          <span className="evidencia-fecha">
            <i className="bi bi-clock" aria-hidden="true" />
            {formatearFecha(evidencia.creadoEn)}
          </span>
          <Link
            className="evidencia-detalle"
            to={`/dashboard/aulas/${aulaId}/estudiantes/${evidencia.estudianteId}`}
          >
            Ver detalle del alumno
            <i className="bi bi-arrow-right" aria-hidden="true" />
          </Link>
        </div>
      </article>
    </li>
  );
}
