import { useEffect, useState } from "react";
import {
  actualizarConfiguracionOrtografia,
  agregarPalabraPermitida,
  listarConfiguracionOrtografia,
  listarPalabrasPermitidas,
  quitarPalabraPermitida,
} from "../api";

const SENSIBILIDADES = [
  { valor: "baja", texto: "Baja" },
  { valor: "media", texto: "Media" },
  { valor: "alta", texto: "Alta" },
];

const COLORES = ["#DCEBFF", "#FFF0B8", "#D3F5EC", "#FFDDE4", "#EADCFB", "#FFE8CC"];

const comoLista = (datos) => (Array.isArray(datos) ? datos : datos?.results ?? []);
const mensajeDe = (err, porDefecto) => {
  const d = err.datos;
  if (d?.palabra) return Array.isArray(d.palabra) ? d.palabra.join(" ") : String(d.palabra);
  if (d?.sensibilidad) return Array.isArray(d.sensibilidad) ? d.sensibilidad.join(" ") : String(d.sensibilidad);
  return d?.detail ?? err.mensaje ?? porDefecto;
};

export default function ConfiguracionOrtografia({ onNoAutorizado, puede = () => true }) {
  const [guardados, setGuardados] = useState([]);
  const [grados, setGrados] = useState([]);
  const [palabras, setPalabras] = useState([]);
  const [nueva, setNueva] = useState("");
  const [errorPalabra, setErrorPalabra] = useState("");
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [agregando, setAgregando] = useState(false);
  const [error, setError] = useState("");
  const [exito, setExito] = useState("");
  const puedeEditar = puede("configuracion.editar");

  const manejarError = (err, porDefecto) => {
    if (err.estado === 401) onNoAutorizado();
    else setError(mensajeDe(err, porDefecto));
  };

  useEffect(() => {
    Promise.all([listarConfiguracionOrtografia(), listarPalabrasPermitidas()])
      .then(([config, lista]) => {
        setGuardados(comoLista(config));
        setGrados(comoLista(config));
        setPalabras(comoLista(lista));
      })
      .catch((err) => manejarError(err, "No se pudo cargar la configuración ortográfica."))
      .finally(() => setCargando(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!exito) return;
    const t = setTimeout(() => setExito(""), 4000);
    return () => clearTimeout(t);
  }, [exito]);

  // --- Sensibilidad y tildes por grado ---
  const cambiar = (grado, campo, valor) =>
    setGrados(grados.map((g) => (g.grado === grado ? { ...g, [campo]: valor } : g)));

  const cambiosDe = (fila) => {
    const original = guardados.find((g) => g.grado === fila.grado);
    const cambios = {};
    if (fila.sensibilidad !== original.sensibilidad) cambios.sensibilidad = fila.sensibilidad;
    if (fila.ignorar_tildes !== original.ignorar_tildes) cambios.ignorar_tildes = fila.ignorar_tildes;
    return cambios;
  };
  const conCambios = grados.filter((g) => Object.keys(cambiosDe(g)).length > 0);

  const guardar = async () => {
    setGuardando(true);
    setError("");
    try {
      const actualizados = await Promise.all(
        conCambios.map((g) => actualizarConfiguracionOrtografia(g.grado, cambiosDe(g)))
      );
      const nuevos = guardados.map((g) => actualizados.find((a) => a.grado === g.grado) ?? g);
      setGuardados(nuevos);
      setGrados(nuevos);
      setExito("Configuración ortográfica guardada correctamente.");
    } catch (err) {
      manejarError(err, "No se pudo guardar la configuración ortográfica.");
    } finally {
      setGuardando(false);
    }
  };

  // --- Palabras permitidas ---
  const agregar = async (e) => {
    e.preventDefault();
    const palabra = nueva.trim();
    if (!palabra) return;
    setAgregando(true);
    setErrorPalabra("");
    try {
      const creada = await agregarPalabraPermitida(palabra);
      setPalabras([...palabras, creada].sort((a, b) => a.palabra.localeCompare(b.palabra, "es")));
      setNueva("");
      setExito(`«${creada.palabra}» se agregó a las palabras permitidas.`);
    } catch (err) {
      if (err.estado === 401) onNoAutorizado();
      else setErrorPalabra(mensajeDe(err, "No se pudo agregar la palabra."));
    } finally {
      setAgregando(false);
    }
  };

  const quitar = async (p) => {
    setError("");
    try {
      await quitarPalabraPermitida(p.id);
      setPalabras(palabras.filter((x) => x.id !== p.id));
      setExito(`«${p.palabra}» se quitó de la lista.`);
    } catch (err) {
      manejarError(err, "No se pudo quitar la palabra.");
    }
  };

  return (
    <section>
      <div className="page-title d-flex flex-wrap justify-content-between align-items-end gap-2 mb-4">
        <div>
          <h1>Validación ortográfica</h1>
          <div className="breadcrumb text-secondary">Configuración IA / Ortografía</div>
        </div>
        {puedeEditar && (
          <button type="button" className="btn btn-primary" onClick={guardar}
            disabled={guardando || conCambios.length === 0}>
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
          {/* Sensibilidad y tildes */}
          <div className="col-xl-7">
            <div className="card h-100">
              <div className="card-header d-flex flex-wrap justify-content-between gap-2">
                <h2>Sensibilidad por grado</h2>
                <span className="note">
                  {conCambios.length > 0 ? `${conCambios.length} grado(s) con cambios sin guardar` : "Qué tan estricta es la IA al marcar errores"}
                </span>
              </div>
              <div className="table-responsive">
                <table className="table tabla-ortografia">
                  <thead>
                    <tr><th>Grado</th><th>Sensibilidad</th><th>Tildes</th></tr>
                  </thead>
                  <tbody>
                    {grados.map((g) => (
                      <tr key={g.grado} className={Object.keys(cambiosDe(g)).length ? "con-cambios" : ""}>
                        <td><span className="badge-soft bs-info">{g.grado}.° grado</span></td>
                        <td>
                          <div className="selector-sensibilidad" role="group" aria-label={`Sensibilidad de ${g.grado}.° grado`}>
                            {SENSIBILIDADES.map((s) => (
                              <button key={s.valor} type="button" disabled={!puedeEditar || guardando}
                                className={`nivel-${s.valor}${g.sensibilidad === s.valor ? " activo" : ""}`}
                                onClick={() => cambiar(g.grado, "sensibilidad", s.valor)}>
                                {s.texto}
                              </button>
                            ))}
                          </div>
                        </td>
                        <td>
                          <div className="form-check form-switch m-0">
                            <input className="form-check-input" type="checkbox" id={`tildes-${g.grado}`}
                              checked={g.ignorar_tildes} disabled={!puedeEditar || guardando}
                              onChange={(e) => cambiar(g.grado, "ignorar_tildes", e.target.checked)} />
                            <label className="form-check-label small" htmlFor={`tildes-${g.grado}`}>
                              {g.ignorar_tildes ? "No marcar tildes" : "Marcar tildes"}
                            </label>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div className="card-footer bg-white note">
                <i className="bi bi-info-circle me-1"></i>Baja: solo errores graves · Media: errores comunes · Alta: todos los errores.
              </div>
            </div>
          </div>

          {/* Palabras permitidas */}
          <div className="col-xl-5">
            <div className="card h-100">
              <div className="card-header d-flex justify-content-between align-items-center">
                <h2>Palabras permitidas</h2>
                <span className="badge-soft bs-info">{palabras.length} palabras</span>
              </div>
              <div className="card-body">
                {puedeEditar && (
                  <form onSubmit={agregar} className="mb-3" noValidate>
                    <div className="input-group">
                      <input className={`form-control${errorPalabra ? " is-invalid" : ""}`}
                        placeholder="Escribe una palabra y presiona Enter" maxLength={60}
                        value={nueva} onChange={(e) => { setNueva(e.target.value); setErrorPalabra(""); }} />
                      <button type="submit" className="btn btn-primary" disabled={agregando || !nueva.trim()}>
                        <i className="bi bi-plus-lg"></i>
                      </button>
                      {errorPalabra && <div className="invalid-feedback">{errorPalabra}</div>}
                    </div>
                  </form>
                )}
                {palabras.length === 0 ? (
                  <p className="note mb-0">Todavía no hay palabras permitidas.</p>
                ) : (
                  <div className="lista-palabras">
                    {palabras.map((p, i) => (
                      <span key={p.id} className="chip-palabra" style={{ background: COLORES[i % COLORES.length] }}>
                        {p.palabra}
                        {puedeEditar && (
                          <button type="button" title={`Quitar «${p.palabra}»`} onClick={() => quitar(p)}>
                            <i className="bi bi-x-circle-fill"></i>
                          </button>
                        )}
                      </span>
                    ))}
                  </div>
                )}
                <p className="note mt-3 mb-0">Palabras quechuas o de la zona que la IA nunca debe marcar como error.</p>
              </div>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}