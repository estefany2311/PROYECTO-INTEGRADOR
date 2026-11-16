// Componente: ResumenColegios
// Padre: Colegios (hermano de ListaColegios: los dos reciben la misma lista)
// Recibe (props): colegios, cargando
// Avisa al padre con: —
// Hijos: —

// Datos derivados: se calculan de la lista en cada render, sin useState
export default function ResumenColegios({ colegios, cargando }) {
  if (cargando) return <span className="ms-auto note">Cargando...</span>;

  const total = colegios.length;
  const activos = colegios.filter((c) => c.activo).length;
  const inactivos = total - activos;
  const sinDocentes = colegios.filter((c) => c.total_docentes === 0).length;

  return (
    <div className="ms-auto resumen-colegios">
      <span className="note">{total} {total === 1 ? "colegio" : "colegios"}</span>
      <span className="badge-soft bs-ok">{activos} {activos === 1 ? "activo" : "activos"}</span>
      <span className="badge-soft bs-off">{inactivos} {inactivos === 1 ? "inactivo" : "inactivos"}</span>
      <span className="badge-soft bs-warn">{sinDocentes} sin docentes</span>
    </div>
  );
}
