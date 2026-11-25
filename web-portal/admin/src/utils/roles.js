// Ayudas compartidas por Roles y permisos y por AsignarRol

export const lista = (datos) => (Array.isArray(datos) ? datos : datos.results ?? []);

// Toma el primer mensaje de un error de DRF ({campo: [msg]} o {detail: msg})
export const mensajeDeError = (err, porDefecto) => {
  if (err.datos?.detail) return err.datos.detail;
  const primero = Object.values(err.datos ?? {})[0];
  if (primero) return Array.isArray(primero) ? primero.join(" ") : String(primero);
  return err.mensaje ?? porDefecto;
};
