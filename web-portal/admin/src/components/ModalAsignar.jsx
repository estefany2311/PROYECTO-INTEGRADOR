// Componente: ModalAsignar
// Padre: Colegios
// Recibe (props): colegio (al que se le asignan docentes)
// Avisa al padre con: onGuardado(mensaje), onCerrar(), onNoAutorizado()
// Hijos: Modal
import { useEffect, useState } from "react";
import { asignarDocentes, listarDocentes } from "../api";
import { erroresDeServidor } from "../utils/errores";
import { nombreCompleto } from "../utils/personas";
import Modal from "./Modal";

export default function ModalAsignar({ colegio, onCerrar, onGuardado, onNoAutorizado }) {
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
