import { useState } from "react";

const SECCIONES = [
  {
    titulo: "General",
    enlaces: [{ id: "dashboard", icono: "bi-grid-1x2", texto: "Dashboard" }],
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
      { id: "usuarios", icono: "bi-people", texto: "Usuarios" },
      { id: "roles", icono: "bi-shield-check", texto: "Roles y permisos", disponible: true, etiqueta: "US-45" },
    ],
  },
  {
    titulo: "Contenido",
    enlaces: [{ id: "catalogo", icono: "bi-journal-richtext", texto: "Catálogo" }],
  },
];

const iniciales = (texto) =>
  texto
    .split(/[\s._-]+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((parte) => parte[0].toUpperCase())
    .join("") || "AD";

export default function Layout({ pagina, onNavegar, onBuscar, onCerrarSesion, usuario, children }) {
  const [menuAbierto, setMenuAbierto] = useState(false);
  const [busqueda, setBusqueda] = useState("");

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
          <div className="brand-logo">
            <i className="bi bi-book-half"></i>
          </div>
          Plataforma Educativa
        </div>

        <nav className="flex-grow-1 overflow-auto">
          {SECCIONES.map((seccion) => (
            <div key={seccion.titulo}>
              <div className="nav-section">{seccion.titulo}</div>
              {seccion.enlaces.map((enlace) =>
                enlace.disponible ? (
                  <button
                    key={enlace.id}
                    type="button"
                    className={`side-link${pagina === enlace.id ? " active" : ""}`}
                    onClick={() => navegar(enlace.id)}
                  >
                    <i className={`bi ${enlace.icono}`}></i>
                    {enlace.texto}
                    <span className="tag">{enlace.etiqueta}</span>
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

      <div className="main">
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
                <div className="note">Administrador</div>
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
