// Sesión del docente: los tokens del login de Admin y los datos de la cuenta.
// Se guarda en sessionStorage: sobrevive a recargar la página, pero se borra al
// cerrar la pestaña, para no dejar la sesión abierta en computadoras compartidas.
import { seleccionarAula } from "./aulaSeleccionada";
import { limpiarFiltrosDashboard } from "./filtrosDashboard";

const CLAVE = "portal-docente.sesion";
const oyentes = new Set();

const leerGuardada = () => {
  try {
    return JSON.parse(window.sessionStorage.getItem(CLAVE));
  } catch {
    return null;
  }
};

const guardar = (sesion) => {
  try {
    if (sesion) window.sessionStorage.setItem(CLAVE, JSON.stringify(sesion));
    else window.sessionStorage.removeItem(CLAVE);
  } catch {
    // sin almacenamiento disponible la sesión vive solo en memoria
  }
};

let actual = leerGuardada();

const avisar = () => oyentes.forEach((oyente) => oyente());

export const obtenerSesion = () => actual;

export function suscribirSesion(oyente) {
  oyentes.add(oyente);
  return () => {
    oyentes.delete(oyente);
  };
}

// Entrar con una cuenta: lo que eligió el docente anterior no se hereda.
export function abrirSesion(sesion) {
  seleccionarAula(null);
  limpiarFiltrosDashboard();
  actual = sesion;
  guardar(actual);
  avisar();
}

export function actualizarToken(access) {
  if (!actual) return;
  actual = { ...actual, access };
  guardar(actual);
}

export function cerrarSesion() {
  actual = null;
  guardar(null);
  seleccionarAula(null);
  limpiarFiltrosDashboard();
  avisar();
}
