// Componente: ListaColegios
// Padre: Colegios
// Recibe (props): colegios (la lista ya filtrada por el buscador), puede (permisos)
// Avisa al padre con: onAsignar(colegio), onEditar(colegio), onAlternar(colegio)
// Hijos: FilaColegio (una por cada colegio)
import FilaColegio from "./FilaColegio";

export default function ListaColegios({ colegios, puede, onAsignar, onEditar, onAlternar }) {
  return (
    <div className="table-responsive">
      <table className="table table-hover">
        <thead>
          <tr>
            <th>Código modular</th>
            <th>Colegio</th>
            <th>Región / Distrito</th>
            <th>Docentes asignados</th>
            <th>Estado</th>
            <th className="text-end">Acciones</th>
          </tr>
        </thead>
        <tbody>
          {colegios.map((c) => (
            <FilaColegio key={c.id} colegio={c} puede={puede}
              onAsignar={onAsignar} onEditar={onEditar} onAlternar={onAlternar} />
          ))}
        </tbody>
      </table>
    </div>
  );
}
