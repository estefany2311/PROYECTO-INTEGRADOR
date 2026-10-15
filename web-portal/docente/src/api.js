// URL base del backend desde variables de entorno o puerto por defecto 8081
const BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8081";

// ID provisional del docente registrado
const DOCENTE_ID = 1; // provisional, hasta que exista el login

// Encabezados HTTP predeterminados para las solicitudes
const headers = {
  "Content-Type": "application/json",
  "X-Docente-Id": DOCENTE_ID,
};

// Función auxiliar reutilizable para realizar peticiones HTTP mediante fetch
async function pedir(ruta, opciones = {}) {
  const res = await fetch(`${BASE_URL}${ruta}`, { headers, ...opciones });
  const datos = await res.json().catch(() => null);
  if (!res.ok) {
    throw new Error(datos?.mensaje || "Ocurrió un error en el servidor");
  }
  return datos;
}

// Funciones exportadas para interactuar con los endpoints de la API
export const listarGrados = () => pedir("/api/docente/grados");
export const listarAulas = () => pedir("/api/docente/aulas");
export const crearAula = (datos) =>
  pedir("/api/docente/aulas", { method: "POST", body: JSON.stringify(datos) });