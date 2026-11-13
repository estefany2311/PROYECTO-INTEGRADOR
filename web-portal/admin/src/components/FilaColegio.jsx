// Componente: FilaColegio
// Padre: ListaColegios
// Recibe (props): colegio (un registro de la lista), puede (revisa los permisos del usuario)
// Avisa al padre con: onAsignar(colegio), onEditar(colegio), onAlternar(colegio)
// Hijos: —
import { inicialesDocente, nombreCompleto } from "../utils/personas";

// La fila solo dibuja un colegio: no cambia la lista, avisa al padre y él decide
export default function FilaColegio({ colegio, puede, onAsignar, onEditar, onAlternar }) {
  const c = colegio;
  return (
    <tr className={c.activo ? "" : "inactivo"}>
      <td>{c.codigo_modular}</td>
      <td className="fw-medium">{c.nombre}</td>
      <td>{c.region}{c.distrito ? ` / ${c.distrito}` : ""}</td>
      <td>
        {c.total_docentes > 0 ? (
          <span className="d-inline-flex align-items-center">
            <span className="avatar-group">
              {c.docentes.slice(0, 3).map((d) => (
                <span key={d.id} className="mini-avatar" title={nombreCompleto(d)}>{inicialesDocente(d)}</span>
              ))}
            </span>
            <span className="note ms-2">
              {c.total_docentes} {c.total_docentes === 1 ? "docente" : "docentes"}
            </span>
          </span>
        ) : (
          <span className="badge-soft bs-warn">Sin docentes</span>
        )}
      </td>
      <td>
        <span className={`badge-soft ${c.activo ? "bs-ok" : "bs-off"}`}>{c.activo ? "Activo" : "Inactivo"}</span>
      </td>
      <td className="text-end text-nowrap">
        {puede("colegios.editar") && (
          <button type="button" className="icon-btn" title="Asignar docentes" onClick={() => onAsignar(c)}>
            <i className="bi bi-person-plus"></i>
          </button>
        )}
        {puede("colegios.editar") && (
          <button type="button" className="icon-btn" title="Editar" onClick={() => onEditar(c)}>
            <i className="bi bi-pencil"></i>
          </button>
        )}
        {puede("colegios.desactivar") && (
          <button type="button" className="icon-btn" title={c.activo ? "Desactivar" : "Activar"} onClick={() => onAlternar(c)}>
            <i className={`bi ${c.activo ? "bi-slash-circle" : "bi-check-circle"}`}></i>
          </button>
        )}
      </td>
    </tr>
  );
}
