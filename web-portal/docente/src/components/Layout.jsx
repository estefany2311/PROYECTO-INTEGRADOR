import { NavLink, Outlet } from "react-router-dom";

const MENU = [
  {
    seccion: "MI AULA",
    items: [
      { ruta: "/aulas", icono: "bi-mortarboard", texto: "Mis aulas" },
      { ruta: "/alumnos", icono: "bi-people", texto: "Alumnos" },
    ],
  },
];

export default function Layout() {
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-icon">
            <i className="bi bi-lightning-charge-fill" />
          </span>
          Portal Docente
        </div>

        {MENU.map((grupo) => (
          <div key={grupo.seccion}>
            <div className="menu-seccion">{grupo.seccion}</div>
            {grupo.items.map((item) => (
              <NavLink key={item.ruta} to={item.ruta} className="menu-item">
                <i className={`bi ${item.icono}`} />
                {item.texto}
              </NavLink>
            ))}
          </div>
        ))}

        <div className="sidebar-user">
          <span className="circulo">N</span>
          <div>
            <b>Noemí De la Cruz</b>
            <small>DOCENTE</small>
          </div>
        </div>
      </aside>

      <div className="content-wrap">
        <header className="topbar">
          <div className="user-chip">
            <span className="circulo">N</span>
            Noemí
          </div>
        </header>
        <main>
          <Outlet />
        </main>
      </div>
    </div>
  );
}