import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { describe, it, expect, vi, beforeEach } from "vitest";
import Evidencias from "./Evidencias";
import * as api from "../api";
import { seleccionarAula, obtenerAulaSeleccionada } from "../aulaSeleccionada";

vi.mock("../api");

beforeEach(() => {
  seleccionarAula(null);
});

const AULA = { id: 10, nombre: "Aula Rural", grados: [{ id: 1, nombre: "1.º" }] };
const AULA2 = { id: 20, nombre: "Aula Costa", grados: [{ id: 2, nombre: "2.º" }] };

const AUDIO = {
  id: 30,
  estudianteId: 1,
  nombres: "Minji",
  apellidos: "Prueba",
  gradoId: 5,
  gradoNombre: "5.º",
  nivelOrden: 1,
  nivelNombre: "Primeros pasos",
  tipo: "audio",
  audioUrl: "https://cdn.ejemplo.pe/evidencias/minji-nivel1.mp3",
  texto: null,
  resultadoIa: "Lee con claridad y buen ritmo.",
  creadoEn: "2026-11-10T09:00:00",
};

const TEXTO = {
  id: 31,
  estudianteId: 2,
  nombres: "Breysi",
  apellidos: "Zavala",
  gradoId: 1,
  gradoNombre: "1.º",
  nivelOrden: 2,
  nivelNombre: "Palabras nuevas",
  tipo: "texto",
  audioUrl: null,
  texto: "Mi mamá prepara sopa. Yo pongo la mesa.",
  resultadoIa: "Escribe oraciones completas.",
  creadoEn: "2026-11-09T15:30:00",
};

const mostrar = () =>
  render(
    <MemoryRouter>
      <Evidencias />
    </MemoryRouter>
  );

const tarjetaDe = (nombre) =>
  screen.findByRole("article", { name: `Evidencia de ${nombre}` });

