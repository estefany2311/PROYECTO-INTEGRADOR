// Componente: ModalLectura
// Padre: Catalogo
// Recibe (props): lectura (si viene, edita esa lectura; si no, crea una nueva)
// Avisa al padre con: onGuardado(mensaje), onCerrar(), onNoAutorizado()
// Hijos: Modal
import { useState } from "react";
import { actualizarLectura, crearLectura } from "../api";
import { erroresDeServidor } from "../utils/errores";
import { DIFICULTADES, GRADOS, LECTURA_VACIA, TIPOS_RETO } from "../utils/catalogo";
import Modal from "./Modal";

// Si recibe "lectura", el formulario edita esa lectura (T-244); si no, crea una nueva
export default function ModalLectura({ lectura = null, onCerrar, onGuardado, onNoAutorizado }) {
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
      : LECTURA_VACIA
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
    <Modal tamano="lg"
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
