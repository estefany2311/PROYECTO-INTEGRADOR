import { useEffect, useState } from "react";
import { actualizarConfiguracionEvaluacion, listarConfiguracionEvaluacion } from "../api";

// Mismos valores por defecto de la migración 0002 (grado: precisión, pausas, palabras por minuto)
const POR_DEFECTO = {
  1: [60, "3.0", 30], 2: [65, "2.5", 45], 3: [70, "2.0", 60],
  4: [75, "2.0", 75], 5: [80, "1.5", 85], 6: [85, "1.5", 95],
};

const CAMPOS = [
  { campo: "umbral_precision", titulo: "Precisión mínima (%)", min: 0, max: 100, paso: 1, ayuda: "Debe estar entre 0 y 100 %." },
  { campo: "tolerancia_pausas", titulo: "Pausas toleradas (s)", min: 0, max: 10, paso: 0.5, ayuda: "Debe estar entre 0 y 10 segundos." },
  { campo: "palabras_por_minuto", titulo: "Palabras por minuto", min: 10, max: 200, paso: 1, ayuda: "Debe estar entre 10 y 200 palabras por minuto." },
];

const aTexto = (fila) => ({
  ...fila,
  umbral_precision: String(fila.umbral_precision),
  tolerancia_pausas: String(fila.tolerancia_pausas),
  palabras_por_minuto: String(fila.palabras_por_minuto),
});

const fecha = (iso) =>
  iso ? new Date(iso).toLocaleString("es-PE", { dateStyle: "short", timeStyle: "short" }) : "—";

