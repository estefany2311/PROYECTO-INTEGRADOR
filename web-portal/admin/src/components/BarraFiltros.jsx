// Componente: BarraFiltros
// Padre: Reportes
// Recibe (props): colegios (para el selector), valores ({colegio, grado, desde, hasta} aplicados)
// Avisa al padre con: onAplicar({colegio, grado, desde, hasta})
// Hijos: —
import { useState } from "react";
import { GRADOS } from "../utils/catalogo";

const VACIO = { colegio: "", grado: "", desde: "", hasta: "" };

// Fecha de hoy en formato AAAA-MM-DD (hora local, no UTC)
function hoyLocal() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

// Las mismas reglas del backend (T-355)
function erroresDe({ desde, hasta }) {
  const hoy = hoyLocal();
  const errores = {};
  if (desde && desde > hoy) errores.desde = "La fecha desde no puede ser posterior a hoy.";
  if (hasta && hasta > hoy) errores.hasta = "La fecha hasta no puede ser posterior a hoy.";
  if (desde && hasta && desde > hasta && !errores.hasta) {
    errores.hasta = "La fecha hasta no puede ser anterior a la fecha desde.";
  }
  return errores;
}

export default function BarraFiltros({ colegios, valores, onAplicar }) {
  // Lo que el admin va eligiendo; recién se envía al padre con "Aplicar"
  const [borrador, setBorrador] = useState({ ...VACIO, ...valores });
  const errores = erroresDe(borrador);
  const hayErrores = Object.keys(errores).length > 0;
  const hoy = hoyLocal();

  const cambiar = (e) => setBorrador({ ...borrador, [e.target.name]: e.target.value });

  const aplicar = (e) => {
    e.preventDefault();
    if (!hayErrores) onAplicar(borrador);
  };

  const limpiar = () => {
    setBorrador(VACIO);
    onAplicar(VACIO);
  };

  return (
    <form className="card barra-filtros mb-4" onSubmit={aplicar} noValidate>
      <div className="card-body d-flex flex-wrap align-items-start gap-3">
        <div>
          <label className="form-label" htmlFor="f-colegio">Colegio</label>
          <select id="f-colegio" name="colegio" className="form-select" value={borrador.colegio} onChange={cambiar}>
            <option value="">Todos los colegios</option>
            {colegios.map((c) => <option key={c.colegio_id} value={c.colegio_id}>{c.nombre}</option>)}
          </select>
        </div>
        <div>
          <label className="form-label" htmlFor="f-grado">Grado</label>
          <select id="f-grado" name="grado" className="form-select" value={borrador.grado} onChange={cambiar}>
            <option value="">Todos los grados</option>
            {GRADOS.map((g) => <option key={g} value={g}>{g}.° grado</option>)}
          </select>
        </div>
        <div>
          <label className="form-label" htmlFor="f-desde">Desde</label>
          <input id="f-desde" type="date" name="desde" max={hoy}
            className={`form-control${errores.desde ? " is-invalid" : ""}`}
            value={borrador.desde} onChange={cambiar} />
          {errores.desde && <div className="invalid-feedback">{errores.desde}</div>}
        </div>
        <div>
          <label className="form-label" htmlFor="f-hasta">Hasta</label>
          <input id="f-hasta" type="date" name="hasta" max={hoy} min={borrador.desde || undefined}
            className={`form-control${errores.hasta ? " is-invalid" : ""}`}
            value={borrador.hasta} onChange={cambiar} />
          {errores.hasta && <div className="invalid-feedback">{errores.hasta}</div>}
        </div>
        <div className="botones-filtros">
          <button type="submit" className="btn btn-primary" disabled={hayErrores}>
            <i className="bi bi-funnel me-1"></i>Aplicar
          </button>
          <button type="button" className="btn btn-light" onClick={limpiar}>Limpiar</button>
        </div>
      </div>
    </form>
  );
}