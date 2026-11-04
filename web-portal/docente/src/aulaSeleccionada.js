let aulaSeleccionada = null;
const oyentes = new Set();

export function obtenerAulaSeleccionada() {
  return aulaSeleccionada;
}

export function seleccionarAula(aulaId) {
  const id = aulaId == null ? null : String(aulaId);
  if (aulaSeleccionada === id) return;
  aulaSeleccionada = id;
  oyentes.forEach((oyente) => oyente(aulaSeleccionada));
}

export function refrescarAulaSeleccionada() {
  oyentes.forEach((oyente) => oyente(aulaSeleccionada));
}

export function suscribirAulaSeleccionada(oyente) {
  oyentes.add(oyente);
  return () => {
    oyentes.delete(oyente);
  };
}
