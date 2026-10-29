// Mascota del portal: un koala original leyendo un libro (SVG propio del proyecto).
export default function Koala({ size = 160, saludando = false, className = "" }) {
  return (
    <svg
      className={className}
      width={size}
      height={size}
      viewBox="0 0 200 200"
      role="img"
      aria-label="Koala, la mascota de la plataforma"
    >
      {/* Orejas */}
      <circle cx="48" cy="62" r="34" fill="#9AA6B2" />
      <circle cx="48" cy="62" r="20" fill="#F6C6D0" />
      <circle cx="152" cy="62" r="34" fill="#9AA6B2" />
      <circle cx="152" cy="62" r="20" fill="#F6C6D0" />

      {/* Cuerpo */}
      <ellipse cx="100" cy="168" rx="52" ry="34" fill="#9AA6B2" />
      <ellipse cx="100" cy="172" rx="32" ry="22" fill="#D9DFE5" />

      {/* Cabeza */}
      <ellipse cx="100" cy="98" rx="60" ry="54" fill="#AEB8C2" />

      {/* Ojos */}
      <circle cx="76" cy="92" r="8" fill="#1D3557" />
      <circle cx="124" cy="92" r="8" fill="#1D3557" />
      <circle cx="79" cy="89" r="3" fill="#FFFFFF" />
      <circle cx="127" cy="89" r="3" fill="#FFFFFF" />

      {/* Nariz y sonrisa */}
      <ellipse cx="100" cy="112" rx="15" ry="18" fill="#3B4A5C" />
      <ellipse cx="95" cy="104" rx="4" ry="5" fill="#5A6B7F" />
      <path d="M90 134 Q100 142 110 134" stroke="#3B4A5C" strokeWidth="3.5" fill="none" strokeLinecap="round" />

      {/* Cachetes */}
      <circle cx="62" cy="118" r="9" fill="#F6A9B8" opacity="0.7" />
      <circle cx="138" cy="118" r="9" fill="#F6A9B8" opacity="0.7" />

      {/* Libro abierto (colores de la marca) */}
      <path d="M100 160 L58 150 L58 186 L100 196 Z" fill="#0E9F92" />
      <path d="M100 160 L142 150 L142 186 L100 196 Z" fill="#0A8378" />
      <path d="M100 162 L64 154 L64 182 L100 190 Z" fill="#FFFFFF" />
      <path d="M100 162 L136 154 L136 182 L100 190 Z" fill="#F4FAFF" />
      <path d="M72 162 L94 167 M72 170 L94 175 M106 167 L128 162 M106 175 L128 170"
        stroke="#B8C4D6" strokeWidth="2" strokeLinecap="round" />

      {/* Patitas sujetando el libro */}
      <ellipse cx="60" cy="166" rx="11" ry="9" fill="#9AA6B2" />
      {saludando ? (
        <g className="koala-mano">
          <ellipse cx="164" cy="128" rx="11" ry="13" fill="#9AA6B2" />
        </g>
      ) : (
        <ellipse cx="140" cy="166" rx="11" ry="9" fill="#9AA6B2" />
      )}

      {/* Estrellita naranja */}
      <path d="M170 30 L174 40 L185 41 L177 48 L179 59 L170 53 L161 59 L163 48 L155 41 L166 40 Z" fill="#FF9F1C" />
    </svg>
  );
}
