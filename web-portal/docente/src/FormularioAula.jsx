import { useState } from "react";

// Lista de grados del 1 al 6 con su formato de visualización (1.º, 2.º, etc.)
const GRADOS = [1, 2, 3, 4, 5, 6].map((n) => ({ id: n, nombre: `${n}.º` }));

export default function FormularioAula() {
  // Estado para el nombre del aula
  const [nombre, setNombre] = useState("");
  // Estado para la lista de IDs de grados seleccionados
  const [gradoIds, setGradoIds] = useState([]);
  // Estado para guardar los mensajes de error de validación
  const [errores, setErrores] = useState({});

  // Agrega o quita un grado del arreglo de grados seleccionados
  const alternar = (id) =>
    setGradoIds((prev) =>
      prev.includes(id) ? prev.filter((g) => g !== id) : [...prev, id]
    );

  // Valida que el nombre no esté vacío y que se haya elegido al menos un grado
  const validar = () => {
    const e = {};
    if (!nombre.trim()) e.nombre = "El nombre del aula es obligatorio";
    if (gradoIds.length === 0) e.grados = "Selecciona al menos un grado";
    return e;
  };

  // Maneja el evento de envío del formulario
  const enviar = (ev) => {
    ev.preventDefault();
    const e = validar();
    setErrores(e);
    if (Object.keys(e).length === 0) {
      console.log("Datos a enviar:", { nombre, gradoIds });
    }
  };

  return (
    <form onSubmit={enviar} style={{ maxWidth: 420, margin: "2rem auto" }}>
      <h2>Registrar aula multigrado</h2>

      {/* Campo de entrada de texto para el nombre del aula */}
      <label>
        Nombre del aula
        <input
          style={{ display: "block", width: "100%", marginTop: 4 }}
          value={nombre}
          onChange={(e) => setNombre(e.target.value)}
        />
      </label>
      {errores.nombre && <p style={{ color: "red" }}>{errores.nombre}</p>}

      {/* Agrupador de checkboxes para seleccionar los grados */}
      <fieldset style={{ marginTop: 16 }}>
        <legend>Grados que atiendes</legend>
        {GRADOS.map((g) => (
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

      {/* Botón para procesar el formulario */}
      <button type="submit" style={{ marginTop: 16 }}>
        Registrar aula
      </button>
    </form>
  );
}