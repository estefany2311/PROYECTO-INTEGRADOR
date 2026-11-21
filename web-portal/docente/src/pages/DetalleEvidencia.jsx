import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  aprobarEvidencia,
  corregirEvidencia,
  obtenerDetalleEvidencia,
} from "../api";
import { seleccionarAula } from "../aulaSeleccionada";
import ReproductorAudio from "../components/ReproductorAudio";

// T-374 (US-32): detalle de la evidencia con el contenido del alumno y la
// transcripción de la IA lado a lado, más el puntaje y el resultado. Consume el
// endpoint de T-371 y reutiliza el reproductor seguro de T-373.
const ESTADOS = {
  pendiente: {
    texto: "Pendiente",
    clase: "pendiente",
    icono: "bi-hourglass-split",
  },
  aprobada: {
    texto: "Aprobada",
    clase: "aprobada",
    icono: "bi-check-circle-fill",
  },
  rechazada: {
    texto: "Rechazada",
    clase: "rechazada",
    icono: "bi-x-circle-fill",
  },
  corregida: {
    texto: "Corregida",
    clase: "corregida",
    icono: "bi-pencil-square",
  },
};

const TIPOS = {
  audio: { texto: "Audio", icono: "bi-mic-fill" },
  texto: { texto: "Texto", icono: "bi-pencil-fill" },
};

const COLORES_AVATAR = ["#8B5CF6", "#2FC5F6", "#F59E0B", "#EC4899", "#10B981", "#6366F1"];

const estadoDe = (clave) => ESTADOS[clave] || ESTADOS.pendiente;
const tipoDe = (clave) => TIPOS[clave] || { texto: clave || "Evidencia", icono: "bi-file-earmark-text-fill" };

const nombreCompleto = (evidencia) =>
  [evidencia?.nombres, evidencia?.apellidos].filter(Boolean).join(" ").trim();

const iniciales = (nombres = "", apellidos = "") => {
  const primera = nombres.trim().charAt(0);
  const segunda = apellidos.trim().charAt(0);
  return `${primera}${segunda}`.toUpperCase() || "?";
};

const colorAvatar = (id) =>
  COLORES_AVATAR[Number(id || 0) % COLORES_AVATAR.length];

