// Componente: Promedio
// Padre: Reportes
// Recibe (props): valor (el promedio de 0 a 100, o vacío)
// Avisa al padre con: — (solo muestra)
import { colorDe } from "../utils/semaforo";

export default function Promedio({ valor }) {
  if (valor === null || valor === undefined) return <span className="note">—</span>;
  return (
    <span className="promedio">
      <span className={`punto punto-${colorDe(valor)}`}></span>
      {valor.toFixed(1)}
    </span>
  );
}
