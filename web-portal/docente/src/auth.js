// Login docente: lo resuelve el backend docente con las cuentas que crea el
// administrador.
import { abrirSesion, actualizarToken, cerrarSesion, obtenerSesion } from "./sesion";

const BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8081";

const SIN_SERVIDOR =
  "No se pudo conectar con el servidor. Comprueba que esté encendido.";

async function enviar(ruta, cuerpo) {
  try {
    return await fetch(`${BASE_URL}${ruta}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(cuerpo),
    });
  } catch {
    throw new Error(SIN_SERVIDOR);
  }
}

export async function iniciarSesion(usuario, contrasena) {
  const res = await enviar("/api/auth/login", {
    username: usuario,
    password: contrasena,
  });
  const datos = await res.json().catch(() => null);
  if (!res.ok || !datos?.access) {
    throw new Error(datos?.mensaje || "No se pudo iniciar sesión. Inténtalo de nuevo.");
  }
  abrirSesion(datos);
}

let renovacion = null;

async function pedirTokenNuevo() {
  const sesion = obtenerSesion();
  if (!sesion?.refresh) return false;

  const res = await enviar("/api/auth/refresh", { refresh: sesion.refresh });
  if (res.status === 401) return false;
  const datos = await res.json().catch(() => null);
  if (!res.ok || !datos?.access) {
    // fallo pasajero: no se cierra la sesión
    throw new Error("No se pudo renovar tu sesión. Inténtalo de nuevo.");
  }
  actualizarToken(datos.access);
  return true;
}

// Renueva el token de acceso (dura 1 hora). Si varias peticiones lo piden a la
// vez se hace una sola llamada. Si ya no se puede, cierra la sesión.
export function renovarSesion() {
  if (!renovacion) {
    renovacion = pedirTokenNuevo()
      .then((renovada) => {
        if (!renovada) cerrarSesion();
        return renovada;
      })
      .finally(() => {
        renovacion = null;
      });
  }
  return renovacion;
}

// Cerrar sesión: sale del portal al momento y pide al servidor anular los tokens.
export async function salir() {
  const refresh = obtenerSesion()?.refresh;
  cerrarSesion();
  if (!refresh) return;
  try {
    await enviar("/api/auth/logout", { refresh });
  } catch {
    // sin conexión: los tokens vencen solos
  }
}
