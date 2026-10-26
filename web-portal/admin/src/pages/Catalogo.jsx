import { useEffect, useState } from "react";
import { crearLectura, listarLecturas } from "../api";

const TIPOS_RETO = [
  { valor: "lectura", texto: "Lectura", icono: "bi-book" },
  { valor: "escritura", texto: "Escritura", icono: "bi-pencil-square" },
  { valor: "oralidad", texto: "Oralidad", icono: "bi-mic" },
];

const DIFICULTADES = [
  { valor: "basica", texto: "Básica", clase: "bs-ok" },
  { valor: "intermedia", texto: "Intermedia", clase: "bs-warn" },
  { valor: "avanzada", texto: "Avanzada", clase: "bs-off" },
];

const GRADOS = [1, 2, 3, 4, 5, 6];

const vacio = { titulo: "", tipo_reto: "lectura", grado: "1", dificultad: "basica", texto: "" };

const tipoDe = (valor) => TIPOS_RETO.find((t) => t.valor === valor) ?? { texto: valor, icono: "bi-journal" };
const dificultadDe = (valor) => DIFICULTADES.find((d) => d.valor === valor) ?? { texto: valor, clase: "bs-info" };

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
        <div className="modal-dialog modal-dialog-centered modal-lg" onClick={(e) => e.stopPropagation()}>
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

