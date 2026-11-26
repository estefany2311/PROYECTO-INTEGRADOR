import { useState } from "react";
import { iniciarSesion } from "../auth";

// Login docente: usa las cuentas que crea el administrador. Al entrar, App
// detecta la sesión y muestra el portal.
export default function Login() {
  const [usuario, setUsuario] = useState("");
  const [contrasena, setContrasena] = useState("");
  const [error, setError] = useState(null);
  const [enviando, setEnviando] = useState(false);

  const entrar = async (ev) => {
    ev.preventDefault();
    if (!usuario.trim() || !contrasena) {
      setError("Escribe tu usuario y tu contraseña.");
      return;
    }

    setError(null);
    setEnviando(true);
    try {
      await iniciarSesion(usuario.trim(), contrasena);
    } catch (e) {
      setError(e.message);
      setContrasena("");
      setEnviando(false);
    }
  };

  return (
    <main className="login-page">
      <form className="panel login-card" onSubmit={entrar} noValidate>
        <span className="login-icono" aria-hidden="true">
          <i className="bi bi-mortarboard-fill" />
        </span>
        <h1 className="login-titulo">Portal Docente</h1>
        <p className="login-sub">Ingresa con la cuenta que te dio el administrador.</p>

        {error && (
          <div className="alert alert-danger" role="alert">
            {error}
          </div>
        )}

        <label htmlFor="login-usuario" className="form-label fw-semibold">
          Usuario
        </label>
        <input
          id="login-usuario"
          className="form-control mb-3"
          autoComplete="username"
          autoCapitalize="none"
          value={usuario}
          onChange={(ev) => setUsuario(ev.target.value)}
        />

        <label htmlFor="login-contrasena" className="form-label fw-semibold">
          Contraseña
        </label>
        <input
          id="login-contrasena"
          type="password"
          className="form-control mb-4"
          autoComplete="current-password"
          value={contrasena}
          onChange={(ev) => setContrasena(ev.target.value)}
        />

        <button type="submit" className="btn btn-primary w-100" disabled={enviando}>
          {enviando ? "Ingresando…" : "Iniciar sesión"}
        </button>
      </form>
    </main>
  );
}
