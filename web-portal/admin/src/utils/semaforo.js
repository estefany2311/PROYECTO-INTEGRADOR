// Rangos del semáforo: los mismos del backend (reportes/consultas.py)

export const colorDe = (promedio) => {
  if (promedio === null || promedio === undefined) return "sin_evaluar";
  if (promedio >= 75) return "verde";
  if (promedio >= 50) return "amarillo";
  return "rojo";
};

export const ETIQUETAS_SEMAFORO = {
  verde: "Logrado (75 o más)",
  amarillo: "En proceso (50 a 74)",
  rojo: "En inicio (menos de 50)",
  sin_evaluar: "Sin evaluar",
};

// US-48 · T-352: escala de 5 tonos del mapa de calor (de menor a mayor)
export const ESCALA_CALOR = [
  { clase: "n1", desde: 0, hasta: 44, texto: "Necesita apoyo" },
  { clase: "n2", desde: 45, hasta: 59, texto: "En proceso" },
  { clase: "n3", desde: 60, hasta: 69, texto: "Regular" },
  { clase: "n4", desde: 70, hasta: 84, texto: "Bien" },
  { clase: "n5", desde: 85, hasta: 100, texto: "Muy bien" },
];
export const SIN_DATOS_CALOR = { clase: "n0", texto: "Sin datos" };

// Tono de un promedio. Con decimales: 44,5 sigue siendo "Necesita apoyo" (es menor que 45)
export const nivelCalor = (valor) => {
  if (valor === null || valor === undefined) return SIN_DATOS_CALOR;
  return ESCALA_CALOR.find((n) => valor < n.hasta + 1) ?? ESCALA_CALOR[ESCALA_CALOR.length - 1];
};
