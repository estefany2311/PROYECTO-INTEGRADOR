import { useEffect, useState } from "react";
import { actualizarReglaPuntuacion, listarReglasPuntuacion } from "../api";

const ICONOS = { lectura: "bi-book", escritura: "bi-pencil-square", oralidad: "bi-mic" };

const CAMPOS = [
  { campo: "puntos", titulo: "Puntos", min: 1, max: 100, ayuda: "Debe estar entre 1 y 100 puntos." },
  { campo: "umbral_1_estrella", titulo: "★ desde (%)", min: 0, max: 100, ayuda: "Debe estar entre 0 y 100 %." },
  { campo: "umbral_2_estrellas", titulo: "★★ desde (%)", min: 0, max: 100, ayuda: "Debe estar entre 0 y 100 %." },
  { campo: "umbral_3_estrellas", titulo: "★★★ desde (%)", min: 0, max: 100, ayuda: "Debe estar entre 0 y 100 %." },
];

const comoLista = (datos) => (Array.isArray(datos) ? datos : datos?.results ?? []);
const aTexto = (r) => ({ ...r, ...Object.fromEntries(CAMPOS.map(({ campo }) => [campo, String(r[campo])])) });

// Validación en el navegador: rangos y umbrales crecientes (el backend valida lo mismo)
function erroresDe(regla) {
  const errores = {};
  CAMPOS.forEach((c) => {
    const n = Number(regla[c.campo]);
    if (regla[c.campo] === "" || !Number.isInteger(n)) errores[c.campo] = "Ingresa un número entero.";
    else if (n < c.min || n > c.max) errores[c.campo] = c.ayuda;
  });
  const [una, dos, tres] = [regla.umbral_1_estrella, regla.umbral_2_estrellas, regla.umbral_3_estrellas].map(Number);
  if (!errores.umbral_2_estrellas && !errores.umbral_1_estrella && dos <= una)
    errores.umbral_2_estrellas = `Debe ser mayor que el umbral de 1 estrella (${una} %).`;
  if (!errores.umbral_3_estrellas && !errores.umbral_2_estrellas && tres <= dos)
    errores.umbral_3_estrellas = `Debe ser mayor que el umbral de 2 estrellas (${dos} %).`;
  return errores;
}

function estrellasPara(regla, porcentaje) {
  if (porcentaje >= Number(regla.umbral_3_estrellas)) return 3;
  if (porcentaje >= Number(regla.umbral_2_estrellas)) return 2;
  if (porcentaje >= Number(regla.umbral_1_estrella)) return 1;
  return 0;
}

