import { useEffect, useState } from "react";
import { actualizarPermisosRol, listarPermisos, listarRoles, obtenerMiPerfil } from "../api";
import AsignarRol from "../components/AsignarRol";
import { lista, mensajeDeError } from "../utils/roles";

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

const mismosPermisos = (a, b) => a.size === b.size && [...a].every((c) => b.has(c));

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
      <h1>Roles y permisos</h1>
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
