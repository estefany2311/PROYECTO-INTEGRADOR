// Componente: BarraSemaforo
// Padre: Reportes
// Recibe (props): semaforo ({ verde, amarillo, rojo, sin_evaluar })
// Avisa al padre con: — (solo muestra)
import { ETIQUETAS_SEMAFORO } from "../utils/semaforo";

export default function BarraSemaforo({ semaforo }) {
  const total = Object.values(semaforo).reduce((a, b) => a + b, 0);
  if (total === 0) return <span className="note">Sin alumnos</span>;
  const titulo = Object.entries(semaforo)
    .map(([color, n]) => `${ETIQUETAS_SEMAFORO[color]}: ${n}`)
    .join(" · ");
  return (
    <div className="semaforo" title={titulo}>
      <div className="semaforo-barra">
        {Object.entries(semaforo).map(([color, n]) =>
          n > 0 ? <span key={color} className={`tramo tramo-${color}`} style={{ width: `${(n / total) * 100}%` }}></span> : null
        )}
      </div>
      <div className="semaforo-numeros">
        <span className="t-verde">{semaforo.verde}</span>
        <span className="t-amarillo">{semaforo.amarillo}</span>
        <span className="t-rojo">{semaforo.rojo}</span>
        <span className="t-gris">{semaforo.sin_evaluar}</span>
      </div>
    </div>
  );
}
