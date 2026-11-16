// Datos compartidos por el Catálogo y sus componentes (US-39, US-40, US-41)

export const TIPOS_RETO = [
  { valor: "lectura", texto: "Lectura", icono: "bi-book" },
  { valor: "escritura", texto: "Escritura", icono: "bi-pencil-square" },
  { valor: "oralidad", texto: "Oralidad", icono: "bi-mic" },
];

export const DIFICULTADES = [
  { valor: "basica", texto: "Básica", clase: "bs-ok" },
  { valor: "intermedia", texto: "Intermedia", clase: "bs-warn" },
  { valor: "avanzada", texto: "Avanzada", clase: "bs-off" },
];

export const GRADOS = [1, 2, 3, 4, 5, 6];

export const LECTURA_VACIA = { titulo: "", tipo_reto: "lectura", grado: "1", dificultad: "basica", texto: "" };

export const FUENTE_OFICIAL = "Biblioteca Escolar Digital – Ministerio de Educación del Perú";
export const MAX_PAGINAS = 10;

export const tipoDe = (valor) => TIPOS_RETO.find((t) => t.valor === valor) ?? { texto: valor, icono: "bi-journal" };
export const dificultadDe = (valor) => DIFICULTADES.find((d) => d.valor === valor) ?? { texto: valor, clase: "bs-info" };
