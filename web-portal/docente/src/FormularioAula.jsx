import { useEffect, useState } from "react";
import { listarGrados, listarAulas, crearAula } from "./api";

export default function FormularioAula() {
  // Estados para datos recibidos del servidor y entradas del usuario
  const [grados, setGrados] = useState([]);
  const [aulas, setAulas] = useState([]);
  const [nombre, setNombre] = useState("");
  const [gradoIds, setGradoIds] = useState([]);
  const [errores, setErrores] = useState({});
  const [mensaje, setMensaje] = useState(null); // { tipo: "ok" | "error", texto }

  // Función para obtener la lista actualizada de aulas desde la API
  const cargarAulas = () =>
    listarAulas()
      .then(setAulas)
      .catch((e) => setMensaje({ tipo: "error", texto: e.message }));

  // Carga inicial de datos al montar el componente
  useEffect(() => {
    listarGrados()
      .then(setGrados)
      .catch((e) => setMensaje({ tipo: "error", texto: e.message }));
    cargarAulas();
  }, []);

  // Función para alternar la selección de checkboxes
  const alternar = (id) =>
    setGradoIds((prev) =>
      prev.includes(id) ? prev.filter((g) => g !== id) : [...prev, id]
    );

  // Validación local del formulario
  const validar = () => {
    const e = {};
    if (!nombre.trim()) e.nombre = "El nombre del aula es obligatorio";
    if (gradoIds.length === 0) e.grados = "Selecciona al menos un grado";
    return e;
  };

  // Manejador del envío del formulario hacia la API
  const enviar = async (ev) => {
    ev.preventDefault();
    const e = validar();
    setErrores(e);
    if (Object.keys(e).length > 0) return;

    try {
      await crearAula({ nombre, gradoIds });
      setMensaje({ tipo: "ok", texto: "Aula registrada correctamente" });
      setNombre("");
      setGradoIds([]);
      cargarAulas();
    } catch (error) {
      setMensaje({ tipo: "error", texto: error.message });
    }
  };

  return (
    <div style={{ maxWidth: 420, margin: "2rem auto" }}>
      <form onSubmit={enviar}>
        <h2>Registrar aula multigrado</h2>

        {/* Notificación de éxito o error devuelto por la API */}
        {mensaje && (
          <p style={{ color: mensaje.tipo === "ok" ? "green" : "red" }}>
            {mensaje.texto}
          </p>
        )}

        {/* Input para el nombre del aula */}
        <label>
          Nombre del aula
          <input
            style={{ display: "block", width: "100%", marginTop: 4 }}
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
          />
        </label>
        {errores.nombre && <p style={{ color: "red" }}>{errores.nombre}</p>}

        {/* Renderizado dinámico de grados desde la API */}
        <fieldset style={{ marginTop: 16 }}>
          <legend>Grados que atiendes</legend>
          {grados.map((g) => (
            <label key={g.id} style={{ marginRight: 12 }}>
              <input
                type="checkbox"
                checked={gradoIds.includes(g.id)}
                onChange={() => alternar(g.id)}
              />
              {g.nombre}
            </label>
          ))}
        </fieldset>
        {errores.grados && <p style={{ color: "red" }}>{errores.grados}</p>}

        {/* Botón de submit */}
        <button type="submit" style={{ marginTop: 16 }}>
          Registrar aula
        </button>
      </form>

      {/* Lista dinámicamente cargada desde la API */}
      <h2 style={{ marginTop: 32 }}>Mis aulas</h2>
      {aulas.length === 0 ? (
        <p>Aún no has registrado aulas.</p>
      ) : (
        <ul>
          {aulas.map((a) => (
            <li key={a.id}>
              <strong>{a.nombre}</strong> — {a.grados.map((g) => g.nombre).join(", ")}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}