import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { obtenerDetalleEstudiante } from "../api";
import { seleccionarAula } from "../aulaSeleccionada";
import GraficoEvolucion from "../components/GraficoEvolucion";
import FortalezasDificultades from "../components/FortalezasDificultades";

// T-292 (US-29): pantalla de detalle del estudiante con el resumen por
// competencia que devuelve el endpoint de T-290. El semáforo no se recalcula
// en el frontend: se usan los colores de la respuesta.
const COMPETENCIAS = [
  { clave: "lectura", texto: "Lectura", icono: "bi-book-fill" },
  { clave: "escritura", texto: "Escritura", icono: "bi-pencil-fill" },
  { clave: "oralidad", texto: "Oralidad", icono: "bi-mic-fill" },
];

const SEMAFORO = {
  verde: {
    texto: "En nivel esperado",
    icono: "bi-check-circle-fill",
    clase: "verde",
  },
  amarillo: {
    texto: "En proceso",
    icono: "bi-exclamation-triangle-fill",
    clase: "amarillo",
  },
  rojo: {
    texto: "Necesita refuerzo",
    icono: "bi-x-octagon-fill",
    clase: "rojo",
  },
  gris: {
    texto: "Sin evaluaciones",
    icono: "bi-dash-circle-fill",
    clase: "gris",
  },
};

const ESTADOS_NIVEL = {
  completado: {
    texto: "Completado",
    icono: "bi-check-circle-fill",
    clase: "verde",
  },
  desbloqueado: {
    texto: "Disponible",
    icono: "bi-unlock-fill",
    clase: "amarillo",
  },
  bloqueado: {
    texto: "Bloqueado",
    icono: "bi-lock-fill",
    clase: "gris",
  },
};

const COLORES_AVATAR = ["#8B5CF6", "#2FC5F6", "#F59E0B", "#EC4899", "#10B981", "#6366F1"];

const datosSemaforo = (color) => SEMAFORO[color] || SEMAFORO.gris;
const estadoDeNivel = (estado) => ESTADOS_NIVEL[estado] || ESTADOS_NIVEL.bloqueado;

const nombreCompleto = (estudiante) =>
  [estudiante?.nombres, estudiante?.apellidos].filter(Boolean).join(" ").trim();

const iniciales = (nombres = "", apellidos = "") => {
  const primera = nombres.trim().charAt(0);
  const segunda = apellidos.trim().charAt(0);
  return `${primera}${segunda}`.toUpperCase() || "?";
};

const colorAvatar = (id) =>
  COLORES_AVATAR[Number(id || 0) % COLORES_AVATAR.length];

const promedioVisible = (valor) => (valor == null ? "—" : Math.round(valor));

const anchoBarra = (valor) => {
  if (valor == null) return 0;
  return Math.min(100, Math.max(0, Math.round(valor)));
};

const textoEvaluaciones = (n) =>
  `${n} ${n === 1 ? "evaluación" : "evaluaciones"}`;

const textoPuntos = (n) => `${n} ${n === 1 ? "punto" : "puntos"}`;

const textoCompetencia = (clave) =>
  COMPETENCIAS.find(({ clave: valor }) => valor === clave)?.texto || clave;

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

