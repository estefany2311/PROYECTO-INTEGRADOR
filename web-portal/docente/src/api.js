const BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8081";
const DOCENTE_ID = 1; // provisional, hasta que exista el login

const headers = {
  "Content-Type": "application/json",
  "X-Docente-Id": DOCENTE_ID,
};

async function pedir(ruta, opciones = {}) {
  const res = await fetch(`${BASE_URL}${ruta}`, { headers, ...opciones });
  const datos = await res.json().catch(() => null);
  if (!res.ok) {
    throw new Error(datos?.mensaje || "Ocurrió un error en el servidor");
  }
  return datos;
}

export const listarGrados = () => pedir("/api/docente/grados");
export const listarAulas = () => pedir("/api/docente/aulas");
export const crearAula = (datos) =>
  pedir("/api/docente/aulas", { method: "POST", body: JSON.stringify(datos) });

export const listarEstudiantes = (aulaId) =>
  pedir(`/api/docente/aulas/${aulaId}/estudiantes`);
export const crearEstudiante = (aulaId, datos) =>
  pedir(`/api/docente/aulas/${aulaId}/estudiantes`, {
    method: "POST",
    body: JSON.stringify(datos),
  });