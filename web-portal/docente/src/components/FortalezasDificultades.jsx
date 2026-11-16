// T-294 (US-29): fortalezas y dificultades del estudiante.
// Reutiliza el resumen por competencia del detalle (T-290) y clasifica con el
// semáforo: verde = fortaleza, rojo = dificultad. El amarillo queda en proceso.
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

const competenciaDe = (clave) =>
  COMPETENCIAS.find(({ clave: valor }) => valor === clave) || {
    clave,
    texto: clave,
    icono: "bi-bookmark-fill",
  };

const datosSemaforo = (color) => SEMAFORO[color] || SEMAFORO.gris;
const promedioVisible = (valor) => (valor == null ? "—" : Math.round(valor));
const textoEvaluaciones = (n) =>
  `${n} ${n === 1 ? "evaluación" : "evaluaciones"}`;

export default function FortalezasDificultades({ puntajes = [] }) {
  const fortalezas = puntajes.filter((item) => item.color === "verde");
  const dificultades = puntajes.filter((item) => item.color === "rojo");
  const enProceso = puntajes.filter((item) => item.color === "amarillo");

  if (puntajes.length === 0) {
    return (
      <section className="panel" aria-label="Fortalezas y dificultades">
        <div className="detalle-seccion-head">
          <div>
            <h2 className="h5 fw-bold mb-1">Fortalezas y dificultades</h2>
            <p className="text-secondary mb-0 small">
              Se calculan con el semáforo de cada competencia.
            </p>
          </div>
        </div>
        <p className="detalle-vacio">
          <i className="bi bi-clipboard-heart" aria-hidden="true" />
          Aún no hay evaluaciones para identificar fortalezas y dificultades.
        </p>
      </section>
    );
  }

  return (
    <section
      className="panel fortalezas-panel"
      aria-label="Fortalezas y dificultades"
    >
      <div className="detalle-seccion-head">
        <div>
          <h2 className="h5 fw-bold mb-1">Fortalezas y dificultades</h2>
          <p className="text-secondary mb-0 small">
            Se calculan con el semáforo de cada competencia.
          </p>
        </div>
      </div>

      <div className="fortalezas-grid">
        <GrupoCompetencias
          tipo="fortaleza"
          titulo="Fortalezas"
          icono="bi-stars"
          descripcion="Competencias en nivel esperado (verde)."
          items={fortalezas}
          vacio="Todavía no hay competencias en nivel esperado."
        />
        <GrupoCompetencias
          tipo="dificultad"
          titulo="Dificultades"
          icono="bi-exclamation-triangle-fill"
          descripcion="Competencias que necesitan refuerzo (rojo)."
          items={dificultades}
          vacio="No se identifican dificultades: ninguna competencia está en rojo."
        />
      </div>

      {enProceso.length > 0 && (
        <p className="fortalezas-nota">
          <i className="bi bi-info-circle" aria-hidden="true" />
          En proceso (amarillo):{" "}
          {enProceso
            .map(
              (item) =>
                `${competenciaDe(item.competencia).texto} ${promedioVisible(item.promedio)}/100`
            )
            .join(" · ")}
          . No se cuentan como fortaleza ni dificultad.
        </p>
      )}
    </section>
  );
}

function GrupoCompetencias({ tipo, titulo, icono, descripcion, items, vacio }) {
  const total = items.length;

  return (
    <section className={`fortalezas-grupo ${tipo}`} aria-label={titulo}>
      <div className="fortalezas-grupo-head">
        <span className="fortalezas-grupo-icono" aria-hidden="true">
          <i className={`bi ${icono}`} />
        </span>
        <div className="fortalezas-grupo-texto">
          <h3>{titulo}</h3>
          <p>{descripcion}</p>
        </div>
        <span
          className="fortalezas-contador"
          aria-label={`${total} ${total === 1 ? "competencia" : "competencias"}`}
        >
          {total}
        </span>
      </div>

      {total === 0 ? (
        <p className="fortalezas-vacio">{vacio}</p>
      ) : (
        <ul className="fortalezas-lista">
          {items.map((item) => {
            const competencia = competenciaDe(item.competencia);
            const semaforo = datosSemaforo(item.color);
            return (
              <li className="fortalezas-item" key={item.competencia}>
                <span className="fortalezas-item-icono" aria-hidden="true">
                  <i className={`bi ${competencia.icono}`} />
                </span>
                <div className="fortalezas-item-info">
                  <span className="fortalezas-item-nombre">
                    {competencia.texto}
                  </span>
                  <span className="fortalezas-item-meta">
                    {textoEvaluaciones(item.evaluaciones || 0)}
                  </span>
                </div>
                <span className="fortalezas-item-promedio">
                  {promedioVisible(item.promedio)}
                  <span>/100</span>
                </span>
                <span className={`semaforo-badge ${semaforo.clase}`}>
                  <i className={`bi ${semaforo.icono}`} aria-hidden="true" />
                  {semaforo.texto}
                </span>
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}
