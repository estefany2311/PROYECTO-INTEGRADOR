// Ayudas para mostrar docentes y usuarios

export const inicialesDocente = (d) =>
  `${d.first_name?.[0] ?? ""}${d.last_name?.[0] ?? ""}`.toUpperCase() || d.username[0].toUpperCase();

export const nombreCompleto = (d) => `${d.first_name} ${d.last_name}`.trim() || d.username;
