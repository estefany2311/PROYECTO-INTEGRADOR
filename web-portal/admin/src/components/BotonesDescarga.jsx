// Componente: BotonesDescarga
// Padre: Reportes
// Recibe (props): colegio (id elegido o ""), filtros ({grado, desde, hasta} aplicados)
// Avisa al padre con: onNoAutorizado()
// Hijos: —
import { useState } from "react";
import { descargarReporte } from "../api";

const FORMATOS = [
  { clave: "excel", texto: "Excel", icono: "bi-file-earmark-spreadsheet", extension: "xlsx" },
  { clave: "pdf", texto: "PDF", icono: "bi-file-earmark-pdf", extension: "pdf" },
];

// Fecha de hoy para el nombre del archivo (hora local)
function hoy() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

export default function BotonesDescarga({ colegio, filtros, onNoAutorizado }) {
  const [descargando, setDescargando] = useState(null);   // "excel" | "pdf" | null
  const [error, setError] = useState("");

  const descargar = async (formato) => {
    setDescargando(formato.clave);
    setError("");
    try {
      const archivo = await descargarReporte(formato.clave, colegio, filtros);
      // Se crea un enlace temporal al archivo y se "hace clic" para que el navegador lo guarde
      const url = URL.createObjectURL(archivo);
      const enlace = document.createElement("a");
      enlace.href = url;
      enlace.download = `reporte-colegios-${hoy()}.${formato.extension}`;
      enlace.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      if (err.estado === 401) return onNoAutorizado();
      const mensajes = err.datos ? Object.values(err.datos).flat().join(" ") : "";
      setError(mensajes || err.mensaje || "No se pudo generar el archivo.");
    } finally {
      setDescargando(null);
    }
  };

  return (
    <div className="botones-descarga text-end">
      <div className="d-flex gap-2 justify-content-end">
        {FORMATOS.map((f) => (
          <button key={f.clave} type="button" className="btn btn-light" disabled={descargando !== null}
            onClick={() => descargar(f)} title={`Descargar el reporte en ${f.texto} con los filtros aplicados`}>
            <i className={`bi ${f.icono} me-1`}></i>{descargando === f.clave ? "Generando…" : f.texto}
          </button>
        ))}
      </div>
      {error && <div className="text-danger small mt-1">{error}</div>}
    </div>
  );
}