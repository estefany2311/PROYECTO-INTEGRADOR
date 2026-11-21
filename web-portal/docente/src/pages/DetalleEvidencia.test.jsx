import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, it, expect, vi, beforeEach } from "vitest";
import DetalleEvidencia from "./DetalleEvidencia";
import * as api from "../api";
import { seleccionarAula, obtenerAulaSeleccionada } from "../aulaSeleccionada";

vi.mock("../api");

const AUDIO = {
  id: 30,
  estudianteId: 1,
  nombres: "Minji",
  apellidos: "Prueba",
  gradoId: 5,
  gradoNombre: "5.º",
  nivelOrden: 1,
  nivelNombre: "Primeros pasos",
  tipoReto: "lectura",
  tipo: "audio",
  audioUrl: "https://cdn.ejemplo.pe/evidencias/minji-nivel1.mp3",
  texto: null,
  transcripcion: "El sol brilla. Los niños juegan en el parque.",
  palabrasMarcadas: ["brilla", "juegan"],
  puntajeIa: 85,
  resultadoIa: "Lee con claridad y buen ritmo.",
  estadoRevision: "pendiente",
  creadoEn: "2026-11-10T09:00:00",
  revisadoEn: null,
};

const TEXTO = {
  id: 29,
  estudianteId: 2,
  nombres: "Breysi",
  apellidos: "Zavala",
  gradoId: 1,
  gradoNombre: "1.º",
  nivelOrden: 2,
  nivelNombre: "Palabras nuevas",
  tipoReto: "escritura",
  tipo: "texto",
  audioUrl: null,
  texto: "Mi mamá prepara sopa. Yo pongo la mesa.",
  transcripcion: null,
  palabrasMarcadas: [],
  puntajeIa: 80,
  resultadoIa: "Escribe oraciones completas.",
  estadoRevision: "aprobada",
  creadoEn: "2026-11-09T15:30:00",
  revisadoEn: "2026-11-10T08:00:00",
};

const mostrar = (aulaId = "10", evidenciaId = "30") =>
  render(
    <MemoryRouter initialEntries={[`/evidencias/${aulaId}/${evidenciaId}`]}>
      <Routes>
        <Route
          path="/evidencias/:aulaId/:evidenciaId"
          element={<DetalleEvidencia />}
        />
        <Route path="/evidencias" element={<p>Lista de evidencias</p>} />
      </Routes>
    </MemoryRouter>
  );

beforeEach(() => {
  vi.resetAllMocks();
  seleccionarAula(null);
});

