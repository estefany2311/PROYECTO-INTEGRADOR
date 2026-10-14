import { useState } from "react";

const inicial = { username: "", first_name: "", last_name: "", email: "", password: "" };

export default function CrearDocente() {
  const [datos, setDatos] = useState(inicial);
  const [errores, setErrores] = useState({});
  const [mensaje, setMensaje] = useState("");

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

  const enviar = (ev) => {
    ev.preventDefault();
    const e = validar();
    setErrores(e);
    if (Object.keys(e).length === 0) {
      setMensaje("Formulario válido. Falta conectarlo con la API (Tarea 6).");
    } else {
      setMensaje("");
    }
  };

  const campo = (name, label, type = "text") => (
    <div className="campo">
      <label htmlFor={name}>{label}</label>
      <input id={name} name={name} type={type} value={datos[name]} onChange={cambiar} />
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
      <button type="submit">Crear docente</button>
      {mensaje && <p className="exito">{mensaje}</p>}
    </form>
  );
}