import { useEffect, useState } from "react";
import { obtenerMapaCalor } from "../api";
import MatrizCalor from "../components/MatrizCalor";
import { colorDe } from "../utils/semaforo";
import { GRADOS } from "../utils/catalogo";

// Componente: MapaCalor (padre de la pestaña, US-48)
// Padre: App
// Guarda el estado: filtros (agrupar, grado), datos del endpoint y la celda elegida
// Hijos: MatrizCalor

const NOMBRES = { lectura: "Lectura", escritura: "Escritura", oralidad: "Oralidad", general: "Promedio general" };
const TEXTO_COLOR = { verde: "Logrado", amarillo: "En proceso", rojo: "Necesita apoyo", sin_evaluar: "Sin datos" };
export default function MapaCalor({ onNoAutorizado }) {
  const [agrupar, setAgrupar] = useState("region");
  const [grado, setGrado] = useState("");
  const [datos, setDatos] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [seleccion, setSeleccion] = useState(null);

  useEffect(() => {
    obtenerMapaCalor({ agrupar, grado })
      .then((respuesta) => {
        setDatos(respuesta);
        setError("");
      })
      .catch((err) => {
        if (err.estado === 401) return onNoAutorizado();
        if (err.estado === 403) return setError("No tienes permiso para ver el mapa de calor.");
        setError(err.datos?.detail ?? err.mensaje ?? "No se pudo cargar el mapa de calor.");
      })
      .finally(() => setCargando(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [agrupar, grado]);

  // Al cambiar un filtro se vuelve a pedir y se limpia la celda elegida
  const cambiarAgrupar = (valor) => { setCargando(true); setSeleccion(null); setAgrupar(valor); };
  const cambiarGrado = (valor) => { setCargando(true); setSeleccion(null); setGrado(valor); };

  const zonas = datos?.zonas ?? [];
  const totalEvaluaciones = zonas.reduce((suma, z) => suma + z.evaluaciones, 0);
  const zonaElegida = seleccion ? zonas.find((z) => z.zona === seleccion.zona) : null;
  const valorElegido = zonaElegida
    ? (seleccion.competencia === "general" ? zonaElegida.promedio_general : zonaElegida.promedios[seleccion.competencia])
    : null;

  return (
    <section>
      <div className="page-title mb-4">
        <h1>Mapa de calor del desempeño</h1>
        <div className="breadcrumb text-secondary">Análisis / Mapa de calor</div>
      </div>

      <div className="card">
        <div className="filtros-calor">
          <div>
            <label className="form-label" htmlFor="agrupar">Agrupar por</label>
            <select id="agrupar" className="form-select" value={agrupar} onChange={(e) => cambiarAgrupar(e.target.value)}>
              <option value="region">Región</option>
              <option value="provincia">Provincia</option>
            </select>
          </div>
          <div>
            <label className="form-label" htmlFor="grado">Grado</label>
            <select id="grado" className="form-select" value={grado} onChange={(e) => cambiarGrado(e.target.value)}>
              <option value="">Todos los grados</option>
              {GRADOS.map((g) => <option key={g} value={g}>{g}.° grado</option>)}
            </select>
          </div>
          <span className="note ms-auto align-self-end">Promedio de 0 a 100 · {totalEvaluaciones} evaluaciones</span>
        </div>

        {error && <div className="alert alert-danger m-3 py-2 small">{error}</div>}

        {cargando ? (
          <div className="text-center text-secondary py-5">Cargando…</div>
        ) : zonas.length === 0 ? (
          <div className="text-center note py-5">No hay colegios activos para mostrar.</div>
        ) : (
          <div className="p-3">
            <MatrizCalor
              zonas={zonas}
              competencias={datos.competencias}
              seleccion={seleccion}
              onElegir={(zona, competencia) => setSeleccion({ zona, competencia })}
            />
            <p className="note mt-2 mb-0">Haz clic en una celda para ver su detalle.</p>
          </div>
        )}
      </div>

      {zonaElegida && (
        <div className="card mt-3 detalle-calor">
          <div className="card-body">
            <h2 className="h6 fw-bold mb-3">
              {zonaElegida.zona} · {NOMBRES[seleccion.competencia]}{" "}
              <span className={`etiqueta-calor ${colorDe(valorElegido)}`}>
                {valorElegido ?? "—"} · {TEXTO_COLOR[colorDe(valorElegido)]}
              </span>
            </h2>
            <div className="row g-3">
              <div className="col-6 col-md-3"><div className="note">Colegios</div><div className="fs-5 fw-bold">{zonaElegida.colegios}</div></div>
              <div className="col-6 col-md-3"><div className="note">Alumnos</div><div className="fs-5 fw-bold">{zonaElegida.alumnos}</div></div>
              <div className="col-6 col-md-3"><div className="note">Evaluaciones</div><div className="fs-5 fw-bold">{zonaElegida.evaluaciones}</div></div>
              <div className="col-6 col-md-3"><div className="note">Promedio general</div><div className="fs-5 fw-bold">{zonaElegida.promedio_general ?? "—"}</div></div>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}