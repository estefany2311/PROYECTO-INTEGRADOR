import { useState } from "react";
import Koala from "./components/Koala";
import { iniciarSesion } from "./api";

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
      {/* Lado de la marca */}
      <section className="login-marca">
        <div className="login-marca-top">
          <div className="brand-logo koala-logo"><Koala size={30} /></div>
          <span>Plataforma Educativa</span>
        </div>

        <div className="login-marca-centro">
          <div className="burbuja">¡Hola! Qué gusto verte de nuevo</div>
          <Koala size={210} saludando className="koala-flota" />
          <h2>Leer, escribir y hablar <span>jugando</span></h2>
          <p>Gestiona colegios, docentes y el catálogo de lecturas que usan los niños en sus retos.</p>
          <div className="login-chips">
            <span><i className="bi bi-book me-1"></i>Lectura</span>
            <span><i className="bi bi-pencil-square me-1"></i>Escritura</span>
            <span><i className="bi bi-mic me-1"></i>Oralidad</span>
          </div>
        </div>

        <small className="login-marca-pie">Proyecto Integrador · IV Ciclo</small>
      </section>

      {/* Lado del formulario */}
      <section className="login-lado-form">
        <form className="login-form" onSubmit={enviar} noValidate>
          <div className="login-koala-movil"><Koala size={90} saludando /></div>
          <h1>¡Hola, administrador! <span className="saludo-mano">👋</span></h1>
          <p className="login-subtitulo">Ingresa tus credenciales para entrar al panel.</p>

          {aviso && (
            <div className="alert alert-warning py-2 small d-flex gap-2 align-items-center">
              <i className="bi bi-clock-history"></i>{aviso}
            </div>
          )}

          <label className="form-label fw-medium" htmlFor="login-usuario">Usuario</label>
          <div className="input-group login-input mb-3">
            <span className="input-group-text"><i className="bi bi-person"></i></span>
            <input id="login-usuario" type="text" className="form-control" placeholder="Tu usuario"
              autoComplete="username" value={usuario} onChange={(e) => setUsuario(e.target.value)} />
          </div>

          <label className="form-label fw-medium" htmlFor="login-clave">Contraseña</label>
          <div className="input-group login-input mb-3">
            <span className="input-group-text"><i className="bi bi-lock"></i></span>
            <input id="login-clave" type={verClave ? "text" : "password"} className="form-control"
              placeholder="••••••••" autoComplete="current-password"
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

          <button type="submit" className="btn btn-primary w-100 py-2 login-boton" disabled={cargando}>
            {cargando ? (
              <><span className="spinner-border spinner-border-sm me-2"></span>Entrando…</>
            ) : (
              <>Ingresar al panel<i className="bi bi-arrow-right ms-2"></i></>
            )}
          </button>

          <p className="login-ayuda">
            <i className="bi bi-shield-lock me-1"></i>Acceso solo para personal autorizado.
          </p>
        </form>
      </section>
    </div>
  );
}
