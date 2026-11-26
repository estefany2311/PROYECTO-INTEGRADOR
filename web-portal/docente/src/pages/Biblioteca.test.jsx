import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import Biblioteca from "./Biblioteca";
import * as api from "../api";

vi.mock("../api");

const LIBRO = {
  id: 1,
  titulo: "Comunicación 1",
  descripcion: "Texto escolar de primaria.",
  grado: 1,
  competencia: "",
  tienePdf: true,
};
const SIN_PDF = { ...LIBRO, id: 2, titulo: "Cuentos", grado: null, tienePdf: false };

beforeEach(() => vi.resetAllMocks());

describe("Biblioteca", () => {
  it("muestra los libros con su grado", async () => {
    api.listarLibros.mockResolvedValue([LIBRO, SIN_PDF]);
    render(<Biblioteca />);

    const libro = await screen.findByRole("article", { name: "Comunicación 1" });
    expect(within(libro).getByText("1.º grado")).toBeInTheDocument();
    expect(within(libro).queryByText("Sin PDF")).not.toBeInTheDocument();

    const otro = screen.getByRole("article", { name: "Cuentos" });
    expect(within(otro).getByText("Sin grado")).toBeInTheDocument();
    expect(within(otro).getByText("Sin PDF")).toBeInTheDocument();
    expect(screen.getByText("2 libros")).toBeInTheDocument();
    expect(api.listarLibros).toHaveBeenCalledWith({});
  });

  it("filtra por grado", async () => {
    api.listarLibros.mockResolvedValueOnce([LIBRO]).mockResolvedValueOnce([]);
    render(<Biblioteca />);
    await screen.findByRole("article", { name: "Comunicación 1" });

    await userEvent.selectOptions(screen.getByLabelText("Grado"), "3");

    expect(api.listarLibros).toHaveBeenLastCalledWith({ grado: "3" });
    expect(
      await screen.findByText("Todavía no hay libros de 3.º grado.")
    ).toBeInTheDocument();
  });

  it("avisa cuando todavía no hay libros", async () => {
    api.listarLibros.mockResolvedValue([]);
    render(<Biblioteca />);

    expect(
      await screen.findByText("Todavía no hay libros disponibles.")
    ).toBeInTheDocument();
  });

  it("muestra el error y permite reintentar", async () => {
    api.listarLibros
      .mockRejectedValueOnce(new Error("No se pudo cargar"))
      .mockResolvedValueOnce([LIBRO]);
    render(<Biblioteca />);

    expect(await screen.findByText("No se pudo cargar")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Reintentar" }));

    expect(
      await screen.findByRole("article", { name: "Comunicación 1" })
    ).toBeInTheDocument();
  });
});
