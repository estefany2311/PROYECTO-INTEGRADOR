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
      <main>
        <h1>Panel de administración</h1>
        <Login
          aviso={aviso}
          onLogin={() => {
            setAviso("");
            setAutenticado(true);
          }}
        />
      </main>
    );
  }

  return (
    <main>
      <header className="barra">
        <h1>Panel de administración</h1>
        <button className="secundario" onClick={cerrarSesion}>Cerrar sesión</button>
      </header>
      <CrearDocente onCreado={() => setRecarga((n) => n + 1)} onNoAutorizado={sesionExpirada} />
      <ListaDocentes recarga={recarga} onNoAutorizado={sesionExpirada} />
    </main>
  );
}