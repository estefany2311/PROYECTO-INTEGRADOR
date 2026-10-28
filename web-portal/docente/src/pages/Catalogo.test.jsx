import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import { describe, it, expect, vi, beforeEach } from "vitest";
import Catalogo from "./Catalogo";
import * as api from "../api";

vi.mock("../api");

const AULA = {
  id: 10,
  nombre: "Aula Rural",
  grados: [{ id: 1, nombre: "1.º" }, { id: 2, nombre: "2.º" }],
};

const ZORRO = {
  id: 5,
  titulo: "El zorro y el cuervo",
  texto: "Un zorro vio a un cuervo con un queso en el pico.",
  tipoReto: "lectura",
  dificultad: "basica",
  gradoId: 1,
  gradoNombre: "1.º",
  activo: true,
};

const CONDOR = {
  id: 6,
  titulo: "El cóndor y la luna",
  texto: "El cóndor subía muy alto cada mañana.",
  tipoReto: "comprension",
  dificultad: "avanzada",
  gradoId: 2,
  gradoNombre: "2.º",
  activo: true,
};

const mostrar = () =>
  render(
    <MemoryRouter>
      <Catalogo />
    </MemoryRouter>
  );

// pantalla de mentira que solo muestra lo que recibió por el state de la ruta
const Destino = () => {
  const { state } = useLocation();
  return (
    <div>
      destino: {state?.lectura?.titulo} en {state?.lectura?.aulaId}
    </div>
  );
};

describe("Pantalla de catálogo (T-190)", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    api.listarAulas.mockResolvedValue([AULA]);
  });

  it("muestra las lecturas que devuelve el backend", async () => {
    api.listarLecturas.mockResolvedValue([ZORRO, CONDOR]);
    mostrar();

    expect(await screen.findByText("El zorro y el cuervo")).toBeInTheDocument();
    expect(screen.getByText("El cóndor y la luna")).toBeInTheDocument();
    expect(screen.getByText("2 lecturas")).toBeInTheDocument();
  });

  it("pide al backend el filtro de dificultad del aula elegida", async () => {
    api.listarLecturas.mockResolvedValue([ZORRO]);
    mostrar();
    await screen.findByText("El zorro y el cuervo");

    await userEvent.selectOptions(screen.getByLabelText("Dificultad"), "avanzada");

    await waitFor(() =>
      expect(api.listarLecturas).toHaveBeenLastCalledWith("10", {
        gradoId: undefined,
        dificultad: "avanzada",
      })
    );
  });

  it("pide al backend el filtro de grado", async () => {
    api.listarLecturas.mockResolvedValue([ZORRO]);
    mostrar();
    await screen.findByText("El zorro y el cuervo");

    await userEvent.selectOptions(screen.getByLabelText("Grado"), "2");

    await waitFor(() =>
      expect(api.listarLecturas).toHaveBeenLastCalledWith("10", {
        gradoId: 2,
        dificultad: undefined,
      })
    );
  });

  it("busca por título sin volver a llamar al servidor", async () => {
    api.listarLecturas.mockResolvedValue([ZORRO, CONDOR]);
    mostrar();
    await screen.findByText("El zorro y el cuervo");

    await userEvent.type(screen.getByLabelText("Buscar por título"), "zorro");

    expect(screen.getByText("El zorro y el cuervo")).toBeInTheDocument();
    expect(screen.queryByText("El cóndor y la luna")).not.toBeInTheDocument();
    expect(api.listarLecturas).toHaveBeenCalledTimes(1);
  });

  it("avisa cuando no hay lecturas para los filtros", async () => {
    api.listarLecturas.mockResolvedValue([]);
    mostrar();

    expect(await screen.findByText(/no hay lecturas con estos filtros/i)).toBeInTheDocument();
  });

  it("muestra el error del servidor", async () => {
    api.listarLecturas.mockRejectedValue(new Error("El aula no existe"));
    mostrar();

    expect(await screen.findByText("El aula no existe")).toBeInTheDocument();
  });

  it("abre el texto completo de una lectura", async () => {
    api.listarLecturas.mockResolvedValue([ZORRO]);
    mostrar();
    await screen.findByText("El zorro y el cuervo");

    await userEvent.click(
      screen.getByRole("button", { name: /ver lectura el zorro y el cuervo/i })
    );

    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(
      within(screen.getByRole("dialog")).getByText(/un cuervo con un queso en el pico/i)
    ).toBeInTheDocument();
    // T-229: la vista previa muestra el tamaño del texto
    expect(within(screen.getByRole("dialog")).getByText("12 palabras")).toBeInTheDocument();
  });

  // T-229: el botón del modal lleva la lectura a Alumnos para asignarla
  it("lleva la lectura elegida a Alumnos con Usar como tarea", async () => {
    api.listarLecturas.mockResolvedValue([ZORRO]);
    render(
      <MemoryRouter initialEntries={["/catalogo"]}>
        <Routes>
          <Route path="/catalogo" element={<Catalogo />} />
          <Route path="/alumnos" element={<Destino />} />
        </Routes>
      </MemoryRouter>
    );
    await screen.findByText("El zorro y el cuervo");

    await userEvent.click(
      screen.getByRole("button", { name: /ver lectura el zorro y el cuervo/i })
    );
    await userEvent.click(screen.getByRole("button", { name: /usar como tarea/i }));

    expect(
      await screen.findByText(/destino: El zorro y el cuervo en 10/)
    ).toBeInTheDocument();
  });
});
