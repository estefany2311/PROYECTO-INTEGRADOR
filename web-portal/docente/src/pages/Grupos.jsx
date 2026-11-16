import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  listarAulas,
  listarEstudiantes,
  listarGrupos,
  crearGrupo,
  listarLecturas,
  asignarLecturaAGrupo,
} from "../api";

const FORM_VACIO = { nombre: "", descripcion: "" };

// T-196 (US-37): pantalla para crear grupos y asignar una lectura a todo el grupo.
// Consume los endpoints de T-195 y reutiliza el catálogo de lecturas (T-190).
export default function Grupos() {
  const [aulas, setAulas] = useState([]);
  const [aulaId, setAulaId] = useState("");
  const [grupos, setGrupos] = useState([]);
  const [alumnos, setAlumnos] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [mensaje, setMensaje] = useState(null); // { tipo, texto }

  // modal "crear grupo"
  const [creando, setCreando] = useState(false);
  const [form, setForm] = useState(FORM_VACIO);
  const [seleccionados, setSeleccionados] = useState([]);
  const [errorForm, setErrorForm] = useState(null);
  const [errorAlumnos, setErrorAlumnos] = useState(null);
  const [guardando, setGuardando] = useState(false);

  // modal "asignar lectura al grupo"
  const [asignando, setAsignando] = useState(null); // grupo elegido
  const [lecturas, setLecturas] = useState([]);
  const [cargandoLecturas, setCargandoLecturas] = useState(false);
  const [lecturaId, setLecturaId] = useState("");
  const [errorAsignar, setErrorAsignar] = useState(null);
  const [asignandoLectura, setAsignandoLectura] = useState(false);

  const aula = aulas.find((a) => String(a.id) === String(aulaId));
  const activos = alumnos.filter((a) => a.activo);

  const textoAlumnos = (n) => `${n} ${n === 1 ? "alumno" : "alumnos"}`;

  // al abrir la pantalla: las aulas del docente. Se elige la primera.
  useEffect(() => {
    listarAulas()
      .then((data) => {
        setAulas(data);
        if (data.length > 0) setAulaId(String(data[0].id));
        else setCargando(false);
      })
      .catch((e) => {
        setError(e.message);
        setCargando(false);
      });
  }, []);

  // cada cambio de aula recarga sus grupos y sus alumnos (para el formulario)
  useEffect(() => {
    if (!aulaId) return;
    let vigente = true;

    Promise.all([listarGrupos(aulaId), listarEstudiantes(aulaId)])
      .then(([gruposData, alumnosData]) => {
        if (!vigente) return;
        setGrupos(gruposData);
        setAlumnos(alumnosData);
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
  }, [aulaId]);

  // el cambio de aula se prepara aquí: el efecto solo hace la petición
  const cambiarAula = (id) => {
    setCargando(true);
    setError(null);
    setGrupos([]);
    setAlumnos([]);
    setAulaId(id);
  };

  const cambiarForm = (campo) => (ev) =>
    setForm({ ...form, [campo]: ev.target.value });

  // los ids se guardan una sola vez; el backend también deduplica
  const alternarAlumno = (id) => {
    setSeleccionados((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]
    );
    setErrorAlumnos(null); // al tocar la lista, el aviso se vuelve a validar al guardar
  };

  const abrirCrear = () => {
    setForm(FORM_VACIO);
    setSeleccionados([]);
    setErrorForm(null);
    setErrorAlumnos(null);
    setCreando(true);
  };

  const guardarGrupo = async (ev) => {
    ev.preventDefault();
    const nombre = form.nombre.trim();
    let valido = true;

    setErrorForm(null);
    setErrorAlumnos(null);
    if (!nombre) {
      setErrorForm("El nombre del grupo es obligatorio");
      valido = false;
    }
    if (seleccionados.length === 0) {
      setErrorAlumnos("Selecciona al menos un alumno");
      valido = false;
    }
    if (!valido) return;

    setGuardando(true);
    try {
      const creado = await crearGrupo(aulaId, {
        nombre,
        descripcion: form.descripcion.trim() || undefined,
        estudiantesIds: seleccionados,
      });
      // el backend devuelve el grupo con sus alumnos: se agrega sin otra llamada
      setGrupos((prev) =>
        [...prev, creado].sort((a, b) => a.nombre.localeCompare(b.nombre))
      );
      setCreando(false);
      setMensaje({ tipo: "success", texto: `Grupo «${nombre}» creado correctamente` });
    } catch (e) {
      setErrorForm(e.message); // ej. 409: nombre repetido en el aula
    } finally {
      setGuardando(false);
    }
  };

  // al abrir el modal se piden solo las lecturas activas del aula
  const abrirAsignar = (grupo) => {
    setAsignando(grupo);
    setLecturas([]);
    setLecturaId("");
    setErrorAsignar(null);
    setCargandoLecturas(true);

    listarLecturas(aulaId)
      .then(setLecturas)
      .catch((e) => setErrorAsignar(e.message))
      .finally(() => setCargandoLecturas(false));
  };

  const asignar = async (ev) => {
    ev.preventDefault();
    const grupo = asignando;
    const lectura = lecturas.find((l) => String(l.id) === String(lecturaId));

    if (!lectura) {
      setErrorAsignar("Elige la lectura que recibirá el grupo");
      return;
    }

    setErrorAsignar(null);
    setAsignandoLectura(true);
    try {
      const resp = await asignarLecturaAGrupo(aulaId, grupo.id, {
        lecturaId: lectura.id,
      });
      setAsignando(null);
      setMensaje({
        tipo: "success",
        texto: mensajeAsignacion(resp, grupo, lectura),
      });
    } catch (e) {
      setErrorAsignar(e.message); // ej. 400 grupo vacío o 409 ya asignada
    } finally {
      setAsignandoLectura(false);
    }
  };

  // resumen legible de la respuesta { asignados, yaTenían }
  const mensajeAsignacion = (resp, grupo, lectura) => {
    if (resp.asignados === 0) {
      return `El grupo «${grupo.nombre}» ya tenía la lectura «${lectura.titulo}» (${textoAlumnos(resp.yaTenían)})`;
    }
    const extra = resp.yaTenían > 0 ? `; ${resp.yaTenían} ya la tenían` : "";
    return `Lectura «${lectura.titulo}» asignada a ${textoAlumnos(resp.asignados)} del grupo «${grupo.nombre}»${extra}`;
  };

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center flex-wrap gap-3 mb-4">
        <div>
          <h1 className="h3 fw-bold mb-1">Grupos</h1>
          <div className="text-secondary">
            Organiza alumnos y asigna una lectura a todo el grupo
          </div>
        </div>
        <div className="d-flex align-items-center gap-3">
          {aulas.length > 0 && (
            <div>
              <label htmlFor="aula" className="visually-hidden">Aula</label>
              <select
                id="aula"
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
            className="btn btn-primary"
            onClick={abrirCrear}
            disabled={!aula}
          >
            <i className="bi bi-plus-lg me-2" />
            Nuevo grupo
          </button>
        </div>
      </div>

      {error && (
        <div className="alert alert-danger alert-dismissible">
          {error}
          <button
            type="button"
            className="btn-close"
            aria-label="Cerrar aviso"
            onClick={() => setError(null)}
          />
        </div>
      )}

      {mensaje && (
        <div className={`alert alert-${mensaje.tipo} alert-dismissible`}>
          {mensaje.texto}
          <button
            type="button"
            className="btn-close"
            aria-label="Cerrar aviso"
            onClick={() => setMensaje(null)}
          />
        </div>
      )}

      {aulas.length === 0 && !cargando ? (
        <div className="panel text-center py-5">
          <p className="mb-3">Aún no tienes aulas registradas.</p>
          <Link to="/aulas" className="btn btn-primary">Registrar mi primera aula</Link>
        </div>
      ) : cargando ? (
        <p className="text-center text-secondary py-4">Cargando grupos…</p>
      ) : (
        <>
          <div className="d-flex justify-content-between align-items-center mb-3">
            <span className="text-secondary">
              {aula?.nombre} · {aula?.grados.map((g) => g.nombre).join(", ")}
            </span>
            <span className="tag tag-neutro">{grupos.length} grupos</span>
          </div>

          {grupos.length === 0 ? (
            <div className="panel text-center py-5">
              <p className="mb-1 fw-semibold">Aún no hay grupos en esta aula.</p>
              <p className="text-secondary mb-3">
                Crea un grupo para asignar una lectura a varios alumnos a la vez.
              </p>
              <button type="button" className="btn btn-primary" onClick={abrirCrear}>
                <i className="bi bi-plus-lg me-2" />
                Crear mi primer grupo
              </button>
            </div>
          ) : (
            <div className="row g-3">
              {grupos.map((g) => {
                const alumnosGrupo = g.alumnos ?? [];
                return (
                  <div className="col-md-6 col-xl-4" key={g.id}>
                    <article className="panel h-100 d-flex flex-column">
                      <div className="d-flex justify-content-between align-items-start gap-2 mb-2">
                        <div>
                          <h2 className="h6 fw-bold mb-1">{g.nombre}</h2>
                          {g.descripcion && (
                            <div className="text-secondary small">{g.descripcion}</div>
                          )}
                        </div>
                        <span className={`tag ${alumnosGrupo.length ? "tag-activo" : "tag-neutro"}`}>
                          {textoAlumnos(alumnosGrupo.length)}
                        </span>
                      </div>

                      {alumnosGrupo.length === 0 ? (
                        <p className="text-secondary small mb-3">
                          Grupo vacío: agrega alumnos al crearlo para poder asignarle lecturas.
                        </p>
                      ) : (
                        <ul className="list-unstyled d-flex flex-column gap-2 mb-3">
                          {alumnosGrupo.map((a) => (
                            <li key={a.id} className="d-flex justify-content-between gap-2">
                              <span className="fw-semibold">{a.nombres} {a.apellidos}</span>
                              <span className="text-secondary small">{a.gradoNombre}</span>
                            </li>
                          ))}
                        </ul>
                      )}

                      <button
                        type="button"
                        className="btn btn-sm btn-outline-primary rounded-pill mt-auto align-self-start"
                        aria-label={`Asignar lectura al grupo ${g.nombre}`}
                        title={alumnosGrupo.length === 0
                          ? "El grupo no tiene alumnos"
                          : "Asignar una lectura a todo el grupo"}
                        onClick={() => abrirAsignar(g)}
                        disabled={alumnosGrupo.length === 0}
                      >
                        <i className="bi bi-journal-plus me-2" />
                        Asignar lectura
                      </button>
                    </article>
                  </div>
                );
              })}
            </div>
          )}
        </>
      )}

      {/* Modal para crear un grupo con nombre, descripción y alumnos */}
      {creando && (
        <div className="modal d-block" tabIndex="-1" role="dialog" aria-label="Crear grupo">
          <div className="modal-dialog modal-dialog-centered">
            <form className="modal-content border-0 rounded-4" onSubmit={guardarGrupo} noValidate>
              <div className="modal-header border-0">
                <h2 className="modal-title h5 fw-bold">Crear grupo</h2>
                <button
                  type="button"
                  className="btn-close"
                  aria-label="Cerrar"
                  onClick={() => setCreando(false)}
                />
              </div>

              <div className="modal-body">
                {errorForm && <div className="alert alert-danger">{errorForm}</div>}

                <div className="mb-3">
                  <label htmlFor="nombre-grupo" className="form-label fw-semibold">
                    Nombre del grupo
                  </label>
                  <input
                    id="nombre-grupo"
                    className="form-control"
                    maxLength={60}
                    value={form.nombre}
                    onChange={cambiarForm("nombre")}
                  />
                </div>

                <div className="mb-3">
                  <label htmlFor="descripcion-grupo" className="form-label fw-semibold">
                    Descripción (opcional)
                  </label>
                  <textarea
                    id="descripcion-grupo"
                    className="form-control"
                    rows="2"
                    maxLength={200}
                    value={form.descripcion}
                    onChange={cambiarForm("descripcion")}
                  />
                </div>

                <div className="mb-2">
                  <span className="form-label fw-semibold d-block">Alumnos del grupo</span>
                  {activos.length === 0 ? (
                    <div className="form-text text-danger">
                      Este aula no tiene alumnos activos.
                    </div>
                  ) : (
                    <div
                      className={`border rounded-3 p-2 ${errorAlumnos ? "border-danger" : ""}`}
                      style={{ maxHeight: 220, overflowY: "auto" }}
                    >
                      {activos.map((a) => (
                        <label
                          key={a.id}
                          className="form-check d-flex align-items-center gap-2"
                        >
                          <input
                            type="checkbox"
                            className="form-check-input mt-0"
                            checked={seleccionados.includes(a.id)}
                            onChange={() => alternarAlumno(a.id)}
                          />
                          <span>
                            {a.nombres} {a.apellidos}
                            <span className="text-secondary small"> · {a.gradoNombre}</span>
                          </span>
                        </label>
                      ))}
                    </div>
                  )}
                  {errorAlumnos && (
                    <div className="invalid-feedback d-block">{errorAlumnos}</div>
                  )}
                  <div className="form-text">Solo se muestran alumnos activos.</div>
                </div>
              </div>

              <div className="modal-footer border-0">
                <button
                  type="button"
                  className="btn btn-outline-secondary rounded-pill"
                  onClick={() => setCreando(false)}
                >
                  Cancelar
                </button>
                <button type="submit" className="btn btn-primary" disabled={guardando}>
                  {guardando ? "Creando…" : "Crear grupo"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal para elegir la lectura que recibirá todo el grupo */}
      {asignando && (
        <div
          className="modal d-block"
          tabIndex="-1"
          role="dialog"
          aria-label="Asignar lectura al grupo"
        >
          <div className="modal-dialog modal-dialog-centered">
            <form className="modal-content border-0 rounded-4" onSubmit={asignar} noValidate>
              <div className="modal-header border-0">
                <h2 className="modal-title h5 fw-bold">Asignar lectura al grupo</h2>
                <button
                  type="button"
                  className="btn-close"
                  aria-label="Cerrar"
                  onClick={() => setAsignando(null)}
                />
              </div>

              <div className="modal-body">
                {errorAsignar && <div className="alert alert-danger">{errorAsignar}</div>}

                <div className="panel mb-3">
                  <div className="text-secondary small text-uppercase fw-semibold mb-1">
                    Grupo
                  </div>
                  <div className="fw-bold">{asignando.nombre}</div>
                  <div className="text-secondary small">
                    {textoAlumnos(asignando.alumnos?.length ?? 0)}
                  </div>
                </div>

                <label htmlFor="lectura-grupo" className="form-label fw-semibold">
                  Lectura
                </label>
                <select
                  id="lectura-grupo"
                  className="form-select"
                  value={lecturaId}
                  disabled={cargandoLecturas}
                  onChange={(ev) => setLecturaId(ev.target.value)}
                >
                  <option value="">Selecciona una lectura</option>
                  {lecturas.map((l) => (
                    <option key={l.id} value={l.id}>
                      {l.titulo} · {l.gradoNombre || `Grado ${l.gradoId}`}
                    </option>
                  ))}
                </select>

                {cargandoLecturas ? (
                  <div className="form-text">Cargando lecturas…</div>
                ) : lecturas.length === 0 ? (
                  <div className="form-text text-danger">
                    No hay lecturas activas para esta aula.
                  </div>
                ) : (
                  <div className="form-text">
                    Se asignará a todos los alumnos del grupo. Si alguno ya la tenía, se omite.
                  </div>
                )}
              </div>

              <div className="modal-footer border-0">
                <button
                  type="button"
                  className="btn btn-outline-secondary rounded-pill"
                  onClick={() => setAsignando(null)}
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={
                    asignandoLectura ||
                    cargandoLecturas ||
                    !lecturaId ||
                    (asignando.alumnos?.length ?? 0) === 0
                  }
                >
                  {asignandoLectura ? "Asignando…" : "Asignar lectura"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {(creando || asignando) && <div className="modal-backdrop fade show" />}
    </div>
  );
}
