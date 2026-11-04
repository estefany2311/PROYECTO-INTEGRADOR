import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { listarAulas, listarDashboardAula } from "../api";

// T-279 (US-27): dashboard del aula con tarjetas por estudiante.
// Consume el endpoint de T-278 y reutiliza la regla del semáforo de T-277.
const COMPETENCIAS = [
  { clave: "lectura", texto: "Lectura" },
  { clave: "escritura", texto: "Escritura" },
  { clave: "oralidad", texto: "Oralidad" },
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

const LEYENDA = ["verde", "amarillo", "rojo", "gris"].map((color) => ({
  color,
  ...SEMAFORO[color],
}));

const COLORES_AVATAR = ["#8B5CF6", "#2FC5F6", "#F59E0B", "#EC4899", "#10B981", "#6366F1"];

const datosSemaforo = (color) => SEMAFORO[color] || SEMAFORO.gris;

const textoEstudiantes = (n) => `${n} ${n === 1 ? "estudiante" : "estudiantes"}`;
const textoAlumnos = (n) => `${n} ${n === 1 ? "alumno" : "alumnos"}`;

const iniciales = (nombres = "", apellidos = "") => {
  const primera = nombres.trim().charAt(0);
  const segunda = apellidos.trim().charAt(0);
  return `${primera}${segunda}`.toUpperCase() || "?";
};

const colorAvatar = (id) =>
  COLORES_AVATAR[Number(id || 0) % COLORES_AVATAR.length];

const nombreCompleto = (estudiante) =>
  [estudiante.nombres, estudiante.apellidos].filter(Boolean).join(" ").trim();

const promedioVisible = (valor) => (valor == null ? "—" : Math.round(valor));

const competenciaDe = (estudiante, clave) =>
  estudiante.competencias?.find((c) => c.competencia === clave)?.promedio;

const porcentajeDe = (valor, total) => {
  if (!total) return 0;
  return Math.min(100, Math.max(0, Math.round((valor / total) * 100)));
};

const anchoBarra = (valor) => {
  if (valor == null) return 0;
  return Math.min(100, Math.max(0, Math.round(valor)));
};

export default function Dashboard() {
  const [aulas, setAulas] = useState([]);
  const [aulaId, setAulaId] = useState("");
  const [dashboard, setDashboard] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [recarga, setRecarga] = useState(0);

  const aula = aulas.find((a) => String(a.id) === String(aulaId));

  // Al abrir la pantalla se cargan las aulas y se elige la primera.
  useEffect(() => {
    let vigente = true;

    listarAulas()
      .then((data) => {
        if (!vigente) return;
        setAulas(data);
        if (data.length > 0) {
          setAulaId(String(data[0].id));
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

  // Cada cambio de aula (o recarga manual) vuelve a pedir el dashboard.
  useEffect(() => {
    if (!aulaId) return;
    let vigente = true;

    listarDashboardAula(aulaId)
      .then((data) => {
        if (vigente) setDashboard(data);
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
  }, [aulaId, recarga]);

  const actualizar = () => {
    setDashboard(null);
    setCargando(true);
    setError(null);
    setRecarga((n) => n + 1);
  };

  const cambiarAula = (id) => {
    setAulaId(id);
    setDashboard(null);
    setCargando(true);
    setError(null);
  };

  const resumen = dashboard?.resumen || {
    totalEstudiantes: 0,
    enNivelEsperado: 0,
    enProceso: 0,
    necesitanRefuerzo: 0,
    sinEvaluaciones: 0,
  };
  const estudiantes = dashboard?.estudiantes || [];
  const estudiantesRefuerzo = estudiantes.filter(
    (estudiante) => estudiante.color === "rojo"
  );
  const promediosAula = dashboard?.promediosPorCompetencia || [];
  const colorGeneral = dashboard?.colorGeneral || "gris";
  const aulaNombre = dashboard?.aulaNombre || aula?.nombre || "";
  const grados = dashboard?.grados || aula?.grados || [];
  const gradosTexto = grados.map((g) => g.nombre).filter(Boolean).join(", ");
  const detallesAula = [aulaNombre, gradosTexto].filter(Boolean).join(" · ");

  const tarjetas = [
    {
      clave: "total",
      etiqueta: "Alumnos en el aula",
      valor: resumen.totalEstudiantes,
      icono: "bi-people-fill",
      clase: "resumen-total",
      pill: aula?.nombre || "Aula",
      barra: resumen.totalEstudiantes > 0 ? 100 : 0,
    },
    {
      clave: "verde",
      etiqueta: "En nivel esperado",
      valor: resumen.enNivelEsperado,
      icono: "bi-check-circle-fill",
      clase: "resumen-verde",
      pill: "Verde",
      barra: porcentajeDe(resumen.enNivelEsperado, resumen.totalEstudiantes),
    },
    {
      clave: "amarillo",
      etiqueta: "En proceso",
      valor: resumen.enProceso,
      icono: "bi-exclamation-triangle-fill",
      clase: "resumen-amarillo",
      pill: "Amarillo",
      barra: porcentajeDe(resumen.enProceso, resumen.totalEstudiantes),
    },
    {
      clave: "rojo",
      etiqueta: "Necesitan refuerzo",
      valor: resumen.necesitanRefuerzo,
      icono: "bi-x-octagon-fill",
      clase: "resumen-rojo",
      pill: "Rojo",
      barra: porcentajeDe(resumen.necesitanRefuerzo, resumen.totalEstudiantes),
    },
  ];

  return (
    <div className="dashboard-page">
      <section className="dashboard-hero" aria-labelledby="titulo-dashboard">
        <div>
          <div className="dashboard-eyebrow">Evaluación</div>
          <h1 id="titulo-dashboard" className="dashboard-titulo">
            Dashboard del aula
            <span className="semaforo-dots" aria-hidden="true">
              <span className="verde" />
              <span className="amarillo" />
              <span className="rojo" />
              <span className="gris" />
            </span>
          </h1>
          <p className="dashboard-sub">
            ¡Hola, Noemí! 👋 Este es el resumen de rendimiento de tu aula hoy
            {detallesAula ? ` · ${detallesAula}` : ""}.
          </p>
        </div>

        <div className="dashboard-controles">
          {aulas.length > 0 && (
            <div>
              <label htmlFor="aula-dashboard" className="visually-hidden">Aula</label>
              <select
                id="aula-dashboard"
                className="form-select rounded-pill"
                value={aulaId}
                onChange={(ev) => cambiarAula(ev.target.value)}
              >
                {aulas.map((a) => (
                  <option key={a.id} value={a.id}>{a.nombre}</option>
                ))}
              </select>
            </div>
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
        <div className="alert alert-danger d-flex justify-content-between align-items-center gap-2 flex-wrap" role="alert">
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
      ) : cargando && !dashboard ? (
        <div className="panel text-center py-5">
          <span className="spinner-border text-warning" role="status" aria-hidden="true" />
          <p className="text-secondary mt-3 mb-0">Cargando dashboard…</p>
        </div>
      ) : dashboard ? (
        <>
          <section className="dashboard-resumen" aria-label="Resumen del aula">
            {tarjetas.map((tarjeta) => (
              <article key={tarjeta.clave} className={`resumen-card ${tarjeta.clase}`}>
                <div className="resumen-card-top">
                  <span className="resumen-icono">
                    <i className={`bi ${tarjeta.icono}`} aria-hidden="true" />
                  </span>
                  <span className="resumen-pill" title={tarjeta.pill}>{tarjeta.pill}</span>
                </div>
                <div>
                  <b>{tarjeta.valor}</b>
                  <span className="resumen-label">{tarjeta.etiqueta}</span>
                  <div className="resumen-barra" aria-hidden="true">
                    <span style={{ width: `${tarjeta.barra}%` }} />
                  </div>
                </div>
              </article>
            ))}
          </section>

          {resumen.sinEvaluaciones > 0 && (
            <p className="resumen-nota">
              <i className="bi bi-dash-circle" aria-hidden="true" />
              {textoAlumnos(resumen.sinEvaluaciones)} sin evaluaciones (gris).
            </p>
          )}

          <SeccionRefuerzo estudiantes={estudiantesRefuerzo} />

          <LeyendaSemaforo />

          {estudiantes.length === 0 ? (
            <div className="panel text-center py-5">
              <p className="mb-1 fw-semibold">Aún no hay alumnos activos en esta aula.</p>
              <p className="text-secondary mb-3">
                Agrega alumnos para comenzar a ver su rendimiento.
              </p>
              <Link to="/alumnos" className="btn btn-primary">Ir a Alumnos</Link>
            </div>
          ) : (
            <section className="dashboard-alumnos" aria-label="Rendimiento por estudiante">
              {estudiantes.map((estudiante) => (
                <TarjetaEstudiante
                  key={estudiante.id}
                  estudiante={estudiante}
                  aulaNombre={aulaNombre}
                />
              ))}
            </section>
          )}

          <PromediosAula
            promedios={promediosAula}
            promedioGeneral={dashboard.promedioGeneral}
            colorGeneral={colorGeneral}
            totalEstudiantes={resumen.totalEstudiantes}
            aulaNombre={aulaNombre}
          />
        </>
      ) : null}
    </div>
  );
}

function LeyendaSemaforo() {
  return (
    <section className="leyenda-semaforo" aria-labelledby="leyenda-semaforo-titulo">
      <h2 id="leyenda-semaforo-titulo" className="leyenda-titulo">
        Leyenda del semáforo
      </h2>
      <ul className="leyenda-lista">
        {LEYENDA.map(({ color, texto, icono }) => (
          <li className="leyenda-item" key={color}>
            <span className={`semaforo-badge ${color}`}>
              <i className={`bi ${icono}`} aria-hidden="true" />
              {texto}
            </span>
          </li>
        ))}
      </ul>
    </section>
  );
}

function SeccionRefuerzo({ estudiantes }) {
  if (estudiantes.length === 0) return null;

  return (
    <section className="dashboard-refuerzo" aria-labelledby="refuerzo-titulo">
      <span className="refuerzo-icono" aria-hidden="true">
        <i className={`bi ${SEMAFORO.rojo.icono}`} />
      </span>
      <div className="refuerzo-contenido">
        <div className="refuerzo-head">
          <h2 id="refuerzo-titulo" className="refuerzo-titulo">
            Necesitan refuerzo
          </h2>
          <span
            className="refuerzo-contador"
            aria-label={`${textoAlumnos(estudiantes.length)} en rojo`}
          >
            {estudiantes.length}
          </span>
        </div>
        <ul className="refuerzo-lista">
          {estudiantes.map((estudiante) => (
            <li className="refuerzo-item" key={estudiante.id}>
              {nombreCompleto(estudiante)} · {estudiante.gradoNombre || "Sin grado"} · Promedio{" "}
              {promedioVisible(estudiante.promedio)}/100
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}

function TarjetaEstudiante({ estudiante, aulaNombre }) {
  const semaforo = datosSemaforo(estudiante.color);
  const nombre = nombreCompleto(estudiante);
  const sinDatos = !estudiante.competencias || estudiante.competencias.length === 0;

  return (
    <article className={`alumno-card semaforo-${semaforo.clase}`}>
      <div className="alumno-card-head">
        <span
          className="alumno-avatar"
          style={{ background: colorAvatar(estudiante.id) }}
          aria-hidden="true"
        >
          {iniciales(estudiante.nombres, estudiante.apellidos)}
        </span>
        <div className="alumno-datos">
          <h2 className="alumno-nombre mb-0">{nombre}</h2>
          <div className="alumno-meta">
            {estudiante.gradoNombre || "Sin grado"} · {aulaNombre}
          </div>
        </div>
      </div>

      <div className="alumno-promedio-line">
        <div className="alumno-promedio">
          <b>{promedioVisible(estudiante.promedio)}</b>
          <span>/100</span>
        </div>
        <span className={`semaforo-badge ${semaforo.clase}`}>
          <i className={`bi ${semaforo.icono}`} aria-hidden="true" />
          {semaforo.texto}
        </span>
      </div>

      {sinDatos ? (
        <p className="alumno-sin-datos mb-0">
          <i className="bi bi-info-circle me-1" aria-hidden="true" />
          Aún no tiene evaluaciones registradas.
        </p>
      ) : (
        <div className="alumno-competencias">
          {COMPETENCIAS.map(({ clave, texto }) => {
            const valor = competenciaDe(estudiante, clave);
            return (
              <div className="competencia-line" key={clave}>
                <span className="competencia-nombre">{texto}</span>
                <div className="competencia-barra" aria-hidden="true">
                  <span style={{ width: `${anchoBarra(valor)}%` }} />
                </div>
                <span className="competencia-valor">{promedioVisible(valor)}</span>
              </div>
            );
          })}
        </div>
      )}
    </article>
  );
}

function PromediosAula({
  promedios,
  promedioGeneral,
  colorGeneral,
  totalEstudiantes,
  aulaNombre,
}) {
  const semaforo = datosSemaforo(colorGeneral);

  return (
    <section className="panel dashboard-aula-panel" aria-labelledby="titulo-promedios-aula">
      <div className="dashboard-aula-head">
        <div>
          <h2 id="titulo-promedios-aula" className="h5 fw-bold mb-1">
            Promedio del aula por competencia
          </h2>
          <p className="text-secondary mb-0 small">
            {textoEstudiantes(totalEstudiantes)} · {aulaNombre}
          </p>
        </div>
      </div>

      {promedios.length === 0 ? (
        <div className="dashboard-vacio">
          <i className="bi bi-bar-chart-line" aria-hidden="true" />
          <p className="mb-0">
            Aún no hay evaluaciones para calcular los promedios del aula.
          </p>
        </div>
      ) : (
        <div className="dashboard-aula-grid">
          <div className="dashboard-aula-barras">
            {COMPETENCIAS.map(({ clave, texto }) => {
              const promedio = promedios.find((p) => p.competencia === clave)?.promedio;
              return (
                <div className="aula-barra-line" key={clave}>
                  <span className="competencia-nombre">{texto}</span>
                  <div className="aula-barra" aria-hidden="true">
                    <span style={{ width: `${anchoBarra(promedio)}%` }} />
                  </div>
                  <span className="aula-barra-valor">
                    {promedioVisible(promedio)}<span>/100</span>
                  </span>
                </div>
              );
            })}
          </div>

          <div className="dashboard-promedio-general">
            <span className="dashboard-promedio-label">Promedio general del aula</span>
            <div className="dashboard-promedio-valor">
              <b>{promedioVisible(promedioGeneral)}</b>
              <span>/100</span>
            </div>
            <span className={`semaforo-badge ${semaforo.clase}`}>
              <i className={`bi ${semaforo.icono}`} aria-hidden="true" />
              {semaforo.texto}
            </span>
          </div>
        </div>
      )}
    </section>
  );
}
