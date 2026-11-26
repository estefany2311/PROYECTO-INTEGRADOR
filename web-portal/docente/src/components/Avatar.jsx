import { colorAvatar } from "../theme/colores";

const inicial = (texto) => (texto || "").trim().charAt(0);

// Círculo con las iniciales de una persona. El tamaño y la forma los pone la
// clase de cada pantalla; el color sale del id, salvo que se indique otro.
// Es decorativo: el nombre completo siempre se muestra al lado.
export default function Avatar({ className, nombres, apellidos, colorId, color }) {
  const iniciales = `${inicial(nombres)}${inicial(apellidos)}`.toUpperCase() || "?";

  return (
    <span
      className={className}
      style={{ background: color || colorAvatar(colorId) }}
      aria-hidden="true"
    >
      {iniciales}
    </span>
  );
}
