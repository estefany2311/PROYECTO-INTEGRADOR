// Componente: MatrizCalor
// Padre: MapaCalor
// Recibe (props): zonas (filas con sus promedios), competencias, seleccion ({zona, competencia})
// Avisa al padre con: onElegir(zona, competencia) al hacer clic en una celda
// Hijos: —
import { colorDe } from "../utils/semaforo";
const NOMBRES = { lectura: "Lectura", escritura: "Escritura", oralidad: "Oralidad", general: "Promedio" };



export default function MatrizCalor({ zonas, competencias, seleccion, onElegir }) {
  const columnas = [...competencias, "general"];

  return (
    <div className="table-responsive">
      <table className="matriz-calor">
        <thead>
          <tr>
            <th className="col-zona">Zona</th>
            {columnas.map((c) => <th key={c}>{NOMBRES[c] ?? c}</th>)}
          </tr>
        </thead>
        <tbody>
          {zonas.map((z) => (
            <tr key={z.zona}>
              <th scope="row" className="col-zona">
                <div className="fw-bold">{z.zona}</div>
                <div className="note">
                  {z.colegios} {z.colegios === 1 ? "colegio" : "colegios"} · {z.alumnos} {z.alumnos === 1 ? "alumno" : "alumnos"}
                </div>
              </th>
              {columnas.map((c) => {
                const valor = c === "general" ? z.promedio_general : z.promedios[c];
                const elegida = seleccion?.zona === z.zona && seleccion?.competencia === c;
                return (
                  <td key={c}>
                    <button
                      type="button"
                      className={`celda-calor ${colorDe(valor)}${elegida ? " elegida" : ""}`}
                      title={`${z.zona} · ${NOMBRES[c]}: ${valor ?? "sin datos"}`}
                      onClick={() => onElegir(z.zona, c)}
                    >
                      {valor ?? "—"}
                    </button>
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}