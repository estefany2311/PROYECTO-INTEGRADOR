import { useEffect, useState } from "react";
import Login from "./Login";
import CrearDocente from "./CrearDocente";
import ListaDocentes from "./ListaDocentes";
import Layout from "./components/Layout";
import Colegios from "./pages/Colegios";
import RolesPermisos from "./pages/RolesPermisos";
import Catalogo from "./pages/Catalogo";
import Usuarios from "./pages/Usuarios";
import Inicio from "./pages/Inicio";
import { obtenerMiPerfil, sesion } from "./api";
import "./App.css";

export default function App() {
  const [autenticado, setAutenticado] = useState(Boolean(sesion.token));
  const [recarga, setRecarga] = useState(0);
  const [aviso, setAviso] = useState("");
  const [pagina, setPagina] = useState("dashboard");
  // La búsqueda de la topbar abre Colegios con ese texto; el id fuerza a reiniciar la página
  const [busquedaGlobal, setBusquedaGlobal] = useState({ id: 0, texto: "" });
  const [perfil, setPerfil] = useState(null);

  // Al iniciar sesión, trae el rol y los permisos del usuario
  useEffect(() => {
    if (!autenticado) return;
    obtenerMiPerfil()
      .then(setPerfil)
      .catch((err) => {
        if (err.estado === 401) {
          sesion.limpiar();
          setAutenticado(false);
          setAviso("Tu sesión expiró. Inicia sesión nuevamente.");
        }
      });
  }, [autenticado]);

  const permisos = perfil?.permisos ?? [];
  const puede = (codigo) => permisos.includes(codigo);

  const cerrarSesion = () => {
    setPerfil(null);
    sesion.limpiar();
    setAutenticado(false);
    setAviso("");
  };

  const sesionExpirada = () => {
    setPerfil(null);
    sesion.limpiar();
    setAutenticado(false);
    setAviso("Tu sesión expiró. Inicia sesión nuevamente.");
  };

  const buscarDesdeTopbar = (texto) => {
    setBusquedaGlobal((actual) => ({ id: actual.id + 1, texto }));
    setPagina("colegios");
  };

  if (!autenticado) {
    return (
      <Login
        aviso={aviso}
        onLogin={() => {
          setAviso("");
          setPagina("dashboard");
          setAutenticado(true);
        }}
      />
    );
  }

  if (!perfil) {
    return <div className="p-5 text-center text-secondary">Cargando permisos…</div>;
  }

  // Inicio siempre está disponible; las demás páginas requieren el permiso "<modulo>.ver"
  const paginaActual = pagina === "dashboard" || puede(`${pagina}.ver`) ? pagina : "dashboard";

  return (
    <Layout
      pagina={paginaActual}
      onNavegar={setPagina}
      onBuscar={buscarDesdeTopbar}
      onCerrarSesion={cerrarSesion}
      usuario={perfil.nombre_completo || sesion.usuario}
      rol={perfil.rol}
      permisos={permisos}
    >
      {paginaActual === "dashboard" && (
        <Inicio
          nombre={perfil.nombre_completo}
          rol={perfil.rol}
          puede={puede}
          onNavegar={setPagina}
          onNoAutorizado={sesionExpirada}
        />
      )}

      {paginaActual === "colegios" && (
        <Colegios
          key={busquedaGlobal.id}
          busquedaInicial={busquedaGlobal.texto}
          onNoAutorizado={sesionExpirada}
          puede={puede}
        />
      )}

      {paginaActual === "roles" && <RolesPermisos onNoAutorizado={sesionExpirada} />}

      {paginaActual === "catalogo" && <Catalogo onNoAutorizado={sesionExpirada} puede={puede} />}

      {paginaActual === "usuarios" && (
        <Usuarios onNoAutorizado={sesionExpirada} puede={puede} usuarioActualId={perfil.id} />
      )}

      {paginaActual === "docentes" && (
        <section>
          <div className="page-title mb-4">
            <h1>
              Docentes <span className="story">US-43</span>
            </h1>
            <div className="breadcrumb text-secondary">Instituciones / Docentes</div>
          </div>
          <div className="row g-3">
            <div className="col-lg-4">
              <div className="card card-body">
                <CrearDocente onCreado={() => setRecarga((n) => n + 1)} onNoAutorizado={sesionExpirada} />
              </div>
            </div>
            <div className="col-lg-8">
              <div className="card card-body">
                <ListaDocentes recarga={recarga} onNoAutorizado={sesionExpirada} />
              </div>
            </div>
          </div>
        </section>
      )}
    </Layout>
  );
}