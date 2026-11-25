// T-390 (US-38): utilidades de la fecha límite de las tareas.

// Fecha de hoy en formato AAAA-MM-DD según la hora local (no UTC), para el
// atributo min del campo de fecha: el backend no acepta fechas pasadas.
export const hoyISO = () => {
  const ahora = new Date();
  const mes = String(ahora.getMonth() + 1).padStart(2, "0");
  const dia = String(ahora.getDate()).padStart(2, "0");
  return `${ahora.getFullYear()}-${mes}-${dia}`;
};

// "2026-11-27" -> "27 nov 2026". Se arma con el texto para evitar que la zona
// horaria corra la fecha un día.
export const formatearFechaLimite = (valor) => {
  if (!valor) return "Sin fecha límite";
  const [anio, mes, dia] = String(valor).split("-").map(Number);
  if (!anio || !mes || !dia) return "Sin fecha límite";
  return new Intl.DateTimeFormat("es-PE", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  }).format(new Date(anio, mes - 1, dia));
};
