// Tarjeta con un número destacado. `tono` es morado, celeste o rojo.
export default function StatCard({ icono, tono, valor, etiqueta }) {
  return (
    <div className="panel stat">
      <span className={`stat-ico stat-ico-${tono}`}>
        <i className={`bi ${icono}`} />
      </span>
      <div>
        <b>{valor}</b>
        <span>{etiqueta}</span>
      </div>
    </div>
  );
}