export default function DetalleEstudiante() {
  const { aulaId, estudianteId } = useParams();
  const [detalle, setDetalle] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [recarga, setRecarga] = useState(0);

  // Mantiene el aula activa del menú alineada con el detalle abierto.
  useEffect(() => {
    if (aulaId) seleccionarAula(aulaId);
  }, [aulaId]);

  useEffect(() => {
    if (!aulaId || !estudianteId) return;
    let vigente = true;

    obtenerDetalleEstudiante(aulaId, estudianteId)
      .then((data) => {
        if (vigente) setDetalle(data);
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
  }, [aulaId, estudianteId, recarga]);

  const reintentar = () => {
    setDetalle(null);
    setCargando(true);
    setError(null);
    setRecarga((n) => n + 1);
  };

  const nombre = nombreCompleto(detalle);
  const semaforoGeneral = datosSemaforo(detalle?.colorGeneral);
  const resumenes = detalle?.puntajesPorCompetencia || [];
  const niveles = detalle?.niveles || [];
  const evidencias = detalle?.ultimasEvidencias || [];
  const totalEvaluaciones = resumenes.reduce(
    (total, resumen) => total + (resumen.evaluaciones || 0),
    0
  );

  return (
    <div className="detalle-page">
      <section className="dashboard-hero detalle-hero" aria-labelledby="titulo-detalle">
        <div className="detalle-hero-main">
          <span
            className="detalle-avatar"
            style={{ background: colorAvatar(detalle?.estudianteId || estudianteId) }}
            aria-hidden="true"
          >
            {detalle ? iniciales(detalle.nombres, detalle.apellidos) : "?"}
          </span>
          <div>
            <Link to="/dashboard" className="detalle-volver">
              <i className="bi bi-arrow-left" aria-hidden="true" />
              Volver al dashboard
            </Link>
            <div className="dashboard-eyebrow">Detalle del estudiante</div>
            <h1 id="titulo-detalle" className="dashboard-titulo detalle-titulo">
              {nombre || "Detalle del estudiante"}
            </h1>
            <p className="dashboard-sub">
              {detalle
                ? [detalle.gradoNombre, "Resumen por competencia"]
                    .filter(Boolean)
                    .join(" · ")
                : "Resumen por competencia del alumno."}
            </p>
          </div>
        </div>

        <button
          type="button"
          className="btn btn-outline-secondary rounded-pill dashboard-refresh"
          onClick={reintentar}
          disabled={cargando}
        >
          {cargando ? (
            <span className="spinner-border spinner-border-sm" aria-hidden="true" />
          ) : (
            <i className="bi bi-arrow-clockwise" aria-hidden="true" />
          )}
          {cargando ? "Actualizando…" : "Actualizar"}
        </button>
      </section>

      {error ? (
        <div
          className="alert alert-danger d-flex justify-content-between align-items-center gap-2 flex-wrap"
          role="alert"
        >
          <span>{error}</span>
          <div className="d-flex gap-2 flex-wrap">
            <button
              type="button"
              className="btn btn-sm btn-outline-danger rounded-pill"
              onClick={reintentar}
            >
              Reintentar
            </button>
            <Link
              to="/dashboard"
              className="btn btn-sm btn-outline-secondary rounded-pill"
            >
              Volver al dashboard
            </Link>
          </div>
        </div>
      ) : cargando && !detalle ? (
        <div className="panel text-center py-5">
          <span
            className="spinner-border text-warning"
            role="status"
            aria-hidden="true"
          />
          <p className="text-secondary mt-3 mb-0">
            Cargando detalle del estudiante…
          </p>
        </div>
      ) : detalle ? (
        <>
          <section
            className="detalle-resumen"
            aria-label="Resumen general del estudiante"
          >
            <article
              className={`detalle-general semaforo-${semaforoGeneral.clase}`}
              aria-label="Promedio general del estudiante"
            >
              <span className="dashboard-promedio-label">Promedio general</span>
              <div className="dashboard-promedio-valor detalle-general-valor">
                <b>{promedioVisible(detalle.promedioGeneral)}</b>
                <span>/100</span>
              </div>
              <span className={`semaforo-badge ${semaforoGeneral.clase}`}>
                <i className={`bi ${semaforoGeneral.icono}`} aria-hidden="true" />
                {semaforoGeneral.texto}
              </span>
              <p className="detalle-general-meta">
                <i className="bi bi-clipboard-check" aria-hidden="true" />
                {textoEvaluaciones(totalEvaluaciones)} registradas
              </p>
            </article>

            <section
              className="panel detalle-competencias-panel"
              aria-label="Resumen por competencia"
            >
              <div className="detalle-seccion-head">
                <div>
                  <h2 className="h5 fw-bold mb-1">Resumen por competencia</h2>
                  <p className="text-secondary mb-0 small">
                    {detalle.gradoNombre || "Sin grado"} · {nombre || "Estudiante"}
                  </p>
                </div>
              </div>

              {resumenes.length === 0 && (
                <p className="detalle-aviso">
                  <i className="bi bi-info-circle" aria-hidden="true" />
                  Aún no hay evaluaciones registradas para este estudiante.
                </p>
              )}

              <div className="detalle-competencias">
                {COMPETENCIAS.map((competencia) => (
                  <TarjetaCompetencia
                    key={competencia.clave}
                    competencia={competencia}
                    resumen={resumenes.find(
                      (item) => item.competencia === competencia.clave
                    )}
                  />
                ))}
              </div>
            </section>
          </section>

          <GraficoEvolucion evidencias={evidencias} nombre={nombre} />

          <FortalezasDificultades puntajes={resumenes} />

          <section className="panel" aria-label="Ruta de aprendizaje">
            <div className="detalle-seccion-head">
              <div>
                <h2 className="h5 fw-bold mb-1">Ruta de aprendizaje</h2>
                <p className="text-secondary mb-0 small">
                  {niveles.length === 0
                    ? "Sin niveles registrados."
                    : `${niveles.length} ${niveles.length === 1 ? "nivel" : "niveles"} del mapa`}
                </p>
              </div>
            </div>

            {niveles.length === 0 ? (
              <p className="detalle-vacio">
                <i className="bi bi-map" aria-hidden="true" />
                El mapa de niveles todavía no tiene información.
              </p>
            ) : (
              <ol className="detalle-niveles-lista">
                {niveles.map((nivel) => {
                  const estado = estadoDeNivel(nivel.estado);
                  return (
                    <li className="detalle-nivel" key={nivel.id}>
                      <span
                        className={`detalle-nivel-icono ${estado.clase}`}
                        aria-hidden="true"
                      >
                        <i className={`bi ${estado.icono}`} />
                      </span>
                      <div className="detalle-nivel-info">
                        <span className="detalle-nivel-nombre">
                          {nivel.orden}. {nivel.nombre}
                        </span>
                        <span className="detalle-nivel-desc">
                          {nivel.descripcion || nivel.tipoReto}
                        </span>
                      </div>
                      <span className={`semaforo-badge ${estado.clase}`}>
                        {estado.texto}
                      </span>
                      <span className="detalle-nivel-puntos">
                        {textoPuntos(nivel.puntos || 0)}
                      </span>
                    </li>
                  );
                })}
              </ol>
            )}
          </section>

          <section className="panel" aria-label="Últimas evaluaciones">
            <div className="detalle-seccion-head">
              <div>
                <h2 className="h5 fw-bold mb-1">Últimas evaluaciones</h2>
                <p className="text-secondary mb-0 small">
                  Hasta 10 evaluaciones, de la más reciente a la más antigua.
                </p>
              </div>
            </div>

            {evidencias.length === 0 ? (
              <p className="detalle-vacio">
                <i className="bi bi-clipboard-x" aria-hidden="true" />
                Todavía no hay evaluaciones registradas.
              </p>
            ) : (
              <div className="table-responsive">
                <table className="table tabla detalle-evaluaciones align-middle mb-0">
                  <thead>
                    <tr>
                      <th scope="col">Competencia</th>
                      <th scope="col">Puntaje</th>
                      <th scope="col">Fecha</th>
                    </tr>
                  </thead>
                  <tbody>
                    {evidencias.map((evidencia) => (
                      <tr key={evidencia.id}>
                        <td>{textoCompetencia(evidencia.competencia)}</td>
                        <td>
                          <span className="detalle-puntaje">
                            {evidencia.puntaje}/100
                          </span>
                        </td>
                        <td>{formatearFecha(evidencia.evaluadoEn)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </>
      ) : null}
    </div>
  );
}

function TarjetaCompetencia({ competencia, resumen }) {
  const semaforo = datosSemaforo(resumen?.color);
  const promedio = resumen?.promedio ?? null;
  const evaluaciones = resumen?.evaluaciones ?? 0;
  const ultimoPuntaje = resumen?.ultimoPuntaje ?? null;

  return (
    <article
      className={`detalle-competencia-card semaforo-${semaforo.clase}`}
      aria-label={`Competencia ${competencia.texto}`}
    >
      <div className="detalle-competencia-head">
        <span className="detalle-competencia-icono" aria-hidden="true">
          <i className={`bi ${competencia.icono}`} />
        </span>
        <h3>{competencia.texto}</h3>
        <span className={`semaforo-badge ${semaforo.clase}`}>
          <i className={`bi ${semaforo.icono}`} aria-hidden="true" />
          {semaforo.texto}
        </span>
      </div>

      <div className="detalle-competencia-valor">
        <b>{promedioVisible(promedio)}</b>
        <span>/100</span>
      </div>

      <div className="detalle-competencia-barra" aria-hidden="true">
        <span style={{ width: `${anchoBarra(promedio)}%` }} />
      </div>

      <dl className="detalle-competencia-meta">
        <div>
          <dt>Evaluaciones</dt>
          <dd>{evaluaciones}</dd>
        </div>
        <div>
          <dt>Último puntaje</dt>
          <dd>{ultimoPuntaje == null ? "—" : `${ultimoPuntaje}/100`}</dd>
        </div>
      </dl>
    </article>
  );
}