describe("Detalle de la evidencia (T-374)", () => {
  it("muestra el audio y la transcripción de la IA lado a lado", async () => {
    api.obtenerDetalleEvidencia.mockResolvedValue(AUDIO);
    mostrar();

    expect(
      await screen.findByRole("heading", { name: "Minji Prueba" })
    ).toBeInTheDocument();
    expect(api.obtenerDetalleEvidencia).toHaveBeenCalledWith("10", "30");
    expect(obtenerAulaSeleccionada()).toBe("10");

    expect(
      screen.getByRole("heading", { name: "Audio original" })
    ).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: "Transcripción de la IA" })
    ).toBeInTheDocument();
    const comparacion = screen.getByRole("region", {
      name: "Contenido del alumno y transcripción de la IA",
    });
    expect(comparacion).toHaveTextContent(
      "El sol brilla. Los niños juegan en el parque."
    );
    expect(screen.getByText("brilla").tagName).toBe("MARK");
    expect(screen.getByText("juegan").tagName).toBe("MARK");
    expect(
      screen.getByText("La IA marcó 2 palabras para revisar.")
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Escuchar audio" })
    ).toBeInTheDocument();

    expect(screen.getByLabelText("Puntaje de la IA: 85")).toBeInTheDocument();
    expect(screen.getByText("Lee con claridad y buen ritmo.")).toBeInTheDocument();
    expect(screen.getAllByText("Pendiente").length).toBeGreaterThan(0);
    expect(screen.getByText("Aún no revisada")).toBeInTheDocument();

    expect(
      screen.getByRole("link", { name: "Volver a evidencias" })
    ).toHaveAttribute("href", "/evidencias");
  });

  it("resalta las palabras marcadas sin distinguir mayúsculas ni tildes", async () => {
    api.obtenerDetalleEvidencia.mockResolvedValue({
      ...AUDIO,
      transcripcion: "Mi mamá me ama. Yo la abrazo todos los días.",
      palabrasMarcadas: ["dias", "ABRAZO"],
    });
    mostrar();

    expect(
      await screen.findByRole("heading", { name: "Minji Prueba" })
    ).toBeInTheDocument();
    expect(screen.getByText("días").tagName).toBe("MARK");
    expect(screen.getByText("abrazo").tagName).toBe("MARK");
  });

  it("muestra el texto original del alumno y explica que no hay transcripción", async () => {
    api.obtenerDetalleEvidencia.mockResolvedValue(TEXTO);
    mostrar("10", "29");

    expect(
      await screen.findByRole("heading", { name: "Breysi Zavala" })
    ).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: "Texto original" })
    ).toBeInTheDocument();
    expect(
      screen.getByText("Mi mamá prepara sopa. Yo pongo la mesa.")
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        "La IA evaluó el texto directamente; esta evidencia no tiene transcripción."
      )
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Escuchar audio" })
    ).not.toBeInTheDocument();

    expect(screen.getByLabelText("Puntaje de la IA: 80")).toBeInTheDocument();
    expect(screen.getByText("Escribe oraciones completas.")).toBeInTheDocument();
    expect(screen.getAllByText("Aprobada").length).toBeGreaterThan(0);
    expect(screen.queryByText("Aún no revisada")).not.toBeInTheDocument();
    expect(screen.queryByText(/La IA marcó/)).not.toBeInTheDocument();
  });

  it("avisa cuando la IA todavía no transcribió el audio", async () => {
    api.obtenerDetalleEvidencia.mockResolvedValue({
      ...AUDIO,
      transcripcion: null,
    });
    mostrar();

    expect(
      await screen.findByText(
        "La IA todavía no generó la transcripción del audio."
      )
    ).toBeInTheDocument();
  });

  it("avisa cuando la IA todavía no tiene puntaje ni resultado", async () => {
    api.obtenerDetalleEvidencia.mockResolvedValue({
      ...AUDIO,
      puntajeIa: null,
      resultadoIa: null,
    });
    mostrar();

    expect(
      await screen.findByLabelText("Puntaje de la IA: sin puntaje")
    ).toBeInTheDocument();
    expect(
      screen.getByText("La IA todavía no generó un resultado.")
    ).toBeInTheDocument();
    expect(
      screen.getByText("La IA todavía no emitió una retroalimentación.")
    ).toBeInTheDocument();
  });

  it("muestra el error del backend y permite reintentar", async () => {
    api.obtenerDetalleEvidencia
      .mockRejectedValueOnce(new Error("La evidencia no existe en esta aula"))
      .mockResolvedValueOnce(AUDIO);
    mostrar();

    const alerta = await screen.findByRole("alert");
    expect(alerta).toHaveTextContent("La evidencia no existe en esta aula");
    expect(
      within(alerta).getByRole("link", { name: "Volver a evidencias" })
    ).toHaveAttribute("href", "/evidencias");

    await userEvent.click(screen.getByRole("button", { name: "Reintentar" }));

    expect(
      await screen.findByRole("heading", { name: "Minji Prueba" })
    ).toBeInTheDocument();
    expect(api.obtenerDetalleEvidencia).toHaveBeenCalledTimes(2);
  });

  it("muestra el estado rechazada de una evidencia ya revisada", async () => {
    api.obtenerDetalleEvidencia.mockResolvedValue({
      ...AUDIO,
      estadoRevision: "rechazada",
      revisadoEn: "2026-11-11T10:00:00",
    });
    mostrar();

    expect(
      await screen.findByRole("heading", { name: "Minji Prueba" })
    ).toBeInTheDocument();
    expect(screen.getAllByText("Rechazada").length).toBeGreaterThan(0);
    expect(screen.queryByText("Aún no revisada")).not.toBeInTheDocument();
  });

  it("actualiza el detalle al pulsar Actualizar", async () => {
    api.obtenerDetalleEvidencia
      .mockResolvedValueOnce(AUDIO)
      .mockResolvedValueOnce({
        ...AUDIO,
        estadoRevision: "aprobada",
        resultadoIa: "Revisado por la docente.",
        revisadoEn: "2026-11-11T10:00:00",
      });
    mostrar();

    expect(
      await screen.findByText("Lee con claridad y buen ritmo.")
    ).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Actualizar" }));

    expect(
      await screen.findByText("Revisado por la docente.")
    ).toBeInTheDocument();
    expect(screen.getAllByText("Aprobada").length).toBeGreaterThan(0);
    expect(api.obtenerDetalleEvidencia).toHaveBeenCalledTimes(2);
  });

  it("resalta las palabras marcadas en una evidencia de texto", async () => {
    api.obtenerDetalleEvidencia.mockResolvedValue({
      ...TEXTO,
      palabrasMarcadas: ["sopa", "mesa"],
    });
    mostrar("10", "29");

    expect(
      await screen.findByRole("heading", { name: "Breysi Zavala" })
    ).toBeInTheDocument();
    expect(screen.getByText("sopa").tagName).toBe("MARK");
    expect(screen.getByText("mesa").tagName).toBe("MARK");
    expect(
      screen.getByText("La IA marcó 2 palabras para revisar.")
    ).toBeInTheDocument();
  });

  it("tolera un detalle sin palabras marcadas", async () => {
    api.obtenerDetalleEvidencia.mockResolvedValue({
      ...AUDIO,
      palabrasMarcadas: undefined,
    });
    mostrar();

    expect(
      await screen.findByRole("heading", { name: "Minji Prueba" })
    ).toBeInTheDocument();
    expect(
      screen.getByText("El sol brilla. Los niños juegan en el parque.")
    ).toBeInTheDocument();
    expect(screen.queryByText(/La IA marcó/)).not.toBeInTheDocument();
  });
});

