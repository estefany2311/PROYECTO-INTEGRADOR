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
// T-279: dashboard del aula (endpoint de T-278).
// T-284: filtro soloRojo del endpoint (T-282) para el contador del menú.
// T-287: filtros de grado y competencia del dashboard (T-286).
export const listarDashboardAula = (aulaId, filtros = {}) => {
  const params = new URLSearchParams();
  if (filtros.soloRojo) params.set("soloRojo", "true");
  if (filtros.gradoId) params.set("gradoId", filtros.gradoId);
  if (filtros.competencia) params.set("competencia", filtros.competencia);
  const query = params.toString();
  return pedir(`/api/docente/aulas/${aulaId}/dashboard${query ? `?${query}` : ""}`);
};
export const crearAula = (datos) =>
  pedir("/api/docente/aulas", { method: "POST", body: JSON.stringify(datos) });

// T-292: detalle del estudiante (endpoint de T-290). Incluye niveles,
// resumen por competencia y últimas evaluaciones en una sola llamada.
export const obtenerDetalleEstudiante = (aulaId, estudianteId) =>
  pedir(`/api/docente/aulas/${aulaId}/estudiantes/${estudianteId}/detalle`);

// T-299: evidencias pendientes del aula (endpoint de T-298).
// El filtro por tipo es opcional: "audio" o "texto".
export const listarEvidenciasPendientes = (aulaId, filtros = {}) => {
  const params = new URLSearchParams();
  if (filtros.tipo) params.set("tipo", filtros.tipo);
  const query = params.toString();
  return pedir(`/api/docente/aulas/${aulaId}/evidencias${query ? `?${query}` : ""}`);
};

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

// T-193: asigna una lectura del catálogo a un alumno concreto.
// El endpoint lo implementa T-192: POST .../asignaciones { lecturaId, estudianteId }
export const asignarLectura = (aulaId, datos) =>
  pedir(`/api/docente/aulas/${aulaId}/asignaciones`, {
    method: "POST",
    body: JSON.stringify(datos),
  });

// T-196: grupos del aula y asignación de una lectura a todo el grupo.
// El backend es T-195: GET/POST .../grupos y POST .../grupos/{id}/asignaciones
export const listarGrupos = (aulaId) =>
  pedir(`/api/docente/aulas/${aulaId}/grupos`);

export const crearGrupo = (aulaId, datos) =>
  pedir(`/api/docente/aulas/${aulaId}/grupos`, {
    method: "POST",
    body: JSON.stringify(datos),
  });

export const asignarLecturaAGrupo = (aulaId, grupoId, datos) =>
  pedir(`/api/docente/aulas/${aulaId}/grupos/${grupoId}/asignaciones`, {
    method: "POST",
    body: JSON.stringify(datos),
  });