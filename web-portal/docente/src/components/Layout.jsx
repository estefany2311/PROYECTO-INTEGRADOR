import { NavLink, Outlet } from "react-router-dom";

const MENU = [
  {
    seccion: "MI AULA",
    items: [
      { ruta: "/aulas", icono: "bi-mortarboard-fill", color: "ico-azul", texto: "Mis aulas" },
      { ruta: "/alumnos", icono: "bi-people-fill", color: "ico-verde", texto: "Alumnos" },
      { ruta: "/grupos", icono: "bi-collection-fill", color: "ico-morado", texto: "Grupos" },
    ],
  },
  {
    seccion: "CONTENIDO",
    items: [
      { ruta: "/catalogo", icono: "bi-book-fill", color: "ico-rosa", texto: "Catálogo" },
    ],
  },
];

export default function Layout() {
  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="topbar-inner">
          <div className="header-brand">
            <span className="logo-mark">
              <i className="bi bi-mortarboard-fill" />
            </span>
            <div className="header-brand-text">
              <b>Portal Docente</b>
              <span>Panel del docente</span>
            </div>
          </div>

          <div className="topbar-actions">
            <span className="sprint-pill">
              <i className="bi bi-stars" />
              Sprint 3
            </span>
            <span className="icon-btn" aria-hidden="true" title="Notificaciones">
              <i className="bi bi-bell" />
              <span className="dot" />
            </span>
            <div className="user-chip">
              <span className="circulo">N</span>
              <div className="user-chip-text">
                <b>Noemí De la Cruz</b>
                <span>Docente</span>
              </div>
            </div>
          </div>
        </div>
      </header>

      <div className="app-body">
        <aside className="sidebar">
          <div className="brand">
            <span className="brand-icon">
              <i className="bi bi-grid-1x2-fill" />
            </span>
            <div className="brand-text">
              <b>Plataforma Educativa</b>
              <span>Panel del docente</span>
            </div>
          </div>
          <div className="brand-bar" />

          <nav aria-label="Menú principal">
            {MENU.map((grupo) => (
              <div key={grupo.seccion}>
                <div className="menu-seccion">{grupo.seccion}</div>
                {grupo.items.map((item) => (
                  <NavLink
                    key={item.ruta}
                    to={item.ruta}
                    className={({ isActive }) => `menu-item${isActive ? " active" : ""}`}
                  >
                    <span className={`menu-ico ${item.color}`}>
                      <i className={`bi ${item.icono}`} />
                    </span>
                    {item.texto}
                  </NavLink>
                ))}
              </div>
            ))}
          </nav>

          <div className="sprint-card">
            <div className="sprint-card-top">
              <span>Sprint 3</span>
              <span className="sprint-status">En curso</span>
            </div>
            <b>Módulo Docente</b>
            <div className="sprint-bar">
              <span />
            </div>
            <small>Dashboard y evidencias en camino</small>
          </div>

          <div className="sidebar-note">
            <i className="bi bi-lightbulb-fill" />
            <span>
              <b>Consejo:</b> comparte el código de tu aula con tus alumnos para que
              encuentren sus lecturas.
            </span>
          </div>
        </aside>

        <div className="content-wrap">
          <main>
            <Outlet />
          </main>
        </div>
      </div>
    </div>
  );
}
