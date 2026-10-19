import { useState } from "react";
import Login from "./Login";
import CrearDocente from "./CrearDocente";
import ListaDocentes from "./ListaDocentes";
import { sesion } from "./api";
import "./App.css";

export default function App() {
  const [autenticado, setAutenticado] = useState(Boolean(sesion.token));
  const [recarga, setRecarga] = useState(0);
  const [aviso, setAviso] = useState("");

  const cerrarSesion = () => {
    sesion.limpiar();
    setAutenticado(false);
    setAviso("");
  };

  const sesionExpirada = () => {
    sesion.limpiar();
    setAutenticado(false);
    setAviso("Tu sesión expiró. Inicia sesión nuevamente.");
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
              setAutenticado(true);
            }}
          />
        </div>
      </div>
    );
  }

  return (
    <div className="dashboard-layout">
      <header className="dashboard-header">
        <div className="header-brand">
          
          <div>
            <h1>Panel de Administración</h1>
            <span className="subtitle">Gestión de Usuarios y Docentes</span>
          </div>
        </div>
        <div className="header-user">
          <span className="user-badge">Administrador</span>
          <button className="btn-logout" onClick={cerrarSesion}>
            Cerrar sesión
          </button>
        </div>
      </header>

      <main className="dashboard-content">
        <CrearDocente 
          onCreado={() => setRecarga((n) => n + 1)} 
          onNoAutorizado={sesionExpirada} 
        />
        <ListaDocentes 
          recarga={recarga} 
          onNoAutorizado={sesionExpirada} 
        />
      </main>
    </div>
  );
}