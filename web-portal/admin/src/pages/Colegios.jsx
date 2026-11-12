import { useEffect, useState } from "react";
import { actualizarColegio, listarColegios } from "../api";
import ModalAsignar from "../components/ModalAsignar";
import ModalColegio from "../components/ModalColegio";
import { REGIONES } from "../utils/colegios";
import { inicialesDocente, nombreCompleto } from "../utils/personas";

  export default function Colegios({ busquedaInicial = "", onNoAutorizado, puede = () => true }) {
  const [colegios, setColegios] = useState([]);
  const [busqueda, setBusqueda] = useState(busquedaInicial);
  const [region, setRegion] = useState("");
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [exito, setExito] = useState("");
  const [recarga, setRecarga] = useState(0);
  const [modalColegio, setModalColegio] = useState(null); // { colegio } — colegio null al registrar
  const [modalAsignar, setModalAsignar] = useState(null);

  useEffect(() => {
    let activo = true;
    // Espera a que el usuario deje de escribir antes de consultar
    const espera = setTimeout(() => {
      setCargando(true);
      listarColegios({ search: busqueda.trim(), region })
        .then((datos) => {
          if (!activo) return;
          setColegios(Array.isArray(datos) ? datos : datos.results ?? []);
          setError("");
        })
        .catch((err) => {
          if (!activo) return;
          if (err.estado === 401) onNoAutorizado();
          else setError(err.datos?.detail ?? err.mensaje ?? "No se pudo cargar la lista de colegios.");
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
  }, [busqueda, region, recarga]);

  useEffect(() => {
    if (!exito) return;
    const t = setTimeout(() => setExito(""), 4000);
    return () => clearTimeout(t);
  }, [exito]);

  const alGuardar = (_colegio, mensaje) => {
    setModalColegio(null);
    setModalAsignar(null);
    setExito(mensaje);
    setRecarga((n) => n + 1);
  };

  const alternarActivo = async (colegio) => {
    try {
      await actualizarColegio(colegio.id, { activo: !colegio.activo });
      setExito(colegio.activo ? `Se desactivó ${colegio.nombre}.` : `Se activó ${colegio.nombre}.`);
      setRecarga((n) => n + 1);
    } catch (err) {
      if (err.estado === 401) onNoAutorizado();
      else setError(err.datos?.detail ?? err.mensaje ?? "No se pudo cambiar el estado del colegio.");
    }
  };

  return (
    <section>
      <div className="page-title d-flex flex-wrap justify-content-between align-items-end gap-2 mb-4">
        <div>
          <h1>Colegios</h1>
          <div className="breadcrumb text-secondary">Instituciones / Colegios</div>
        </div>
                {puede("colegios.crear") && (
          <button type="button" className="btn btn-primary" onClick={() => setModalColegio({ colegio: null })}>
            <i className="bi bi-plus-lg me-1"></i>Registrar colegio
          </button>
        )}
      </div>

      {exito && (
        <div className="alert alert-success py-2 small d-flex gap-2 align-items-center">
          <i className="bi bi-check-circle"></i>{exito}
        </div>
      )}

      <div className="card">
        <div className="card-header d-flex flex-wrap gap-2 align-items-center">
          <input className="form-control" style={{ maxWidth: 260 }} placeholder="Buscar por nombre o código modular"
            value={busqueda} onChange={(e) => setBusqueda(e.target.value)} />
          <select className="form-select" style={{ maxWidth: 180 }} value={region} onChange={(e) => setRegion(e.target.value)}>
            <option value="">Todas las regiones</option>
            {REGIONES.map((r) => <option key={r} value={r}>{r}</option>)}
          </select>
          <span className="ms-auto note">
            {cargando ? "Cargando..." : `${colegios.length} ${colegios.length === 1 ? "colegio" : "colegios"}`}
          </span>
        </div>

        {error && <div className="alert alert-danger m-3 py-2 small">{error}</div>}

        {!error && !cargando && colegios.length === 0 && (
          <div className="text-center text-secondary py-5">
            <i className="bi bi-building fs-2 d-block mb-2"></i>
            {busqueda || region ? "Ningún colegio coincide con la búsqueda." : "Aún no hay colegios registrados."}
          </div>
        )}

        {colegios.length > 0 && (
          <div className="table-responsive">
            <table className="table table-hover">
              <thead>
                <tr>
                  <th>Código modular</th>
                  <th>Colegio</th>
                  <th>Región / Distrito</th>
                  <th>Docentes asignados</th>
                  <th>Estado</th>
                  <th className="text-end">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {colegios.map((c) => (
                  <tr key={c.id} className={c.activo ? "" : "inactivo"}>
                    <td>{c.codigo_modular}</td>
                    <td className="fw-medium">{c.nombre}</td>
                    <td>{c.region}{c.distrito ? ` / ${c.distrito}` : ""}</td>
                    <td>
                      {c.total_docentes > 0 ? (
                        <span className="d-inline-flex align-items-center">
                          <span className="avatar-group">
                            {c.docentes.slice(0, 3).map((d) => (
                              <span key={d.id} className="mini-avatar" title={nombreCompleto(d)}>{inicialesDocente(d)}</span>
                            ))}
                          </span>
                          <span className="note ms-2">
                            {c.total_docentes} {c.total_docentes === 1 ? "docente" : "docentes"}
                          </span>
                        </span>
                      ) : (
                        <span className="badge-soft bs-warn">Sin docentes</span>
                      )}
                    </td>
                    <td>
                      <span className={`badge-soft ${c.activo ? "bs-ok" : "bs-off"}`}>{c.activo ? "Activo" : "Inactivo"}</span>
                    </td>
                    <td className="text-end text-nowrap">
                                            {puede("colegios.editar") && (
                        <button type="button" className="icon-btn" title="Asignar docentes" onClick={() => setModalAsignar(c)}>
                          <i className="bi bi-person-plus"></i>
                        </button>
                      )}
                      {puede("colegios.editar") && (
                        <button type="button" className="icon-btn" title="Editar" onClick={() => setModalColegio({ colegio: c })}>
                          <i className="bi bi-pencil"></i>
                        </button>
                      )}
                      {puede("colegios.desactivar") && (
                        <button type="button" className="icon-btn" title={c.activo ? "Desactivar" : "Activar"}
                          onClick={() => alternarActivo(c)}>
                          <i className={`bi ${c.activo ? "bi-slash-circle" : "bi-check-circle"}`}></i>
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {modalColegio && (
        <ModalColegio
          colegio={modalColegio.colegio}
          onCerrar={() => setModalColegio(null)}
          onGuardado={alGuardar}
          onNoAutorizado={onNoAutorizado}
        />
      )}
      {modalAsignar && (
        <ModalAsignar
          colegio={modalAsignar}
          onCerrar={() => setModalAsignar(null)}
          onGuardado={alGuardar}
          onNoAutorizado={onNoAutorizado}
        />
      )}
    </section>
  );
}