describe("Aprobar y corregir la evidencia (T-381)", () => {
  it("aprueba la nota de la IA y muestra el detalle actualizado", async () => {
    api.obtenerDetalleEvidencia.mockResolvedValue(AUDIO);
    api.aprobarEvidencia.mockResolvedValue({
      ...AUDIO,
      calificacionFinal: 85,
      estadoRevision: "aprobada",
      revisadoEn: "2026-11-12T10:00:00",
    });
    mostrar();

    const aprobar = await screen.findByRole("button", {
      name: "Aprobar nota de la IA",
    });
    expect(aprobar).toBeEnabled();
    expect(screen.getByLabelText("Sin nota final")).toBeInTheDocument();

    await userEvent.click(aprobar);

    expect(api.aprobarEvidencia).toHaveBeenCalledWith("10", "30");
    expect(await screen.findByRole("status")).toHaveTextContent(
      "La evidencia quedó aprobada con la nota de la IA."
    );
    expect(screen.getAllByText("Aprobada").length).toBeGreaterThan(0);
    expect(screen.getByLabelText("Nota final: 85")).toBeInTheDocument();
  });

  it("deshabilita Aprobar cuando la IA todavía no evaluó", async () => {
    api.obtenerDetalleEvidencia.mockResolvedValue({
      ...AUDIO,
      puntajeIa: null,
    });
    mostrar();

    expect(
      await screen.findByRole("button", { name: "Aprobar nota de la IA" })
    ).toBeDisabled();
    expect(
      screen.getByText(
        "La IA todavía no evaluó esta evidencia; no hay nota que aprobar."
      )
    ).toBeInTheDocument();
    expect(api.aprobarEvidencia).not.toHaveBeenCalled();
  });

  it("valida la nota y el motivo antes de corregir", async () => {
    api.obtenerDetalleEvidencia.mockResolvedValue(AUDIO);
    mostrar();

    await screen.findByRole("heading", { name: "Minji Prueba" });
    await userEvent.click(
      screen.getByRole("button", { name: "Corregir nota" })
    );

    const nota = screen.getByLabelText(/Calificación final/);
    expect(nota).toHaveValue(85);
    await userEvent.clear(nota);
    await userEvent.click(
      screen.getByRole("button", { name: "Guardar corrección" })
    );

    expect(
      screen.getByText("Escribe una calificación entera entre 0 y 100.")
    ).toBeInTheDocument();
    expect(
      screen.getByText("Debes indicar el motivo de la corrección.")
    ).toBeInTheDocument();
    expect(api.corregirEvidencia).not.toHaveBeenCalled();
  });

  it("corrige la calificación con la nota y el motivo", async () => {
    api.obtenerDetalleEvidencia.mockResolvedValue(AUDIO);
    api.corregirEvidencia.mockResolvedValue({
      ...AUDIO,
      calificacionFinal: 70,
      motivoCorreccion: "El audio tiene ruido de fondo.",
      estadoRevision: "corregida",
      revisadoEn: "2026-11-12T10:30:00",
    });
    mostrar();

    await screen.findByRole("heading", { name: "Minji Prueba" });
    await userEvent.click(
      screen.getByRole("button", { name: "Corregir nota" })
    );

    const nota = screen.getByLabelText(/Calificación final/);
    await userEvent.clear(nota);
    await userEvent.type(nota, "70");
    await userEvent.type(
      screen.getByLabelText("Motivo de la corrección"),
      "El audio tiene ruido de fondo."
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Guardar corrección" })
    );

    expect(api.corregirEvidencia).toHaveBeenCalledWith("10", "30", {
      calificacionFinal: 70,
      motivo: "El audio tiene ruido de fondo.",
    });
    expect(await screen.findByRole("status")).toHaveTextContent(
      "La calificación quedó corregida y guardada."
    );
    expect(screen.getAllByText("Corregida").length).toBeGreaterThan(0);
    expect(screen.getByLabelText("Nota final: 70")).toBeInTheDocument();
    expect(
      screen.getByText("El audio tiene ruido de fondo.")
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Guardar corrección" })
    ).not.toBeInTheDocument();
  });

  it("cancela la corrección sin llamar al backend", async () => {
    api.obtenerDetalleEvidencia.mockResolvedValue(AUDIO);
    mostrar();

    await screen.findByRole("heading", { name: "Minji Prueba" });
    await userEvent.click(
      screen.getByRole("button", { name: "Corregir nota" })
    );
    await userEvent.click(screen.getByRole("button", { name: "Cancelar" }));

    expect(
      screen.queryByRole("button", { name: "Guardar corrección" })
    ).not.toBeInTheDocument();
    expect(api.corregirEvidencia).not.toHaveBeenCalled();
  });

  it("muestra el error del backend y deja el formulario abierto", async () => {
    api.obtenerDetalleEvidencia.mockResolvedValue(AUDIO);
    api.corregirEvidencia.mockRejectedValue(
      new Error("El motivo no puede superar los 500 caracteres")
    );
    mostrar();

    await screen.findByRole("heading", { name: "Minji Prueba" });
    await userEvent.click(
      screen.getByRole("button", { name: "Corregir nota" })
    );
    await userEvent.type(
      screen.getByLabelText("Motivo de la corrección"),
      "Ruido"
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Guardar corrección" })
    );

    const alerta = await screen.findByRole("alert");
    expect(alerta).toHaveTextContent(
      "El motivo no puede superar los 500 caracteres"
    );
    expect(
      screen.getByRole("button", { name: "Guardar corrección" })
    ).toBeInTheDocument();
  });
});
