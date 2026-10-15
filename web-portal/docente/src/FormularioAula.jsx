// Importación de hooks de React para manejar el estado y el ciclo de vida del componente
import { useEffect, useState } from "react";
// Importación de las funciones auxiliares que hacen las peticiones HTTP a la API (backend)
import { listarGrados, listarAulas, crearAula } from "./api";

export default function FormularioAula() {
  // Estado para guardar la lista de grados obtenidos del backend (1.° a 6.°)
  const [grados, setGrados] = useState([]);
  // Estado para guardar la lista de aulas registradas por el docente
  const [aulas, setAulas] = useState([]);
  // Estado para guardar el texto que el usuario escribe en el campo del nombre del aula
  const [nombre, setNombre] = useState("");
  // Estado para guardar un arreglo con los IDs de los grados seleccionados en los checkboxes
  const [gradoIds, setGradoIds] = useState([]);
  // Estado para manejar los mensajes de error de validación local (frontend)
  const [errores, setErrores] = useState({});
  // Estado para mostrar avisos de éxito ("ok") o de error ("fallo") provenientes de la API
  const [mensaje, setMensaje] = useState(null); // { tipo: "ok" | "fallo", texto }

  // Función para consultar las aulas a la API y actualizar el estado
  const cargarAulas = () =>
    listarAulas()
      .then(setAulas)
      .catch((e) => setMensaje({ tipo: "fallo", texto: e.message }));

  // useEffect se ejecuta una sola vez al cargar la pantalla por primera vez
  useEffect(() => {
    // Obtiene el catálogo de grados desde la API
    listarGrados()
      .then(setGrados)
      .catch((e) => setMensaje({ tipo: "fallo", texto: e.message }));
    // Carga la lista inicial de aulas existentes
    cargarAulas();
  }, []);

  // Función para marcar o desmarcar un grado (checkbox)
  const alternar = (id) =>
    setGradoIds((prev) =>
      // Si el ID ya existía en el arreglo, lo remueve; si no existía, lo agrega
      prev.includes(id) ? prev.filter((g) => g !== id) : [...prev, id]
    );

  // Función para validar que los campos obligatorios del formulario no estén vacíos
  const validar = () => {
    const e = {};
    if (!nombre.trim()) e.nombre = "El nombre del aula es obligatorio";
    if (gradoIds.length === 0) e.grados = "Selecciona al menos un grado";
    return e;
  };

  // Función que maneja el evento de envío (submit) del formulario
  const enviar = async (ev) => {
    ev.preventDefault(); // Evita que la página se recargue automáticamente
    const e = validar();
    setErrores(e);
    if (Object.keys(e).length > 0) return; // Si hay errores locales, detiene el envío

    try {
      // Envía la petición a la API con el nombre y los grados seleccionados
      await crearAula({ nombre, gradoIds });
      // Muestra mensaje de éxito y limpia los campos del formulario
      setMensaje({ tipo: "ok", texto: "¡Aula registrada correctamente!" });
      setNombre("");
      setGradoIds([]);
      // Recarga la lista de aulas para mostrar la recién creada
      cargarAulas();
    } catch (error) {
      // Captura y muestra los errores que devuelva el backend
      setMensaje({ tipo: "fallo", texto: error.message });
    }
  };

  return (
    <div className="app">
      {/* Formulario de registro de aula */}
      <form className="tarjeta" onSubmit={enviar}>
        <h2>Registrar aula multigrado</h2>

        {/* Muestra mensajes informativos si existen (éxito o error de API) */}
        {mensaje && (
          <p className={`mensaje ${mensaje.tipo}`}>{mensaje.texto}</p>
        )}

        {/* Campo de texto para el nombre del aula */}
        <label className="campo">
          Nombre del aula
          <input value={nombre} onChange={(e) => setNombre(e.target.value)} />
        </label>
        {errores.nombre && <p className="error">{errores.nombre}</p>}

        {/* Sección de checkboxes para elegir los grados */}
        <fieldset>
          <legend>Grados que atiendes</legend>
          <div className="grados">
            {grados.map((g) => (
              <label key={g.id} className="chip">
                <input
                  type="checkbox"
                  checked={gradoIds.includes(g.id)}
                  onChange={() => alternar(g.id)}
                />
                {g.nombre}
              </label>
            ))}
          </div>
        </fieldset>
        {errores.grados && <p className="error">{errores.grados}</p>}

        {/* Botón para confirmar y enviar el formulario */}
        <button className="boton" type="submit">
          Registrar aula
        </button>
      </form>

      {/* Sección donde se listan las aulas previamente registradas */}
      <section className="tarjeta">
        <h2>Mis aulas</h2>
        {aulas.length === 0 ? (
          <p>Aún no has registrado aulas.</p>
        ) : (
          <ul className="aulas">
            {aulas.map((a) => (
              <li key={a.id} className="aula">
                <strong>{a.nombre}</strong>
                {a.grados.map((g) => (
                  <span key={g.id} className={`insignia g${g.id % 4}`}>
                    {g.nombre}
                  </span>
                ))}
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}