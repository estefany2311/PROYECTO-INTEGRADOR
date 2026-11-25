import { useEffect, useState } from "react";
import { actualizarReglaDesbloqueo, listarReglasDesbloqueo, listarReglasPuntuacion } from "../api";
import TarjetaNivel from "../components/TarjetaNivel";

// Componente: ReglasDesbloqueo (padre de la pestaña, US-54)
// Padre: App
// Guarda el estado: niveles (con su regla), lo guardado, puntos máximos por tipo de reto y errores
// Hijos: TarjetaNivel (una por nivel del mapa)

const comoLista = (datos) => (Array.isArray(datos) ? datos : datos?.results ?? []);

// Validación en el navegador (el backend valida lo mismo, T-338)
function erroresDe(nivel, puntosMaximos) {
  const errores = {};
  const puntaje = Number(nivel.puntaje_minimo);
  if (nivel.puntaje_minimo === "" || !Number.isInteger(puntaje) || puntaje < 0) {
    errores.puntaje_minimo = "Ingresa un número entero desde 0.";
  } else if (puntosMaximos != null && puntaje > puntosMaximos) {
    errores.puntaje_minimo = `Un reto de ${nivel.tipo_reto} da como máximo ${puntosMaximos} puntos.`;
  }
  return errores;
}

export default function ReglasDesbloqueo({ onNoAutorizado, puede = () => true }) {
  const [guardados, setGuardados] = useState([]);
  const [niveles, setNiveles] = useState([]);
  const [puntosPorTipo, setPuntosPorTipo] = useState({});
  const [erroresServidor, setErroresServidor] = useState({});
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState("");
  const [exito, setExito] = useState("");
  const puedeEditar = puede("configuracion.editar");

  useEffect(() => {
    Promise.all([listarReglasDesbloqueo(), listarReglasPuntuacion()])
      .then(([datosNiveles, datosPuntos]) => {
        const lista = comoLista(datosNiveles);
        setGuardados(lista);
        setNiveles(lista);
        setPuntosPorTipo(Object.fromEntries(comoLista(datosPuntos).map((r) => [r.tipo_reto, r.puntos])));
      })
      .catch((err) => {
        if (err.estado === 401) onNoAutorizado();
        else setError(err.datos?.detail ?? err.mensaje ?? "No se pudieron cargar los niveles del mapa.");
      })
      .finally(() => setCargando(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!exito) return;
    const t = setTimeout(() => setExito(""), 4000);
    return () => clearTimeout(t);
  }, [exito]);

  const cambiar = (nivelId, campo, valor) => {
    setNiveles(niveles.map((n) => (n.nivel_id === nivelId ? { ...n, [campo]: valor } : n)));
    setErroresServidor({ ...erroresServidor, [nivelId]: undefined });
  };

  const cambiosDe = (nivel) => {
    const original = guardados.find((g) => g.nivel_id === nivel.nivel_id);
    const cambios = {};
    ["estrellas_minimas", "puntaje_minimo"].forEach((campo) => {
      if (Number(nivel[campo]) !== Number(original[campo])) cambios[campo] = Number(nivel[campo]);
    });
    return cambios;
  };

  const conCambios = niveles.filter((n) => Object.keys(cambiosDe(n)).length > 0);
  const hayErrores = niveles.some((n) => Object.keys(erroresDe(n, puntosPorTipo[n.tipo_reto])).length > 0);

  const guardar = async () => {
    setGuardando(true);
    setError("");
    const nuevosErrores = {};
    let actualizados = [...guardados];
    for (const nivel of conCambios) {
      try {
        const guardado = await actualizarReglaDesbloqueo(nivel.nivel_id, cambiosDe(nivel));
        actualizados = actualizados.map((g) => (g.nivel_id === nivel.nivel_id ? guardado : g));
      } catch (err) {
        if (err.estado === 401) return onNoAutorizado();
        if (err.estado === 400) {
          nuevosErrores[nivel.nivel_id] = Object.fromEntries(
            Object.entries(err.datos ?? {}).map(([k, v]) => [k, Array.isArray(v) ? v.join(" ") : String(v)])
          );
        } else {
          setError(err.datos?.detail ?? err.mensaje ?? "No se pudieron guardar las reglas.");
        }
      }
    }
    setGuardados(actualizados);
    // Los niveles con error conservan lo escrito; los demás quedan como en el servidor
    setNiveles(niveles.map((n) => (nuevosErrores[n.nivel_id] ? n : actualizados.find((g) => g.nivel_id === n.nivel_id))));
    setErroresServidor(nuevosErrores);
    if (Object.keys(nuevosErrores).length === 0) setExito("¡Reglas de desbloqueo guardadas!");
    setGuardando(false);
  };

  return (
    <section>
      <div className="page-title d-flex flex-wrap justify-content-between align-items-end gap-2 mb-4">
        <div>
          <h1>Desbloqueo de niveles</h1>
          <div className="breadcrumb text-secondary">Configuración IA / Desbloqueo de niveles</div>
        </div>
        {puedeEditar && (
          <button type="button" className="btn btn-primary" onClick={guardar}
            disabled={guardando || hayErrores || conCambios.length === 0}>
            <i className="bi bi-check2 me-1"></i>{guardando ? "Guardando..." : "Guardar cambios"}
          </button>
        )}
      </div>

      {exito && (
        <div className="alert alert-success py-2 small d-flex gap-2 align-items-center">
          <i className="bi bi-check-circle"></i>{exito}
        </div>
      )}
      {error && <div className="alert alert-danger py-2 small">{error}</div>}

      {cargando ? (
        <div className="text-center text-secondary py-5">Cargando…</div>
      ) : niveles.length === 0 ? (
        <div className="alert alert-warning">No se encontraron niveles del mapa. Revisa que la tabla de niveles exista en la base de datos.</div>
      ) : (
        <>
          <p className="note mb-3">
            <i className="bi bi-info-circle me-1"></i>
            Define qué debe lograr el alumno en cada nivel para pasar al siguiente. La app del estudiante usa estas reglas al terminar cada reto.
            {conCambios.length > 0 && <b className="ms-1">{conCambios.length} nivel(es) con cambios sin guardar.</b>}
          </p>
          <div className="row g-3 mapa-niveles">
            {niveles.map((n, i) => (
              <div key={n.nivel_id} className="col-md-6 col-xl-4">
                <TarjetaNivel
                  nivel={n}
                  puntosMaximos={puntosPorTipo[n.tipo_reto]}
                  errores={{ ...erroresDe(n, puntosPorTipo[n.tipo_reto]), ...(erroresServidor[n.nivel_id] ?? {}) }}
                  conCambios={Object.keys(cambiosDe(n)).length > 0}
                  esUltimo={i === niveles.length - 1}
                  deshabilitado={!puedeEditar || guardando}
                  onCambiar={cambiar}
                />
              </div>
            ))}
          </div>
          {Object.values(erroresServidor).some((e) => e?.detail) && (
            <div className="alert alert-danger py-2 small mt-3">
              {Object.values(erroresServidor).map((e) => e?.detail).filter(Boolean).join(" ")}
            </div>
          )}
        </>
      )}
    </section>
  );
}
