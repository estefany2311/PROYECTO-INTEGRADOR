// US-47 · T-270: gráficos de la pantalla de Reportes (sin librerías, solo HTML/SVG y CSS)

const COMPETENCIAS = [
  { clave: "lectura", texto: "Lectura" },
  { clave: "escritura", texto: "Escritura" },
  { clave: "oralidad", texto: "Oralidad" },
];

const SEMAFORO = [
  { clave: "verde", texto: "Logrado", rango: "75 o más" },
  { clave: "amarillo", texto: "En proceso", rango: "50 a 74" },
  { clave: "rojo", texto: "En inicio", rango: "menos de 50" },
  { clave: "sin_evaluar", texto: "Sin evaluar", rango: "sin evaluaciones" },
];

// Barras verticales: promedio (0 a 100) de cada competencia
export function GraficoCompetencias({ promedios = {}, alcance, cargando }) {
  const hayDatos = COMPETENCIAS.some((c) => promedios[c.clave] !== null && promedios[c.clave] !== undefined);

  return (
    <div className="card h-100">
      <div className="card-header d-flex flex-wrap justify-content-between align-items-center gap-2">
        <h2>Promedio por competencia</h2>
        <span className="note">{alcance}</span>
      </div>
      <div className="card-body">
        {cargando ? (
          <div className="text-center text-secondary py-5">Cargando…</div>
        ) : !hayDatos ? (
          <div className="text-center text-secondary py-5">Todavía no hay evaluaciones para mostrar.</div>
        ) : (
          <div className="grafico-barras" role="img"
            aria-label={COMPETENCIAS.map((c) => `${c.texto}: ${promedios[c.clave] ?? "sin datos"}`).join(", ")}>
            {/* Líneas guía del semáforo */}
            <div className="guia guia-75"><span>75</span></div>
            <div className="guia guia-50"><span>50</span></div>
            {COMPETENCIAS.map((c) => {
              const valor = promedios[c.clave];
              const sinDatos = valor === null || valor === undefined;
              return (
                <div className="columna" key={c.clave}>
                  <div className="columna-area">
                    {sinDatos ? (
                      <span className="valor note">—</span>
                    ) : (
                      <div className={`barra barra-${c.clave}`} style={{ height: `${Math.min(valor, 100)}%` }}>
                        <span className="valor">{valor.toFixed(1)}</span>
                      </div>
                    )}
                  </div>
                  <div className="columna-nombre">{c.texto}</div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}

// Dona: cuántos alumnos hay en cada color del semáforo
export function GraficoSemaforo({ semaforo = {}, alcance, cargando }) {
  const total = SEMAFORO.reduce((s, c) => s + (semaforo[c.clave] ?? 0), 0);
  const RADIO = 15.915; // circunferencia ≈ 100, así cada tramo se mide en porcentaje
  // Cada tramo empieza donde terminó el anterior
  const tramos = SEMAFORO.filter((c) => (semaforo[c.clave] ?? 0) > 0).reduce((lista, c) => {
    const inicio = lista.length ? lista[lista.length - 1].fin : 0;
    const porcentaje = (semaforo[c.clave] / total) * 100;
    return [...lista, { clave: c.clave, porcentaje, inicio, fin: inicio + porcentaje }];
  }, []);

  return (
    <div className="card h-100">
      <div className="card-header d-flex flex-wrap justify-content-between align-items-center gap-2">
        <h2>Alumnos por semáforo</h2>
        <span className="note">{alcance}</span>
      </div>
      <div className="card-body">
        {cargando ? (
          <div className="text-center text-secondary py-5">Cargando…</div>
        ) : total === 0 ? (
          <div className="text-center text-secondary py-5">Todavía no hay alumnos para mostrar.</div>
        ) : (
          <div className="grafico-dona">
            <svg viewBox="0 0 42 42" className="dona" role="img"
              aria-label={SEMAFORO.map((c) => `${c.texto}: ${semaforo[c.clave] ?? 0}`).join(", ")}>
              <circle cx="21" cy="21" r={RADIO} className="dona-fondo" />
              {tramos.map((t) => (
                <circle key={t.clave} cx="21" cy="21" r={RADIO} className={`dona-tramo tramo-${t.clave}`}
                  strokeDasharray={`${t.porcentaje} ${100 - t.porcentaje}`} strokeDashoffset={25 - t.inicio} />
              ))}
              <text x="21" y="21.5" className="dona-total">{total}</text>
              <text x="21" y="26.5" className="dona-etiqueta">alumnos</text>
            </svg>
            <ul className="dona-leyenda">
              {SEMAFORO.map((c) => {
                const n = semaforo[c.clave] ?? 0;
                return (
                  <li key={c.clave}>
                    <span className={`punto punto-${c.clave}`}></span>
                    <span className="flex-grow-1">
                      <b>{c.texto}</b>
                      <span className="note d-block">{c.rango}</span>
                    </span>
                    <b>{n}</b>
                    <span className="note porcentaje">{Math.round((n / total) * 100)} %</span>
                  </li>
                );
              })}
            </ul>
          </div>
        )}
      </div>
    </div>
  );
}
