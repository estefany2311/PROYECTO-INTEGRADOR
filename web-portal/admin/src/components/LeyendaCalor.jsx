// Componente: LeyendaCalor
// Padre: MapaCalor
// Recibe (props): —
// Avisa al padre con: —
// Hijos: —
import { ESCALA_CALOR, SIN_DATOS_CALOR } from "../utils/semaforo";

// T-352: leyenda de la escala. Cada tono lleva su rango y su nombre (no depende solo del color)
export default function LeyendaCalor() {
  return (
    <ul className="leyenda-calor" aria-label="Escala de colores del mapa de calor">
      {ESCALA_CALOR.map((n) => (
        <li key={n.clase}>
          <span className={`cuadro ${n.clase}`} aria-hidden="true"></span>
          <b>{n.desde}–{n.hasta}</b> {n.texto}
        </li>
      ))}
      <li>
        <span className={`cuadro ${SIN_DATOS_CALOR.clase}`} aria-hidden="true"></span>
        <b>—</b> {SIN_DATOS_CALOR.texto}
      </li>
    </ul>
  );
}