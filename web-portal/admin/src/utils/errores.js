// Convierte los errores 400 de DRF en { campo: "mensaje" }
export const erroresDeServidor = (datos) => {
  const errores = {};
  for (const [campo, msgs] of Object.entries(datos ?? {})) {
    errores[campo] = Array.isArray(msgs) ? msgs.join(" ") : String(msgs);
  }
  return errores;
};
