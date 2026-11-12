import { useEffect, useState } from "react";
import {
  actualizarLectura,
  crearLectura,
  eliminarLectura,
  importarLectura,
  listarLecturas,
  listarRecursosOficiales,
  sincronizarRecursos,
  textoDeRecurso,
} from "../api";

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

// Si recibe "lectura", el formulario edita esa lectura (T-244); si no, crea una nueva
function ModalLectura({ lectura = null, onCerrar, onGuardado, onNoAutorizado }) {
  const editando = Boolean(lectura);
  const [datos, setDatos] = useState(
    editando
      ? {
          titulo: lectura.titulo,
          tipo_reto: lectura.tipo_reto,
          grado: String(lectura.grado),
          dificultad: lectura.dificultad,
          texto: lectura.texto,
        }
      : vacio
  );
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
            const enviar = { ...datos, grado: Number(datos.grado) };
      const guardada = editando ? await actualizarLectura(lectura.id, enviar) : await crearLectura(enviar);
      onGuardado(guardada, editando ? "Lectura actualizada correctamente." : "Lectura registrada correctamente.");
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
      titulo={editando ? "Editar lectura" : "Nueva lectura"}
      onCerrar={onCerrar}
      pie={
        <>
          <button type="button" className="btn btn-light" onClick={onCerrar}>Cancelar</button>
          <button type="submit" form="form-lectura" className="btn btn-primary" disabled={enviando}>
            {enviando ? "Guardando..." : editando ? "Guardar cambios" : "Guardar lectura"}
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

// T-175: confirmación antes de eliminar (borrado lógico)
function ModalEliminar({ lectura, onCerrar, onEliminada, onNoAutorizado }) {
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState(lectura.asignada
    ? "La lectura está asignada a alumnos: no se puede eliminar, solo desactivar."
    : "");

  const confirmar = async () => {
    setEnviando(true);
    try {
      await eliminarLectura(lectura.id);
      onEliminada(`«${lectura.titulo}» se desactivó correctamente.`);
    } catch (err) {
      if (err.estado === 401) onNoAutorizado();
      else setError(err.datos?.detail ?? err.mensaje ?? "No se pudo eliminar la lectura.");
    } finally {
      setEnviando(false);
    }
  };

  return (
    <>
      <div className="modal d-block" tabIndex="-1" role="dialog" aria-modal="true" onClick={onCerrar}>
        <div className="modal-dialog modal-dialog-centered modal-sm" onClick={(e) => e.stopPropagation()}>
          <div className="modal-content text-center p-3">
            <div className="mx-auto mb-2 fs-2 text-danger"><i className={`bi ${error ? "bi-exclamation-triangle" : "bi-trash3"}`}></i></div>
            <h5 className="fw-semibold">{error ? "No se puede eliminar" : `¿Eliminar «${lectura.titulo}»?`}</h5>
            {error && <p className="note">{error}</p>}
            <div className="d-flex gap-2">
              <button type="button" className={`btn btn-light ${error ? "w-100" : "w-50"}`} onClick={onCerrar}>
                {error ? "Entendido" : "Cancelar"}
              </button>
              {!error && (
                <button type="button" className="btn btn-danger w-50" onClick={confirmar} disabled={enviando}>
                  {enviando ? "Eliminando..." : "Eliminar"}
                </button>
              )}
            </div>
          </div>
        </div>
      </div>
      <div className="modal-backdrop show"></div>
    </>
  );
}


const FUENTE_OFICIAL = "Biblioteca Escolar Digital – Ministerio de Educación del Perú";
const MAX_PAGINAS = 10;

// T-444: importar una lectura desde las páginas de un libro oficial
function ModalImportar({ recurso, onCerrar, onImportada, onNoAutorizado }) {
  const [paginas, setPaginas] = useState({ desde: "", hasta: "" });
  const [datos, setDatos] = useState({ titulo: "", texto: "", tipo_reto: "lectura", dificultad: "basica", grado: "1" });
  const [errores, setErrores] = useState({});
  const [aviso, setAviso] = useState("");
  const [totalPaginas, setTotalPaginas] = useState(null);
  const [extrayendo, setExtrayendo] = useState(false);
  const [enviando, setEnviando] = useState(false);

  const cambiar = (e) => setDatos({ ...datos, [e.target.name]: e.target.value });
  const cambiarPagina = (e) => setPaginas({ ...paginas, [e.target.name]: e.target.value });

  const validarPaginas = () => {
    const desde = Number(paginas.desde);
    const hasta = Number(paginas.hasta);
    const e = {};
    if (!paginas.desde || desde < 1) e.desde = "Indica desde qué página (1 o más).";
    if (!paginas.hasta || hasta < 1) e.hasta = "Indica hasta qué página (1 o más).";
    else if (hasta < desde) e.hasta = "La página final no puede ser menor que la inicial.";
    else if (hasta - desde + 1 > MAX_PAGINAS) e.hasta = `Puedes sacar hasta ${MAX_PAGINAS} páginas a la vez.`;
    return e;
  };

  const sacarTexto = async () => {
    const e = validarPaginas();
    setErrores(e);
    setAviso("");
    if (Object.keys(e).length > 0) return;
    setExtrayendo(true);
    try {
      const r = await textoDeRecurso(recurso.id, paginas.desde, paginas.hasta);
      setDatos((actual) => ({ ...actual, texto: r.texto }));
      setTotalPaginas(r.paginas_total);
      setAviso(r.aviso);
    } catch (err) {
      if (err.estado === 401) return onNoAutorizado();
      if (err.estado === 400 && err.datos && !err.datos.detail) setErrores(erroresDeServidor(err.datos));
      else setErrores({ general: err.datos?.detail ?? err.mensaje ?? "No se pudo leer el PDF." });
    } finally {
      setExtrayendo(false);
    }
  };

  const guardar = async (ev) => {
    ev.preventDefault();
    const e = {};
    if (!datos.titulo.trim()) e.titulo = "El título es obligatorio.";
    if (datos.texto.trim().length < 20) e.texto = "El texto de la lectura debe tener al menos 20 caracteres.";
    setErrores(e);
    if (Object.keys(e).length > 0) return;

    setEnviando(true);
    try {
      const enviar = { titulo: datos.titulo, texto: datos.texto, tipo_reto: datos.tipo_reto, dificultad: datos.dificultad };
      if (!recurso.grado) enviar.grado = Number(datos.grado);
      const lectura = await importarLectura(recurso.id, enviar);
      onImportada(`«${lectura.titulo}» se agregó al catálogo (${lectura.grado}.° grado).`);
    } catch (err) {
      if (err.estado === 401) onNoAutorizado();
      else if (err.estado === 400) setErrores(erroresDeServidor(err.datos));
      else setErrores({ general: err.datos?.detail ?? err.mensaje ?? "No se pudo guardar la lectura." });
    } finally {
      setEnviando(false);
    }
  };

  const clase = (campo, base = "form-control") => `${base}${errores[campo] ? " is-invalid" : ""}`;
  const feedback = (campo) => errores[campo] && <div className="invalid-feedback">{errores[campo]}</div>;

  return (
    <Modal
      titulo={`Importar lectura · ${recurso.titulo.split(":")[0].trim()}`}
      onCerrar={onCerrar}
      pie={
        <>
          <span className="note me-auto">Fuente: {FUENTE_OFICIAL}</span>
          <button type="button" className="btn btn-light" onClick={onCerrar}>Cancelar</button>
          <button type="submit" form="form-importar" className="btn btn-primary" disabled={enviando || extrayendo}>
            {enviando ? "Guardando..." : "Guardar lectura"}
          </button>
        </>
      }
    >
      <form id="form-importar" className="modal-body row g-3" onSubmit={guardar} noValidate>
        {errores.general && (
          <div className="col-12"><div className="alert alert-danger py-2 small mb-0">{errores.general}</div></div>
        )}

        <div className="col-12">
          <div className="paso-importar">
            <span className="paso-numero">1</span>
            <div className="flex-grow-1">
              <div className="fw-bold">Elige las páginas del libro</div>
              <div className="note">
                Usa el número de página del visor de PDF{totalPaginas ? ` (el libro tiene ${totalPaginas} páginas)` : ""}.
                {recurso.url_pdf && (
                  <> <a href={recurso.url_pdf} target="_blank" rel="noreferrer">Abrir el PDF <i className="bi bi-box-arrow-up-right"></i></a></>
                )}
              </div>
            </div>
          </div>
        </div>
        <div className="col-sm-4">
          <label className="form-label" htmlFor="desde">Desde la página</label>
          <input id="desde" name="desde" type="number" min="1" className={clase("desde")} value={paginas.desde} onChange={cambiarPagina} />
          {feedback("desde")}
        </div>
        <div className="col-sm-4">
          <label className="form-label" htmlFor="hasta">Hasta la página</label>
          <input id="hasta" name="hasta" type="number" min="1" className={clase("hasta")} value={paginas.hasta} onChange={cambiarPagina} />
          {feedback("hasta")}
        </div>
        <div className="col-sm-4 d-flex align-items-end">
          <button type="button" className="btn btn-light w-100" onClick={sacarTexto} disabled={extrayendo}>
            {extrayendo ? (
              <><span className="spinner-border spinner-border-sm me-2"></span>Leyendo el PDF…</>
            ) : (
              <><i className="bi bi-file-earmark-text me-1"></i>Sacar texto</>
            )}
          </button>
        </div>
        {extrayendo && (
          <div className="col-12 note">La primera vez se descarga el libro completo: puede tardar un poco.</div>
        )}

        <div className="col-12">
          <div className="paso-importar">
            <span className="paso-numero">2</span>
            <div>
              <div className="fw-bold">Revisa el texto y completa los datos</div>
              <div className="note">Corrige lo que haga falta (por ejemplo, palabras cortadas o instrucciones del libro).</div>
            </div>
          </div>
        </div>
        {aviso && <div className="col-12"><div className="alert alert-warning py-2 small mb-0">{aviso}</div></div>}
        <div className="col-md-8">
          <label className="form-label" htmlFor="imp-titulo">Título *</label>
          <input id="imp-titulo" name="titulo" className={clase("titulo")} maxLength={150}
            placeholder="El título que aparece en el libro" value={datos.titulo} onChange={cambiar} />
          {feedback("titulo")}
        </div>
        <div className="col-md-4">
          <label className="form-label" htmlFor="imp-grado">Grado</label>
          {recurso.grado ? (
            <input id="imp-grado" className="form-control" value={`${recurso.grado}.° grado (del libro)`} disabled />
          ) : (
            <select id="imp-grado" name="grado" className={clase("grado", "form-select")} value={datos.grado} onChange={cambiar}>
              {GRADOS.map((g) => <option key={g} value={g}>{g}.° grado</option>)}
            </select>
          )}
          {feedback("grado")}
        </div>
        <div className="col-md-6">
          <label className="form-label" htmlFor="imp-tipo">Tipo de reto *</label>
          <select id="imp-tipo" name="tipo_reto" className={clase("tipo_reto", "form-select")} value={datos.tipo_reto} onChange={cambiar}>
            {TIPOS_RETO.map((t) => <option key={t.valor} value={t.valor}>{t.texto}</option>)}
          </select>
          {feedback("tipo_reto")}
        </div>
        <div className="col-md-6">
          <label className="form-label" htmlFor="imp-dificultad">Dificultad *</label>
          <select id="imp-dificultad" name="dificultad" className={clase("dificultad", "form-select")} value={datos.dificultad} onChange={cambiar}>
            {DIFICULTADES.map((d) => <option key={d.valor} value={d.valor}>{d.texto}</option>)}
          </select>
          {feedback("dificultad")}
        </div>
        <div className="col-12">
          <label className="form-label" htmlFor="imp-texto">Texto de la lectura *</label>
          <textarea id="imp-texto" name="texto" rows={8} className={clase("texto")}
            placeholder="Elige las páginas y presiona «Sacar texto», o pega el texto aquí."
            value={datos.texto} onChange={cambiar}></textarea>
          {feedback("texto")}
          <div className="form-text">{datos.texto.trim().length} caracteres (mínimo 20).</div>
        </div>
      </form>
    </Modal>
  );
}

// T-444: libros oficiales disponibles para importar lecturas
function LibrosOficiales({ recursos, cargando, puedeImportar, onImportar }) {
  return (
    <div className="card mb-4">
      <div className="card-header d-flex flex-wrap align-items-center gap-2">
        <h2>Libros oficiales disponibles</h2>
        {!cargando && recursos.length > 0 && (
          <span className="badge-soft bs-p">{recursos.length} {recursos.length === 1 ? "libro" : "libros"}</span>
        )}
      </div>
      <div className="card-body">
        {cargando ? (
          <div className="text-center text-secondary py-3">Cargando…</div>
        ) : recursos.length === 0 ? (
          <div className="text-center text-secondary py-3">
            Todavía no hay libros oficiales. Presiona «Actualizar catálogo» para traerlos.
          </div>
        ) : (
          <div className="libros-oficiales">
            {recursos.map((r) => (
              <div key={r.id} className={`libro libro-grado-${r.grado ?? 0}`}>
                <div className="note">{r.titulo.split(":")[0].replace(/\s*\d+\s*$/, "").trim() || "Libro"}</div>
                <div className="libro-grado">{r.grado ? `${r.grado}.° grado` : "Sin grado"}</div>
                <div className="note">
                  {r.lecturas_importadas === 1 ? "1 lectura importada" : `${r.lecturas_importadas} lecturas importadas`}
                </div>
                <div className="d-flex gap-1 mt-2">
                  {puedeImportar && (
                    <button type="button" className="btn btn-sm btn-light flex-grow-1" onClick={() => onImportar(r)}>
                      <i className="bi bi-download me-1"></i>Importar
                    </button>
                  )}
                  {r.url_pdf && (
                    <a className="btn btn-sm btn-light" href={r.url_pdf} target="_blank" rel="noreferrer" title="Ver el PDF">
                      <i className="bi bi-file-earmark-pdf"></i>
                    </a>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
        <div className="note mt-3">Fuente: {FUENTE_OFICIAL}</div>
      </div>
    </div>
  );
}


const sinFiltros = { search: "", tipo_reto: "", grado: "", dificultad: "", activo: "" };

// T-245: filtro por estado ("" = todas, "true" = activas, "false" = desactivadas)
const ESTADOS = [
  { valor: "", texto: "Todas" },
  { valor: "true", texto: "Activas" },
  { valor: "false", texto: "Desactivadas" },
];

export default function Catalogo({ onNoAutorizado, puede = () => true }) {
  const [lecturas, setLecturas] = useState([]);
  const [filtros, setFiltros] = useState(sinFiltros);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [exito, setExito] = useState("");
  const [recarga, setRecarga] = useState(0);
  const [modalAbierto, setModalAbierto] = useState(false);
  const [porEliminar, setPorEliminar] = useState(null);
  const [porEditar, setPorEditar] = useState(null);
  const [recursos, setRecursos] = useState([]);
  const [cargandoRecursos, setCargandoRecursos] = useState(true);
  const [recargaRecursos, setRecargaRecursos] = useState(0);
  const [porImportar, setPorImportar] = useState(null);
  const [actualizando, setActualizando] = useState(false);

  const hayFiltros = Object.values(filtros).some((v) => v);

  useEffect(() => {
    let activo = true;
    // Espera a que el usuario deje de escribir antes de consultar
    const espera = setTimeout(() => {
      setCargando(true);
      listarLecturas({ ...filtros, search: filtros.search.trim() })
        .then((datos) => {
          if (!activo) return;
          setLecturas(Array.isArray(datos) ? datos : datos.results ?? []);
          setError("");
        })
        .catch((err) => {
          if (!activo) return;
          if (err.estado === 401) onNoAutorizado();
          else if (err.estado === 400) setError(Object.values(err.datos ?? {}).flat().join(" ") || "Revisa los filtros.");
          else setError(err.datos?.detail ?? err.mensaje ?? "No se pudo cargar el catálogo.");
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
  }, [filtros, recarga]);

  // T-444: libros oficiales (se vuelven a pedir al actualizar el catálogo o al importar)
  useEffect(() => {
    let activo = true;
    listarRecursosOficiales()
      .then((datos) => activo && setRecursos(Array.isArray(datos) ? datos : datos.results ?? []))
      .catch((err) => {
        if (activo && err.estado === 401) onNoAutorizado();
      })
      .finally(() => activo && setCargandoRecursos(false));
    return () => {
      activo = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [recargaRecursos]);

  useEffect(() => {
    if (!exito) return;
    const t = setTimeout(() => setExito(""), 4000);
    return () => clearTimeout(t);
  }, [exito]);

  const cambiarFiltro = (e) => setFiltros({ ...filtros, [e.target.name]: e.target.value });

  const alGuardar = (_lectura, mensaje) => {
    setModalAbierto(false);
    setPorEditar(null);
    setExito(mensaje);
    setRecarga((n) => n + 1);
  };

  
  // T-444: "Actualizar catálogo" vuelve a leer la Biblioteca Escolar Digital
  const actualizarCatalogo = async () => {
    setActualizando(true);
    setError("");
    try {
      const r = await sincronizarRecursos();
      setExito(`Catálogo actualizado. ${r.resultado}`);
      setCargandoRecursos(true);
      setRecargaRecursos((n) => n + 1);
    } catch (err) {
      if (err.estado === 401) onNoAutorizado();
      else setError(err.datos?.detail ?? err.mensaje ?? "No se pudo actualizar el catálogo.");
    } finally {
      setActualizando(false);
    }
  };

  const alImportar = (mensaje) => {
    setPorImportar(null);
    setExito(mensaje);
    setRecarga((n) => n + 1);
    setRecargaRecursos((n) => n + 1);
  };

  const alEliminar = (mensaje) => {
    setPorEliminar(null);
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
        <div className="d-flex flex-wrap gap-2">
          {puede("catalogo.crear") && (
            <button type="button" className="btn btn-light" onClick={() => setModalAbierto(true)}>
              <i className="bi bi-plus-lg me-1"></i>Agregar manual
            </button>
          )}
          {puede("catalogo.editar") && (
            <button type="button" className="btn btn-primary" onClick={actualizarCatalogo} disabled={actualizando}>
              {actualizando ? (
                <><span className="spinner-border spinner-border-sm me-2"></span>Actualizando…</>
              ) : (
                <><i className="bi bi-arrow-repeat me-1"></i>Actualizar catálogo</>
              )}
            </button>
          )}
        </div>
      </div>

      {exito && (
        <div className="alert alert-success py-2 small d-flex gap-2 align-items-center">
          <i className="bi bi-check-circle"></i>{exito}
        </div>
      )}

      <LibrosOficiales
        recursos={recursos}
        cargando={cargandoRecursos}
        puedeImportar={puede("catalogo.crear")}
        onImportar={setPorImportar}
      />

      <div className="card">
        <div className="card-header d-flex flex-wrap gap-2 align-items-center">
          <input name="search" className="form-control" style={{ maxWidth: 240 }} placeholder="Buscar por título"
            aria-label="Buscar por título" value={filtros.search} onChange={cambiarFiltro} />
          <select name="tipo_reto" className="form-select" style={{ maxWidth: 200 }} aria-label="Tipo de reto"
            value={filtros.tipo_reto} onChange={cambiarFiltro}>
            <option value="">Todo tipo de reto</option>
            {TIPOS_RETO.map((t) => <option key={t.valor} value={t.valor}>{t.texto}</option>)}
          </select>
          <select name="grado" className="form-select" style={{ maxWidth: 185 }} aria-label="Grado"
            value={filtros.grado} onChange={cambiarFiltro}>
            <option value="">Todos los grados</option>
            {GRADOS.map((g) => <option key={g} value={g}>{g}.° grado</option>)}
          </select>
        <select name="dificultad" className="form-select" style={{ maxWidth: 185 }} aria-label="Dificultad"
            value={filtros.dificultad} onChange={cambiarFiltro}>
            <option value="">Toda dificultad</option>
            {DIFICULTADES.map((d) => <option key={d.valor} value={d.valor}>{d.texto}</option>)}
          </select>
            <div className="filtro-estado" role="group" aria-label="Estado">
            {ESTADOS.map((e) => (
              <button
                key={e.valor || "todas"}
                type="button"
                className={filtros.activo === e.valor ? "activo" : ""}
                onClick={() => setFiltros({ ...filtros, activo: e.valor })}
              >
                {e.texto}
              </button>
            ))}
          </div>
          {hayFiltros && (
            <button type="button" className="btn btn-sm btn-light" onClick={() => setFiltros(sinFiltros)}>
              <i className="bi bi-x-lg me-1"></i>Limpiar filtros
            </button>
          )}
          <span className="ms-auto note">
            {cargando ? "Cargando..." : `${lecturas.length} ${lecturas.length === 1 ? "lectura" : "lecturas"}`}
          </span>
        </div>

        {error && <div className="alert alert-danger m-3 py-2 small">{error}</div>}

        {!error && !cargando && lecturas.length === 0 && (
          <div className="text-center text-secondary py-5">
            <i className="bi bi-journal-richtext fs-2 d-block mb-2"></i>
            {hayFiltros ? "Ninguna lectura coincide con los filtros." : "Aún no hay lecturas en el catálogo."}
          </div>
        )}

        {!error && lecturas.length > 0 && (
          <div className="table-responsive">
            <table className="table table-hover">
              <thead>
                <tr>
                  <th>Título</th>
                  <th>Tipo de reto</th>
                  <th>Grado</th>
                  <th>Dificultad</th>
                  <th>Fuente</th>
                  <th>Estado</th>
                  <th>Registrada por</th>
                  <th className="text-end">Acciones</th>
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
                        {l.fuente === "oficial" ? (
                          l.url_recurso ? (
                            <a href={l.url_recurso} target="_blank" rel="noreferrer" className="badge-soft bs-p text-decoration-none"
                              title="Ver el recurso oficial">Oficial <i className="bi bi-box-arrow-up-right"></i></a>
                          ) : (
                            <span className="badge-soft bs-p">Oficial</span>
                          )
                        ) : (
                          <span className="badge-soft bs-info">Manual</span>
                        )}
                      </td>
                      <td>
                        <span className={`badge-soft ${l.activo ? "bs-ok" : "bs-off"}`}>{l.activo ? "Activa" : "Inactiva"}</span>
                      </td>
                      <td className="note">{l.creado_por ?? "—"}</td>
                      <td className="text-end text-nowrap">
                          {l.asignada && <span className="badge-soft bs-info me-2" title="Asignada a alumnos">Asignada</span>}
                            {puede("catalogo.editar") && (
                            <button type="button" className="icon-btn me-1" title="Editar" onClick={() => setPorEditar(l)}>
                              <i className="bi bi-pencil"></i>
                            </button>
                          )}
                          {puede("catalogo.desactivar") && l.activo && (
                            <button type="button" className="icon-btn" title="Eliminar" onClick={() => setPorEliminar(l)}>
                              <i className="bi bi-trash3"></i>
                            </button>
                          )}
                        </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {porEliminar && (
        <ModalEliminar
          lectura={porEliminar}
          onCerrar={() => setPorEliminar(null)}
          onEliminada={alEliminar}
          onNoAutorizado={onNoAutorizado}
        />
      )}

        {porImportar && (
        <ModalImportar
          recurso={porImportar}
          onCerrar={() => setPorImportar(null)}
          onImportada={alImportar}
          onNoAutorizado={onNoAutorizado}
        />
      )}

        {(modalAbierto || porEditar) && (
        <ModalLectura
          lectura={porEditar}
          onCerrar={() => { setModalAbierto(false); setPorEditar(null); }}
          onGuardado={alGuardar}
          onNoAutorizado={onNoAutorizado}
        />
      )}
    </section>
  );
}
