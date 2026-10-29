import { useEffect, useState } from "react";
import { actualizarColegio, asignarDocentes, crearColegio, listarColegios, listarDocentes } from "../api";

const REGIONES = [
  "Amazonas", "Áncash", "Apurímac", "Arequipa", "Ayacucho", "Cajamarca", "Callao", "Cusco",
  "Huancavelica", "Huánuco", "Ica", "Junín", "La Libertad", "Lambayeque", "Lima", "Loreto",
  "Madre de Dios", "Moquegua", "Pasco", "Piura", "Puno", "San Martín", "Tacna", "Tumbes", "Ucayali",
];

const vacio = { codigo_modular: "", nombre: "", region: "", distrito: "" };

const iniciales = (d) =>
  `${d.first_name?.[0] ?? ""}${d.last_name?.[0] ?? ""}`.toUpperCase() || d.username[0].toUpperCase();

const nombreCompleto = (d) => `${d.first_name} ${d.last_name}`.trim() || d.username;

// Convierte los errores 400 de DRF en { campo: "mensaje" }
const erroresDeServidor = (datos) => {
  const errores = {};
  for (const [campo, msgs] of Object.entries(datos ?? {})) {
    errores[campo] = Array.isArray(msgs) ? msgs.join(" ") : String(msgs);
  }
  return errores;
};

function Modal({ titulo, onCerrar, children, pie }) {
  useEffect(() => {
    const alPresionar = (e) => e.key === "Escape" && onCerrar();
    document.addEventListener("keydown", alPresionar);
    return () => document.removeEventListener("keydown", alPresionar);
  }, [onCerrar]);

  return (
    <>
      <div className="modal d-block" tabIndex="-1" role="dialog" aria-modal="true" onClick={onCerrar}>
        <div className="modal-dialog modal-dialog-centered" onClick={(e) => e.stopPropagation()}>
          <div className="modal-content">
            <div className="modal-header">
              <h5 className="modal-title fw-semibold">{titulo}</h5>
              <button type="button" className="btn-close" aria-label="Cerrar" onClick={onCerrar}></button>
            </div>
            {children}
            <div className="modal-footer">{pie}</div>
          </div>
        </div>
      </div>
      <div className="modal-backdrop show"></div>
    </>
  );
}

function ModalColegio({ colegio, onCerrar, onGuardado, onNoAutorizado }) {
  const editando = Boolean(colegio);
  const [datos, setDatos] = useState(
    editando
      ? { codigo_modular: colegio.codigo_modular, nombre: colegio.nombre, region: colegio.region, distrito: colegio.distrito }
      : vacio
  );
  const [errores, setErrores] = useState({});
  const [enviando, setEnviando] = useState(false);

  const cambiar = (e) => setDatos({ ...datos, [e.target.name]: e.target.value });

  const guardar = async (ev) => {
    ev.preventDefault();
    setEnviando(true);
    setErrores({});
    try {
      const guardado = editando ? await actualizarColegio(colegio.id, datos) : await crearColegio(datos);
      onGuardado(guardado, editando ? "Colegio actualizado correctamente." : "Colegio registrado correctamente.");
    } catch (err) {
      if (err.estado === 401) onNoAutorizado();
      else if (err.estado === 400) setErrores(erroresDeServidor(err.datos));
      else setErrores({ general: err.datos?.detail ?? err.mensaje ?? "Ocurrió un error inesperado." });
    } finally {
      setEnviando(false);
    }
  };

  const clase = (campo, base = "form-control") => `${base}${errores[campo] ? " is-invalid" : ""}`;
  const feedback = (campo) => errores[campo] && <div className="invalid-feedback">{errores[campo]}</div>;

  return (
    <Modal
      titulo={editando ? "Editar colegio" : "Registrar colegio"}
      onCerrar={onCerrar}
      pie={
        <>
          <button type="button" className="btn btn-light" onClick={onCerrar}>Cancelar</button>
          <button type="submit" form="form-colegio" className="btn btn-primary" disabled={enviando}>
            {enviando ? "Guardando..." : "Guardar colegio"}
          </button>
        </>
      }
    >
      <form id="form-colegio" className="modal-body row g-3" onSubmit={guardar} noValidate>
        {(errores.general || errores.non_field_errors) && (
          <div className="col-12">
            <div className="alert alert-danger py-2 small mb-0">{errores.general || errores.non_field_errors}</div>
          </div>
        )}
        <div className="col-6">
          <label className="form-label" htmlFor="codigo_modular">Código modular *</label>
          <input id="codigo_modular" name="codigo_modular" className={clase("codigo_modular")} placeholder="0512346"
            maxLength={7} inputMode="numeric" value={datos.codigo_modular} onChange={cambiar} />
          {feedback("codigo_modular")}
        </div>
        <div className="col-6">
          <label className="form-label" htmlFor="region">Región *</label>
          <select id="region" name="region" className={clase("region", "form-select")} value={datos.region} onChange={cambiar}>
            <option value="">Selecciona…</option>
            {REGIONES.map((r) => <option key={r} value={r}>{r}</option>)}
          </select>
          {feedback("region")}
        </div>
        <div className="col-12">
          <label className="form-label" htmlFor="nombre">Nombre del colegio *</label>
          <input id="nombre" name="nombre" className={clase("nombre")} placeholder="IE N° 50123 Ccollpa"
            value={datos.nombre} onChange={cambiar} />
          {feedback("nombre")}
        </div>
        <div className="col-12">
          <label className="form-label" htmlFor="distrito">Distrito / Centro poblado</label>
          <input id="distrito" name="distrito" className={clase("distrito")} value={datos.distrito} onChange={cambiar} />
          {feedback("distrito")}
        </div>
      </form>
    </Modal>
  );
}

