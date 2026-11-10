import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, it, expect, vi, beforeEach } from "vitest";
import DetalleEstudiante from "./DetalleEstudiante";
import * as api from "../api";
import { seleccionarAula } from "../aulaSeleccionada";

vi.mock("../api");

beforeEach(() => {
  seleccionarAula(null);
});

const DETALLE = {
  estudianteId: 1,
  nombres: "Minji",
  apellidos: "Prueba",
  gradoId: 5,
  gradoNombre: "5.º",
  promedioGeneral: 78,
  colorGeneral: "amarillo",
  niveles: [
    {
      id: 1,
      orden: 1,
      nombre: "Primeros pasos",
      tipoReto: "lectura",
      descripcion: "Lee palabras y oraciones cortas",
      estado: "completado",
      puntos: 10,
    },
    {
      id: 2,
      orden: 2,
      nombre: "Explorador",
      tipoReto: "escritura",
      descripcion: "Escribe frases sencillas",
      estado: "desbloqueado",
      puntos: 0,
    },
    {
      id: 3,
      orden: 3,
      nombre: "Aventurero",
      tipoReto: "oralidad",
      descripcion: "Cuenta historias",
      estado: "bloqueado",
      puntos: 0,
    },
  ],
  puntajesPorCompetencia: [
    {
      competencia: "lectura",
      promedio: 90,
      evaluaciones: 3,
      ultimoPuntaje: 95,
      color: "verde",
    },
    {
      competencia: "escritura",
      promedio: 70,
      evaluaciones: 2,
      ultimoPuntaje: 65,
      color: "amarillo",
    },
    {
      competencia: "oralidad",
      promedio: 50,
      evaluaciones: 1,
      ultimoPuntaje: 50,
      color: "rojo",
    },
  ],
  ultimasEvidencias: [
    {
      id: 4,
      competencia: "lectura",
      puntaje: 95,
      evaluadoEn: "2026-11-01T10:00:00",
    },
    {
      id: 3,
      competencia: "escritura",
      puntaje: 65,
      evaluadoEn: "2026-10-30T09:00:00",
    },
  ],
};

const SIN_EVALUACIONES = {
  ...DETALLE,
  promedioGeneral: null,
  colorGeneral: "gris",
  niveles: [],
  puntajesPorCompetencia: [],
  ultimasEvidencias: [],
};

const mostrar = (ruta = "/dashboard/aulas/10/estudiantes/1") =>
  render(
    <MemoryRouter initialEntries={[ruta]}>
      <Routes>
        <Route
          path="/dashboard/aulas/:aulaId/estudiantes/:estudianteId"
          element={<DetalleEstudiante />}
        />
        <Route path="/dashboard" element={<div>Pantalla del dashboard</div>} />
      </Routes>
    </MemoryRouter>
  );

