const FILTROS_INICIALES = {
  gradoId: "",
  competencia: "",
};

let filtrosDashboard = { ...FILTROS_INICIALES };

export function obtenerFiltrosDashboard() {
  return { ...filtrosDashboard };
}

export function guardarFiltrosDashboard(nuevos) {
  filtrosDashboard = { ...filtrosDashboard, ...nuevos };
}

export function limpiarFiltrosDashboard() {
  filtrosDashboard = { ...FILTROS_INICIALES };
}
