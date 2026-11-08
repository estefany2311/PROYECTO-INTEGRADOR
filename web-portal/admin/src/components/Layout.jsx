import { useState } from "react";
import Koala from "./Koala";

const SECCIONES = [
  {
    titulo: "General",
    enlaces: [{ id: "dashboard", icono: "bi-house-heart", texto: "Inicio", disponible: true, libre: true }],
  },
  {
    titulo: "Instituciones",
    enlaces: [
      { id: "colegios", icono: "bi-building", texto: "Colegios", disponible: true, etiqueta: "US-44" },
      { id: "docentes", icono: "bi-person-badge", texto: "Docentes", disponible: true, etiqueta: "US-43" },
    ],
  },
  {
    titulo: "Seguridad",
    enlaces: [
      { id: "usuarios", icono: "bi-people", texto: "Usuarios", disponible: true },
      { id: "roles", icono: "bi-shield-check", texto: "Roles y permisos", disponible: true, etiqueta: "US-45" },
    ],
  },
  {
    titulo: "Contenido",
        enlaces: [{ id: "catalogo", icono: "bi-journal-richtext", texto: "Catálogo", disponible: true }],
  },

    {
    titulo: "Configuración IA",
        enlaces: [
      { id: "configuracion", icono: "bi-mic", texto: "Evaluación de voz", disponible: true },
      { id: "ortografia", modulo: "configuracion", icono: "bi-spellcheck", texto: "Ortografía", disponible: true },
      { id: "puntuacion", modulo: "configuracion", icono: "bi-star", texto: "Puntos y estrellas", disponible: true },
    ],
  },
];

const iniciales = (texto) =>
  texto
    .split(/[\s._-]+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((parte) => parte[0].toUpperCase())
    .join("") || "AD";

export default function Layout({ pagina, onNavegar, onBuscar, onCerrarSesion, usuario, permisos = [], rol, children }) {
  const [menuAbierto, setMenuAbierto] = useState(false);
  const [busqueda, setBusqueda] = useState("");
    // Un enlace disponible solo se muestra si el rol tiene el permiso "<modulo>.ver"
    // "modulo" permite que una pestaña use el permiso de otro módulo (ej. Ortografía → configuracion.ver)
  const puedeVer = (enlace) =>
    !enlace.disponible || enlace.libre || permisos.includes(`${enlace.modulo ?? enlace.id}.ver`);

  const navegar = (id) => {
    onNavegar(id);
    setMenuAbierto(false);
  };

  const buscar = (ev) => {
    ev.preventDefault();
    onBuscar(busqueda.trim());
  };

  return (
    <div className="app-layout">
      <aside className={`sidebar${menuAbierto ? " open" : ""}`}>
        <div className="brand">
          <div className="brand-logo koala-logo">
            <Koala size={30} />
          </div>
          Plataforma Educativa
        </div>

        <nav className="flex-grow-1 overflow-auto">
          {SECCIONES.map((seccion) => (
            <div key={seccion.titulo}>
              <div className="nav-section">{seccion.titulo}</div>
              {seccion.enlaces.filter(puedeVer).map((enlace) =>
                enlace.disponible ? (
                  <button
                    key={enlace.id}
                    type="button"
                    className={`side-link${pagina === enlace.id ? " active" : ""}`}
                    onClick={() => navegar(enlace.id)}
                  >
                    <i className={`bi ${enlace.icono}`}></i>
                    {enlace.texto}
                  </button>
                ) : (
                  <span key={enlace.id} className="side-link disabled" aria-disabled="true">
                    <i className={`bi ${enlace.icono}`}></i>
                    {enlace.texto}
                    <span className="tag">Próximamente</span>
                  </span>
                )
              )}
            </div>
          ))}
        </nav>
      </aside>

      {menuAbierto && <div className="sidebar-backdrop d-lg-none" onClick={() => setMenuAbierto(false)}></div>}

        <div className={`main pagina-${pagina}`}>
        <header className="topbar">
          <button
            type="button"
            className="btn btn-light d-lg-none"
            onClick={() => setMenuAbierto(!menuAbierto)}
            aria-label="Abrir menú"
          >
            <i className="bi bi-list"></i>
          </button>

          <form className="search position-relative flex-grow-1" onSubmit={buscar} role="search">
            <i className="bi bi-search"></i>
            <input
              className="form-control"
              placeholder="Buscar colegio por nombre o código modular…"
              value={busqueda}
              onChange={(e) => setBusqueda(e.target.value)}
            />
          </form>

          <div className="ms-auto d-flex align-items-center gap-3">
            <div className="d-flex align-items-center gap-2">
              <div className="avatar">{iniciales(usuario)}</div>
              <div className="lh-sm d-none d-sm-block">
                <div className="fw-semibold">{usuario || "Administrador"}</div>
                  <div className="note">{rol || "Sin rol"}</div>
              </div>
            </div>
            <button type="button" className="btn btn-sm btn-outline-danger" onClick={onCerrarSesion}>
              <i className="bi bi-box-arrow-right me-1"></i>
              <span className="d-none d-sm-inline">Cerrar sesión</span>
            </button>
          </div>
        </header>

        <main className="content">{children}</main>
      </div>
    </div>
  );
}
