import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { describe, it, expect, vi, beforeEach } from "vitest";
import Tareas from "./Tareas";
import * as api from "../api";
import { seleccionarAula } from "../aulaSeleccionada";

vi.mock("../api");

const AULA = { id: 10, nombre: "Aula Rural" };

const VENCIDA = {
  id: 2,
  lecturaId: 7,
  lecturaTitulo: "El zorro y el cuervo",
  estudianteId: 5,
  nombres: "Ana",
  apellidos: "Quispe",
  gradoNombre: "1.º",
  fechaAsignacion: "2026-11-10T09:00:00",
  fechaLimite: "2026-11-15",
  estado: "vencida",
  entregadaEn: null,
  entregadaATiempo: null,
};

const PENDIENTE = {
  ...VENCIDA,
  id: 3,
  estudianteId: 6,
  nombres: "Luis",
  apellidos: "Mamani",
  fechaLimite: null,
  estado: "pendiente",
};

const ENTREGADA = {
  ...VENCIDA,
  id: 1,
  estudianteId: 7,
  nombres: "Rosa",
  apellidos: "Ccori",
  fechaLimite: "2026-11-20",
  estado: "entregada",
  entregadaEn: "2026-11-22T10:00:00",
  entregadaATiempo: false,
};

const SEGUIMIENTO = {
  pendientes: 1,
  entregadas: 1,
  vencidas: 1,
  tareas: [VENCIDA, PENDIENTE, ENTREGADA],
};

const mostrar = () =>
  render(
    <MemoryRouter>
      <Tareas />
    </MemoryRouter>
  );

beforeEach(() => {
  vi.resetAllMocks();
  seleccionarAula(null);
  api.listarAulas.mockResolvedValue([AULA]);
});

describe("Seguimiento de tareas (T-390)", () => {
  it("muestra el resumen y cada tarea con su estado", async () => {
    api.listarSeguimientoTareas.mockResolvedValue(SEGUIMIENTO);
    mostrar();

    expect(
      await screen.findByRole("heading", { name: "Seguimiento de tareas" })
    ).toBeInTheDocument();
    await waitFor(() =>
      expect(api.listarSeguimientoTareas).toHaveBeenCalledWith("10", {})
    );

    const resumen = await screen.findByRole("region", {
      name: "Resumen de tareas",
    });
    expect(within(resumen).getByText("Vencidas")).toBeInTheDocument();
    expect(within(resumen).getByText("Pendientes")).toBeInTheDocument();
    expect(within(resumen).getByText("Entregadas")).toBeInTheDocument();

    const vencida = screen.getByRole("article", {
      name: /Tarea de Ana Quispe/,
    });
    expect(vencida).toHaveClass("tarea-vencida");
    expect(within(vencida).getByText("Vencida")).toBeInTheDocument();
    expect(within(vencida).getByText(/15 nov.? 2026/)).toBeInTheDocument();

    const pendiente = screen.getByRole("article", {
      name: /Tarea de Luis Mamani/,
    });
    expect(pendiente).toHaveClass("tarea-pendiente");
    expect(within(pendiente).getByText(/Sin fecha límite/)).toBeInTheDocument();

    const entregada = screen.getByRole("article", {
      name: /Tarea de Rosa Ccori/,
    });
    expect(entregada).toHaveClass("tarea-entregada");
    expect(within(entregada).getByText(/fuera de plazo/)).toBeInTheDocument();
  });

  it("filtra por estado al elegirlo en la lista", async () => {
    api.listarSeguimientoTareas
      .mockResolvedValueOnce(SEGUIMIENTO)
      .mockResolvedValueOnce({ ...SEGUIMIENTO, tareas: [VENCIDA] });
    mostrar();

    await userEvent.selectOptions(
      await screen.findByLabelText("Estado de la tarea"),
      "vencida"
    );

    expect(api.listarSeguimientoTareas).toHaveBeenLastCalledWith("10", {
      estado: "vencida",
    });
    expect(
      await screen.findByRole("article", { name: /Tarea de Ana Quispe/ })
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("article", { name: /Tarea de Luis Mamani/ })
    ).not.toBeInTheDocument();
  });

  it("filtra al tocar una tarjeta del resumen y la vuelve a tocar para quitar el filtro", async () => {
    api.listarSeguimientoTareas.mockResolvedValue(SEGUIMIENTO);
    mostrar();

    const boton = await screen.findByRole("button", { name: /Entregadas/ });
    await userEvent.click(boton);
    expect(api.listarSeguimientoTareas).toHaveBeenLastCalledWith("10", {
      estado: "entregada",
    });

    await userEvent.click(await screen.findByRole("button", { name: /Entregadas/ }));
    expect(api.listarSeguimientoTareas).toHaveBeenLastCalledWith("10", {});
  });

  it("avisa cuando el aula todavía no tiene tareas", async () => {
    api.listarSeguimientoTareas.mockResolvedValue({
      pendientes: 0,
      entregadas: 0,
      vencidas: 0,
      tareas: [],
    });
    mostrar();

    expect(
      await screen.findByText("Todavía no asignaste tareas en esta aula.")
    ).toBeInTheDocument();
  });

  it("muestra el error del backend y permite reintentar", async () => {
    api.listarSeguimientoTareas
      .mockRejectedValueOnce(new Error("El aula no pertenece al docente"))
      .mockResolvedValueOnce(SEGUIMIENTO);
    mostrar();

    expect(
      await screen.findByText("El aula no pertenece al docente")
    ).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Reintentar" }));

    expect(
      await screen.findByRole("article", { name: /Tarea de Ana Quispe/ })
    ).toBeInTheDocument();
  });

  it("invita a registrar un aula cuando no hay ninguna", async () => {
    api.listarAulas.mockResolvedValue([]);
    mostrar();

    expect(
      await screen.findByText("Aún no tienes aulas registradas.")
    ).toBeInTheDocument();
    expect(api.listarSeguimientoTareas).not.toHaveBeenCalled();
  });
});