export default function ConfiguracionVoz({ onNoAutorizado, puede = () => true }) {
  const [guardadas, setGuardadas] = useState([]); // lo que está en la base
  const [filas, setFilas] = useState([]);         // lo que se ve en pantalla
  const [errores, setErrores] = useState({});     // { "3": { umbral_precision: "..." } }
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState("");
  const [exito, setExito] = useState("");
  const puedeEditar = puede("configuracion.editar");

  useEffect(() => {
    listarConfiguracionEvaluacion()
      .then((datos) => {
        const lista = (Array.isArray(datos) ? datos : datos.results ?? []).map(aTexto);
        setGuardadas(lista);
        setFilas(lista);
      })
      .catch((err) => {
        if (err.estado === 401) onNoAutorizado();
        else setError(err.datos?.detail ?? err.mensaje ?? "No se pudo cargar la configuración.");
      })
      .finally(() => setCargando(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!exito) return;
    const t = setTimeout(() => setExito(""), 4000);
    return () => clearTimeout(t);
  }, [exito]);

  // Validación en el navegador (el backend también valida la regla por grado)
  const validarCampo = (c, valor) => {
    const n = Number(valor);
    if (valor === "" || Number.isNaN(n)) return "Ingresa un número válido.";
    if (n < c.min || n > c.max) return c.ayuda;
    return "";
  };

  const cambiar = (grado, campo, valor) => {
    setFilas(filas.map((f) => (f.grado === grado ? { ...f, [campo]: valor } : f)));
    const c = CAMPOS.find((x) => x.campo === campo);
    setErrores({ ...errores, [grado]: { ...errores[grado], [campo]: validarCampo(c, valor) } });
  };

  const cambiosDe = (fila) => {
    const original = guardadas.find((g) => g.grado === fila.grado);
    const cambios = {};
    CAMPOS.forEach(({ campo }) => {
      if (Number(fila[campo]) !== Number(original[campo])) cambios[campo] = fila[campo];
    });
    return cambios;
  };

  const conCambios = filas.filter((f) => Object.keys(cambiosDe(f)).length > 0);
  const hayErrores = Object.values(errores).some((e) => Object.values(e ?? {}).some(Boolean));

  const porDefecto = () => {
    setFilas(filas.map((f) => {
      const [precision, pausas, ppm] = POR_DEFECTO[f.grado];
      return { ...f, umbral_precision: String(precision), tolerancia_pausas: pausas, palabras_por_minuto: String(ppm) };
    }));
    setErrores({});
  };

  const guardar = async () => {
    setGuardando(true);
    setError("");
    let pendientes = conCambios;
    const nuevosErrores = {};
    let actualizadas = [...guardadas];
    // Dos vueltas: si se cambian grados vecinos, el segundo intento ya ve los valores nuevos
    for (let vuelta = 0; vuelta < 2 && pendientes.length > 0; vuelta++) {
      const fallidas = [];
      for (const fila of pendientes) {
        try {
          const guardada = aTexto(await actualizarConfiguracionEvaluacion(fila.grado, cambiosDe(fila)));
          actualizadas = actualizadas.map((g) => (g.grado === fila.grado ? guardada : g));
          delete nuevosErrores[fila.grado];
        } catch (err) {
          if (err.estado === 401) return onNoAutorizado();
          if (err.estado === 400) {
            nuevosErrores[fila.grado] = Object.fromEntries(
              Object.entries(err.datos ?? {}).map(([k, v]) => [k, Array.isArray(v) ? v.join(" ") : String(v)])
            );
            fallidas.push(fila);
          } else {
            setError(err.datos?.detail ?? err.mensaje ?? "No se pudo guardar la configuración.");
          }
        }
      }
      pendientes = fallidas;
    }
    setGuardadas(actualizadas);
    setFilas(filas.map((f) => (nuevosErrores[f.grado] ? f : actualizadas.find((g) => g.grado === f.grado))));
    setErrores(nuevosErrores);
    if (Object.keys(nuevosErrores).length === 0) setExito("Configuración de voz guardada correctamente.");
    setGuardando(false);
  };

  return (
    <section>
      <div className="page-title d-flex flex-wrap justify-content-between align-items-end gap-2 mb-4">
        <div>
          <h1>Rigor de la evaluación de voz</h1>
          <div className="breadcrumb text-secondary">Configuración IA / Evaluación de voz</div>
        </div>
        {puedeEditar && (
          <div className="d-flex gap-2">
            <button type="button" className="btn btn-light" onClick={porDefecto} disabled={cargando || guardando}>
              <i className="bi bi-arrow-counterclockwise me-1"></i>Valores por defecto
            </button>
            <button type="button" className="btn btn-primary" onClick={guardar}
              disabled={guardando || hayErrores || conCambios.length === 0}>
              <i className="bi bi-check2 me-1"></i>{guardando ? "Guardando..." : "Guardar cambios"}
            </button>
          </div>
        )}
      </div>

      {exito && (
        <div className="alert alert-success py-2 small d-flex gap-2 align-items-center">
          <i className="bi bi-check-circle"></i>{exito}
        </div>
      )}
      {error && <div className="alert alert-danger py-2 small">{error}</div>}

      <div className="card">
        <div className="card-header d-flex flex-wrap justify-content-between gap-2">
          <h2>Parámetros por grado</h2>
          <span className="note">
            {conCambios.length > 0 ? `${conCambios.length} grado(s) con cambios sin guardar` : "El motor de IA usa estos valores al evaluar la lectura en voz alta"}
          </span>
        </div>
        {cargando ? (
          <div className="text-center text-secondary py-5">Cargando…</div>
        ) : (
          <div className="table-responsive">
            <table className="table tabla-voz">
              <thead>
                <tr>
                  <th>Grado</th>
                  {CAMPOS.map((c) => <th key={c.campo}>{c.titulo}</th>)}
                  <th>Última modificación</th>
                </tr>
              </thead>
              <tbody>
                {filas.map((f) => (
                  <tr key={f.grado} className={Object.keys(cambiosDe(f)).length ? "con-cambios" : ""}>
                    <td><span className="badge-soft bs-info">{f.grado}.° grado</span></td>
                    {CAMPOS.map((c) => {
                      const msg = errores[f.grado]?.[c.campo];
                      return (
                        <td key={c.campo}>
                          <input type="number" min={c.min} max={c.max} step={c.paso}
                            className={`form-control form-control-sm${msg ? " is-invalid" : ""}`}
                            value={f[c.campo]} disabled={!puedeEditar || guardando}
                            aria-label={`${c.titulo} de ${f.grado}.° grado`}
                            onChange={(e) => cambiar(f.grado, c.campo, e.target.value)} />
                          {msg && <div className="invalid-feedback">{msg}</div>}
                        </td>
                      );
                    })}
                    <td className="note">{f.actualizado_por ? `${f.actualizado_por} · ${fecha(f.actualizado_en)}` : "Valores iniciales"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <div className="card-footer bg-white note">
          <i className="bi bi-info-circle me-1"></i>A mayor grado, mayor exigencia: la precisión y las palabras por minuto no pueden ser menores que las del grado anterior.
        </div>
      </div>
    </section>
  );
}