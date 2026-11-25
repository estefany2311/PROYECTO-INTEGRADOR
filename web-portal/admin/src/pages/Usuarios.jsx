import { useEffect, useState } from "react";
import { cambiarEstadoUsuario, listarUsuarios } from "../api";
import ModalConfirmar from "../components/ModalConfirmar";

const iniciales = (u) =>
  (u.nombre_completo || u.username)
    .split(/[\s._-]+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0].toUpperCase())
    .join("");

export default function Usuarios({ onNoAutorizado, puede = () => true, usuarioActualId }) {
  const [usuarios, setUsuarios] = useState([]);
  const [busqueda, setBusqueda] = useState("");
  const [estado, setEstado] = useState("");
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [exito, setExito] = useState("");
  const [recarga, setRecarga] = useState(0);
  const [confirmar, setConfirmar] = useState(null); // usuario a activar o desactivar
  const [enviando, setEnviando] = useState(false);
  const [errorModal, setErrorModal] = useState("");

  useEffect(() => {
    let activo = true;
    // Espera a que el usuario deje de escribir antes de consultar
    const espera = setTimeout(() => {
      setCargando(true);
      listarUsuarios(busqueda.trim())
        .then((datos) => {
          if (!activo) return;
          setUsuarios(Array.isArray(datos) ? datos : datos.results ?? []);
          setError("");
        })
        .catch((err) => {
          if (!activo) return;
          if (err.estado === 401) onNoAutorizado();
          else setError(err.datos?.detail ?? err.mensaje ?? "No se pudo cargar la lista de usuarios.");
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
  }, [busqueda, recarga]);

  useEffect(() => {
    if (!exito) return;
    const t = setTimeout(() => setExito(""), 4000);
    return () => clearTimeout(t);
  }, [exito]);

  const abrirConfirmar = (usuario) => {
    setErrorModal("");
    setConfirmar(usuario);
  };

  const cambiarEstado = async () => {
    setEnviando(true);
    setErrorModal("");
    try {
      await cambiarEstadoUsuario(confirmar.id, !confirmar.is_active);
      setExito(confirmar.is_active ? `Se desactivó la cuenta de ${confirmar.username}.` : `Se reactivó la cuenta de ${confirmar.username}.`);
      setConfirmar(null);
      setRecarga((n) => n + 1);
    } catch (err) {
      if (err.estado === 401) onNoAutorizado();
      else if (err.estado === 400) setErrorModal(Object.values(err.datos ?? {}).flat().join(" "));
      else setErrorModal(err.datos?.detail ?? err.mensaje ?? "No se pudo cambiar el estado de la cuenta.");
    } finally {
      setEnviando(false);
    }
  };

  const visibles = usuarios.filter((u) =>
    estado === "" ? true : estado === "activos" ? u.is_active : !u.is_active
  );

  return (
    <section>
      <div className="page-title mb-4">
        <h1>Usuarios</h1>
        <div className="breadcrumb text-secondary">Seguridad / Usuarios</div>
      </div>

      {exito && (
        <div className="alert alert-success py-2 small d-flex gap-2 align-items-center">
          <i className="bi bi-check-circle"></i>{exito}
        </div>
      )}

      <div className="card">
        <div className="card-header d-flex flex-wrap gap-2 align-items-center">
          <input className="form-control" style={{ maxWidth: 260 }} placeholder="Buscar usuario, nombre o correo"
            aria-label="Buscar usuario" value={busqueda} onChange={(e) => setBusqueda(e.target.value)} />
          <select className="form-select" style={{ maxWidth: 170 }} aria-label="Estado"
            value={estado} onChange={(e) => setEstado(e.target.value)}>
            <option value="">Todos los estados</option>
            <option value="activos">Activos</option>
            <option value="inactivos">Inactivos</option>
          </select>
          <span className="ms-auto note">
            {cargando ? "Cargando..." : `${visibles.length} ${visibles.length === 1 ? "usuario" : "usuarios"}`}
          </span>
        </div>

        {error && <div className="alert alert-danger m-3 py-2 small">{error}</div>}

        {!error && !cargando && visibles.length === 0 && (
          <div className="text-center text-secondary py-5">
            <i className="bi bi-people fs-2 d-block mb-2"></i>
            {busqueda || estado ? "Ningún usuario coincide con la búsqueda." : "Aún no hay usuarios."}
          </div>
        )}

        {!error && visibles.length > 0 && (
          <div className="table-responsive">
            <table className="table table-hover">
              <thead>
                <tr>
                  <th>Usuario</th>
                  <th>Nombre</th>
                  <th>Correo</th>
                  <th>Rol</th>
                  <th>Estado</th>
                  <th className="text-end">Acción</th>
                </tr>
              </thead>
              <tbody>
                {visibles.map((u) => {
                  const esYo = u.id === usuarioActualId;
                  return (
                    <tr key={u.id} className={u.is_active ? "" : "inactivo"}>
                      <td><span className="mini-avatar me-2">{iniciales(u)}</span>{u.username}</td>
                      <td>{u.nombre_completo || "—"}</td>
                      <td>{u.email || "—"}</td>
                      <td>
                        {u.rol ? (
                          <span className={`badge-soft ${u.rol.nombre === "ADMIN" ? "bs-p" : "bs-info"}`}>{u.rol.nombre}</span>
                        ) : (
                          <span className="note">Sin rol</span>
                        )}
                      </td>
                      <td>
                        <span className={`badge-soft ${u.is_active ? "bs-ok" : "bs-off"}`}>{u.is_active ? "Activo" : "Inactivo"}</span>
                      </td>
                      <td className="text-end text-nowrap">
                        {esYo ? (
                          <span className="note">Tu cuenta</span>
                        ) : puede("usuarios.desactivar") ? (
                          <button type="button"
                            className={`btn btn-sm ${u.is_active ? "btn-outline-danger" : "btn-outline-success"}`}
                            onClick={() => abrirConfirmar(u)}>
                            <i className={`bi ${u.is_active ? "bi-slash-circle" : "bi-arrow-counterclockwise"} me-1`}></i>
                            {u.is_active ? "Desactivar" : "Reactivar"}
                          </button>
                        ) : (
                          <span className="note">Solo lectura</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        <div className="card-footer bg-white note">
          <i className="bi bi-info-circle me-1"></i>Un usuario inactivo no puede iniciar sesión. Su información se conserva.
        </div>
      </div>

      {confirmar && (
        <ModalConfirmar
          usuario={confirmar}
          enviando={enviando}
          error={errorModal}
          onConfirmar={cambiarEstado}
          onCerrar={() => !enviando && setConfirmar(null)}
        />
      )}
    </section>
  );
}