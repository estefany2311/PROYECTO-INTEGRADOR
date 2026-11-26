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
    // Si se envía un archivo (FormData), el navegador pone solo el Content-Type correcto
  const headers = opciones.body instanceof FormData ? {} : { "Content-Type": "application/json" };
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

export const obtenerMiPerfil = () => pedir("/auth/me/");

export const listarPermisos = () => pedir("/permisos/");

export const listarRoles = () => pedir("/roles/");

export const actualizarPermisosRol = (id, permisos) =>
  pedir(`/roles/${id}/permisos/`, { method: "PUT", body: JSON.stringify({ permisos }) });

export const listarUsuarios = (search = "") =>
  pedir(`/usuarios/${search ? `?${new URLSearchParams({ search })}` : ""}`);

export const cambiarRolUsuario = (id, rol) =>
  pedir(`/usuarios/${id}/rol/`, { method: "PATCH", body: JSON.stringify({ rol }) });

export const listarLecturas = (params = {}) => {
  const filtros = Object.entries(params).filter(([, valor]) => valor);
  const query = new URLSearchParams(filtros).toString();
  return pedir(`/lecturas/${query ? `?${query}` : ""}`);
};

export const crearLectura = (datos) =>
  pedir("/lecturas/", { method: "POST", body: JSON.stringify(datos) });


export const eliminarLectura = (id) =>
  pedir(`/lecturas/${id}/`, { method: "DELETE" });


export const actualizarLectura = (id, datos) =>
  pedir(`/lecturas/${id}/`, { method: "PATCH", body: JSON.stringify(datos) });


// US-42: imágenes y audios de una lectura
export const listarRecursosLectura = (lecturaId) => pedir(`/lecturas/${lecturaId}/recursos/`);

export const subirRecursoLectura = (lecturaId, archivo, descripcion = "") => {
  const datos = new FormData();
  datos.append("archivo", archivo);
  datos.append("descripcion", descripcion);
  return pedir(`/lecturas/${lecturaId}/recursos/`, { method: "POST", body: datos });
};

export const eliminarRecursoLectura = (lecturaId, recursoId) =>
  pedir(`/lecturas/${lecturaId}/recursos/${recursoId}/`, { method: "DELETE" });

export const cambiarEstadoUsuario = (id, isActive) =>
  pedir(`/usuarios/${id}/estado/`, { method: "PATCH", body: JSON.stringify({ is_active: isActive }) });


export const listarConfiguracionEvaluacion = () => pedir("/configuracion/evaluacion/");

export const actualizarConfiguracionEvaluacion = (grado, datos) =>
  pedir(`/configuracion/evaluacion/${grado}/`, { method: "PATCH", body: JSON.stringify(datos) });



export const listarConfiguracionOrtografia = () => pedir("/configuracion/ortografia/");

export const actualizarConfiguracionOrtografia = (grado, datos) =>
  pedir(`/configuracion/ortografia/${grado}/`, { method: "PATCH", body: JSON.stringify(datos) });

export const listarPalabrasPermitidas = () => pedir("/configuracion/palabras/");

export const agregarPalabraPermitida = (palabra) =>
  pedir("/configuracion/palabras/", { method: "POST", body: JSON.stringify({ palabra }) });

export const quitarPalabraPermitida = (id) =>
  pedir(`/configuracion/palabras/${id}/`, { method: "DELETE" });


export const listarReglasPuntuacion = () => pedir("/configuracion/reglas/");

export const actualizarReglaPuntuacion = (tipoReto, datos) =>
pedir(`/configuracion/reglas/${tipoReto}/`, { method: "PATCH", body: JSON.stringify(datos) });

export const listarReglasDesbloqueo = () => pedir("/configuracion/desbloqueo/");

export const actualizarReglaDesbloqueo = (nivelId, datos) =>
pedir(`/configuracion/desbloqueo/${nivelId}/`, { method: "PATCH", body: JSON.stringify(datos) });


// US-49 · T-356: arma ?grado=&desde=&hasta= solo con los filtros que tienen valor
const parametrosReporte = (valores) => {
  const params = new URLSearchParams();
  Object.entries(valores).forEach(([clave, valor]) => { if (valor) params.set(clave, valor); });
  const texto = params.toString();
  return texto ? `?${texto}` : "";
};

export const obtenerReporteColegios = (colegio = "", filtros = {}) =>
  pedir(`/reportes/colegios/${parametrosReporte({ colegio, ...filtros })}`);


// US-50 · T-362: el Excel o el PDF del reporte, con los mismos filtros de la pantalla.
// Un enlace <a> no puede mandar el token, por eso se pide con fetch y se devuelve como archivo (blob)
export async function descargarReporte(formato, colegio = "", filtros = {}) {
  let resp;
  try {
    resp = await fetch(`${API}/reportes/colegios/${formato}/${parametrosReporte({ colegio, ...filtros })}`, {
      headers: sesion.token ? { Authorization: `Bearer ${sesion.token}` } : {},
    });
  } catch {
    throw { estado: 0, mensaje: "No se pudo conectar con el servidor." };
  }
  if (!resp.ok) throw { estado: resp.status, datos: await resp.json().catch(() => null) };
  return resp.blob();
}

export const obtenerReporteAulas = (colegioId, filtros = {}) =>
  pedir(`/reportes/colegios/${colegioId}/aulas/${parametrosReporte(filtros)}`);

// US-48 · Mapa de calor (T-350)
export const obtenerMapaCalor = ({ agrupar = "region", grado = "" } = {}) => {
  const params = new URLSearchParams({ agrupar });
  if (grado) params.set("grado", grado);
  return pedir(`/reportes/mapa-calor/?${params}`);
};

// US-39 · Libros oficiales de la Biblioteca Escolar Digital (T-443)
export const listarRecursosOficiales = () => pedir("/recursos-oficiales/");

// El admin activa o desactiva un libro: desactivado = los docentes ya no lo ven
export const cambiarEstadoLibro = (id, activo) =>
  pedir(`/recursos-oficiales/${id}/estado/`, { method: "PATCH", body: JSON.stringify({ activo }) });

export const sincronizarRecursos = () => pedir("/recursos-oficiales/sincronizar/", { method: "POST" });
