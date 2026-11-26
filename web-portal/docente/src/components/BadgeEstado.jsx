// Etiqueta de estado. `className` es la clase base de la pantalla y `variante`
// la que le da el color; el ícono es opcional.
export default function BadgeEstado({ className, variante, icono, texto }) {
  return (
    <span className={`${className} ${variante}`}>
      {icono && <i className={`bi ${icono}`} aria-hidden="true" />}
      {texto}
    </span>
  );
}
