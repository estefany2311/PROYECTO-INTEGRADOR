import { useEffect, useState } from "react";
import { listarAvatares, asignarAvatar, generarPin } from "../api";

/**
 * T-187: modal "Avatar y PIN" de un alumno.
 * - Carga el catálogo de 12 avatares (T-185) y guarda el elegido (PUT).
 * - Genera o restablece el PIN (POST) y lo muestra UNA sola vez.
 * El PIN no se guarda en el estado del padre: vive solo en este modal.
 */
export default function AccesoAlumno({ aulaId, alumno, onCerrar, onActualizado }) {
  const [avatares, setAvatares] = useState([]);
  const [elegido, setElegido] = useState(alumno.avatarId ?? null);
  const [tienePin, setTienePin] = useState(alumno.tienePin ?? false);
  const [pin, setPin] = useState(null); // PIN recién generado (en claro, solo aquí)
  const [error, setError] = useState(null);
  const [guardando, setGuardando] = useState(false);

  const nombreCompleto = `${alumno.nombres} ${alumno.apellidos}`;

  // los avatares se cargan al abrir el modal (no al entrar a la pantalla)
  useEffect(() => {
    listarAvatares()
      .then(setAvatares)
      .catch((e) => setError(e.message));
  }, []);

  const guardarAvatar = async () => {
    if (!elegido) return;
    setError(null);
    setGuardando(true);
    try {
      await asignarAvatar(aulaId, alumno.id, elegido);
      onActualizado("Avatar actualizado correctamente");
    } catch (e) {
      setError(e.message);
    } finally {
      setGuardando(false);
    }
  };

  const pedirPin = async () => {
    setError(null);
    try {
      const respuesta = await generarPin(aulaId, alumno.id);
      setPin(respuesta.pin);
      setTienePin(true); // a partir de ahora el botón dice "Restablecer"
      onActualizado("PIN generado correctamente");
    } catch (e) {
      setError(e.message);
    }
  };

  return (
    <div className="modal d-block" tabIndex="-1" role="dialog" aria-label={`Avatar y PIN de ${nombreCompleto}`}>
      <div className="modal-dialog modal-dialog-centered modal-lg">
        <div className="modal-content border-0 rounded-4">
          <div className="modal-header border-0">
            <div>
              <h2 className="modal-title h5 fw-bold mb-0">Avatar y PIN</h2>
              <div className="text-secondary small">{nombreCompleto}</div>
            </div>
            <button type="button" className="btn-close" aria-label="Cerrar" onClick={onCerrar} />
          </div>

          <div className="modal-body">
            {error && <div className="alert alert-danger">{error}</div>}

            {/* Selector de avatar: se marca con un borde morado el elegido */}
            <h3 className="h6 fw-bold mb-2">Elige el avatar del alumno</h3>
            <div className="d-flex flex-wrap gap-2 mb-3">
              {avatares.map((a) => (
                <button
                  key={a.id}
                  type="button"
                  className={`avatar-opcion ${String(elegido) === String(a.id) ? "elegido" : ""}`}
                  aria-label={`Avatar ${a.nombre}`}
                  aria-pressed={String(elegido) === String(a.id)}
                  onClick={() => setElegido(a.id)}
                >
                  <img className="avatar-mini" src={a.url} alt={a.nombre} />
                </button>
              ))}
              {avatares.length === 0 && !error && (
                <span className="text-secondary small">Cargando avatares...</span>
              )}
            </div>
            <button
              type="button"
              className="btn btn-primary rounded-pill"
              onClick={guardarAvatar}
              disabled={!elegido || guardando}
            >
              <i className="bi bi-check2-circle me-2" />
              {guardando ? "Guardando..." : "Guardar avatar"}
            </button>

            <hr className="my-4" />

            {/* PIN: generar la primera vez o restablecer si ya tiene uno */}
            <div className="d-flex justify-content-between align-items-center flex-wrap gap-3">
              <div>
                <h3 className="h6 fw-bold mb-1">PIN de acceso</h3>
                <div className="text-secondary small">
                  {tienePin
                    ? "El alumno ya tiene PIN. Si lo restableces, el anterior deja de funcionar."
                    : "El alumno aún no tiene PIN: genéralo para que pueda entrar en la tablet."}
                </div>
              </div>
              <button type="button" className="btn btn-outline-primary rounded-pill" onClick={pedirPin}>
                <i className="bi bi-key me-2" />
                {tienePin ? "Restablecer PIN" : "Generar PIN"}
              </button>
            </div>

            {pin && (
              <div className="alert alert-warning mt-3 mb-0" role="status">
                PIN generado: <b className="fs-4">{pin}</b>
                <div className="small">Anótalo y entrégaselo al alumno: no se volverá a mostrar.</div>
              </div>
            )}
          </div>

          <div className="modal-footer border-0">
            <button type="button" className="btn btn-outline-secondary rounded-pill" onClick={onCerrar}>
              Cerrar
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
