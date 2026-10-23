import { useEffect, useState } from "react";
import {
  actualizarPermisosRol,
  cambiarRolUsuario,
  listarPermisos,
  listarRoles,
  listarUsuarios,
  obtenerMiPerfil,
} from "../api";

const ACCIONES = ["ver", "crear", "editar", "desactivar"];

const MODULOS = {
  colegios: "Colegios",
  docentes: "Docentes",
  usuarios: "Usuarios",
  roles: "Roles y permisos",
  catalogo: "Catálogo de lecturas",
  alumnos: "Alumnos",
  asignaciones: "Asignaciones",
  reportes: "Reportes",
};

const ICONOS_ROL = { ADMIN: "bi-shield-fill-check", DOCENTE: "bi-person-badge" };

const lista = (datos) => (Array.isArray(datos) ? datos : datos.results ?? []);

// Toma el primer mensaje de un error de DRF ({campo: [msg]} o {detail: msg})
const mensajeDeError = (err, porDefecto) => {
  if (err.datos?.detail) return err.datos.detail;
  const primero = Object.values(err.datos ?? {})[0];
  if (primero) return Array.isArray(primero) ? primero.join(" ") : String(primero);
  return err.mensaje ?? porDefecto;
};

const mismosPermisos = (a, b) => a.size === b.size && [...a].every((c) => b.has(c));

