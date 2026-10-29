import { useEffect, useState } from "react";
import Koala from "../components/Koala";
import { listarColegios, listarDocentes, listarLecturas, listarUsuarios } from "../api";

const saludoDelDia = () => {
  const hora = new Date().getHours();
  if (hora < 12) return "Buenos días";
  if (hora < 19) return "Buenas tardes";
  return "Buenas noches";
};

const fechaDeHoy = () =>
  new Date().toLocaleDateString("es-PE", { weekday: "long", day: "numeric", month: "long" });

const comoLista = (datos) => (Array.isArray(datos) ? datos : datos?.results ?? []);

const TARJETAS = [
  { id: "colegios", permiso: "colegios.ver", titulo: "Colegios", icono: "bi-building", tono: "turquesa",
    cargar: () => listarColegios().then((d) => comoLista(d).length) },
  { id: "docentes", permiso: "docentes.ver", titulo: "Docentes", icono: "bi-person-badge", tono: "azul",
    cargar: () => listarDocentes().then((d) => comoLista(d).length) },
  { id: "catalogo", permiso: "catalogo.ver", titulo: "Lecturas", icono: "bi-journal-richtext", tono: "naranja",
    cargar: () => listarLecturas().then((d) => comoLista(d).length) },
  { id: "usuarios", permiso: "usuarios.ver", titulo: "Cuentas inactivas", icono: "bi-person-x", tono: "rojo",
    cargar: () => listarUsuarios().then((d) => comoLista(d).filter((u) => !u.is_active).length) },
];

const ACCESOS = [
  { id: "colegios", permiso: "colegios.crear", texto: "Registrar un colegio", icono: "bi-building-add" },
  { id: "docentes", permiso: "docentes.crear", texto: "Crear un docente", icono: "bi-person-plus" },
  { id: "catalogo", permiso: "catalogo.crear", texto: "Agregar una lectura", icono: "bi-journal-plus" },
  { id: "roles", permiso: "roles.editar", texto: "Gestionar permisos", icono: "bi-shield-lock" },
  { id: "usuarios", permiso: "usuarios.desactivar", texto: "Activar o desactivar cuentas", icono: "bi-people" },
];

export default function Inicio({ nombre, rol, puede = () => true, onNavegar, onNoAutorizado }) {
  const [totales, setTotales] = useState({});

  const tarjetas = TARJETAS.filter((t) => puede(t.permiso));
  const accesos = ACCESOS.filter((a) => puede(a.permiso));
  const primerNombre = (nombre || "").trim().split(" ")[0] || (rol === "ADMIN" ? "administrador" : "");

  useEffect(() => {
    let activo = true;
    tarjetas.forEach((t) => {
      t.cargar()
        .then((n) => activo && setTotales((actual) => ({ ...actual, [t.id]: n })))
        .catch((err) => {
          if (!activo) return;
          if (err.estado === 401) onNoAutorizado();
          else setTotales((actual) => ({ ...actual, [t.id]: "—" }));
        });
    });
    return () => {
      activo = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <section>
      <div className="bienvenida mb-4">
        <div className="bienvenida-texto">
          <span className="bienvenida-chip">
            <i className="bi bi-stars me-1"></i>{saludoDelDia()} · <span className="text-capitalize">{fechaDeHoy()}</span>
          </span>
          <h1>¡Hola, {primerNombre}! <span className="saludo-mano">👋</span></h1>
          <p>
            {rol === "ADMIN"
              ? "Qué bueno verte. Desde aquí gestionas los colegios, los docentes, el catálogo de lecturas y las cuentas de la plataforma."
              : "Qué bueno verte. Aquí tienes un resumen de lo que puedes consultar en la plataforma."}
          </p>
          {rol && <span className="badge-rol"><i className="bi bi-shield-check me-1"></i>{rol === "ADMIN" ? "Administrador" : rol}</span>}
        </div>
        <div className="bienvenida-koala">
          <div className="burbuja">¡Vamos a aprender juntos!</div>
          <Koala size={170} saludando className="koala-flota" />
        </div>
      </div>

      {tarjetas.length > 0 && (
        <div className="row g-3 mb-4">
          {tarjetas.map((t) => (
            <div key={t.id} className="col-sm-6 col-xl-3">
              <button type="button" className={`kpi-card tono-${t.tono}`} onClick={() => onNavegar(t.id)}>
                <span className="kpi-icono"><i className={`bi ${t.icono}`}></i></span>
                <span className="kpi-datos">
                  <span className="kpi-valor">{totales[t.id] ?? <span className="spinner-border spinner-border-sm"></span>}</span>
                  <span className="kpi-titulo">{t.titulo}</span>
                </span>
                <i className="bi bi-chevron-right kpi-flecha"></i>
              </button>
            </div>
          ))}
        </div>
      )}

      <div className="row g-3">
        <div className="col-lg-7">
          <div className="card h-100">
            <div className="card-header"><h2 className="h6 m-0 fw-semibold">Accesos rápidos</h2></div>
            <div className="card-body d-grid gap-2">
              {accesos.length === 0 && <p className="note m-0">Tu rol no tiene acciones de gestión en este portal.</p>}
              {accesos.map((a) => (
                <button key={a.texto} type="button" className="acceso" onClick={() => onNavegar(a.id)}>
                  <i className={`bi ${a.icono}`}></i>
                  <span>{a.texto}</span>
                  <i className="bi bi-arrow-right ms-auto"></i>
                </button>
              ))}
            </div>
          </div>
        </div>
        <div className="col-lg-5">
          <div className="card h-100 consejo">
            <div className="card-body d-flex gap-3 align-items-start">
              <Koala size={64} />
              <div>
                <h2 className="h6 fw-semibold mb-1">Consejo del koala</h2>
                <p className="note mb-0">
                  Clasifica cada lectura por grado y dificultad. Así los docentes encuentran rápido el texto ideal
                  para cada alumno, y la evaluación es más justa.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
