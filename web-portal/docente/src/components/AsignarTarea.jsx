import { useState } from "react";
import { asignarLectura } from "../api";

/**
 * T-193 (US-36): modal para asignar una lectura del catálogo a un alumno concreto.
 * Llega desde el catálogo ("Usar como tarea") y muestra solo alumnos activos:
 * los inactivos no aparecen en la tablet, así que no tiene sentido asignarles.
 */
export default function AsignarTarea({ aulaId, lectura, alumnos, cargando = false, onCerrar, onAsignada }) {
  const activos = alumnos.filter((a) => a.activo);
  const [estudianteId, setEstudianteId] = useState("");
  const [error, setError] = useState(null);
  const [enviando, setEnviando] = useState(false);

  const elegido = activos.find((a) => String(a.id) === String(estudianteId));

  const asignar = async (ev) => {
    ev.preventDefault();
    if (!elegido) {
      setError("Elige al alumno que recibirá la tarea");
      return;
    }

    setError(null);
    setEnviando(true);
    try {
      await asignarLectura(aulaId, {
        lecturaId: lectura.id,
        estudianteId: elegido.id,
      });
      onAsignada(
        `Tarea «${lectura.titulo}» asignada a ${elegido.nombres} ${elegido.apellidos}`
      );
    } catch (e) {
      setError(e.message); // ej. 409: el alumno ya tiene esa lectura
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div className="modal d-block" tabIndex="-1" role="dialog" aria-label="Asignar lectura">
      <div className="modal-dialog modal-dialog-centered">
        <form className="modal-content border-0 rounded-4" onSubmit={asignar} noValidate>
          <div className="modal-header border-0">
            <h2 className="modal-title h5 fw-bold">Asignar lectura como tarea</h2>
            <button type="button" className="btn-close" aria-label="Cerrar" onClick={onCerrar} />
          </div>

          <div className="modal-body">
            {error && <div className="alert alert-danger">{error}</div>}

            {/* resumen de la lectura que se va a asignar */}
            <div className="panel mb-3">
              <div className="text-secondary small text-uppercase fw-semibold mb-1">Lectura</div>
              <div className="fw-bold">{lectura.titulo}</div>
              <div className="d-flex flex-wrap gap-2 mt-2">
                <span className="insignia">
                  {lectura.gradoNombre || `Grado ${lectura.gradoId}`}
                </span>
                {lectura.tipoReto && <span className="tag tag-neutro">{lectura.tipoReto}</span>}
              </div>
            </div>

            <label htmlFor="alumno-tarea" className="form-label fw-semibold">Alumno</label>
            <select
              id="alumno-tarea"
              className="form-select"
              value={estudianteId}
              disabled={cargando}
              onChange={(ev) => setEstudianteId(ev.target.value)}
            >
              <option value="">Selecciona un alumno</option>
              {activos.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.nombres} {a.apellidos} · {a.gradoNombre}
                </option>
              ))}
            </select>
            {cargando ? (
              <div className="form-text">Cargando alumnos…</div>
            ) : activos.length === 0 ? (
              <div className="form-text text-danger">Este aula no tiene alumnos activos.</div>
            ) : (
              <div className="form-text">Solo se muestran alumnos activos.</div>
            )}
          </div>

          <div className="modal-footer border-0">
            <button
              type="button"
              className="btn btn-outline-secondary rounded-pill"
              onClick={onCerrar}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={enviando || cargando || activos.length === 0}
            >
              {enviando ? "Asignando…" : "Asignar tarea"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
