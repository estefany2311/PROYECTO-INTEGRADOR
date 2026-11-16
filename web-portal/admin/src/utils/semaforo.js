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
