import { renovarSesion } from "./auth";
import { cerrarSesion, obtenerSesion } from "./sesion";

const BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8081";

const conToken = (cabeceras = {}) => {
  const token = obtenerSesion()?.access;
  return token ? { ...cabeceras, Authorization: `Bearer ${token}` } : cabeceras;
};

// Llama a la API con el token del docente. El backend saca de ese token quién es.
// Si el token venció, lo renueva y repite la petición una sola vez.
async function llamar(ruta, { headers, ...opciones } = {}) {
  const enviar = () =>
    fetch(`${BASE_URL}${ruta}`, { ...opciones, headers: conToken(headers) });

  let res = await enviar();
  if (res.status === 401 && (await renovarSesion())) {
    res = await enviar();
    if (res.status === 401) cerrarSesion();
  }
  return res;
}

async function pedir(ruta, opciones = {}) {
  const res = await llamar(ruta, {
    headers: { "Content-Type": "application/json" },
    ...opciones,
  });
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

// T-373: audio de una evidencia (endpoint de T-372). El endpoint exige el token
// del docente, que un <audio src> no puede enviar, así que el archivo se
// descarga con fetch y se reproduce desde un blob local.
export const descargarAudioEvidencia = async (aulaId, evidenciaId) => {
  const res = await llamar(
    `/api/docente/aulas/${aulaId}/evidencias/${evidenciaId}/audio`
  );
  if (!res.ok) {
    const datos = await res.json().catch(() => null);
    throw new Error(datos?.mensaje || "No se pudo cargar el audio");
  }
  return res.blob();
};

// T-374: detalle de una evidencia (endpoint de T-371). Trae el contenido del
// alumno, la transcripción de la IA, el puntaje y el resultado.
export const obtenerDetalleEvidencia = (aulaId, evidenciaId) =>
  pedir(`/api/docente/aulas/${aulaId}/evidencias/${evidenciaId}`);

// T-381: el docente acepta la nota de la IA (endpoint de T-378). No lleva
// cuerpo y devuelve el detalle actualizado.
export const aprobarEvidencia = (aulaId, evidenciaId) =>
  pedir(`/api/docente/aulas/${aulaId}/evidencias/${evidenciaId}/aprobar`, {
    method: "POST",
  });

// T-381: el docente cambia la nota de la IA y explica por qué (T-378). El
// backend valida la nota 0–100 y el motivo obligatorio de hasta 500 caracteres.
export const corregirEvidencia = (aulaId, evidenciaId, datos) =>
  pedir(`/api/docente/aulas/${aulaId}/evidencias/${evidenciaId}/corregir`, {
    method: "POST",
    body: JSON.stringify(datos),
  });

// T-385 (US-34): el docente guarda su comentario de retroalimentación (T-384).
// El backend lo exige no vacío y de hasta 500 caracteres; devuelve el detalle.
export const guardarComentarioEvidencia = (aulaId, evidenciaId, comentario) =>
  pedir(`/api/docente/aulas/${aulaId}/evidencias/${evidenciaId}/comentario`, {
    method: "PUT",
    body: JSON.stringify({ comentario }),
  });

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

// T-390 (US-38): seguimiento de las tareas del aula (endpoint de T-389).
// estado opcional: pendiente, entregada o vencida.
export const listarSeguimientoTareas = (aulaId, { estado } = {}) =>
  pedir(
    `/api/docente/aulas/${aulaId}/asignaciones${
      estado ? `?estado=${encodeURIComponent(estado)}` : ""
    }`
  );

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