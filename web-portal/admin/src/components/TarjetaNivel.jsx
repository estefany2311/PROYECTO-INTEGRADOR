// Componente: TarjetaNivel
// Padre: ReglasDesbloqueo
// Recibe (props): nivel (nivel del mapa con su regla), puntosMaximos (lo que da su tipo de reto),
//                 errores ({campo: mensaje}), conCambios, esUltimo, deshabilitado
// Avisa al padre con: onCambiar(nivelId, campo, valor)
// Hijos: —

const ICONOS = { lectura: "bi-book", escritura: "bi-pencil-square", oralidad: "bi-mic" };
const TIPOS = { lectura: "Lectura", escritura: "Escritura", oralidad: "Oralidad" };

export default function TarjetaNivel({ nivel, puntosMaximos, errores = {}, conCambios, esUltimo, deshabilitado, onCambiar }) {
  const n = nivel;
  const bloqueado = deshabilitado || !n.activo;

  return (
    <div className={`tarjeta-nivel card h-100${conCambios ? " con-cambios" : ""}${n.activo ? "" : " inactivo"}`}>
      <div className="card-body">
        <div className="d-flex align-items-center gap-2 mb-2">
          <span className={`numero-nivel reto-${n.tipo_reto}`}>{n.orden}</span>
          <div className="flex-grow-1">
            <div className="fw-bold">{n.nombre}</div>
            <div className="note"><i className={`bi ${ICONOS[n.tipo_reto]} me-1`}></i>{TIPOS[n.tipo_reto] ?? n.tipo_reto}</div>
          </div>
          {!n.activo ? (
            <span className="badge-soft bs-off">Desactivado</span>
          ) : n.personalizada ? (
            <span className="badge-soft bs-ok">Personalizada</span>
          ) : (
            <span className="badge-soft bs-warn">Por defecto</span>
          )}
        </div>

        {/* Estrellas mínimas: se eligen con un clic (0 = basta con terminar el reto) */}
        <div className="form-label fw-semibold mb-1">Estrellas mínimas</div>
        <div className="selector-estrellas" role="radiogroup" aria-label={`Estrellas mínimas del nivel ${n.orden}`}>
          {[0, 1, 2, 3].map((e) => (
            <button key={e} type="button" role="radio" aria-checked={Number(n.estrellas_minimas) === e}
              className={`opcion-estrella${Number(n.estrellas_minimas) === e ? " elegida" : ""}`}
              disabled={bloqueado} onClick={() => onCambiar(n.nivel_id, "estrellas_minimas", e)}>
              {e === 0 ? "0" : "★".repeat(e)}
            </button>
          ))}
        </div>
        {errores.estrellas_minimas && <div className="text-danger small mt-1">{errores.estrellas_minimas}</div>}

        <label className="form-label fw-semibold mt-3 mb-1" htmlFor={`puntaje-${n.nivel_id}`}>Puntaje mínimo</label>
        <div className="input-group input-group-sm">
          <input id={`puntaje-${n.nivel_id}`} type="number" min="0" max={puntosMaximos ?? 100} step="1"
            className={`form-control${errores.puntaje_minimo ? " is-invalid" : ""}`}
            value={n.puntaje_minimo} disabled={bloqueado}
            onChange={(e) => onCambiar(n.nivel_id, "puntaje_minimo", e.target.value)} />
          <span className="input-group-text">de {puntosMaximos ?? "—"} pts</span>
          {errores.puntaje_minimo && <div className="invalid-feedback">{errores.puntaje_minimo}</div>}
        </div>

        <div className="note mt-2">
          {esUltimo
            ? "Es el último nivel: su regla marca el nivel como completado."
            : `Para desbloquear el nivel ${n.orden + 1}.`}
        </div>
      </div>
    </div>
  );
}
