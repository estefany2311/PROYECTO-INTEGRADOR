import { useEffect, useState } from "react";
import { listarLibros } from "../api";

// Biblioteca: los libros oficiales del Minedu que carga el administrador.
// Desde aquí el docente elegirá más adelante las páginas para su catálogo.
const GRADOS = [1, 2, 3, 4, 5, 6];

const textoLibros = (n) => `${n} ${n === 1 ? "libro" : "libros"}`;

export default function Biblioteca() {
  const [grado, setGrado] = useState("");
  const [libros, setLibros] = useState(null);
  const [error, setError] = useState(null);
  const [recarga, setRecarga] = useState(0);

  useEffect(() => {
    let vigente = true;

    listarLibros(grado ? { grado } : {})
      .then((data) => {
        if (vigente) setLibros(data || []);
      })
      .catch((e) => {
        if (vigente) setError(e.message);
      });

    return () => {
      vigente = false;
    };
  }, [grado, recarga]);

  const reiniciar = () => {
    setLibros(null);
    setError(null);
  };

  const cambiarGrado = (valor) => {
    reiniciar();
    setGrado(valor);
  };

  const reintentar = () => {
    reiniciar();
    setRecarga((n) => n + 1);
  };

  return (
    <div className="biblioteca-page">
      <section className="dashboard-hero" aria-labelledby="titulo-biblioteca">
        <div>
          <div className="dashboard-eyebrow">Contenido</div>
          <h1 id="titulo-biblioteca" className="dashboard-titulo">
            Biblioteca
          </h1>
          <p className="dashboard-sub">
            Libros del Ministerio de Educación del Perú.
          </p>
        </div>

        <div className="dashboard-controles">
          <div>
            <label htmlFor="grado-biblioteca" className="visually-hidden">
              Grado
            </label>
            <select
              id="grado-biblioteca"
              className="form-select rounded-pill"
              value={grado}
              onChange={(ev) => cambiarGrado(ev.target.value)}
            >
              <option value="">Todos los grados</option>
              {GRADOS.map((g) => (
                <option key={g} value={g}>
                  {g}.º grado
                </option>
              ))}
            </select>
          </div>
        </div>
      </section>

      {error ? (
        <div
          className="alert alert-danger d-flex justify-content-between align-items-center gap-2 flex-wrap"
          role="alert"
        >
          <span>{error}</span>
          <button
            type="button"
            className="btn btn-sm btn-outline-danger rounded-pill"
            onClick={reintentar}
          >
            Reintentar
          </button>
        </div>
      ) : !libros ? (
        <div className="panel text-center py-5">
          <span className="spinner-border text-warning" role="status" aria-hidden="true" />
          <p className="text-secondary mt-3 mb-0">Cargando los libros…</p>
        </div>
      ) : libros.length === 0 ? (
        <div className="panel">
          <p className="detalle-vacio">
            <i className="bi bi-journals" aria-hidden="true" />
            {grado
              ? `Todavía no hay libros de ${grado}.º grado.`
              : "Todavía no hay libros disponibles."}
          </p>
        </div>
      ) : (
        <section className="panel" aria-label="Libros disponibles">
          <p className="text-secondary small">{textoLibros(libros.length)}</p>
          <ul className="biblioteca-lista">
            {libros.map((libro) => (
              <TarjetaLibro key={libro.id} libro={libro} />
            ))}
          </ul>
        </section>
      )}
    </div>
  );
}

function TarjetaLibro({ libro }) {
  return (
    <li>
      <article className="libro-card" aria-label={libro.titulo}>
        <span className="libro-portada" aria-hidden="true">
          <i className="bi bi-book-half" />
        </span>
        <h2 className="libro-titulo">{libro.titulo}</h2>
        <div className="d-flex flex-wrap gap-2">
          <span className="insignia">
            {libro.grado ? `${libro.grado}.º grado` : "Sin grado"}
          </span>
          {libro.competencia && (
            <span className="tag tag-neutro">{libro.competencia}</span>
          )}
          {!libro.tienePdf && <span className="tag tag-neutro">Sin PDF</span>}
        </div>
        {libro.descripcion && (
          <p className="libro-descripcion">{libro.descripcion}</p>
        )}
      </article>
    </li>
  );
}
