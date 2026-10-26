import { useEffect, useState } from "react";
import Login from "./Login";
import CrearDocente from "./CrearDocente";
import ListaDocentes from "./ListaDocentes";
import Layout from "./components/Layout";
import Colegios from "./pages/Colegios";
import RolesPermisos from "./pages/RolesPermisos";
import Catalogo from "./pages/Catalogo";
import { obtenerMiPerfil, sesion } from "./api";
import "./App.css";

export default function App() {
  const [autenticado, setAutenticado] = useState(Boolean(sesion.token));
  const [recarga, setRecarga] = useState(0);
  const [aviso, setAviso] = useState("");
  const [pagina, setPagina] = useState("colegios");
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
      <div className="login-viewport">
        <div className="login-container">
          <div className="login-header">
            <span className="brand-badge"></span>
            <h1>Panel de Administración</h1>
            <p>Ingresa tus credenciales para gestionar el portal de docentes</p>
          </div>
          <Login
            aviso={aviso}
            onLogin={() => {
              setAviso("");
              setPagina("colegios");
              setAutenticado(true);
            }}
          />
        </div>
      </div>
    );
  }

  if (!perfil) {
    return <div className="p-5 text-center text-secondary">Cargando permisos…</div>;
  }

  const modulos = ["colegios", "docentes", "roles", "catalogo"];
  const paginaActual = puede(`${pagina}.ver`) ? pagina : modulos.find((m) => puede(`${m}.ver`));

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
      {!paginaActual && (
        <div className="alert alert-warning">No tienes acceso a ningún módulo. Contacta al administrador.</div>
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
