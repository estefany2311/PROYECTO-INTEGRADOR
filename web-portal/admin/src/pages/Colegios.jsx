import { useEffect, useState } from "react";
import { actualizarColegio, listarColegios } from "../api";
import ListaColegios from "../components/ListaColegios";
import ModalAsignar from "../components/ModalAsignar";
import ModalColegio from "../components/ModalColegio";
import ResumenColegios from "../components/ResumenColegios";
import { REGIONES } from "../utils/colegios";

// Componente: Colegios (padre de la pestaña)
// Padre: App
// Guarda el estado: colegios, busqueda, region y qué ventana está abierta
// Hijos: ResumenColegios y ListaColegios (reciben la misma lista), ModalColegio, ModalAsignar
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

  // El formulario entrega el colegio: se agrega o se reemplaza con un arreglo nuevo (sin push ni splice)
  const alGuardarColegio = (guardado, mensaje) => {
    setModalColegio(null);
    setExito(mensaje);
    setColegios((actuales) =>
      actuales.some((c) => c.id === guardado.id)
        ? actuales.map((c) => (c.id === guardado.id ? guardado : c))   // editar
        : [guardado, ...actuales]                                       // registrar
    );
  };

  const alGuardar = (_colegio, mensaje) => {
    setModalColegio(null);
    setModalAsignar(null);
    setExito(mensaje);
    setRecarga((n) => n + 1);
  };

  const alternarActivo = async (colegio) => {
    try {
      await actualizarColegio(colegio.id, { activo: !colegio.activo });
      // Arreglo nuevo con map(): solo cambia el colegio tocado, sin push ni splice
      setColegios((actuales) => actuales.map((c) => (c.id === colegio.id ? { ...c, activo: !c.activo } : c)));
      setExito(colegio.activo ? `Se desactivó ${colegio.nombre}.` : `Se activó ${colegio.nombre}.`);
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
          <ResumenColegios colegios={colegios} cargando={cargando} />
        </div>

        {error && <div className="alert alert-danger m-3 py-2 small">{error}</div>}

        {!error && !cargando && colegios.length === 0 && (
          <div className="text-center text-secondary py-5">
            <i className="bi bi-building fs-2 d-block mb-2"></i>
            {busqueda || region ? "Ningún colegio coincide con la búsqueda." : "Aún no hay colegios registrados."}
          </div>
        )}

        {colegios.length > 0 && (
          <ListaColegios
            colegios={colegios}
            puede={puede}
            onAsignar={(c) => setModalAsignar(c)}
            onEditar={(c) => setModalColegio({ colegio: c })}
            onAlternar={alternarActivo}
          />
        )}
      </div>

      {modalColegio && (
        <ModalColegio
          colegio={modalColegio.colegio}
          onCerrar={() => setModalColegio(null)}
          onGuardado={alGuardarColegio}
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
