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
// T-183: editar alumno (PUT) y desactivar/reactivar (PATCH)
export const actualizarEstudiante = (aulaId, id, datos) =>
  pedir(`/api/docente/aulas/${aulaId}/estudiantes/${id}`, {
    method: "PUT",
    body: JSON.stringify(datos),
  });
export const cambiarEstadoEstudiante = (aulaId, id, activo) =>
  pedir(`/api/docente/aulas/${aulaId}/estudiantes/${id}/estado`, {
    method: "PATCH",
    body: JSON.stringify({ activo }),
  });

// T-187: catálogo de avatares y PIN de acceso del alumno
export const listarAvatares = () => pedir("/api/docente/avatares");

export const asignarAvatar = (aulaId, id, avatarId) =>
  pedir(`/api/docente/aulas/${aulaId}/estudiantes/${id}/avatar`, {
    method: "PUT",
    body: JSON.stringify({ avatarId }),
  });

// genera o restablece el PIN: la respuesta trae el PIN en claro UNA sola vez
export const generarPin = (aulaId, id) =>
  pedir(`/api/docente/aulas/${aulaId}/estudiantes/${id}/pin`, { method: "POST" });

// T-190: catálogo de lecturas del aula. Los filtros son opcionales y los aplica el backend (T-189)
export const listarLecturas = (aulaId, filtros = {}) => {
  const params = new URLSearchParams();
  if (filtros.gradoId) params.set("gradoId", filtros.gradoId);
  if (filtros.dificultad) params.set("dificultad", filtros.dificultad);
  if (filtros.tipoReto) params.set("tipoReto", filtros.tipoReto);
  const query = params.toString();
  return pedir(`/api/docente/aulas/${aulaId}/lecturas${query ? `?${query}` : ""}`);
};