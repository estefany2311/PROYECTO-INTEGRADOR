import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { describe, it, expect, vi, beforeEach } from "vitest";
import Alumnos from "./Alumnos";
import * as api from "../api";

vi.mock("../api");

const AULA = { id: 10, nombre: "Aula Rural", grados: [{ id: 1, nombre: "1.º" }] };
const AULA2 = { id: 20, nombre: "Aula Costa", grados: [{ id: 2, nombre: "2.º" }] };
const ANA = { id: 1, nombres: "Ana", apellidos: "Quispe", gradoId: 1, gradoNombre: "1.º", activo: true };

const mostrar = (state) =>
  render(
    <MemoryRouter initialEntries={[{ pathname: "/alumnos", state }]}>
      <Alumnos />
    </MemoryRouter>
  );

describe("Pantalla de alumnos", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    api.listarAulas.mockResolvedValue([AULA]);
  });

  it("muestra los alumnos del aula", async () => {
    api.listarEstudiantes.mockResolvedValue([ANA]);
    mostrar();
    expect(await screen.findByText("Ana Quispe")).toBeInTheDocument();
  });

  it("muestra un mensaje cuando el aula no tiene alumnos", async () => {
    api.listarEstudiantes.mockResolvedValue([]);
    mostrar();
    expect(await screen.findByText(/aún no hay alumnos/i)).toBeInTheDocument();
  });

  it("valida los campos obligatorios del formulario", async () => {
    api.listarEstudiantes.mockResolvedValue([]);
    mostrar();
    await userEvent.click(await screen.findByRole("button", { name: /agregar alumno/i }));
    await userEvent.click(screen.getByRole("button", { name: "Guardar" }));

    expect(await screen.findByText("Los nombres son obligatorios")).toBeInTheDocument();
    expect(screen.getByText("Los apellidos son obligatorios")).toBeInTheDocument();
    expect(screen.getByText("Selecciona el grado del alumno")).toBeInTheDocument();
    expect(api.crearEstudiante).not.toHaveBeenCalled();
  });

  it("registra un alumno nuevo", async () => {
    api.listarEstudiantes.mockResolvedValue([]);
    api.crearEstudiante.mockResolvedValue(ANA);
    mostrar();
    await userEvent.click(await screen.findByRole("button", { name: /agregar alumno/i }));

    await userEvent.type(screen.getByLabelText("Nombres"), "Ana");
    await userEvent.type(screen.getByLabelText("Apellidos"), "Quispe");
    await userEvent.selectOptions(screen.getByLabelText("Grado"), "1");
    await userEvent.click(screen.getByRole("button", { name: "Guardar" }));

    expect(api.crearEstudiante).toHaveBeenCalledWith("10", {
      nombres: "Ana",
      apellidos: "Quispe",
      gradoId: 1,
    });
    expect(await screen.findByText("Alumno registrado correctamente")).toBeInTheDocument();
  });

  // T-229: la lectura elegida en el catálogo llega a esta pantalla
  it("muestra la lectura elegida desde el catálogo", async () => {
    api.listarEstudiantes.mockResolvedValue([ANA]);
    mostrar({
      lectura: {
        id: 5,
        titulo: "El zorro y el cuervo",
        gradoNombre: "1.º",
        aulaId: "10",
        aulaNombre: "Aula Rural",
      },
    });

    expect(await screen.findByText(/lectura elegida/i)).toBeInTheDocument();
    expect(screen.getByText(/el zorro y el cuervo/i)).toBeInTheDocument();
    expect(screen.getByText(/catálogo de Aula Rural/i)).toBeInTheDocument();
  });

  it("abre el aula de la lectura elegida", async () => {
    api.listarAulas.mockResolvedValue([AULA, AULA2]);
    api.listarEstudiantes.mockResolvedValue([]);
    mostrar({ lectura: { id: 5, titulo: "El zorro y el cuervo", aulaId: "20" } });

    await waitFor(() => expect(api.listarEstudiantes).toHaveBeenCalledWith("20"));
    expect(screen.getByLabelText("Aula")).toHaveValue("20");
  });

  it("permite quitar el aviso de la lectura elegida", async () => {
    api.listarEstudiantes.mockResolvedValue([ANA]);
    mostrar({ lectura: { id: 5, titulo: "El zorro y el cuervo", aulaId: "10" } });

    await userEvent.click(
      await screen.findByRole("button", { name: /quitar lectura elegida/i })
    );

    expect(screen.queryByText(/lectura elegida/i)).not.toBeInTheDocument();
  });
});