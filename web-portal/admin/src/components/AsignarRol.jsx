// Componente: AsignarRol
// Padre: RolesPermisos
// Recibe (props): roles (lista de roles), miId (para no cambiar mi propio rol)
// Avisa al padre con: onRolCambiado(), onNoAutorizado()
import { useEffect, useState } from "react";
import { cambiarRolUsuario, listarUsuarios } from "../api";
import { lista, mensajeDeError } from "../utils/roles";

export default function AsignarRol({ roles, miId, onRolCambiado, onNoAutorizado }) {
  const [usuarios, setUsuarios] = useState([]);
  const [busqueda, setBusqueda] = useState("");
  const [elegidos, setElegidos] = useState({}); // { usuarioId: rolId }
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [aviso, setAviso] = useState(null);
  const [aplicando, setAplicando] = useState(null);

  useEffect(() => {
    let activo = true;
    const espera = setTimeout(() => {
      setCargando(true);
      listarUsuarios(busqueda.trim())
        .then((datos) => {
          if (!activo) return;
          setUsuarios(lista(datos));
          setError("");
        })
        .catch((err) => {
          if (!activo) return;
          if (err.estado === 401) onNoAutorizado();
          else setError(mensajeDeError(err, "No se pudo cargar la lista de usuarios."));
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
  }, [busqueda]);

  const aplicar = async (usuario) => {
    const rolId = Number(elegidos[usuario.id]);
    setAplicando(usuario.id);
    setAviso(null);
    try {
      const actualizado = await cambiarRolUsuario(usuario.id, rolId);
      setUsuarios((actuales) => actuales.map((u) => (u.id === usuario.id ? actualizado : u)));
      setElegidos((actuales) => {
        const copia = { ...actuales };
        delete copia[usuario.id];
        return copia;
      });
      onRolCambiado(usuario.rol?.id, actualizado.rol?.id);
      setAviso({ tipo: "success", texto: `${usuario.username} ahora tiene el rol ${actualizado.rol.nombre}.` });
    } catch (err) {
      if (err.estado === 401) onNoAutorizado();
      else setAviso({ tipo: "danger", texto: mensajeDeError(err, "No se pudo cambiar el rol.") });
    } finally {
      setAplicando(null);
    }
  };

  return (
    <div className="card mt-3">
      <div className="card-header d-flex flex-wrap gap-2 align-items-center">
        <h2 className="me-auto">Asignar rol a un usuario</h2>
        <input className="form-control" style={{ maxWidth: 260 }} placeholder="Buscar usuario"
          value={busqueda} onChange={(e) => setBusqueda(e.target.value)} />
      </div>

      {aviso && <div className={`alert alert-${aviso.tipo} m-3 mb-0 py-2 small`}>{aviso.texto}</div>}
      {error && <div className="alert alert-danger m-3 py-2 small">{error}</div>}
      {!error && cargando && usuarios.length === 0 && <p className="note m-3">Cargando usuarios...</p>}
      {!error && !cargando && usuarios.length === 0 && (
        <p className="text-center text-secondary py-4 mb-0">
          {busqueda ? "Ningún usuario coincide con la búsqueda." : "Aún no hay usuarios registrados."}
        </p>
      )}

      {usuarios.length > 0 && (
        <div className="table-responsive">
          <table className="table table-hover">
            <thead>
              <tr>
                <th>Usuario</th>
                <th>Nombre</th>
                <th>Rol actual</th>
                <th className="text-end">Cambiar rol</th>
              </tr>
            </thead>
            <tbody>
              {usuarios.map((u) => {
                const esMiCuenta = u.id === miId;
                const elegido = elegidos[u.id] ?? u.rol?.id ?? "";
                const cambio = elegidos[u.id] !== undefined && Number(elegidos[u.id]) !== u.rol?.id;
                return (
                  <tr key={u.id}>
                    <td><span className="mini-avatar me-2">{u.username[0].toUpperCase()}</span>{u.username}</td>
                    <td>{u.nombre_completo}</td>
                    <td>
                      {u.rol
                        ? <span className={`badge-soft ${u.rol.nombre === "ADMIN" ? "bs-p" : "bs-info"}`}>{u.rol.nombre}</span>
                        : <span className="badge-soft bs-warn">Sin rol</span>}
                    </td>
                    <td className="text-end">
                      {esMiCuenta ? (
                        <select className="form-select form-select-sm d-inline-block w-auto" disabled>
                          <option>Tu cuenta</option>
                        </select>
                      ) : (
                        <div className="d-inline-flex gap-2">
                          <select className="form-select form-select-sm w-auto" value={elegido}
                            onChange={(e) => setElegidos({ ...elegidos, [u.id]: e.target.value })}>
                            {!u.rol && <option value="">Selecciona…</option>}
                            {roles.map((r) => <option key={r.id} value={r.id}>{r.nombre}</option>)}
                          </select>
                          <button type="button" className="btn btn-sm btn-outline-primary"
                            disabled={!cambio || aplicando === u.id} onClick={() => aplicar(u)}>
                            {aplicando === u.id ? "Aplicando..." : "Aplicar"}
                          </button>
                        </div>
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
  );
}
