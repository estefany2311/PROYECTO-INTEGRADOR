// Componente: ModalColegio
// Padre: Colegios
// Recibe (props): colegio (si viene, lo edita; si no, crea uno nuevo)
// Avisa al padre con: onGuardado(mensaje), onCerrar(), onNoAutorizado()
// Hijos: Modal
import { useState } from "react";
import { actualizarColegio, crearColegio } from "../api";
import { erroresDeServidor } from "../utils/errores";
import { REGIONES } from "../utils/colegios";
import Modal from "./Modal";

const COLEGIO_VACIO = { codigo_modular: "", nombre: "", region: "", distrito: "" };

export default function ModalColegio({ colegio, onCerrar, onGuardado, onNoAutorizado }) {
  const editando = Boolean(colegio);
  const [datos, setDatos] = useState(
    editando
      ? { codigo_modular: colegio.codigo_modular, nombre: colegio.nombre, region: colegio.region, distrito: colegio.distrito }
      : COLEGIO_VACIO
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
