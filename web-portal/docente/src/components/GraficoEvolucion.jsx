import { useId } from "react";

// T-293 (US-29): gráfico de evolución de los puntajes del estudiante.
// Reutiliza las últimas evaluaciones del detalle (T-290) y las ordena de la
// más antigua a la más reciente para mostrar la tendencia.
const COMPETENCIAS = [
  { clave: "lectura", texto: "Lectura" },
  { clave: "escritura", texto: "Escritura" },
  { clave: "oralidad", texto: "Oralidad" },
];

const COLORES_COMPETENCIA = {
  lectura: "#8B5CF6",
  escritura: "#F59E0B",
  oralidad: "#EC4899",
};

const MARCAS_Y = [0, 20, 40, 60, 80, 100];
const ANCHO = 640;
const ALTO = 260;
const MARGEN = { superior: 20, derecho: 18, inferior: 38, izquierdo: 44 };
const ANCHO_UTIL = ANCHO - MARGEN.izquierdo - MARGEN.derecho;
const ALTO_UTIL = ALTO - MARGEN.superior - MARGEN.inferior;

const FORMATO_EJE = new Intl.DateTimeFormat("es-PE", {
  day: "2-digit",
  month: "2-digit",
});

const FORMATO_RESUMEN = new Intl.DateTimeFormat("es-PE", {
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
});

const FORMATO_LARGO = new Intl.DateTimeFormat("es-PE", {
  day: "2-digit",
  month: "short",
  year: "numeric",
  hour: "2-digit",
  minute: "2-digit",
});

const textoCompetencia = (clave) =>
  COMPETENCIAS.find(({ clave: valor }) => valor === clave)?.texto || clave;

const puntajeValido = (valor) => {
  const numero = Number(valor);
  if (!Number.isFinite(numero)) return null;
  return Math.min(100, Math.max(0, numero));
};

const fechaValida = (valor) => {
  const fecha = new Date(valor);
  return Number.isNaN(fecha.getTime()) ? null : fecha;
};

const yDe = (puntaje) =>
  MARGEN.superior + (1 - puntaje / 100) * ALTO_UTIL;