const formatearFecha = (valor) => {
  if (!valor) return "—";
  const fecha = new Date(valor);
  if (Number.isNaN(fecha.getTime())) return "—";
  return new Intl.DateTimeFormat("es-PE", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(fecha);
};

const anchoBarra = (valor) => {
  if (valor == null) return 0;
  return Math.min(100, Math.max(0, Math.round(valor)));
};

// T-375 (US-32): resalta las palabras que la IA marcó. Se comparan sin
// distinguir mayúsculas ni tildes y se conservan los separadores del texto.
const SEPARADORES = /(\s+|[.,;:!?¡¿"«»()])/;

const esPalabraMarcada = (parte, palabras) =>
  palabras.some(
    (palabra) => palabra.localeCompare(parte, "es", { sensitivity: "base" }) === 0
  );

const resaltarPalabras = (texto, palabras) => {
  if (!texto || !palabras?.length) return texto;
  return texto.split(SEPARADORES).map((parte, indice) =>
    esPalabraMarcada(parte, palabras) ? (
      <mark key={indice} className="detalle-palabra-marcada">
        {parte}
      </mark>
    ) : (
      parte
    )
  );
};

const textoMarcadas = (total) =>
  `La IA marcó ${total} ${total === 1 ? "palabra" : "palabras"} para revisar.`;

export default function DetalleEvidencia() {
  const { aulaId, evidenciaId } = useParams();
  const [detalle, setDetalle] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [recarga, setRecarga] = useState(0);

  // T-381 (US-33): aprobar la nota de la IA o corregirla con un formulario.
  const [accion, setAccion] = useState(null); // "aprobar" | "corregir"
  const [formCorreccion, setFormCorreccion] = useState(false);
  const [nota, setNota] = useState("");
  const [motivo, setMotivo] = useState("");
  const [erroresForm, setErroresForm] = useState({});
  const [mensaje, setMensaje] = useState(null);

  // Mantiene el aula activa del menú alineada con la evidencia abierta.
  useEffect(() => {
    if (aulaId) seleccionarAula(aulaId);
  }, [aulaId]);

  useEffect(() => {
    if (!aulaId || !evidenciaId) return;
    let vigente = true;

    obtenerDetalleEvidencia(aulaId, evidenciaId)
      .then((data) => {
        if (vigente) setDetalle(data);
      })
      .catch((e) => {
        if (vigente) setError(e.message);
      })
      .finally(() => {
        if (vigente) setCargando(false);
      });

    return () => {
      vigente = false;
    };
  }, [aulaId, evidenciaId, recarga]);

  const reintentar = () => {
    setDetalle(null);
    setCargando(true);
    setError(null);
    setMensaje(null);
    setFormCorreccion(false);
    setRecarga((n) => n + 1);
  };

  // T-381: aprobar solo tiene sentido si la IA ya dejó una nota.
  const puedeAprobar = detalle?.puntajeIa != null;

  const aprobar = async () => {
    setMensaje(null);
    setAccion("aprobar");
    try {
      const actualizado = await aprobarEvidencia(aulaId, evidenciaId);
      setDetalle(actualizado);
      setMensaje({
        tipo: "ok",
        texto: "La evidencia quedó aprobada con la nota de la IA.",
      });
    } catch (e) {
      setMensaje({ tipo: "error", texto: e.message });
    } finally {
      setAccion(null);
    }
  };

  const abrirCorreccion = () => {
    setFormCorreccion(true);
    setNota(String(detalle?.calificacionFinal ?? detalle?.puntajeIa ?? ""));
    setMotivo("");
    setErroresForm({});
    setMensaje(null);
  };

  const validarCorreccion = () => {
    const nuevos = {};
    const numero = Number(nota);
    if (nota === "" || !Number.isInteger(numero) || numero < 0 || numero > 100) {
      nuevos.nota = "Escribe una calificación entera entre 0 y 100.";
    }
    if (!motivo.trim()) {
      nuevos.motivo = "Debes indicar el motivo de la corrección.";
    }
    setErroresForm(nuevos);
    return Object.keys(nuevos).length === 0;
  };

  const corregir = async (event) => {
    event.preventDefault();
    setMensaje(null);
    if (!validarCorreccion()) return;

    setAccion("corregir");
    try {
      const actualizado = await corregirEvidencia(aulaId, evidenciaId, {
        calificacionFinal: Number(nota),
        motivo: motivo.trim(),
      });
      setDetalle(actualizado);
      setFormCorreccion(false);
      setMensaje({
        tipo: "ok",
        texto: "La calificación quedó corregida y guardada.",
      });
    } catch (e) {
      setMensaje({ tipo: "error", texto: e.message });
    } finally {
      setAccion(null);
    }
  };

  const nombre = nombreCompleto(detalle);
  const tipo = tipoDe(detalle?.tipo);
  const estado = estadoDe(detalle?.estadoRevision);
  const esAudio = detalle?.tipo === "audio";
  const tieneTexto = Boolean(detalle?.texto);
  const palabras = detalle?.palabrasMarcadas || [];
  const tituloIzquierda = tieneTexto
    ? "Texto original"
    : esAudio
      ? "Audio original"
      : "Contenido del alumno";
  const meta = detalle
    ? [
        detalle.gradoNombre,
        detalle.nivelNombre
          ? `${detalle.nivelOrden}. ${detalle.nivelNombre}`
          : null,
        tipo.texto,
      ]
        .filter(Boolean)
        .join(" · ")
    : "";

  return (
    <div className="detalle-page">
      <section
        className="dashboard-hero detalle-hero"
        aria-labelledby="titulo-evidencia"
      >
        <div className="detalle-hero-main">
          <span
            className="detalle-avatar"
            style={{ background: colorAvatar(detalle?.estudianteId || evidenciaId) }}
            aria-hidden="true"
          >
            {detalle ? iniciales(detalle.nombres, detalle.apellidos) : "?"}
          </span>
          <div>
            <Link to="/evidencias" className="detalle-volver">
              <i className="bi bi-arrow-left" aria-hidden="true" />
              Volver a evidencias
            </Link>
            <div className="dashboard-eyebrow">Detalle de la evidencia</div>
            <h1 id="titulo-evidencia" className="dashboard-titulo detalle-titulo">
              {nombre || "Evidencia del alumno"}
            </h1>
            <p className="dashboard-sub">
              {detalle
                ? `${meta} · ${formatearFecha(detalle.creadoEn)}`
                : "Contenido del alumno y evaluación de la IA."}
            </p>
          </div>
        </div>

        <div className="detalle-evidencia-acciones">
          {detalle && (
            <span className={`detalle-estado ${estado.clase}`}>
              <i className={`bi ${estado.icono}`} aria-hidden="true" />
              {estado.texto}
            </span>
          )}
          <button
            type="button"
            className="btn btn-outline-secondary rounded-pill dashboard-refresh"
            onClick={reintentar}
            disabled={cargando}
          >
            {cargando ? (
              <span className="spinner-border spinner-border-sm" aria-hidden="true" />
            ) : (
              <i className="bi bi-arrow-clockwise" aria-hidden="true" />
            )}
            {cargando ? "Actualizando…" : "Actualizar"}
          </button>
        </div>
      </section>

      {error ? (
        <div
          className="alert alert-danger d-flex justify-content-between align-items-center gap-2 flex-wrap"
          role="alert"
        >
          <span>{error}</span>
          <div className="d-flex gap-2 flex-wrap">
            <button
              type="button"
              className="btn btn-sm btn-outline-danger rounded-pill"
              onClick={reintentar}
            >
              Reintentar
            </button>
            <Link
              to="/evidencias"
              className="btn btn-sm btn-outline-secondary rounded-pill"
            >
              Volver a evidencias
            </Link>
          </div>
        </div>
      ) : cargando && !detalle ? (
        <div className="panel text-center py-5">
          <span
            className="spinner-border text-warning"
            role="status"
            aria-hidden="true"
          />
          <p className="text-secondary mt-3 mb-0">Cargando la evidencia…</p>
        </div>
      ) : detalle ? (
        <>
          <section
            className="detalle-evidencia-comparacion"
            aria-label="Contenido del alumno y transcripción de la IA"
          >
            <article
              className="panel detalle-evidencia-columna"
              aria-labelledby="titulo-original"
            >
              <div className="detalle-evidencia-head">
                <h2 id="titulo-original" className="h5 fw-bold mb-0">
                  {tituloIzquierda}
                </h2>
                <span className={`detalle-tipo ${detalle.tipo || ""}`}>
                  <i className={`bi ${tipo.icono}`} aria-hidden="true" />
                  {tipo.texto}
                </span>
              </div>

              {esAudio && (
                <ReproductorAudio
                  aulaId={aulaId}
                  evidenciaId={evidenciaId}
                  nombre={nombre}
                />
              )}

              {tieneTexto && palabras.length > 0 && (
                <p className="detalle-marcadas-aviso">
                  <i className="bi bi-highlighter" aria-hidden="true" />
                  {textoMarcadas(palabras.length)}
                </p>
              )}

              {tieneTexto && (
                <blockquote className="detalle-evidencia-texto">
                  {resaltarPalabras(detalle.texto, palabras)}
                </blockquote>
              )}

              {!esAudio && !tieneTexto && (
                <p className="detalle-evidencia-aviso">
                  <i className="bi bi-info-circle" aria-hidden="true" />
                  Esta evidencia no tiene contenido registrado.
                </p>
              )}
            </article>

            <article
              className="panel detalle-evidencia-columna"
              aria-labelledby="titulo-transcripcion"
            >
              <div className="detalle-evidencia-head">
                <h2 id="titulo-transcripcion" className="h5 fw-bold mb-0">
                  Transcripción de la IA
                </h2>
                <span className="detalle-tipo ia">
                  <i className="bi bi-stars" aria-hidden="true" />
                  IA
                </span>
              </div>

              {detalle.transcripcion && palabras.length > 0 && (
                <p className="detalle-marcadas-aviso">
                  <i className="bi bi-highlighter" aria-hidden="true" />
                  {textoMarcadas(palabras.length)}
                </p>
              )}

              {detalle.transcripcion ? (
                <blockquote className="detalle-evidencia-texto ia">
                  {resaltarPalabras(detalle.transcripcion, palabras)}
                </blockquote>
              ) : (
                <p className="detalle-evidencia-aviso">
                  <i className="bi bi-info-circle" aria-hidden="true" />
                  {esAudio
                    ? "La IA todavía no generó la transcripción del audio."
                    : "La IA evaluó el texto directamente; esta evidencia no tiene transcripción."}
                </p>
              )}
            </article>
          </section>

          <section
            className="panel detalle-evidencia-ia"
            aria-label="Evaluación de la IA"
          >
            <div className="detalle-evidencia-ia-head">
              <div>
                <h2 className="h5 fw-bold mb-1">Evaluación de la IA</h2>
                <p className="text-secondary mb-0 small">
                  {detalle.resultadoIa
                    ? "Retroalimentación automática para contrastar con el contenido."
                    : "La IA todavía no emitió una retroalimentación."}
                </p>
              </div>

              <div
                className="detalle-puntaje-ia"
                aria-label={`Puntaje de la IA: ${detalle.puntajeIa ?? "sin puntaje"}`}
              >
                <b>{detalle.puntajeIa ?? "—"}</b>
                <span>/100</span>
              </div>
            </div>

            <div className="detalle-puntaje-barra" aria-hidden="true">
              <span style={{ width: `${anchoBarra(detalle.puntajeIa)}%` }} />
            </div>

            <p className="detalle-evidencia-resultado">
              <i className="bi bi-chat-quote-fill" aria-hidden="true" />
              {detalle.resultadoIa || "La IA todavía no generó un resultado."}
            </p>

            <dl className="detalle-evidencia-meta">
              <div>
                <dt>Estado</dt>
                <dd>
                  <span className={`detalle-estado ${estado.clase}`}>
                    {estado.texto}
                  </span>
                </dd>
              </div>
              <div>
                <dt>Registrada</dt>
                <dd>{formatearFecha(detalle.creadoEn)}</dd>
              </div>
              <div>
                <dt>Revisada</dt>
                <dd>
                  {detalle.revisadoEn
                    ? formatearFecha(detalle.revisadoEn)
                    : "Aún no revisada"}
                </dd>
              </div>
            </dl>
          </section>

          <section
            className="panel detalle-evidencia-revision"
            aria-labelledby="titulo-revision"
          >
            <div className="detalle-evidencia-ia-head">
              <div>
                <h2 id="titulo-revision" className="h5 fw-bold mb-1">
                  Revisión del docente
                </h2>
                <p className="text-secondary mb-0 small">
                  Acepta la nota de la IA o escribe la tuya con un motivo.
                </p>
              </div>

              <div
                className="detalle-revision-nota"
                aria-label={
                  detalle.calificacionFinal != null
                    ? `Nota final: ${detalle.calificacionFinal}`
                    : "Sin nota final"
                }
              >
                <b>{detalle.calificacionFinal ?? "—"}</b>
                <span>/100</span>
              </div>
            </div>

            {detalle.motivoCorreccion && (
              <p className="detalle-revision-motivo">
                <i className="bi bi-chat-left-text" aria-hidden="true" />
                <span>
                  <b>Motivo:</b> {detalle.motivoCorreccion}
                </span>
              </p>
            )}

            <div className="detalle-revision-acciones">
              <button
                type="button"
                className="btn btn-success rounded-pill"
                onClick={aprobar}
                disabled={!puedeAprobar || accion !== null}
              >
                {accion === "aprobar" ? (
                  <span
                    className="spinner-border spinner-border-sm"
                    aria-hidden="true"
                  />
                ) : (
                  <i className="bi bi-check2-circle" aria-hidden="true" />
                )}
                {accion === "aprobar"
                  ? "Aprobando…"
                  : "Aprobar nota de la IA"}
              </button>

              <button
                type="button"
                className="btn btn-outline-primary rounded-pill"
                onClick={abrirCorreccion}
                disabled={accion !== null || formCorreccion}
              >
                <i className="bi bi-pencil-square" aria-hidden="true" />
                Corregir nota
              </button>
            </div>

            {!puedeAprobar && (
              <p className="detalle-evidencia-aviso">
                <i className="bi bi-info-circle" aria-hidden="true" />
                La IA todavía no evaluó esta evidencia; no hay nota que aprobar.
              </p>
            )}

            {formCorreccion && (
              <form className="detalle-revision-form" onSubmit={corregir} noValidate>
                <div className="detalle-revision-campo">
                  <label htmlFor="calificacion-final" className="form-label">
                    Calificación final{" "}
                    <span className="text-secondary fw-normal">(0 a 100)</span>
                  </label>
                  <input
                    id="calificacion-final"
                    type="number"
                    min="0"
                    max="100"
                    step="1"
                    className={`form-control ${
                      erroresForm.nota ? "is-invalid" : ""
                    }`}
                    value={nota}
                    onChange={(event) => setNota(event.target.value)}
                    aria-invalid={Boolean(erroresForm.nota)}
                    aria-describedby={
                      erroresForm.nota ? "error-calificacion-final" : undefined
                    }
                  />
                  {erroresForm.nota && (
                    <p
                      id="error-calificacion-final"
                      className="detalle-revision-error"
                    >
                      {erroresForm.nota}
                    </p>
                  )}
                </div>

                <div className="detalle-revision-campo">
                  <label htmlFor="motivo-correccion" className="form-label">
                    Motivo de la corrección
                  </label>
                  <textarea
                    id="motivo-correccion"
                    className={`form-control ${
                      erroresForm.motivo ? "is-invalid" : ""
                    }`}
                    rows="3"
                    maxLength="500"
                    value={motivo}
                    onChange={(event) => setMotivo(event.target.value)}
                    aria-invalid={Boolean(erroresForm.motivo)}
                    aria-describedby={
                      erroresForm.motivo ? "error-motivo-correccion" : undefined
                    }
                  />
                  <div className="detalle-revision-contador">
                    {erroresForm.motivo ? (
                      <p
                        id="error-motivo-correccion"
                        className="detalle-revision-error"
                      >
                        {erroresForm.motivo}
                      </p>
                    ) : (
                      <span />
                    )}
                    <span className="text-secondary small">
                      {motivo.length}/500
                    </span>
                  </div>
                </div>

                <div className="detalle-revision-acciones">
                  <button
                    type="submit"
                    className="btn btn-primary rounded-pill"
                    disabled={accion !== null}
                  >
                    {accion === "corregir"
                      ? "Guardando…"
                      : "Guardar corrección"}
                  </button>
                  <button
                    type="button"
                    className="btn btn-outline-secondary rounded-pill"
                    onClick={() => setFormCorreccion(false)}
                    disabled={accion !== null}
                  >
                    Cancelar
                  </button>
                </div>
              </form>
            )}

            {mensaje && (
              <div
                className={`alert ${
                  mensaje.tipo === "ok" ? "alert-success" : "alert-danger"
                } mb-0`}
                role={mensaje.tipo === "ok" ? "status" : "alert"}
              >
                {mensaje.texto}
              </div>
            )}
          </section>
        </>
      ) : null}
    </div>
  );
}