describe("Detalle del estudiante (T-292)", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it("consulta el detalle con el aula y el estudiante de la ruta", async () => {
    api.obtenerDetalleEstudiante.mockResolvedValue(DETALLE);
    mostrar();

    expect(
      await screen.findByRole("heading", { level: 1, name: "Minji Prueba" })
    ).toBeInTheDocument();
    expect(api.obtenerDetalleEstudiante).toHaveBeenCalledWith("10", "1");
    expect(screen.getByText("5.º · Resumen por competencia")).toBeInTheDocument();
  });

  it("muestra el resumen por competencia con color, ícono y texto", async () => {
    api.obtenerDetalleEstudiante.mockResolvedValue(DETALLE);
    mostrar();

    const resumen = await screen.findByRole("region", {
      name: "Resumen por competencia",
    });

    const lectura = within(resumen).getByRole("article", {
      name: "Competencia Lectura",
    });
    expect(lectura).toHaveClass("semaforo-verde");
    expect(within(lectura).getByText("Lectura")).toBeInTheDocument();
    expect(within(lectura).getByText("En nivel esperado")).toBeInTheDocument();
    expect(lectura.querySelector(".bi-book-fill")).not.toBeNull();
    expect(within(lectura).getByText("90")).toBeInTheDocument();
    expect(within(lectura).getByText("3")).toBeInTheDocument();
    expect(within(lectura).getByText("95/100")).toBeInTheDocument();

    const escritura = within(resumen).getByRole("article", {
      name: "Competencia Escritura",
    });
    expect(escritura).toHaveClass("semaforo-amarillo");
    expect(within(escritura).getByText("En proceso")).toBeInTheDocument();
    expect(within(escritura).getByText("70")).toBeInTheDocument();
    expect(within(escritura).getByText("2")).toBeInTheDocument();
    expect(within(escritura).getByText("65/100")).toBeInTheDocument();

    const oralidad = within(resumen).getByRole("article", {
      name: "Competencia Oralidad",
    });
    expect(oralidad).toHaveClass("semaforo-rojo");
    expect(within(oralidad).getByText("Necesita refuerzo")).toBeInTheDocument();
    expect(within(oralidad).getByText("1")).toBeInTheDocument();
    expect(within(oralidad).getByText("50/100")).toBeInTheDocument();

    expect(within(resumen).getAllByRole("article")).toHaveLength(3);
  });

  it("muestra el promedio general y el total de evaluaciones", async () => {
    api.obtenerDetalleEstudiante.mockResolvedValue(DETALLE);
    mostrar();

    const general = await screen.findByRole("article", {
      name: "Promedio general del estudiante",
    });

    expect(within(general).getByText("78")).toBeInTheDocument();
    expect(within(general).getByText("En proceso")).toBeInTheDocument();
    expect(
      within(general).getByText(/6 evaluaciones registradas/i)
    ).toBeInTheDocument();
  });

  it("muestra los niveles y las últimas evaluaciones", async () => {
    api.obtenerDetalleEstudiante.mockResolvedValue(DETALLE);
    mostrar();

    const niveles = await screen.findByRole("region", {
      name: "Ruta de aprendizaje",
    });
    expect(within(niveles).getByText("1. Primeros pasos")).toBeInTheDocument();
    expect(within(niveles).getByText("Completado")).toBeInTheDocument();
    expect(within(niveles).getByText("Disponible")).toBeInTheDocument();
    expect(within(niveles).getByText("Bloqueado")).toBeInTheDocument();
    expect(within(niveles).getByText("10 puntos")).toBeInTheDocument();

    const evolucion = screen.getByRole("region", {
      name: "Evolución de puntajes",
    });
    expect(
      within(evolucion).getByRole("img", {
        name: /gráfico de evolución de minji prueba/i,
      })
    ).toBeInTheDocument();

    const evidencias = screen.getByRole("region", {
      name: "Últimas evaluaciones",
    });
    expect(within(evidencias).getAllByRole("row")).toHaveLength(3);
    expect(within(evidencias).getByText("95/100")).toBeInTheDocument();
    expect(within(evidencias).getByText("65/100")).toBeInTheDocument();
  });

  it("muestra las competencias en gris cuando no hay evaluaciones", async () => {
    api.obtenerDetalleEstudiante.mockResolvedValue(SIN_EVALUACIONES);
    mostrar();

    const resumen = await screen.findByRole("region", {
      name: "Resumen por competencia",
    });

    const tarjetas = within(resumen).getAllByRole("article");
    expect(tarjetas).toHaveLength(3);
    tarjetas.forEach((tarjeta) => expect(tarjeta).toHaveClass("semaforo-gris"));
    expect(within(resumen).getAllByText("Sin evaluaciones")).toHaveLength(3);
    expect(
      within(resumen).getByText(/aún no hay evaluaciones registradas/i)
    ).toBeInTheDocument();

    const general = screen.getByRole("article", {
      name: "Promedio general del estudiante",
    });
    expect(within(general).getByText("—")).toBeInTheDocument();

    expect(
      screen.getByText(/el mapa de niveles todavía no tiene información/i)
    ).toBeInTheDocument();
    expect(
      screen.getByText(/aún no hay evaluaciones para mostrar la evolución/i)
    ).toBeInTheDocument();
    expect(
      screen.getByText(/todavía no hay evaluaciones registradas/i)
    ).toBeInTheDocument();
  });

  it("muestra el error del endpoint y permite reintentar", async () => {
    api.obtenerDetalleEstudiante
      .mockRejectedValueOnce(new Error("El alumno no existe en esta aula"))
      .mockResolvedValueOnce(DETALLE);
    mostrar();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "El alumno no existe en esta aula"
    );

    await userEvent.click(screen.getByRole("button", { name: /reintentar/i }));

    expect(
      await screen.findByRole("heading", { level: 1, name: "Minji Prueba" })
    ).toBeInTheDocument();
    expect(api.obtenerDetalleEstudiante).toHaveBeenCalledTimes(2);
  });

  it("permite volver al dashboard desde el encabezado", async () => {
    api.obtenerDetalleEstudiante.mockResolvedValue(DETALLE);
    mostrar();

    expect(
      await screen.findByRole("link", { name: /volver al dashboard/i })
    ).toHaveAttribute("href", "/dashboard");
  });
});
