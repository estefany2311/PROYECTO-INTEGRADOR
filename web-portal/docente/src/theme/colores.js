// Colores que se consumen desde JavaScript (avatares, gráfico y estadísticas).
// Los valores viven en src/styles/tokens.css: aquí solo se referencian.
export const COLORES_AVATAR = [
  "var(--avatar-1)",
  "var(--avatar-2)",
  "var(--avatar-3)",
  "var(--avatar-4)",
  "var(--avatar-5)",
  "var(--avatar-6)",
];

export const COLORES_INICIALES = [
  "var(--inicial-1)",
  "var(--inicial-2)",
  "var(--inicial-3)",
  "var(--inicial-4)",
  "var(--inicial-5)",
  "var(--inicial-6)",
];

export const COLORES_COMPETENCIA = {
  lectura: "var(--competencia-lectura)",
  escritura: "var(--competencia-escritura)",
  oralidad: "var(--competencia-oralidad)",
};

export const colorAvatar = (id) =>
  COLORES_AVATAR[Number(id || 0) % COLORES_AVATAR.length];
