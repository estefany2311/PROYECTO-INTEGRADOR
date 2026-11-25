import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import ReproductorAudio from "./ReproductorAudio";
import * as api from "../api";

vi.mock("../api");

const BLOB = new Blob(["audio"], { type: "audio/mpeg" });

const mostrar = (props = {}) =>
  render(
    <ReproductorAudio aulaId="10" evidenciaId={30} nombre="Minji Prueba" {...props} />
  );

describe("Reproductor de audio de la evidencia (T-373)", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    URL.createObjectURL = vi.fn(() => "blob:audio-30");
    URL.revokeObjectURL = vi.fn();
  });

  it("no descarga el audio hasta que el docente lo pide", () => {
    mostrar();

    expect(
      screen.getByRole("button", { name: "Escuchar audio" })
    ).toBeInTheDocument();
    expect(screen.queryByLabelText("Audio de Minji Prueba")).not.toBeInTheDocument();
    expect(api.descargarAudioEvidencia).not.toHaveBeenCalled();
  });

  it("descarga el audio con el header del docente y lo reproduce", async () => {
    api.descargarAudioEvidencia.mockResolvedValue(BLOB);
    mostrar();

    await userEvent.click(screen.getByRole("button", { name: "Escuchar audio" }));

    const audio = await screen.findByLabelText("Audio de Minji Prueba");
    expect(audio).toHaveAttribute("src", "blob:audio-30");
    expect(audio).toHaveAttribute("controls");
    expect(audio).toHaveAttribute("autoplay");
    expect(api.descargarAudioEvidencia).toHaveBeenCalledWith("10", 30);
    expect(URL.createObjectURL).toHaveBeenCalledWith(BLOB);
  });

  it("muestra el error del backend y permite reintentar", async () => {
    api.descargarAudioEvidencia
      .mockRejectedValueOnce(new Error("El audio no está disponible"))
      .mockResolvedValueOnce(BLOB);
    mostrar();

    await userEvent.click(screen.getByRole("button", { name: "Escuchar audio" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "El audio no está disponible"
    );

    await userEvent.click(screen.getByRole("button", { name: "Reintentar audio" }));

    expect(await screen.findByLabelText("Audio de Minji Prueba")).toHaveAttribute(
      "src",
      "blob:audio-30"
    );
    expect(api.descargarAudioEvidencia).toHaveBeenCalledTimes(2);
  });

  it("libera el blob al desmontar el reproductor", async () => {
    api.descargarAudioEvidencia.mockResolvedValue(BLOB);
    const { unmount } = mostrar();

    await userEvent.click(screen.getByRole("button", { name: "Escuchar audio" }));
    await screen.findByLabelText("Audio de Minji Prueba");

    unmount();

    expect(URL.revokeObjectURL).toHaveBeenCalledWith("blob:audio-30");
  });

  it("usa un nombre genérico cuando la evidencia no trae alumno", async () => {
    api.descargarAudioEvidencia.mockResolvedValue(BLOB);
    mostrar({ nombre: "" });

    await userEvent.click(screen.getByRole("button", { name: "Escuchar audio" }));

    expect(await screen.findByLabelText("Audio de la evidencia")).toBeInTheDocument();
  });
});
