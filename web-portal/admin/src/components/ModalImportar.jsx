// Componente: ModalImportar
// Padre: Catalogo
// Recibe (props): recurso (el libro oficial elegido)
// Avisa al padre con: onImportada(mensaje), onCerrar(), onNoAutorizado()
// Hijos: Modal
import { useState } from "react";
import { importarLectura, textoDeRecurso } from "../api";
import { erroresDeServidor } from "../utils/errores";
import { DIFICULTADES, FUENTE_OFICIAL, GRADOS, MAX_PAGINAS, TIPOS_RETO } from "../utils/catalogo";
import Modal from "./Modal";

// T-444: importar una lectura desde las páginas de un libro oficial
export default function ModalImportar({ recurso, onCerrar, onImportada, onNoAutorizado }) {
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
    <Modal tamano="lg"
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