function ModalAsignar({ colegio, onCerrar, onGuardado, onNoAutorizado }) {
  const [docentes, setDocentes] = useState([]);
  const [seleccion, setSeleccion] = useState(() => new Set(colegio.docentes.map((d) => d.id)));
  const [filtro, setFiltro] = useState("");
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    let activo = true;
    listarDocentes()
      .then((datos) => {
        if (activo) setDocentes(Array.isArray(datos) ? datos : datos.results ?? []);
      })
      .catch((err) => {
        if (!activo) return;
        if (err.estado === 401) onNoAutorizado();
        else setError(err.datos?.detail ?? err.mensaje ?? "No se pudo cargar la lista de docentes.");
      })
      .finally(() => {
        if (activo) setCargando(false);
      });
    return () => {
      activo = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const alternar = (id) => {
    const nueva = new Set(seleccion);
    if (nueva.has(id)) nueva.delete(id);
    else nueva.add(id);
    setSeleccion(nueva);
  };

  const guardar = async () => {
    setEnviando(true);
    setError("");
    try {
      const actualizado = await asignarDocentes(colegio.id, [...seleccion]);
      onGuardado(actualizado, "Asignación de docentes guardada.");
    } catch (err) {
      if (err.estado === 401) onNoAutorizado();
      else if (err.estado === 400) setError(erroresDeServidor(err.datos).docentes ?? "Revisa la selección.");
      else setError(err.datos?.detail ?? err.mensaje ?? "Ocurrió un error inesperado.");
    } finally {
      setEnviando(false);
    }
  };

  const texto = filtro.trim().toLowerCase();
  const visibles = docentes.filter((d) =>
    `${nombreCompleto(d)} ${d.username}`.toLowerCase().includes(texto)
  );

  return (
    <Modal
      titulo={`Asignar docentes · ${colegio.nombre}`}
      onCerrar={onCerrar}
      pie={
        <>
          <span className="note me-auto">{seleccion.size} seleccionados</span>
          <button type="button" className="btn btn-light" onClick={onCerrar}>Cancelar</button>
          <button type="button" className="btn btn-primary" onClick={guardar} disabled={enviando || cargando}>
            {enviando ? "Guardando..." : "Guardar asignación"}
          </button>
        </>
      }
    >
      <div className="modal-body">
        {error && <div className="alert alert-danger py-2 small">{error}</div>}
        <input className="form-control mb-3" placeholder="Buscar docente" value={filtro} onChange={(e) => setFiltro(e.target.value)} />
        {cargando && <p className="note mb-0">Cargando docentes...</p>}
        {!cargando && docentes.length === 0 && !error && <p className="note mb-0">Aún no hay docentes registrados.</p>}
        {!cargando && docentes.length > 0 && visibles.length === 0 && <p className="note mb-0">Ningún docente coincide con la búsqueda.</p>}
        {visibles.length > 0 && (
          <div className="list-group">
            {visibles.map((d) => {
              const enOtro = d.colegio && d.colegio.id !== colegio.id;
              return (
                <label key={d.id} className={`list-group-item d-flex gap-2 align-items-center${enOtro ? " text-secondary" : ""}`}>
                  <input className="form-check-input m-0" type="checkbox" checked={seleccion.has(d.id)}
                    disabled={enOtro} onChange={() => alternar(d.id)} />
                  {nombreCompleto(d)}
                  <span className="ms-auto note">{enOtro ? `en ${d.colegio.nombre}` : d.colegio ? "" : "sin colegio"}</span>
                </label>
              );
            })}
          </div>
        )}
      </div>
    </Modal>
  );
}

  export default function Colegios({ busquedaInicial = "", onNoAutorizado, puede = () => true }) {
  const [colegios, setColegios] = useState([]);
  const [busqueda, setBusqueda] = useState(busquedaInicial);
  const [region, setRegion] = useState("");
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [exito, setExito] = useState("");
  const [recarga, setRecarga] = useState(0);
  const [modalColegio, setModalColegio] = useState(null); // { colegio } — colegio null al registrar
  const [modalAsignar, setModalAsignar] = useState(null);

  useEffect(() => {
    let activo = true;
    // Espera a que el usuario deje de escribir antes de consultar
    const espera = setTimeout(() => {
      setCargando(true);
      listarColegios({ search: busqueda.trim(), region })
        .then((datos) => {
          if (!activo) return;
          setColegios(Array.isArray(datos) ? datos : datos.results ?? []);
          setError("");
        })
        .catch((err) => {
          if (!activo) return;
          if (err.estado === 401) onNoAutorizado();
          else setError(err.datos?.detail ?? err.mensaje ?? "No se pudo cargar la lista de colegios.");
        })
        .finally(() => {
          if (activo) setCargando(false);
        });
    }, 300);
    return () => {
      activo = false;
      clearTimeout(espera);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [busqueda, region, recarga]);

  useEffect(() => {
    if (!exito) return;
    const t = setTimeout(() => setExito(""), 4000);
    return () => clearTimeout(t);
  }, [exito]);

  const alGuardar = (_colegio, mensaje) => {
    setModalColegio(null);
    setModalAsignar(null);
    setExito(mensaje);
    setRecarga((n) => n + 1);
  };

  const alternarActivo = async (colegio) => {
    try {
      await actualizarColegio(colegio.id, { activo: !colegio.activo });
      setExito(colegio.activo ? `Se desactivó ${colegio.nombre}.` : `Se activó ${colegio.nombre}.`);
      setRecarga((n) => n + 1);
    } catch (err) {
      if (err.estado === 401) onNoAutorizado();
      else setError(err.datos?.detail ?? err.mensaje ?? "No se pudo cambiar el estado del colegio.");
    }
  };

  return (
    <section>
      <div className="page-title d-flex flex-wrap justify-content-between align-items-end gap-2 mb-4">
        <div>
          <h1>Colegios <span className="story">US-44</span></h1>
          <div className="breadcrumb text-secondary">Instituciones / Colegios</div>
        </div>
                {puede("colegios.crear") && (
          <button type="button" className="btn btn-primary" onClick={() => setModalColegio({ colegio: null })}>
            <i className="bi bi-plus-lg me-1"></i>Registrar colegio
          </button>
        )}
      </div>

      {exito && (
        <div className="alert alert-success py-2 small d-flex gap-2 align-items-center">
          <i className="bi bi-check-circle"></i>{exito}
        </div>
      )}

      <div className="card">
        <div className="card-header d-flex flex-wrap gap-2 align-items-center">
          <input className="form-control" style={{ maxWidth: 260 }} placeholder="Buscar por nombre o código modular"
            value={busqueda} onChange={(e) => setBusqueda(e.target.value)} />
          <select className="form-select" style={{ maxWidth: 180 }} value={region} onChange={(e) => setRegion(e.target.value)}>
            <option value="">Todas las regiones</option>
            {REGIONES.map((r) => <option key={r} value={r}>{r}</option>)}
          </select>
          <span className="ms-auto note">
            {cargando ? "Cargando..." : `${colegios.length} ${colegios.length === 1 ? "colegio" : "colegios"}`}
          </span>
        </div>

        {error && <div className="alert alert-danger m-3 py-2 small">{error}</div>}

        {!error && !cargando && colegios.length === 0 && (
          <div className="text-center text-secondary py-5">
            <i className="bi bi-building fs-2 d-block mb-2"></i>
            {busqueda || region ? "Ningún colegio coincide con la búsqueda." : "Aún no hay colegios registrados."}
          </div>
        )}

        {colegios.length > 0 && (
          <div className="table-responsive">
            <table className="table table-hover">
              <thead>
                <tr>
                  <th>Código modular</th>
                  <th>Colegio</th>
                  <th>Región / Distrito</th>
                  <th>Docentes asignados</th>
                  <th>Estado</th>
                  <th className="text-end">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {colegios.map((c) => (
                  <tr key={c.id} className={c.activo ? "" : "inactivo"}>
                    <td>{c.codigo_modular}</td>
                    <td className="fw-medium">{c.nombre}</td>
                    <td>{c.region}{c.distrito ? ` / ${c.distrito}` : ""}</td>
                    <td>
                      {c.total_docentes > 0 ? (
                        <span className="d-inline-flex align-items-center">
                          <span className="avatar-group">
                            {c.docentes.slice(0, 3).map((d) => (
                              <span key={d.id} className="mini-avatar" title={nombreCompleto(d)}>{iniciales(d)}</span>
                            ))}
                          </span>
                          <span className="note ms-2">
                            {c.total_docentes} {c.total_docentes === 1 ? "docente" : "docentes"}
                          </span>
                        </span>
                      ) : (
                        <span className="badge-soft bs-warn">Sin docentes</span>
                      )}
                    </td>
                    <td>
                      <span className={`badge-soft ${c.activo ? "bs-ok" : "bs-off"}`}>{c.activo ? "Activo" : "Inactivo"}</span>
                    </td>
                    <td className="text-end text-nowrap">
                                            {puede("colegios.editar") && (
                        <button type="button" className="icon-btn" title="Asignar docentes" onClick={() => setModalAsignar(c)}>
                          <i className="bi bi-person-plus"></i>
                        </button>
                      )}
                      {puede("colegios.editar") && (
                        <button type="button" className="icon-btn" title="Editar" onClick={() => setModalColegio({ colegio: c })}>
                          <i className="bi bi-pencil"></i>
                        </button>
                      )}
                      {puede("colegios.desactivar") && (
                        <button type="button" className="icon-btn" title={c.activo ? "Desactivar" : "Activar"}
                          onClick={() => alternarActivo(c)}>
                          <i className={`bi ${c.activo ? "bi-slash-circle" : "bi-check-circle"}`}></i>
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {modalColegio && (
        <ModalColegio
          colegio={modalColegio.colegio}
          onCerrar={() => setModalColegio(null)}
          onGuardado={alGuardar}
          onNoAutorizado={onNoAutorizado}
        />
      )}
      {modalAsignar && (
        <ModalAsignar
          colegio={modalAsignar}
          onCerrar={() => setModalAsignar(null)}
          onGuardado={alGuardar}
          onNoAutorizado={onNoAutorizado}
        />
      )}
    </section>
  );
}