function ModalLectura({ onCerrar, onGuardado, onNoAutorizado }) {
  const [datos, setDatos] = useState(vacio);
  const [errores, setErrores] = useState({});
  const [enviando, setEnviando] = useState(false);

  const cambiar = (e) => setDatos({ ...datos, [e.target.name]: e.target.value });

  // Validación en el navegador (el backend vuelve a validar todo)
  const validar = () => {
    const e = {};
    if (!datos.titulo.trim()) e.titulo = "El título es obligatorio.";
    if (datos.texto.trim().length < 20) e.texto = "El texto de la lectura debe tener al menos 20 caracteres.";
    return e;
  };

  const guardar = async (ev) => {
    ev.preventDefault();
    const e = validar();
    setErrores(e);
    if (Object.keys(e).length > 0) return;

    setEnviando(true);
    try {
      const guardada = await crearLectura({ ...datos, grado: Number(datos.grado) });
      onGuardado(guardada, "Lectura registrada correctamente.");
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
      titulo="Nueva lectura"
      onCerrar={onCerrar}
      pie={
        <>
          <button type="button" className="btn btn-light" onClick={onCerrar}>Cancelar</button>
          <button type="submit" form="form-lectura" className="btn btn-primary" disabled={enviando}>
            {enviando ? "Guardando..." : "Guardar lectura"}
          </button>
        </>
      }
    >
      <form id="form-lectura" className="modal-body row g-3" onSubmit={guardar} noValidate>
        {(errores.general || errores.non_field_errors) && (
          <div className="col-12">
            <div className="alert alert-danger py-2 small mb-0">{errores.general || errores.non_field_errors}</div>
          </div>
        )}
        <div className="col-md-8">
          <label className="form-label" htmlFor="titulo">Título *</label>
          <input id="titulo" name="titulo" className={clase("titulo")} placeholder="El cóndor y el zorro"
            maxLength={150} value={datos.titulo} onChange={cambiar} />
          {feedback("titulo")}
        </div>
        <div className="col-md-4">
          <label className="form-label" htmlFor="tipo_reto">Tipo de reto *</label>
          <select id="tipo_reto" name="tipo_reto" className={clase("tipo_reto", "form-select")} value={datos.tipo_reto} onChange={cambiar}>
            {TIPOS_RETO.map((t) => <option key={t.valor} value={t.valor}>{t.texto}</option>)}
          </select>
          {feedback("tipo_reto")}
        </div>
        <div className="col-md-6">
          <label className="form-label" htmlFor="grado">Grado *</label>
          <select id="grado" name="grado" className={clase("grado", "form-select")} value={datos.grado} onChange={cambiar}>
            {GRADOS.map((g) => <option key={g} value={g}>{g}.° grado</option>)}
          </select>
          {feedback("grado")}
        </div>
        <div className="col-md-6">
          <label className="form-label" htmlFor="dificultad">Dificultad *</label>
          <select id="dificultad" name="dificultad" className={clase("dificultad", "form-select")} value={datos.dificultad} onChange={cambiar}>
            {DIFICULTADES.map((d) => <option key={d.valor} value={d.valor}>{d.texto}</option>)}
          </select>
          {feedback("dificultad")}
        </div>
        <div className="col-12">
          <label className="form-label" htmlFor="texto">Texto de la lectura *</label>
          <textarea id="texto" name="texto" rows={6} className={clase("texto")}
            placeholder="Había una vez un cóndor que vivía en lo alto de la montaña…"
            value={datos.texto} onChange={cambiar}></textarea>
          {feedback("texto")}
          <div className="form-text">{datos.texto.trim().length} caracteres (mínimo 20).</div>
        </div>
      </form>
    </Modal>
  );
}

export default function Catalogo({ onNoAutorizado, puede = () => true }) {
  const [lecturas, setLecturas] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [exito, setExito] = useState("");
  const [recarga, setRecarga] = useState(0);
  const [modalAbierto, setModalAbierto] = useState(false);

  useEffect(() => {
    let activo = true;
    listarLecturas()
      .then((datos) => {
        if (!activo) return;
        setLecturas(Array.isArray(datos) ? datos : datos.results ?? []);
        setError("");
      })
      .catch((err) => {
        if (!activo) return;
        if (err.estado === 401) onNoAutorizado();
        else setError(err.datos?.detail ?? err.mensaje ?? "No se pudo cargar el catálogo.");
      })
      .finally(() => {
        if (activo) setCargando(false);
      });
    return () => {
      activo = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [recarga]);

  useEffect(() => {
    if (!exito) return;
    const t = setTimeout(() => setExito(""), 4000);
    return () => clearTimeout(t);
  }, [exito]);

  const alGuardar = (_lectura, mensaje) => {
    setModalAbierto(false);
    setExito(mensaje);
    setRecarga((n) => n + 1);
  };

  return (
    <section>
      <div className="page-title d-flex flex-wrap justify-content-between align-items-end gap-2 mb-4">
        <div>
          <h1>Catálogo de lecturas</h1>
          <div className="breadcrumb text-secondary">Contenido / Catálogo</div>
        </div>
        {puede("catalogo.crear") && (
          <button type="button" className="btn btn-primary" onClick={() => setModalAbierto(true)}>
            <i className="bi bi-plus-lg me-1"></i>Nueva lectura
          </button>
        )}
      </div>

      {exito && (
        <div className="alert alert-success py-2 small d-flex gap-2 align-items-center">
          <i className="bi bi-check-circle"></i>{exito}
        </div>
      )}

      <div className="card">
        <div className="card-header d-flex align-items-center">
          <span className="fw-semibold">Lecturas registradas</span>
          <span className="ms-auto note">
            {cargando ? "Cargando..." : `${lecturas.length} ${lecturas.length === 1 ? "lectura" : "lecturas"}`}
          </span>
        </div>

        {error && <div className="alert alert-danger m-3 py-2 small">{error}</div>}

        {!error && !cargando && lecturas.length === 0 && (
          <div className="text-center text-secondary py-5">
            <i className="bi bi-journal-richtext fs-2 d-block mb-2"></i>
            Aún no hay lecturas en el catálogo.
          </div>
        )}

        {lecturas.length > 0 && (
          <div className="table-responsive">
            <table className="table table-hover">
              <thead>
                <tr>
                  <th>Título</th>
                  <th>Tipo de reto</th>
                  <th>Grado</th>
                  <th>Dificultad</th>
                  <th>Estado</th>
                  <th>Registrada por</th>
                </tr>
              </thead>
              <tbody>
                {lecturas.map((l) => {
                  const tipo = tipoDe(l.tipo_reto);
                  const dificultad = dificultadDe(l.dificultad);
                  return (
                    <tr key={l.id}>
                      <td className="fw-medium">{l.titulo}</td>
                      <td><i className={`bi ${tipo.icono} me-1 text-primary`}></i>{tipo.texto}</td>
                      <td>{l.grado}.°</td>
                      <td><span className={`badge-soft ${dificultad.clase}`}>{dificultad.texto}</span></td>
                      <td>
                        <span className={`badge-soft ${l.activo ? "bs-ok" : "bs-off"}`}>{l.activo ? "Activa" : "Inactiva"}</span>
                      </td>
                      <td className="note">{l.creado_por ?? "—"}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {modalAbierto && (
        <ModalLectura
          onCerrar={() => setModalAbierto(false)}
          onGuardado={alGuardar}
          onNoAutorizado={onNoAutorizado}
        />
      )}
    </section>
  );
}
