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
});