describe("Pantalla de evidencias pendientes (T-299)", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    api.listarAulas.mockResolvedValue([AULA]);
  });

  it("muestra las evidencias con alumno, nivel, tipo y resultado de la IA", async () => {
    api.listarEvidenciasPendientes.mockResolvedValue([AUDIO, TEXTO]);
    mostrar();

    expect(
      await screen.findByRole("heading", { name: "Evidencias pendientes" })
    ).toBeInTheDocument();

    const audio = await tarjetaDe("Minji Prueba");
    expect(api.listarEvidenciasPendientes).toHaveBeenCalledWith("10", {});
    expect(within(audio).getByText("Audio")).toBeInTheDocument();
    expect(within(audio).getByText(/Primeros pasos/)).toBeInTheDocument();
    expect(within(audio).getByText(/Lee con claridad y buen ritmo/)).toBeInTheDocument();
    expect(
      within(audio).getByRole("link", { name: "Escuchar audio" })
    ).toHaveAttribute("href", "https://cdn.ejemplo.pe/evidencias/minji-nivel1.mp3");
    expect(
      within(audio).getByRole("link", { name: "Ver detalle del alumno" })
    ).toHaveAttribute("href", "/dashboard/aulas/10/estudiantes/1");

    const texto = await tarjetaDe("Breysi Zavala");
    expect(within(texto).getByText("Texto")).toBeInTheDocument();
    expect(within(texto).getByText(/Palabras nuevas/)).toBeInTheDocument();
    expect(
      within(texto).getByText("Mi mamá prepara sopa. Yo pongo la mesa.")
    ).toBeInTheDocument();
    expect(
      within(texto).queryByRole("link", { name: "Escuchar audio" })
    ).not.toBeInTheDocument();

    expect(screen.getByText(/2 evidencias pendientes/)).toBeInTheDocument();
  });

  it("filtra por tipo y vuelve a consultar el endpoint", async () => {
    api.listarEvidenciasPendientes.mockResolvedValue([AUDIO, TEXTO]);
    mostrar();

    await tarjetaDe("Minji Prueba");

    api.listarEvidenciasPendientes.mockResolvedValue([TEXTO]);
    await userEvent.selectOptions(
      screen.getByLabelText("Tipo de evidencia"),
      "texto"
    );

    await waitFor(() =>
      expect(api.listarEvidenciasPendientes).toHaveBeenLastCalledWith("10", {
        tipo: "texto",
      })
    );

    expect(await tarjetaDe("Breysi Zavala")).toBeInTheDocument();
    expect(
      screen.queryByRole("article", { name: "Evidencia de Minji Prueba" })
    ).not.toBeInTheDocument();
  });

  it("muestra el estado vacío cuando no hay pendientes", async () => {
    api.listarEvidenciasPendientes.mockResolvedValue([]);
    mostrar();

    expect(
      await screen.findByText("No hay evidencias pendientes de revisión.")
    ).toBeInTheDocument();
  });

  it("explica el estado vacío del filtro por tipo", async () => {
    api.listarEvidenciasPendientes.mockResolvedValue([AUDIO]);
    mostrar();

    await tarjetaDe("Minji Prueba");

    api.listarEvidenciasPendientes.mockResolvedValue([]);
    await userEvent.selectOptions(
      screen.getByLabelText("Tipo de evidencia"),
      "texto"
    );

    expect(
      await screen.findByText("No hay evidencias de texto pendientes de revisión.")
    ).toBeInTheDocument();
  });

  it("muestra el error del endpoint y permite reintentar", async () => {
    api.listarEvidenciasPendientes
      .mockRejectedValueOnce(new Error("El aula no pertenece al docente"))
      .mockResolvedValueOnce([AUDIO]);
    mostrar();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "El aula no pertenece al docente"
    );

    await userEvent.click(screen.getByRole("button", { name: "Reintentar" }));

    expect(await tarjetaDe("Minji Prueba")).toBeInTheDocument();
  });

  it("restaura el aula activa y la comparte con el menú", async () => {
    seleccionarAula("20");
    api.listarAulas.mockResolvedValue([AULA, AULA2]);
    api.listarEvidenciasPendientes.mockResolvedValue([]);
    mostrar();

    await waitFor(() =>
      expect(api.listarEvidenciasPendientes).toHaveBeenCalledWith("20", {})
    );
    expect(obtenerAulaSeleccionada()).toBe("20");
    expect(screen.getByLabelText("Aula")).toHaveValue("20");
  });

  it("cambia de aula y pide las evidencias del aula nueva", async () => {
    api.listarAulas.mockResolvedValue([AULA, AULA2]);
    api.listarEvidenciasPendientes.mockResolvedValue([AUDIO]);
    mostrar();

    await tarjetaDe("Minji Prueba");

    api.listarEvidenciasPendientes.mockResolvedValue([]);
    await userEvent.selectOptions(screen.getByLabelText("Aula"), "20");

    await waitFor(() =>
      expect(api.listarEvidenciasPendientes).toHaveBeenLastCalledWith("20", {})
    );
    expect(obtenerAulaSeleccionada()).toBe("20");
  });

  it("respeta el orden de la respuesta: de la más reciente a la más antigua", async () => {
    api.listarEvidenciasPendientes.mockResolvedValue([AUDIO, TEXTO]);
    mostrar();

    await tarjetaDe("Minji Prueba");

    const tarjetas = screen.getAllByRole("article");
    expect(tarjetas.map((tarjeta) => tarjeta.getAttribute("aria-label"))).toEqual([
      "Evidencia de Minji Prueba",
      "Evidencia de Breysi Zavala",
    ]);
  });

  it("avisa cuando la IA todavía no generó un resultado", async () => {
    api.listarEvidenciasPendientes.mockResolvedValue([
      { ...AUDIO, resultadoIa: null },
    ]);
    mostrar();

    const audio = await tarjetaDe("Minji Prueba");
    expect(
      within(audio).getByText(/todavía no generó un resultado\./)
    ).toBeInTheDocument();
  });

  it("resume cuántas evidencias hay de audio y de texto", async () => {
    api.listarEvidenciasPendientes.mockResolvedValue([
      AUDIO,
      { ...AUDIO, id: 32, estudianteId: 3, nombres: "Karla", apellidos: "Rojas" },
      TEXTO,
    ]);
    mostrar();

    await tarjetaDe("Minji Prueba");

    expect(screen.getByText(/2 audio · 1 texto/)).toBeInTheDocument();
    expect(screen.getByText(/3 evidencias pendientes/)).toBeInTheDocument();
  });

  it("vuelve a pedir la lista al pulsar Actualizar", async () => {
    api.listarEvidenciasPendientes.mockResolvedValue([AUDIO]);
    mostrar();

    await tarjetaDe("Minji Prueba");
    expect(api.listarEvidenciasPendientes).toHaveBeenCalledTimes(1);

    api.listarEvidenciasPendientes.mockResolvedValue([AUDIO, TEXTO]);
    await userEvent.click(screen.getByRole("button", { name: "Actualizar" }));

    await waitFor(() =>
      expect(api.listarEvidenciasPendientes).toHaveBeenCalledTimes(2)
    );
    expect(await tarjetaDe("Breysi Zavala")).toBeInTheDocument();
  });

  it("invita a registrar un aula cuando el docente no tiene ninguna", async () => {
    api.listarAulas.mockResolvedValue([]);
    mostrar();

    expect(
      await screen.findByText("Aún no tienes aulas registradas.")
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Registrar mi primera aula" })
    ).toHaveAttribute("href", "/aulas");
    expect(api.listarEvidenciasPendientes).not.toHaveBeenCalled();
  });

  it("vuelve a todos los tipos después de filtrar", async () => {
    api.listarEvidenciasPendientes.mockResolvedValue([AUDIO, TEXTO]);
    mostrar();

    await tarjetaDe("Minji Prueba");

    await userEvent.selectOptions(
      screen.getByLabelText("Tipo de evidencia"),
      "texto"
    );
    await waitFor(() =>
      expect(api.listarEvidenciasPendientes).toHaveBeenLastCalledWith("10", {
        tipo: "texto",
      })
    );

    await userEvent.selectOptions(
      screen.getByLabelText("Tipo de evidencia"),
      ""
    );

    await waitFor(() =>
      expect(api.listarEvidenciasPendientes).toHaveBeenLastCalledWith("10", {})
    );
    expect(await tarjetaDe("Minji Prueba")).toBeInTheDocument();
  });
});
