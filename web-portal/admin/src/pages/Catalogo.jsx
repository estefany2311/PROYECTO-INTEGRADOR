import { useEffect, useState } from "react";
import { listarLecturas, listarRecursosOficiales, sincronizarRecursos } from "../api";
import LibrosOficiales from "../components/LibrosOficiales";
import ModalEliminar from "../components/ModalEliminar";
import ModalImportar from "../components/ModalImportar";
import ModalLectura from "../components/ModalLectura";
import { DIFICULTADES, GRADOS, TIPOS_RETO, dificultadDe, tipoDe } from "../utils/catalogo";

const sinFiltros = { search: "", tipo_reto: "", grado: "", dificultad: "", activo: "" };

// T-245: filtro por estado ("" = todas, "true" = activas, "false" = desactivadas)
const ESTADOS = [
  { valor: "", texto: "Todas" },
  { valor: "true", texto: "Activas" },
  { valor: "false", texto: "Desactivadas" },
];

export default function Catalogo({ onNoAutorizado, puede = () => true }) {
  const [lecturas, setLecturas] = useState([]);
  const [filtros, setFiltros] = useState(sinFiltros);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [exito, setExito] = useState("");
  const [recarga, setRecarga] = useState(0);
  const [modalAbierto, setModalAbierto] = useState(false);
  const [porEliminar, setPorEliminar] = useState(null);
  const [porEditar, setPorEditar] = useState(null);
  const [recursos, setRecursos] = useState([]);
  const [cargandoRecursos, setCargandoRecursos] = useState(true);
  const [recargaRecursos, setRecargaRecursos] = useState(0);
  const [porImportar, setPorImportar] = useState(null);
  const [actualizando, setActualizando] = useState(false);

  const hayFiltros = Object.values(filtros).some((v) => v);

  useEffect(() => {
    let activo = true;
    // Espera a que el usuario deje de escribir antes de consultar
    const espera = setTimeout(() => {
      setCargando(true);
      listarLecturas({ ...filtros, search: filtros.search.trim() })
        .then((datos) => {
          if (!activo) return;
          setLecturas(Array.isArray(datos) ? datos : datos.results ?? []);
          setError("");
        })
        .catch((err) => {
          if (!activo) return;
          if (err.estado === 401) onNoAutorizado();
          else if (err.estado === 400) setError(Object.values(err.datos ?? {}).flat().join(" ") || "Revisa los filtros.");
          else setError(err.datos?.detail ?? err.mensaje ?? "No se pudo cargar el catálogo.");
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
  }, [filtros, recarga]);

  // T-444: libros oficiales (se vuelven a pedir al actualizar el catálogo o al importar)
  useEffect(() => {
    let activo = true;
    listarRecursosOficiales()
      .then((datos) => activo && setRecursos(Array.isArray(datos) ? datos : datos.results ?? []))
      .catch((err) => {
        if (activo && err.estado === 401) onNoAutorizado();
      })
      .finally(() => activo && setCargandoRecursos(false));
    return () => {
      activo = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [recargaRecursos]);

  useEffect(() => {
    if (!exito) return;
    const t = setTimeout(() => setExito(""), 4000);
    return () => clearTimeout(t);
  }, [exito]);

  const cambiarFiltro = (e) => setFiltros({ ...filtros, [e.target.name]: e.target.value });

  const alGuardar = (_lectura, mensaje) => {
    setModalAbierto(false);
    setPorEditar(null);
    setExito(mensaje);
    setRecarga((n) => n + 1);
  };

  
  // T-444: "Actualizar catálogo" vuelve a leer la Biblioteca Escolar Digital
  const actualizarCatalogo = async () => {
    setActualizando(true);
    setError("");
    try {
      const r = await sincronizarRecursos();
      setExito(`Catálogo actualizado. ${r.resultado}`);
      setCargandoRecursos(true);
      setRecargaRecursos((n) => n + 1);
    } catch (err) {
      if (err.estado === 401) onNoAutorizado();
      else setError(err.datos?.detail ?? err.mensaje ?? "No se pudo actualizar el catálogo.");
    } finally {
      setActualizando(false);
    }
  };

  const alImportar = (mensaje) => {
    setPorImportar(null);
    setExito(mensaje);
    setRecarga((n) => n + 1);
    setRecargaRecursos((n) => n + 1);
  };

  const alEliminar = (mensaje) => {
    setPorEliminar(null);
    setExito(mensaje);
    setRecarga((n) => n + 1);
  };

  return (
    <section>
      <div className="page-title d-flex flex-wrap justify-content-between align-items-end gap-2 mb-4">
        <div>
          <h1>Catálogo de lecturas</h1>
          <div className="breadcrumb text-secondary">Contenido / Catálogo</div>
        </div>
        <div className="d-flex flex-wrap gap-2">
          {puede("catalogo.crear") && (
            <button type="button" className="btn btn-light" onClick={() => setModalAbierto(true)}>
              <i className="bi bi-plus-lg me-1"></i>Agregar manual
            </button>
          )}
          {puede("catalogo.editar") && (
            <button type="button" className="btn btn-primary" onClick={actualizarCatalogo} disabled={actualizando}>
              {actualizando ? (
                <><span className="spinner-border spinner-border-sm me-2"></span>Actualizando…</>
              ) : (
                <><i className="bi bi-arrow-repeat me-1"></i>Actualizar catálogo</>
              )}
            </button>
          )}
        </div>
      </div>

      {exito && (
        <div className="alert alert-success py-2 small d-flex gap-2 align-items-center">
          <i className="bi bi-check-circle"></i>{exito}
        </div>
      )}

      <LibrosOficiales
        recursos={recursos}
        cargando={cargandoRecursos}
        puedeImportar={puede("catalogo.crear")}
        onImportar={setPorImportar}
      />

      <div className="card">
        <div className="card-header d-flex flex-wrap gap-2 align-items-center">
          <input name="search" className="form-control" style={{ maxWidth: 240 }} placeholder="Buscar por título"
            aria-label="Buscar por título" value={filtros.search} onChange={cambiarFiltro} />
          <select name="tipo_reto" className="form-select" style={{ maxWidth: 200 }} aria-label="Tipo de reto"
            value={filtros.tipo_reto} onChange={cambiarFiltro}>
            <option value="">Todo tipo de reto</option>
            {TIPOS_RETO.map((t) => <option key={t.valor} value={t.valor}>{t.texto}</option>)}
          </select>
          <select name="grado" className="form-select" style={{ maxWidth: 185 }} aria-label="Grado"
            value={filtros.grado} onChange={cambiarFiltro}>
            <option value="">Todos los grados</option>
            {GRADOS.map((g) => <option key={g} value={g}>{g}.° grado</option>)}
          </select>
        <select name="dificultad" className="form-select" style={{ maxWidth: 185 }} aria-label="Dificultad"
            value={filtros.dificultad} onChange={cambiarFiltro}>
            <option value="">Toda dificultad</option>
            {DIFICULTADES.map((d) => <option key={d.valor} value={d.valor}>{d.texto}</option>)}
          </select>
            <div className="filtro-estado" role="group" aria-label="Estado">
            {ESTADOS.map((e) => (
              <button
                key={e.valor || "todas"}
                type="button"
                className={filtros.activo === e.valor ? "activo" : ""}
                onClick={() => setFiltros({ ...filtros, activo: e.valor })}
              >
                {e.texto}
              </button>
            ))}
          </div>
          {hayFiltros && (
            <button type="button" className="btn btn-sm btn-light" onClick={() => setFiltros(sinFiltros)}>
              <i className="bi bi-x-lg me-1"></i>Limpiar filtros
            </button>
          )}
          <span className="ms-auto note">
            {cargando ? "Cargando..." : `${lecturas.length} ${lecturas.length === 1 ? "lectura" : "lecturas"}`}
          </span>
        </div>

        {error && <div className="alert alert-danger m-3 py-2 small">{error}</div>}

        {!error && !cargando && lecturas.length === 0 && (
          <div className="text-center text-secondary py-5">
            <i className="bi bi-journal-richtext fs-2 d-block mb-2"></i>
            {hayFiltros ? "Ninguna lectura coincide con los filtros." : "Aún no hay lecturas en el catálogo."}
          </div>
        )}

        {!error && lecturas.length > 0 && (
          <div className="table-responsive">
            <table className="table table-hover">
              <thead>
                <tr>
                  <th>Título</th>
                  <th>Tipo de reto</th>
                  <th>Grado</th>
                  <th>Dificultad</th>
                  <th>Fuente</th>
                  <th>Estado</th>
                  <th>Registrada por</th>
                  <th className="text-end">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {lecturas.map((l) => {
                  const tipo = tipoDe(l.tipo_reto);
                  const dificultad = dificultadDe(l.dificultad);
                  return (
                    <tr key={l.id}>
                      <td className="fw-medium">{l.titulo}</td>
                      <td><i className={`bi ${tipo.icono} me-1 text-primary`}></i>{tipo.texto}</td>
                      <td>{l.grado}.°</td>
                      <td><span className={`badge-soft ${dificultad.clase}`}>{dificultad.texto}</span></td>
                      <td>
                        {l.fuente === "oficial" ? (
                          l.url_recurso ? (
                            <a href={l.url_recurso} target="_blank" rel="noreferrer" className="badge-soft bs-p text-decoration-none"
                              title="Ver el recurso oficial">Oficial <i className="bi bi-box-arrow-up-right"></i></a>
                          ) : (
                            <span className="badge-soft bs-p">Oficial</span>
                          )
                        ) : (
                          <span className="badge-soft bs-info">Manual</span>
                        )}
                      </td>
                      <td>
                        <span className={`badge-soft ${l.activo ? "bs-ok" : "bs-off"}`}>{l.activo ? "Activa" : "Inactiva"}</span>
                      </td>
                      <td className="note">{l.creado_por ?? "—"}</td>
                      <td className="text-end text-nowrap">
                          {l.asignada && <span className="badge-soft bs-info me-2" title="Asignada a alumnos">Asignada</span>}
                            {puede("catalogo.editar") && (
                            <button type="button" className="icon-btn me-1" title="Editar" onClick={() => setPorEditar(l)}>
                              <i className="bi bi-pencil"></i>
                            </button>
                          )}
                          {puede("catalogo.desactivar") && l.activo && (
                            <button type="button" className="icon-btn" title="Eliminar" onClick={() => setPorEliminar(l)}>
                              <i className="bi bi-trash3"></i>
                            </button>
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

      {porEliminar && (
        <ModalEliminar
          lectura={porEliminar}
          onCerrar={() => setPorEliminar(null)}
          onEliminada={alEliminar}
          onNoAutorizado={onNoAutorizado}
        />
      )}

        {porImportar && (
        <ModalImportar
          recurso={porImportar}
          onCerrar={() => setPorImportar(null)}
          onImportada={alImportar}
          onNoAutorizado={onNoAutorizado}
        />
      )}

        {(modalAbierto || porEditar) && (
        <ModalLectura
          lectura={porEditar}
          onCerrar={() => { setModalAbierto(false); setPorEditar(null); }}
          onGuardado={alGuardar}
          onNoAutorizado={onNoAutorizado}
        />
      )}
    </section>
  );
}