export default function GraficoEvolucion({ evidencias = [], nombre = "" }) {
  const idGenerado = useId();
  const gradienteId = `gradiente-evolucion-${idGenerado.replace(/[^a-zA-Z0-9_-]/g, "")}`;

  const datos = evidencias
    .map((evidencia) => {
      const fecha = fechaValida(evidencia.evaluadoEn);
      const puntaje = puntajeValido(evidencia.puntaje);
      if (!fecha || puntaje == null) return null;
      return { ...evidencia, fecha, puntaje };
    })
    .filter(Boolean)
    .sort(
      (a, b) =>
        a.fecha.getTime() - b.fecha.getTime() ||
        Number(a.id || 0) - Number(b.id || 0)
    );

  if (datos.length === 0) {
    return (
      <section className="panel" aria-label="Evolución de puntajes">
        <div className="detalle-seccion-head">
          <div>
            <h2 className="h5 fw-bold mb-1">Evolución de puntajes</h2>
            <p className="text-secondary mb-0 small">
              La tendencia se construye con las últimas evaluaciones.
            </p>
          </div>
        </div>
        <p className="detalle-vacio">
          <i className="bi bi-graph-up" aria-hidden="true" />
          Aún no hay evaluaciones para mostrar la evolución.
        </p>
      </section>
    );
  }

  const total = datos.length;
  const puntos = datos.map((dato, indice) => ({
    ...dato,
    cx:
      total === 1
        ? MARGEN.izquierdo + ANCHO_UTIL / 2
        : MARGEN.izquierdo + (indice / (total - 1)) * ANCHO_UTIL,
    cy: yDe(dato.puntaje),
  }));

  const linea = puntos
    .map((punto, indice) => `${indice === 0 ? "M" : "L"} ${punto.cx} ${punto.cy}`)
    .join(" ");
  const baseY = MARGEN.superior + ALTO_UTIL;
  const area = `${linea} L ${puntos[puntos.length - 1].cx} ${baseY} L ${puntos[0].cx} ${baseY} Z`;

  const minimo = Math.min(...datos.map((dato) => dato.puntaje));
  const maximo = Math.max(...datos.map((dato) => dato.puntaje));
  const primeraFecha = FORMATO_RESUMEN.format(datos[0].fecha);
  const ultimaFecha = FORMATO_RESUMEN.format(datos[datos.length - 1].fecha);
  const nombreVisible = nombre || "el estudiante";
  const etiqueta =
    total === 1
      ? `Gráfico de evolución de ${nombreVisible}: una evaluación del ${primeraFecha} con puntaje ${minimo} sobre 100.`
      : `Gráfico de evolución de ${nombreVisible}: ${total} evaluaciones entre el ${primeraFecha} y el ${ultimaFecha}, con puntajes de ${minimo} a ${maximo} sobre 100.`;

  const competenciasPresentes = COMPETENCIAS.filter(({ clave }) =>
    datos.some((dato) => dato.competencia === clave)
  );

  return (
    <section className="panel" aria-label="Evolución de puntajes">
      <div className="detalle-seccion-head">
        <div>
          <h2 className="h5 fw-bold mb-1">Evolución de puntajes</h2>
          <p className="text-secondary mb-0 small">
            {total === 1 ? "1 evaluación" : `${total} evaluaciones`} ·{" "}
            {total === 1 ? primeraFecha : `${primeraFecha} → ${ultimaFecha}`}
          </p>
        </div>
      </div>

      <svg
        className="grafico-evolucion"
        viewBox={`0 0 ${ANCHO} ${ALTO}`}
        role="img"
        aria-label={etiqueta}
        preserveAspectRatio="xMidYMid meet"
      >
        <defs>
          <linearGradient id={gradienteId} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#8B5CF6" stopOpacity="0.22" />
            <stop offset="100%" stopColor="#8B5CF6" stopOpacity="0.02" />
          </linearGradient>
        </defs>

        <rect
          x={MARGEN.izquierdo}
          y={yDe(100)}
          width={ANCHO_UTIL}
          height={yDe(80) - yDe(100)}
          fill="#22C55E"
          opacity="0.06"
        />
        <rect
          x={MARGEN.izquierdo}
          y={yDe(80)}
          width={ANCHO_UTIL}
          height={yDe(60) - yDe(80)}
          fill="#F59E0B"
          opacity="0.06"
        />
        <rect
          x={MARGEN.izquierdo}
          y={yDe(60)}
          width={ANCHO_UTIL}
          height={yDe(0) - yDe(60)}
          fill="#F43F5E"
          opacity="0.05"
        />

        {MARCAS_Y.map((marca) => (
          <g key={marca}>
            <line
              x1={MARGEN.izquierdo}
              x2={ANCHO - MARGEN.derecho}
              y1={yDe(marca)}
              y2={yDe(marca)}
              stroke="#EAE6DC"
              strokeWidth="1"
              strokeDasharray={marca === 0 ? "0" : "4 4"}
            />
            <text
              x={MARGEN.izquierdo - 8}
              y={yDe(marca) + 3}
              textAnchor="end"
              className="grafico-eje-texto"
            >
              {marca}
            </text>
          </g>
        ))}

        {puntos.map((punto) => (
          <text
            key={`eje-${punto.id}`}
            x={punto.cx}
            y={ALTO - 14}
            textAnchor="middle"
            className="grafico-eje-texto"
          >
            {FORMATO_EJE.format(punto.fecha)}
          </text>
        ))}

        {total > 1 && (
          <>
            <path d={area} fill={`url(#${gradienteId})`} />
            <path
              d={linea}
              fill="none"
              stroke="#8B5CF6"
              strokeWidth="2.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </>
        )}

        {puntos.map((punto) => (
          <circle
            key={punto.id}
            className="punto-evolucion"
            cx={punto.cx}
            cy={punto.cy}
            r="5"
            fill={COLORES_COMPETENCIA[punto.competencia] || "#6B7280"}
            stroke="#fff"
            strokeWidth="2"
            data-puntaje={punto.puntaje}
            data-competencia={punto.competencia}
          >
            <title>
              {`${textoCompetencia(punto.competencia)} · ${punto.puntaje}/100 · ${FORMATO_LARGO.format(punto.fecha)}`}
            </title>
          </circle>
        ))}

        {puntos.map((punto) => (
          <text
            key={`puntaje-${punto.id}`}
            x={punto.cx}
            y={punto.cy - 10}
            textAnchor="middle"
            className="grafico-punto-texto"
          >
            {punto.puntaje}
          </text>
        ))}
      </svg>

      <ul className="grafico-leyenda" aria-label="Competencias del gráfico">
        {competenciasPresentes.map(({ clave, texto }) => (
          <li key={clave}>
            <span
              className="grafico-leyenda-punto"
              style={{ background: COLORES_COMPETENCIA[clave] }}
              aria-hidden="true"
            />
            {texto}
          </li>
        ))}
      </ul>

      <p className="grafico-zonas">
        Zonas del semáforo: rojo 0-59 · amarillo 60-79 · verde 80-100.
      </p>

      {total === 1 && (
        <p className="grafico-nota">
          <i className="bi bi-info-circle" aria-hidden="true" />
          Con una sola evaluación todavía no se puede ver una tendencia.
        </p>
      )}
    </section>
  );
}
