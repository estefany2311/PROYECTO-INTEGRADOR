import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { describe, it, expect, vi, beforeEach } from "vitest";
import Dashboard from "./Dashboard";
import * as api from "../api";

vi.mock("../api");

const AULA = {
  id: 10,
  nombre: "Aula Rural",
  grados: [
    { id: 1, nombre: "1.º" },
    { id: 3, nombre: "3.º" },
    { id: 5, nombre: "5.º" },
  ],
};

const AULA2 = {
  id: 20,
  nombre: "Aula Costa",
  grados: [{ id: 2, nombre: "2.º" }],
};

const DASHBOARD = {
  aulaId: 10,
  aulaNombre: "Aula Rural",
  grados: AULA.grados,
  resumen: {
    totalEstudiantes: 3,
    enNivelEsperado: 1,
    enProceso: 1,
    necesitanRefuerzo: 1,
    sinEvaluaciones: 0,
  },
  estudiantes: [
    {
      id: 1,
      nombres: "minji",
      apellidos: "prueba",
      gradoId: 5,
      gradoNombre: "5.º",
      promedio: 85,
      color: "verde",
      competencias: [
        { competencia: "lectura", promedio: 90 },
        { competencia: "escritura", promedio: 85 },
        { competencia: "oralidad", promedio: 80 },
      ],
    },
    {
      id: 2,
      nombres: "Breysi",
      apellidos: "Zavala",
      gradoId: 3,
      gradoNombre: "3.º",
      promedio: 68,
      color: "amarillo",
      competencias: [
        { competencia: "lectura", promedio: 75 },
        { competencia: "escritura", promedio: 68 },
        { competencia: "oralidad", promedio: 62 },
      ],
    },
    {
      id: 3,
      nombres: "Ester",
      apellidos: "Zavala",
      gradoId: 1,
      gradoNombre: "1.º",
      promedio: 50,
      color: "rojo",
      competencias: [
        { competencia: "lectura", promedio: 55 },
        { competencia: "escritura", promedio: 50 },
        { competencia: "oralidad", promedio: 45 },
      ],
    },
  ],
  promediosPorCompetencia: [
    { competencia: "lectura", promedio: 73.33 },
    { competencia: "escritura", promedio: 67.67 },
    { competencia: "oralidad", promedio: 62.33 },
  ],
  promedioGeneral: 68,
  colorGeneral: "amarillo",
};

const DASHBOARD_GRIS = {
  aulaId: 10,
  aulaNombre: "Prueba Noemi",
  grados: [{ id: 1, nombre: "1.º" }],
  resumen: {
    totalEstudiantes: 1,
    enNivelEsperado: 0,
    enProceso: 0,
    necesitanRefuerzo: 0,
    sinEvaluaciones: 1,
  },
  estudiantes: [
    {
      id: 1,
      nombres: "NOEMI",
      apellidos: "DE LA CRUZ",
      gradoId: 1,
      gradoNombre: "1.º",
      promedio: null,
      color: "gris",
      competencias: [],
    },
  ],
  promediosPorCompetencia: [],
  promedioGeneral: null,
  colorGeneral: "gris",
};

const DASHBOARD2 = {
  aulaId: 20,
  aulaNombre: "Aula Costa",
  grados: AULA2.grados,
  resumen: {
    totalEstudiantes: 1,
    enNivelEsperado: 1,
    enProceso: 0,
    necesitanRefuerzo: 0,
    sinEvaluaciones: 0,
  },
  estudiantes: [
    {
      id: 4,
      nombres: "Luis",
      apellidos: "Costa",
      gradoId: 2,
      gradoNombre: "2.º",
      promedio: 90,
      color: "verde",
      competencias: [
        { competencia: "lectura", promedio: 92 },
        { competencia: "escritura", promedio: 90 },
        { competencia: "oralidad", promedio: 88 },
      ],
    },
  ],
  promediosPorCompetencia: [
    { competencia: "lectura", promedio: 92 },
    { competencia: "escritura", promedio: 90 },
    { competencia: "oralidad", promedio: 88 },
  ],
  promedioGeneral: 90,
  colorGeneral: "verde",
};

