import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { listarAulas, listarEstudiantes, crearEstudiante } from "../api";

const COLORES = ["#9B22E1", "#2FC5F6", "#F59E0B", "#F51E38", "#22C55E", "#6366F1"];
const FORM_VACIO = { nombres: "", apellidos: "", gradoId: "" };

export default function Alumnos() {
  const [aulas, setAulas] = useState([]);
  const [aulaId, setAulaId] = useState("");
  const [alumnos, setAlumnos] = useState([]);
  const [mensaje, setMensaje] = useState(null); // { tipo: "success" | "danger", texto }
  const [abierto, setAbierto] = useState(false);
  const [form, setForm] = useState(FORM_VACIO);
  const [errores, setErrores] = useState({});
  const [errorServidor, setErrorServidor] = useState(null);

  const aula = aulas.find((a) => String(a.id) === String(aulaId));
  const activos = alumnos.filter((a) => a.activo).length;

  const cargarAlumnos = (id) =>
    listarEstudiantes(id)
      .then(setAlumnos)
      .catch((e) => setMensaje({ tipo: "danger", texto: e.message }));

  // al abrir la pantalla: cargar las aulas del docente
  useEffect(() => {
    listarAulas()
      .then((data) => {
        setAulas(data);
        if (data.length > 0) setAulaId(String(data[0].id));
      })
      .catch((e) => setMensaje({ tipo: "danger", texto: e.message }));
  }, []);


  // cada vez que cambia el aula: cargar sus alumnos
    useEffect(() => {
      if (!aulaId) return;
      listarEstudiantes(aulaId)
        .then(setAlumnos)
        .catch((e) => setMensaje({ tipo: "danger", texto: e.message }));
    }, [aulaId]);

  const abrirFormulario = () => {
    setForm(FORM_VACIO);
    setErrores({});
    setErrorServidor(null);
    setAbierto(true);
  };

  const cambiar = (campo) => (ev) => setForm({ ...form, [campo]: ev.target.value });

  const validar = () => {
    const e = {};
    if (!form.nombres.trim()) e.nombres = "Los nombres son obligatorios";
    if (!form.apellidos.trim()) e.apellidos = "Los apellidos son obligatorios";
    if (!form.gradoId) e.gradoId = "Selecciona el grado del alumno";
    return e;
  };

  const guardar = async (ev) => {
    ev.preventDefault();
    const e = validar();
    setErrores(e);
    if (Object.keys(e).length > 0) return;

    try {
      await crearEstudiante(aulaId, {
        nombres: form.nombres,
        apellidos: form.apellidos,
        gradoId: Number(form.gradoId),
      });
      setAbierto(false);
      setMensaje({ tipo: "success", texto: "Alumno registrado correctamente" });
      cargarAlumnos(aulaId);
    } catch (error) {
      setErrorServidor(error.message);
    }
  };

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center flex-wrap gap-3 mb-4">
        <div>
          <h1 className="h3 fw-bold mb-1">Alumnos</h1>
          <div className="text-secondary">Registra y gestiona a los alumnos de tu aula</div>
        </div>
        <div className="d-flex align-items-center gap-3">
          {aulas.length > 0 && (
            <div>
              <label htmlFor="aula" className="visually-hidden">Aula</label>
              <select
                id="aula"
                className="form-select rounded-pill"
                value={aulaId}
                onChange={(ev) => setAulaId(ev.target.value)}
              >
                {aulas.map((a) => (
                  <option key={a.id} value={a.id}>{a.nombre}</option>
                ))}
              </select>
            </div>
          )}
          <button className="btn btn-primary" onClick={abrirFormulario} disabled={!aula}>
            <i className="bi bi-plus-lg me-2" />
            Agregar alumno
          </button>
        </div>
      </div>

      {mensaje && (
        <div className={`alert alert-${mensaje.tipo} alert-dismissible`}>
          {mensaje.texto}
          <button type="button" className="btn-close" aria-label="Cerrar aviso" onClick={() => setMensaje(null)} />
        </div>
      )}

      {aulas.length === 0 ? (
        <div className="panel text-center py-5">
          <p className="mb-3">Aún no tienes aulas registradas.</p>
          <Link to="/aulas" className="btn btn-primary">Registrar mi primera aula</Link>
        </div>
      ) : (
        <>
          <div className="row g-3 mb-4">
            <div className="col-md-4">
              <div className="panel stat">
                <span className="stat-ico" style={{ background: "rgba(155,34,225,.12)", color: "#9B22E1" }}>
                  <i className="bi bi-people" />
                </span>
                <div><b>{alumnos.length}</b><span>Total de alumnos</span></div>
              </div>
            </div>
            <div className="col-md-4">
              <div className="panel stat">
                <span className="stat-ico" style={{ background: "rgba(47,197,246,.16)", color: "#0B7FA6" }}>
                  <i className="bi bi-person-check" />
                </span>
                <div><b>{activos}</b><span>Alumnos activos</span></div>
              </div>
            </div>
            <div className="col-md-4">
              <div className="panel stat">
                <span className="stat-ico" style={{ background: "rgba(245,30,56,.12)", color: "#C0152B" }}>
                  <i className="bi bi-person-x" />
                </span>
                <div><b>{alumnos.length - activos}</b><span>Alumnos inactivos</span></div>
              </div>
            </div>
          </div>

          <div className="panel">
            <h2 className="h5 fw-bold mb-1">Lista de alumnos</h2>
            <div className="text-secondary mb-3">
              {aula?.nombre} · {aula?.grados.map((g) => g.nombre).join(", ")}
            </div>

            {alumnos.length === 0 ? (
              <p className="text-center text-secondary py-4 mb-0">
                Aún no hay alumnos en esta aula.
              </p>
            ) : (
              <table className="table tabla align-middle mb-0">
                <thead>
                  <tr>
                    <th>ALUMNO</th>
                    <th>GRADO</th>
                    <th>ESTADO</th>
                  </tr>
                </thead>
                <tbody>
                  {alumnos.map((a, i) => (
                    <tr key={a.id}>
                      <td>
                        <div className="d-flex align-items-center gap-3">
                          <span className="inicial" style={{ background: COLORES[i % COLORES.length] }}>
                            {a.nombres[0]}{a.apellidos[0]}
                          </span>
                          <span className="fw-semibold">{a.nombres} {a.apellidos}</span>
                        </div>
                      </td>
                      <td>{a.gradoNombre}</td>
                      <td>
                        <span className={`tag ${a.activo ? "tag-activo" : "tag-inactivo"}`}>
                          {a.activo ? "Activo" : "Inactivo"}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </>
      )}

      {abierto && (
        <>
          <div className="modal d-block" tabIndex="-1" role="dialog">
            <div className="modal-dialog modal-dialog-centered">
              <form className="modal-content border-0 rounded-4" onSubmit={guardar} noValidate>
                <div className="modal-header border-0">
                  <h2 className="modal-title h5 fw-bold">Agregar alumno</h2>
                  <button type="button" className="btn-close" aria-label="Cerrar" onClick={() => setAbierto(false)} />
                </div>
                <div className="modal-body">
                  {errorServidor && <div className="alert alert-danger">{errorServidor}</div>}

                  <div className="mb-3">
                    <label htmlFor="nombres" className="form-label fw-semibold">Nombres</label>
                    <input id="nombres" className={`form-control ${errores.nombres ? "is-invalid" : ""}`}
                      value={form.nombres} onChange={cambiar("nombres")} />
                    {errores.nombres && <div className="invalid-feedback">{errores.nombres}</div>}
                  </div>

                  <div className="mb-3">
                    <label htmlFor="apellidos" className="form-label fw-semibold">Apellidos</label>
                    <input id="apellidos" className={`form-control ${errores.apellidos ? "is-invalid" : ""}`}
                      value={form.apellidos} onChange={cambiar("apellidos")} />
                    {errores.apellidos && <div className="invalid-feedback">{errores.apellidos}</div>}
                  </div>

                  <div className="mb-2">
                    <label htmlFor="grado" className="form-label fw-semibold">Grado</label>
                    <select id="grado" className={`form-select ${errores.gradoId ? "is-invalid" : ""}`}
                      value={form.gradoId} onChange={cambiar("gradoId")}>
                      <option value="">Selecciona un grado</option>
                      {aula?.grados.map((g) => (
                        <option key={g.id} value={g.id}>{g.nombre}</option>
                      ))}
                    </select>
                    {errores.gradoId && <div className="invalid-feedback">{errores.gradoId}</div>}
                  </div>
                </div>
                <div className="modal-footer border-0">
                  <button type="button" className="btn btn-outline-secondary rounded-pill" onClick={() => setAbierto(false)}>
                    Cancelar
                  </button>
                  <button type="submit" className="btn btn-primary">Guardar</button>
                </div>
              </form>
            </div>
          </div>
          <div className="modal-backdrop fade show" />
        </>
      )}
    </div>
  );
}