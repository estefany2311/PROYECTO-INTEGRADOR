import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import FormularioAula from "./FormularioAula";

// Se simula la API: no se llama al servidor real
vi.mock("./api", () => ({
  listarGrados: vi.fn().mockResolvedValue([{ id: 1, nombre: "1.º" }]),
  listarAulas: vi.fn().mockResolvedValue([
    { id: 1, nombre: "Aula Norte", codigo: "K7M2QX", grados: [{ id: 1, nombre: "1.º" }] },
  ]),
  crearAula: vi.fn(),
}));

describe("FormularioAula: código de aula", () => {
  it("muestra el código para los alumnos en la lista de aulas", async () => {
    render(<FormularioAula />);
    expect(await screen.findByText("K7M2QX")).toBeInTheDocument();
  });
});