const mostrar = () =>
  render(
    <MemoryRouter>
      <Dashboard />
    </MemoryRouter>
  );

describe("Pantalla del dashboard (T-279)", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    api.listarAulas.mockResolvedValue([AULA]);
  });

  it("muestra el resumen y las tarjetas por estudiante", async () => {
    api.listarDashboardAula.mockResolvedValue(DASHBOARD);
    mostrar();

    expect(await screen.findByText("minji prueba")).toBeInTheDocument();
    expect(screen.getByText("Breysi Zavala")).toBeInTheDocument();
    expect(screen.getByText("Ester Zavala")).toBeInTheDocument();

    const resumen = screen.getByRole("region", { name: "Resumen del aula" });
    expect(within(resumen).getByText("Alumnos en el aula")).toBeInTheDocument();
    expect(within(resumen).getByText("3")).toBeInTheDocument();

    const tarjetaMinji = screen.getByText("minji prueba").closest("article");
    expect(tarjetaMinji.querySelector(".alumno-promedio b")).toHaveTextContent("85");
    expect(within(tarjetaMinji).getByText("En nivel esperado")).toBeInTheDocument();
    expect(within(tarjetaMinji).getByText("90")).toBeInTheDocument();

    const panelAula = screen.getByRole("region", {
      name: "Promedio del aula por competencia",
    });
    expect(within(panelAula).getByText("Promedio general del aula")).toBeInTheDocument();
    expect(
      within(panelAula.querySelector(".dashboard-promedio-general")).getByText("68")
    ).toBeInTheDocument();
    expect(within(panelAula).getByText("73")).toBeInTheDocument();
  });

  it("muestra en gris a los alumnos sin evaluaciones", async () => {
    api.listarDashboardAula.mockResolvedValue(DASHBOARD_GRIS);
    mostrar();

    expect(await screen.findByText("NOEMI DE LA CRUZ")).toBeInTheDocument();

    const tarjeta = screen.getByText("NOEMI DE LA CRUZ").closest("article");
    expect(tarjeta.querySelector(".alumno-promedio b")).toHaveTextContent("—");
    expect(within(tarjeta).getByText("Sin evaluaciones")).toBeInTheDocument();
    expect(screen.getByText(/aún no tiene evaluaciones registradas/i)).toBeInTheDocument();
    expect(
      screen.getByText(/aún no hay evaluaciones para calcular los promedios del aula/i)
    ).toBeInTheDocument();
    expect(screen.getByText(/1 alumno sin evaluaciones/i)).toBeInTheDocument();
  });

  it("cambia de aula y recarga el dashboard", async () => {
    api.listarAulas.mockResolvedValue([AULA, AULA2]);
    api.listarDashboardAula
      .mockResolvedValueOnce(DASHBOARD)
      .mockResolvedValueOnce(DASHBOARD2);
    mostrar();

    expect(await screen.findByText("minji prueba")).toBeInTheDocument();
    await userEvent.selectOptions(screen.getByLabelText("Aula"), "20");

    expect(await screen.findByText("Luis Costa")).toBeInTheDocument();
    expect(api.listarDashboardAula).toHaveBeenCalledWith("20");
  });

  it("muestra el estado vacío cuando no hay aulas", async () => {
    api.listarAulas.mockResolvedValue([]);
    mostrar();

    expect(await screen.findByText(/aún no tienes aulas registradas/i)).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: /registrar mi primera aula/i })
    ).toHaveAttribute("href", "/aulas");
    expect(api.listarDashboardAula).not.toHaveBeenCalled();
  });

  it("muestra el error del endpoint y permite reintentar", async () => {
    api.listarDashboardAula.mockRejectedValueOnce(
      new Error("No se pudo cargar el dashboard")
    );
    mostrar();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "No se pudo cargar el dashboard"
    );
    expect(screen.getByRole("button", { name: /reintentar/i })).toBeInTheDocument();
  });
});