function AsignarRol({ roles, miId, onRolCambiado, onNoAutorizado }) {
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

export default function RolesPermisos({ onNoAutorizado }) {
  const [roles, setRoles] = useState([]);
  const [permisos, setPermisos] = useState([]);
  const [miId, setMiId] = useState(null);
  const [seleccionado, setSeleccionado] = useState(null);
  const [marcados, setMarcados] = useState(new Set());
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [aviso, setAviso] = useState(null);
  const [guardando, setGuardando] = useState(false);

  useEffect(() => {
    let activo = true;
    Promise.all([listarRoles(), listarPermisos(), obtenerMiPerfil()])
      .then(([datosRoles, datosPermisos, perfil]) => {
        if (!activo) return;
        const listaRoles = lista(datosRoles);
        setRoles(listaRoles);
        setPermisos(lista(datosPermisos));
        setMiId(perfil.id);
        const inicial = listaRoles.find((r) => r.nombre === "DOCENTE") ?? listaRoles[0];
        if (inicial) {
          setSeleccionado(inicial.id);
          setMarcados(new Set(inicial.permisos));
        }
      })
      .catch((err) => {
        if (!activo) return;
        if (err.estado === 401) onNoAutorizado();
        else setError(mensajeDeError(err, "No se pudieron cargar los roles."));
      })
      .finally(() => {
        if (activo) setCargando(false);
      });
    return () => {
      activo = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (aviso?.tipo !== "success") return;
    const t = setTimeout(() => setAviso(null), 4000);
    return () => clearTimeout(t);
  }, [aviso]);

  const rol = roles.find((r) => r.id === seleccionado);
  const hayCambios = rol ? !mismosPermisos(marcados, new Set(rol.permisos)) : false;

  // Módulos en el orden del menú; los desconocidos van al final
  const codigos = new Set(permisos.map((p) => p.codigo));
  const modulos = [...new Set(permisos.map((p) => p.modulo))].sort((a, b) => {
    const orden = Object.keys(MODULOS);
    return (orden.indexOf(a) + 1 || 99) - (orden.indexOf(b) + 1 || 99);
  });

  const elegirRol = (r) => {
    setSeleccionado(r.id);
    setMarcados(new Set(r.permisos));
    setAviso(null);
  };

  const alternar = (codigo) => {
    const nuevos = new Set(marcados);
    if (nuevos.has(codigo)) nuevos.delete(codigo);
    else nuevos.add(codigo);
    setMarcados(nuevos);
  };

  const guardar = async () => {
    setGuardando(true);
    setAviso(null);
    try {
      const actualizado = await actualizarPermisosRol(rol.id, [...marcados]);
      setRoles((actuales) => actuales.map((r) => (r.id === actualizado.id ? actualizado : r)));
      setMarcados(new Set(actualizado.permisos));
      setAviso({ tipo: "success", texto: `Permisos del rol ${actualizado.nombre} guardados.` });
    } catch (err) {
      if (err.estado === 401) onNoAutorizado();
      else setAviso({ tipo: "danger", texto: mensajeDeError(err, "No se pudieron guardar los permisos.") });
    } finally {
      setGuardando(false);
    }
  };

  // Mantiene el número de usuarios de cada rol al cambiar el rol de alguien
  const alCambiarRol = (rolAnterior, rolNuevo) => {
    setRoles((actuales) =>
      actuales.map((r) => ({
        ...r,
        total_usuarios: r.total_usuarios + (r.id === rolNuevo ? 1 : 0) - (r.id === rolAnterior ? 1 : 0),
      }))
    );
  };

  return (
    <section>
      <div className="page-title d-flex flex-wrap justify-content-between align-items-end gap-2 mb-4">
        <div>
          <h1>Roles y permisos <span className="story">US-45</span></h1>
          <div className="breadcrumb text-secondary">Seguridad / Roles y permisos</div>
        </div>
        <button type="button" className="btn btn-primary" onClick={guardar} disabled={!hayCambios || guardando}>
          <i className="bi bi-check2 me-1"></i>{guardando ? "Guardando..." : "Guardar cambios"}
        </button>
      </div>

      {aviso && (
        <div className={`alert alert-${aviso.tipo} py-2 small d-flex gap-2 align-items-center`}>
          <i className={`bi ${aviso.tipo === "success" ? "bi-check-circle" : "bi-exclamation-circle"}`}></i>{aviso.texto}
        </div>
      )}
      {error && <div className="alert alert-danger py-2 small">{error}</div>}
      {cargando && <p className="note">Cargando roles y permisos...</p>}
      {!cargando && !error && roles.length === 0 && <p className="text-secondary">Aún no hay roles registrados.</p>}

      {roles.length > 0 && (
        <div className="row g-3">
          <div className="col-lg-4">
            <div className="card">
              <div className="card-header"><h2>Roles</h2></div>
              <div className="card-body d-grid gap-2">
                {roles.map((r) => (
                  <button key={r.id} type="button" className={`role-item${r.id === seleccionado ? " active" : ""}`}
                    onClick={() => elegirRol(r)}>
                    <div className="d-flex justify-content-between">
                      <span className="fw-semibold">
                        <i className={`bi ${ICONOS_ROL[r.nombre] ?? "bi-person"} me-2 text-primary`}></i>{r.nombre}
                      </span>
                      <span className="note">{r.total_usuarios} {r.total_usuarios === 1 ? "usuario" : "usuarios"}</span>
                    </div>
                    <div className="note mt-1">{r.permisos.length} permisos</div>
                  </button>
                ))}
              </div>
            </div>
          </div>

          <div className="col-lg-8">
            <div className="card">
              <div className="card-header d-flex flex-wrap justify-content-between gap-2">
                <h2>Permisos del rol: {rol?.nombre}</h2>
                <span className="note">{hayCambios ? "Hay cambios sin guardar" : "Se aplican en cada endpoint (403 si no tiene permiso)"}</span>
              </div>
              <div className="table-responsive">
                <table className="table perm-table">
                  <thead>
                    <tr>
                      <th>Módulo</th>
                      {ACCIONES.map((a) => <th key={a}>{a[0].toUpperCase() + a.slice(1)}</th>)}
                    </tr>
                  </thead>
                  <tbody>
                    {modulos.map((modulo) => (
                      <tr key={modulo}>
                        <td className="fw-medium">{MODULOS[modulo] ?? modulo}</td>
                        {ACCIONES.map((accion) => {
                          const codigo = `${modulo}.${accion}`;
                          return (
                            <td key={accion}>
                              {codigos.has(codigo) ? (
                                <input className="form-check-input" type="checkbox" aria-label={codigo}
                                  checked={marcados.has(codigo)} onChange={() => alternar(codigo)} />
                              ) : "—"}
                            </td>
                          );
                        })}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      )}

      {roles.length > 0 && (
        <AsignarRol roles={roles} miId={miId} onRolCambiado={alCambiarRol} onNoAutorizado={onNoAutorizado} />
      )}
    </section>
  );
}
