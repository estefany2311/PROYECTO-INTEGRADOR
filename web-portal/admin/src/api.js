const API = import.meta.env.VITE_API_URL ?? "http://127.0.0.1:8000/api";

export const sesion = {
  get token() {
    return sessionStorage.getItem("access");
  },
  get usuario() {
    return sessionStorage.getItem("usuario") ?? "";
  },
  guardar(token, usuario = "") {
    sessionStorage.setItem("access", token);
    sessionStorage.setItem("usuario", usuario);
  },
  limpiar() {
    sessionStorage.removeItem("access");
    sessionStorage.removeItem("usuario");
  },
};

async function pedir(ruta, opciones = {}) {
  const headers = { "Content-Type": "application/json" };
  if (sesion.token) headers.Authorization = `Bearer ${sesion.token}`;

  let resp;
  try {
    resp = await fetch(`${API}${ruta}`, { ...opciones, headers });
  } catch {
    throw { estado: 0, mensaje: "No se pudo conectar con el servidor." };
  }

  const datos = await resp.json().catch(() => null);
  if (!resp.ok) throw { estado: resp.status, datos };
  return datos;
}

export async function iniciarSesion(username, password) {
  const datos = await pedir("/auth/login/", {
    method: "POST",
    body: JSON.stringify({ username, password }),
  });
  sesion.guardar(datos.access, username);
}

export const crearDocente = (docente) =>
  pedir("/docentes/", { method: "POST", body: JSON.stringify(docente) });

export const listarDocentes = () => pedir("/docentes/");

export const listarColegios = (params = {}) => {
  const filtros = Object.entries(params).filter(([, valor]) => valor);
  const query = new URLSearchParams(filtros).toString();
  return pedir(`/colegios/${query ? `?${query}` : ""}`);
};

export const crearColegio = (datos) =>
  pedir("/colegios/", { method: "POST", body: JSON.stringify(datos) });

export const actualizarColegio = (id, datos) =>
  pedir(`/colegios/${id}/`, { method: "PATCH", body: JSON.stringify(datos) });

export const asignarDocentes = (id, docentes) =>
  pedir(`/colegios/${id}/docentes/`, { method: "POST", body: JSON.stringify({ docentes }) });