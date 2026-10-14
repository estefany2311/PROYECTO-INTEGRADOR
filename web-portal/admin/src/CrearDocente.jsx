import { useState } from "react";
import { crearDocente } from "./api";

const inicial = { username: "", first_name: "", last_name: "", email: "", password: "" };

export default function CrearDocente({ onCreado, onNoAutorizado }) {
  const [datos, setDatos] = useState(inicial);
  const [errores, setErrores] = useState({});
  const [mensaje, setMensaje] = useState(null);
  const [enviando, setEnviando] = useState(false);

  const cambiar = (e) => setDatos({ ...datos, [e.target.name]: e.target.value });

  const validar = () => {
    const e = {};
    if (!datos.username.trim()) e.username = "El usuario es obligatorio.";
    if (!datos.first_name.trim()) e.first_name = "Los nombres son obligatorios.";
    if (!datos.last_name.trim()) e.last_name = "Los apellidos son obligatorios.";
    if (!datos.email.trim()) e.email = "El correo es obligatorio.";
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(datos.email))
      e.email = "Ingresa un correo válido.";
    if (datos.password.length < 8)
      e.password = "La contraseña debe tener al menos 8 caracteres.";
    return e;
  };

  const enviar = async (ev) => {
    ev.preventDefault();
    const e = validar();
    setErrores(e);
    setMensaje(null);
    if (Object.keys(e).length > 0) return;

    setEnviando(true);
    try {
      await crearDocente({ ...datos, username: datos.username.trim(), email: datos.email.trim() });
      setMensaje({ tipo: "exito", texto: "Docente creado correctamente." });
      setDatos(inicial);
      onCreado();
    } catch (err) {
      if (err.estado === 401) {
        onNoAutorizado();
      } else if (err.estado === 400 && err.datos) {
        const deServidor = {};
        for (const [campo, msgs] of Object.entries(err.datos)) {
          deServidor[campo] = Array.isArray(msgs) ? msgs.join(" ") : String(msgs);
        }
        setErrores(deServidor);
        setMensaje({ tipo: "error", texto: "Revisa los campos marcados." });
      } else if (err.estado === 403) {
        setMensaje({ tipo: "error", texto: err.datos?.detail ?? "No tienes permiso para esta acción." });
      } else {
        setMensaje({ tipo: "error", texto: err.mensaje ?? "Ocurrió un error inesperado." });
      }
    } finally {
      setEnviando(false);
    }
  };

 const campo = (name, label, type = "text") => (
  <div className="campo">
    <label htmlFor={name}>{label}</label>
    <input
      id={name}
      name={name}
      type={type}
      value={datos[name]}
      onChange={cambiar}
      autoComplete={type === "password" ? "new-password" : "off"}
    />
    {errores[name] && <span className="error">{errores[name]}</span>}
  </div>
);

  return (
    <form className="formulario" onSubmit={enviar} noValidate>
      <h2>Crear docente</h2>
      {campo("username", "Usuario")}
      {campo("first_name", "Nombres")}
      {campo("last_name", "Apellidos")}
      {campo("email", "Correo", "email")}
      {campo("password", "Contraseña", "password")}
      <button type="submit" disabled={enviando}>
        {enviando ? "Creando..." : "Crear docente"}
      </button>
      {mensaje && <p className={mensaje.tipo === "exito" ? "exito" : "error"}>{mensaje.texto}</p>}
    </form>
  );
}