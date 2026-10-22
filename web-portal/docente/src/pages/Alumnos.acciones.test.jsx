import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import * as api from "../api";
import Alumnos from "./Alumnos";

// Se simula toda la API: ninguna prueba llama al servidor real
vi.mock("../api", () => ({
  listarAulas: vi.fn(),
  listarEstudiantes: vi.fn(),
  crearEstudiante: vi.fn(),
  actualizarEstudiante: vi.fn(),
  cambiarEstadoEstudiante: vi.fn(),
}));

const AULAS = [{
  id: 1, nombre: "Aula Multigrado",
  grados: [{ id: 1, nombre: "1.º" }, { id: 2, nombre: "2.º" }],
}];
const ALUMNOS = [
  { id: 10, nombres: "Ana", apellidos: "Quispe", gradoId: 1, gradoNombre: "1.º", activo: true },
  { id: 11, nombres: "Luis", apellidos: "Mamani", gradoId: 2, gradoNombre: "2.º", activo: false },
];

const abrirPantalla = async () => {
  render(<MemoryRouter><Alumnos /></MemoryRouter>);
  await screen.findByRole("button", { name: "Editar Ana Quispe" }); // espera a que cargue la tabla
};

describe("Alumnos: acciones (T-183)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    api.listarAulas.mockResolvedValue(AULAS);
    api.listarEstudiantes.mockResolvedValue(ALUMNOS);
    api.actualizarEstudiante.mockResolvedValue({});
    api.cambiarEstadoEstudiante.mockResolvedValue({});
  });

  it("edita un alumno: el modal viene con sus datos y envía el PUT", async () => {
    const user = userEvent.setup();
    await abrirPantalla();

    await user.click(screen.getByRole("button", { name: "Editar Ana Quispe" }));
    expect(screen.getByLabelText("Nombres")).toHaveValue("Ana");

    await user.clear(screen.getByLabelText("Nombres"));
    await user.type(screen.getByLabelText("Nombres"), "Anita");
    await user.click(screen.getByRole("button", { name: "Guardar" }));

    expect(api.actualizarEstudiante).toHaveBeenCalledWith("1", 10, {
      nombres: "Anita", apellidos: "Quispe", gradoId: 1,
    });
    expect(await screen.findByText("Alumno actualizado correctamente")).toBeInTheDocument();
  });

  it("muestra el error del servidor al editar (ej. nombre repetido)", async () => {
    api.actualizarEstudiante.mockRejectedValue(new Error("Ya existe un alumno con ese nombre"));
    const user = userEvent.setup();
    await abrirPantalla();

    await user.click(screen.getByRole("button", { name: "Editar Ana Quispe" }));
    await user.click(screen.getByRole("button", { name: "Guardar" }));

    expect(await screen.findByText("Ya existe un alumno con ese nombre")).toBeInTheDocument();
  });

  it("desactiva solo después de confirmar", async () => {
    const user = userEvent.setup();
    await abrirPantalla();

    await user.click(screen.getByRole("button", { name: "Desactivar Ana Quispe" }));
    expect(api.cambiarEstadoEstudiante).not.toHaveBeenCalled(); // aún no: falta confirmar

    await user.click(screen.getByRole("button", { name: "Sí, desactivar" }));
    expect(api.cambiarEstadoEstudiante).toHaveBeenCalledWith("1", 10, false);
    expect(await screen.findByText("Alumno desactivado correctamente")).toBeInTheDocument();
  });

  it("cancelar la confirmación no llama a la API", async () => {
    const user = userEvent.setup();
    await abrirPantalla();

    await user.click(screen.getByRole("button", { name: "Desactivar Ana Quispe" }));
    await user.click(screen.getByRole("button", { name: "Cancelar" }));

    expect(api.cambiarEstadoEstudiante).not.toHaveBeenCalled();
  });

  it("reactiva a un alumno inactivo tras confirmar", async () => {
    const user = userEvent.setup();
    await abrirPantalla();

    await user.click(screen.getByRole("button", { name: "Reactivar Luis Mamani" }));
    await user.click(screen.getByRole("button", { name: "Sí, reactivar" }));

    expect(api.cambiarEstadoEstudiante).toHaveBeenCalledWith("1", 11, true);
  });
});