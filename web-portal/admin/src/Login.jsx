import { useState } from "react";
import { iniciarSesion } from "./api";

export default function Login({ onLogin, aviso }) {
  const [usuario, setUsuario] = useState("");
  const [clave, setClave] = useState("");
  const [error, setError] = useState("");
  const [cargando, setCargando] = useState(false);

  const enviar = async (ev) => {
    ev.preventDefault();
    if (!usuario.trim() || !clave) {
      setError("Ingresa usuario y contraseña.");
      return;
    }
    setCargando(true);
    setError("");
    try {
      await iniciarSesion(usuario.trim(), clave);
      onLogin();
    } catch (err) {
      setError(
        err.estado === 401
          ? "Usuario o contraseña incorrectos."
          : err.mensaje ?? "No se pudo iniciar sesión."
      );
    } finally {
      setCargando(false);
    }
  };

  return (
    <form className="formulario" onSubmit={enviar} noValidate>
      <h2>Iniciar sesión</h2>
      {aviso && <p className="aviso">{aviso}</p>}
      <div className="campo">
        <label htmlFor="login-usuario">Usuario</label>
        <input id="login-usuario" value={usuario} onChange={(e) => setUsuario(e.target.value)} />
      </div>
      <div className="campo">
        <label htmlFor="login-clave">Contraseña</label>
        <input id="login-clave" type="password" value={clave} onChange={(e) => setClave(e.target.value)} />
      </div>
      <button type="submit" disabled={cargando}>
        {cargando ? "Ingresando..." : "Ingresar"}
      </button>
      {error && <span className="error">{error}</span>}
    </form>
  );
}