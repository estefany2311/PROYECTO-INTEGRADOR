// Pantalla: Login
// Padre: App
// Recibe (props): aviso (por ejemplo, "tu sesión expiró")
// Avisa al padre con: onLogin() cuando el usuario entra
// Hijos: Koala
import { useState } from "react";
import Koala from "../components/Koala";
import { iniciarSesion } from "../api";

export default function Login({ onLogin, aviso }) {
  const [usuario, setUsuario] = useState("");
  const [clave, setClave] = useState("");
  const [verClave, setVerClave] = useState(false);
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
        err.datos?.code === "cuenta_inactiva"
          ? err.datos.detail
          : err.estado === 401
            ? "Usuario o contraseña incorrectos."
            : err.mensaje ?? "No se pudo iniciar sesión."
      );
    } finally {
      setCargando(false);
    }
  };

  return (
    <div className="login-pagina">
      <main className="login-cuadro">
        {/* Lado de la marca: logo grande y nombre */}
        <section className="login-marca">
          <div className="login-halo">
            <div>
              <Koala size={230} className="koala-flota" />
            </div>
          </div>
          <div className="login-nombre">
            Yalliq
            <small>Panel de administración</small>
          </div>
        </section>

        {/* Lado del formulario */}
        <section className="login-lado-form">
          <form className="login-form" onSubmit={enviar} noValidate>
            <span className="login-etiqueta"><i className="bi bi-shield-check"></i>Acceso de administrador</span>
            <h1>Te damos la bienvenida</h1>
            <p className="login-subtitulo">Ingresa con tu cuenta para continuar.</p>

            {aviso && (
              <div className="alert alert-warning py-2 small d-flex gap-2 align-items-center">
                <i className="bi bi-clock-history"></i>{aviso}
              </div>
            )}

            <label className="form-label" htmlFor="login-usuario">Usuario</label>
            <div className="input-group login-input mb-3">
              <span className="input-group-text"><i className="bi bi-person"></i></span>
              <input id="login-usuario" type="text" className="form-control" placeholder="Tu usuario"
                autoComplete="username" value={usuario} onChange={(e) => setUsuario(e.target.value)} />
            </div>

            <label className="form-label" htmlFor="login-clave">Contraseña</label>
            <div className="input-group login-input mb-3">
              <span className="input-group-text"><i className="bi bi-lock"></i></span>
              <input id="login-clave" type={verClave ? "text" : "password"} className="form-control"
                placeholder="Tu contraseña" autoComplete="current-password"
                value={clave} onChange={(e) => setClave(e.target.value)} />
              <button type="button" className="input-group-text login-ojo" onClick={() => setVerClave(!verClave)}
                aria-label={verClave ? "Ocultar contraseña" : "Mostrar contraseña"}>
                <i className={`bi ${verClave ? "bi-eye-slash" : "bi-eye"}`}></i>
              </button>
            </div>

            {error && (
              <div className="alert alert-danger py-2 small d-flex gap-2 align-items-center">
                <i className="bi bi-exclamation-circle"></i>{error}
              </div>
            )}

            <button type="submit" className="btn btn-primary w-100 login-boton" disabled={cargando}>
              {cargando ? (
                <><span className="spinner-border spinner-border-sm me-2"></span>Entrando…</>
              ) : (
                <>Ingresar<i className="bi bi-arrow-right ms-2"></i></>
              )}
            </button>

            <p className="login-ayuda">
              <i className="bi bi-lock me-1"></i>Conexión segura · Solo personal autorizado
            </p>
            <p className="login-pie">© 2026 Yalliq</p>
          </form>
        </section>
      </main>
    </div>
  );
}