export default function ReglasPuntuacion({ onNoAutorizado, puede = () => true }) {
  const [guardadas, setGuardadas] = useState([]);
  const [reglas, setReglas] = useState([]);
  const [erroresServidor, setErroresServidor] = useState({});
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState("");
  const [exito, setExito] = useState("");
  const [simTipo, setSimTipo] = useState("lectura");
  const [simPuntaje, setSimPuntaje] = useState(75);
  const puedeEditar = puede("configuracion.editar");

  useEffect(() => {
    listarReglasPuntuacion()
      .then((datos) => {
        const lista = comoLista(datos).map(aTexto);
        setGuardadas(lista);
        setReglas(lista);
      })
      .catch((err) => {
        if (err.estado === 401) onNoAutorizado();
        else setError(err.datos?.detail ?? err.mensaje ?? "No se pudieron cargar las reglas.");
      })
      .finally(() => setCargando(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!exito) return;
    const t = setTimeout(() => setExito(""), 4000);
    return () => clearTimeout(t);
  }, [exito]);

  const cambiar = (tipo, campo, valor) => {
    setReglas(reglas.map((r) => (r.tipo_reto === tipo ? { ...r, [campo]: valor } : r)));
    setErroresServidor({ ...erroresServidor, [tipo]: undefined });
  };

  const cambiosDe = (regla) => {
    const original = guardadas.find((g) => g.tipo_reto === regla.tipo_reto);
    const cambios = {};
    CAMPOS.forEach(({ campo }) => {
      if (Number(regla[campo]) !== Number(original[campo])) cambios[campo] = Number(regla[campo]);
    });
    return cambios;
  };

  const conCambios = reglas.filter((r) => Object.keys(cambiosDe(r)).length > 0);
  const hayErrores = reglas.some((r) => Object.keys(erroresDe(r)).length > 0);

  const guardar = async () => {
    setGuardando(true);
    setError("");
    const nuevosErrores = {};
    let actualizadas = [...guardadas];
    for (const regla of conCambios) {
      try {
        const guardada = aTexto(await actualizarReglaPuntuacion(regla.tipo_reto, cambiosDe(regla)));
        actualizadas = actualizadas.map((g) => (g.tipo_reto === regla.tipo_reto ? guardada : g));
      } catch (err) {
        if (err.estado === 401) return onNoAutorizado();
        if (err.estado === 400) {
          nuevosErrores[regla.tipo_reto] = Object.fromEntries(
            Object.entries(err.datos ?? {}).map(([k, v]) => [k, Array.isArray(v) ? v.join(" ") : String(v)])
          );
        } else {
          setError(err.datos?.detail ?? err.mensaje ?? "No se pudieron guardar las reglas.");
        }
      }
    }
    setGuardadas(actualizadas);
    setReglas(reglas.map((r) => (nuevosErrores[r.tipo_reto] ? r : actualizadas.find((g) => g.tipo_reto === r.tipo_reto))));
    setErroresServidor(nuevosErrores);
    if (Object.keys(nuevosErrores).length === 0) setExito("¡Reglas de puntuación guardadas!");
    setGuardando(false);
  };

  const reglaSim = reglas.find((r) => r.tipo_reto === simTipo);
  const simEstrellas = reglaSim && !Object.keys(erroresDe(reglaSim)).length ? estrellasPara(reglaSim, simPuntaje) : 0;

  return (
    <section>
      <div className="page-title d-flex flex-wrap justify-content-between align-items-end gap-2 mb-4">
        <div>
          <h1>Puntos y estrellas</h1>
          <div className="breadcrumb text-secondary">Configuración IA / Puntos y estrellas</div>
        </div>
        {puedeEditar && (
          <button type="button" className="btn btn-primary" onClick={guardar}
            disabled={guardando || hayErrores || conCambios.length === 0}>
            <i className="bi bi-check2 me-1"></i>{guardando ? "Guardando..." : "Guardar cambios"}
          </button>
        )}
      </div>

      {exito && (
        <div className="alert alert-success py-2 small d-flex gap-2 align-items-center">
          <i className="bi bi-check-circle"></i>{exito}
        </div>
      )}
      {error && <div className="alert alert-danger py-2 small">{error}</div>}

      {cargando ? (
        <div className="text-center text-secondary py-5">Cargando…</div>
      ) : (
        <div className="row g-3">
          {/* Reglas por tipo de reto */}
          <div className="col-xl-8">
            <div className="card h-100">
              <div className="card-header d-flex flex-wrap justify-content-between gap-2">
                <h2>Reglas por tipo de reto</h2>
                <span className="note">
                  {conCambios.length > 0 ? `${conCambios.length} regla(s) con cambios sin guardar` : "La app del estudiante usa estas reglas al terminar cada reto"}
                </span>
              </div>
              <div className="table-responsive">
                <table className="table tabla-reglas">
                  <thead>
                    <tr>
                      <th>Tipo de reto</th>
                      {CAMPOS.map((c) => <th key={c.campo}>{c.titulo}</th>)}
                    </tr>
                  </thead>
                  <tbody>
                    {reglas.map((r) => {
                      const errores = { ...erroresDe(r), ...(erroresServidor[r.tipo_reto] ?? {}) };
                      return (
                        <tr key={r.tipo_reto} className={Object.keys(cambiosDe(r)).length ? "con-cambios" : ""}>
                          <td className="text-nowrap fw-medium">
                            <span className={`reto reto-${r.tipo_reto}`}><i className={`bi ${ICONOS[r.tipo_reto]}`}></i></span>
                            {r.tipo_reto_texto}
                          </td>
                          {CAMPOS.map((c) => (
                            <td key={c.campo}>
                              <input type="number" min={c.min} max={c.max} step="1"
                                className={`form-control form-control-sm${errores[c.campo] ? " is-invalid" : ""}`}
                                value={r[c.campo]} disabled={!puedeEditar || guardando}
                                aria-label={`${c.titulo} de ${r.tipo_reto_texto}`}
                                onChange={(e) => cambiar(r.tipo_reto, c.campo, e.target.value)} />
                              {errores[c.campo] && <div className="invalid-feedback">{errores[c.campo]}</div>}
                            </td>
                          ))}
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
              <div className="card-footer bg-white note">
                <i className="bi bi-info-circle me-1"></i>Los umbrales deben subir: 1 ★ &lt; 2 ★ &lt; 3 ★.
              </div>
            </div>
          </div>

          {/* Simulador */}
          <div className="col-xl-4">
            <div className="card h-100">
              <div className="card-header"><h2>Prueba las reglas</h2></div>
              <div className="card-body">
                <label className="form-label fw-semibold" htmlFor="simTipo">Tipo de reto</label>
                <select id="simTipo" className="form-select mb-3" value={simTipo} onChange={(e) => setSimTipo(e.target.value)}>
                  {reglas.map((r) => <option key={r.tipo_reto} value={r.tipo_reto}>{r.tipo_reto_texto}</option>)}
                </select>
                <label className="form-label fw-semibold d-flex justify-content-between" htmlFor="simPuntaje">
                  Puntaje del alumno <span>{simPuntaje} %</span>
                </label>
                <input id="simPuntaje" type="range" className="form-range mb-3" min="0" max="100"
                  value={simPuntaje} onChange={(e) => setSimPuntaje(Number(e.target.value))} />
                <div className="simulador text-center">
                  <div className="estrellas" aria-label={`${simEstrellas} estrellas`}>
                    {[1, 2, 3].map((n) => <span key={n} className={n <= simEstrellas ? "" : "apagada"}>★</span>)}
                  </div>
                  <div className="fw-semibold">
                    {reglaSim && (simEstrellas
                      ? `¡Gana ${reglaSim.puntos} puntos y ${simEstrellas} estrella${simEstrellas > 1 ? "s" : ""}!`
                      : `¡Sigue intentando! Gana ${reglaSim.puntos} puntos por participar.`)}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}