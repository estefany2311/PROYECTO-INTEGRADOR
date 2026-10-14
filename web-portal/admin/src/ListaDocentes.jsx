import { useEffect, useState } from "react";
import { listarDocentes } from "./api";

export default function ListaDocentes({ recarga, onNoAutorizado }) {
  const [docentes, setDocentes] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let activo = true;
    listarDocentes()
      .then((datos) => {
        if (!activo) return;
        setDocentes(Array.isArray(datos) ? datos : datos.results ?? []);
        setError("");
      })
      .catch((err) => {
        if (!activo) return;
        if (err.estado === 401) onNoAutorizado();
        else setError(err.datos?.detail ?? err.mensaje ?? "No se pudo cargar la lista.");
      })
      .finally(() => {
        if (activo) setCargando(false);
      });
    return () => {
      activo = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [recarga]);

  return (
    <section className="lista">
      <h2>Docentes registrados</h2>
      {cargando && <p>Cargando...</p>}
      {error && <p className="error">{error}</p>}
      {!cargando && !error && docentes.length === 0 && <p>Aún no hay docentes registrados.</p>}
      {docentes.length > 0 && (
        <div className="tabla-scroll">
          <table>
            <thead>
              <tr>
                <th>Usuario</th>
                <th>Nombres</th>
                <th>Apellidos</th>
                <th>Correo</th>
                <th>Activo</th>
              </tr>
            </thead>
            <tbody>
              {docentes.map((d) => (
                <tr key={d.id}>
                  <td>{d.username}</td>
                  <td>{d.first_name}</td>
                  <td>{d.last_name}</td>
                  <td>{d.email}</td>
                  <td>{d.is_active ? "Sí" : "No"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}