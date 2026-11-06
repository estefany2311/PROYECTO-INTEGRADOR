import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { describe, it, expect, vi, beforeEach } from "vitest";
import Dashboard from "./Dashboard";
import * as api from "../api";
import { seleccionarAula } from "../aulaSeleccionada";
import {
  guardarFiltrosDashboard,
  limpiarFiltrosDashboard,
} from "../filtrosDashboard";

vi.mock("../api");

beforeEach(() => {
  seleccionarAula(null);
  limpiarFiltrosDashboard();
});

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

const DASHBOARD_DOS_ROJOS = {
  ...DASHBOARD,
  resumen: {
    ...DASHBOARD.resumen,
    totalEstudiantes: 4,
    necesitanRefuerzo: 2,
  },
  estudiantes: [
    ...DASHBOARD.estudiantes,
    {
      id: 5,
      nombres: "Ana",
      apellidos: "Roja",
      gradoId: 3,
      gradoNombre: "3.º",
      promedio: 40,
      color: "rojo",
      competencias: [
        { competencia: "lectura", promedio: 40 },
        { competencia: "escritura", promedio: 40 },
        { competencia: "oralidad", promedio: 40 },
      ],
    },
  ],
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

const DASHBOARD_GRADO_UNO = {
  ...DASHBOARD,
  resumen: {
    totalEstudiantes: 1,
    enNivelEsperado: 0,
    enProceso: 0,
    necesitanRefuerzo: 1,
    sinEvaluaciones: 0,
  },
  estudiantes: [DASHBOARD.estudiantes[2]],
  promediosPorCompetencia: [
    { competencia: "lectura", promedio: 55 },
    { competencia: "escritura", promedio: 50 },
    { competencia: "oralidad", promedio: 45 },
  ],
  promedioGeneral: 50,
  colorGeneral: "rojo",
};

const colorPorPromedio = (promedio) => {
  if (promedio >= 80) return "verde";
  if (promedio >= 60) return "amarillo";
  return "rojo";
};

const DASHBOARD_LECTURA = {
  ...DASHBOARD,
  estudiantes: DASHBOARD.estudiantes.map((estudiante) => {
    const lectura = estudiante.competencias.find(
      (competencia) => competencia.competencia === "lectura"
    );
    return {
      ...estudiante,
      promedio: lectura.promedio,
      color: colorPorPromedio(lectura.promedio),
      competencias: [lectura],
    };
  }),
  promediosPorCompetencia: [{ competencia: "lectura", promedio: 73.33 }],
  promedioGeneral: 73,
  colorGeneral: "amarillo",
};

const DASHBOARD_SIN_ALUMNOS = {
  ...DASHBOARD,
  resumen: {
    totalEstudiantes: 0,
    enNivelEsperado: 0,
    enProceso: 0,
    necesitanRefuerzo: 0,
    sinEvaluaciones: 0,
  },
  estudiantes: [],
  promediosPorCompetencia: [],
  promedioGeneral: null,
  colorGeneral: "gris",
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

  it("muestra la leyenda accesible del semáforo", async () => {
    api.listarDashboardAula.mockResolvedValue(DASHBOARD);
    mostrar();

    const leyenda = await screen.findByRole("region", {
      name: "Leyenda del semáforo",
    });

    expect(within(leyenda).getAllByRole("listitem")).toHaveLength(4);
    expect(within(leyenda).getByText("En nivel esperado")).toBeInTheDocument();
    expect(within(leyenda).getByText("En proceso")).toBeInTheDocument();
    expect(within(leyenda).getByText("Necesita refuerzo")).toBeInTheDocument();
    expect(within(leyenda).getByText("Sin evaluaciones")).toBeInTheDocument();
    expect(leyenda.querySelectorAll(".leyenda-item .bi")).toHaveLength(4);
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
    expect(api.listarDashboardAula).toHaveBeenCalledWith("20", {});
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

describe("Sección Necesitan refuerzo (T-283)", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    api.listarAulas.mockResolvedValue([AULA]);
  });

  it("muestra el conteo y los datos del alumno en rojo sin duplicar la consulta", async () => {
    api.listarDashboardAula.mockResolvedValue(DASHBOARD);
    mostrar();

    const refuerzo = await screen.findByRole("region", {
      name: "Necesitan refuerzo",
    });

    expect(within(refuerzo).getByLabelText("1 alumno en rojo")).toHaveTextContent("1");
    expect(within(refuerzo).getByText(/Ester Zavala/)).toHaveTextContent(
      "Ester Zavala · 1.º · Promedio 50/100"
    );
    expect(within(refuerzo).queryByText(/minji prueba/)).not.toBeInTheDocument();
    expect(within(refuerzo).queryByText(/Breysi Zavala/)).not.toBeInTheDocument();
    expect(api.listarDashboardAula).toHaveBeenCalledTimes(1);
  });

  it("muestra a todos los alumnos en rojo y usa el plural en el conteo", async () => {
    api.listarDashboardAula.mockResolvedValue(DASHBOARD_DOS_ROJOS);
    mostrar();

    const refuerzo = await screen.findByRole("region", {
      name: "Necesitan refuerzo",
    });

    expect(within(refuerzo).getByLabelText("2 alumnos en rojo")).toHaveTextContent("2");
    const alumnos = within(refuerzo).getAllByRole("listitem");
    expect(alumnos).toHaveLength(2);
    expect(alumnos[0]).toHaveTextContent("Ester Zavala · 1.º · Promedio 50/100");
    expect(alumnos[1]).toHaveTextContent("Ana Roja · 3.º · Promedio 40/100");
  });

  it("no muestra la sección cuando no hay alumnos en rojo", async () => {
    api.listarDashboardAula.mockResolvedValue(DASHBOARD_GRIS);
    mostrar();

    expect(await screen.findByText("NOEMI DE LA CRUZ")).toBeInTheDocument();
    expect(
      screen.queryByRole("region", { name: "Necesitan refuerzo" })
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("heading", { name: "Necesitan refuerzo" })
    ).not.toBeInTheDocument();
  });

  it("ubica la sección después del resumen y antes de la leyenda y las tarjetas", async () => {
    api.listarDashboardAula.mockResolvedValue(DASHBOARD);
    mostrar();

    const refuerzo = await screen.findByRole("region", {
      name: "Necesitan refuerzo",
    });
    const resumen = screen.getByRole("region", { name: "Resumen del aula" });
    const leyenda = screen.getByRole("region", { name: "Leyenda del semáforo" });
    const alumnos = screen.getByRole("region", {
      name: "Rendimiento por estudiante",
    });
    const regiones = screen.getAllByRole("region");

    expect(regiones.indexOf(resumen)).toBeLessThan(regiones.indexOf(refuerzo));
    expect(regiones.indexOf(refuerzo)).toBeLessThan(regiones.indexOf(leyenda));
    expect(regiones.indexOf(refuerzo)).toBeLessThan(regiones.indexOf(alumnos));
  });

  it("oculta la sección al cambiar a un aula sin alumnos en rojo", async () => {
    api.listarAulas.mockResolvedValue([AULA, AULA2]);
    api.listarDashboardAula
      .mockResolvedValueOnce(DASHBOARD)
      .mockResolvedValueOnce(DASHBOARD2);
    mostrar();

    expect(
      await screen.findByRole("region", { name: "Necesitan refuerzo" })
    ).toBeInTheDocument();

    await userEvent.selectOptions(screen.getByLabelText("Aula"), "20");

    expect(await screen.findByText("Luis Costa")).toBeInTheDocument();
    expect(
      screen.queryByRole("region", { name: "Necesitan refuerzo" })
    ).not.toBeInTheDocument();
    expect(api.listarDashboardAula).toHaveBeenCalledTimes(2);
  });
});

describe("Filtros de grado y competencia (T-287)", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    api.listarAulas.mockResolvedValue([AULA]);
  });

  it("envía el grado elegido al endpoint y actualiza el dashboard", async () => {
    api.listarDashboardAula
      .mockResolvedValueOnce(DASHBOARD)
      .mockResolvedValueOnce(DASHBOARD_GRADO_UNO);
    mostrar();

    expect(await screen.findByText("minji prueba")).toBeInTheDocument();

    await userEvent.selectOptions(screen.getByLabelText("Grado"), "1");

    expect(await screen.findByText("Ester Zavala")).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.queryByText("minji prueba")).not.toBeInTheDocument()
    );
    expect(screen.getByLabelText("Grado")).toHaveValue("1");
    expect(api.listarDashboardAula).toHaveBeenLastCalledWith("10", {
      gradoId: "1",
    });
  });

  it("envía la competencia elegida y muestra solo esa competencia", async () => {
    api.listarDashboardAula
      .mockResolvedValueOnce(DASHBOARD)
      .mockResolvedValueOnce(DASHBOARD_LECTURA);
    mostrar();

    expect(await screen.findByText("minji prueba")).toBeInTheDocument();

    await userEvent.selectOptions(screen.getByLabelText("Competencia"), "lectura");

    const panel = await screen.findByRole("region", {
      name: "Promedio de lectura del aula",
    });
    expect(within(panel).getByText("Lectura")).toBeInTheDocument();
    expect(within(panel).queryByText("Escritura")).not.toBeInTheDocument();
    expect(within(panel).queryByText("Oralidad")).not.toBeInTheDocument();

    const tarjeta = screen.getByText("minji prueba").closest("article");
    expect(within(tarjeta).getByText("Lectura")).toBeInTheDocument();
    expect(within(tarjeta).queryByText("Escritura")).not.toBeInTheDocument();
    expect(within(tarjeta).queryByText("Oralidad")).not.toBeInTheDocument();
    expect(api.listarDashboardAula).toHaveBeenLastCalledWith("10", {
      competencia: "lectura",
    });
  });

  it("limpia el grado al cambiar de aula", async () => {
    api.listarAulas.mockResolvedValue([AULA, AULA2]);
    api.listarDashboardAula
      .mockResolvedValueOnce(DASHBOARD)
      .mockResolvedValueOnce(DASHBOARD_GRADO_UNO)
      .mockResolvedValueOnce(DASHBOARD2);
    mostrar();

    expect(await screen.findByText("minji prueba")).toBeInTheDocument();

    await userEvent.selectOptions(screen.getByLabelText("Grado"), "1");
    expect(await screen.findByText("Ester Zavala")).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.queryByText("minji prueba")).not.toBeInTheDocument()
    );
    expect(screen.getByLabelText("Grado")).toHaveValue("1");

    await userEvent.selectOptions(screen.getByLabelText("Aula"), "20");

    expect(await screen.findByText("Luis Costa")).toBeInTheDocument();
    expect(screen.getByLabelText("Grado")).toHaveValue("");
    expect(api.listarDashboardAula).toHaveBeenLastCalledWith("20", {});
  });

  it("muestra un estado vacío propio cuando los filtros no devuelven alumnos", async () => {
    api.listarDashboardAula
      .mockResolvedValueOnce(DASHBOARD)
      .mockResolvedValueOnce(DASHBOARD_SIN_ALUMNOS);
    mostrar();

    expect(await screen.findByText("minji prueba")).toBeInTheDocument();

    await userEvent.selectOptions(screen.getByLabelText("Grado"), "1");

    expect(
      await screen.findByText("No hay alumnos para los filtros seleccionados.")
    ).toBeInTheDocument();
    expect(
      screen.getByText("Prueba con otro grado o competencia.")
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("link", { name: /ir a alumnos/i })
    ).not.toBeInTheDocument();
    expect(api.listarDashboardAula).toHaveBeenLastCalledWith("10", {
      gradoId: "1",
    });
  });
});

describe("Persistencia de filtros (T-288)", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    api.listarAulas.mockResolvedValue([AULA]);
  });

  it("restaura el grado y la competencia al volver a la pantalla", async () => {
    api.listarDashboardAula
      .mockResolvedValueOnce(DASHBOARD)
      .mockResolvedValueOnce(DASHBOARD_GRADO_UNO)
      .mockResolvedValueOnce(DASHBOARD_LECTURA)
      .mockResolvedValueOnce(DASHBOARD_LECTURA);
    const { unmount } = mostrar();

    expect(await screen.findByText("minji prueba")).toBeInTheDocument();

    await userEvent.selectOptions(screen.getByLabelText("Grado"), "1");
    expect(await screen.findByText("Ester Zavala")).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.queryByText("minji prueba")).not.toBeInTheDocument()
    );

    await userEvent.selectOptions(screen.getByLabelText("Competencia"), "lectura");
    expect(
      await screen.findByRole("region", {
        name: "Promedio de lectura del aula",
      })
    ).toBeInTheDocument();

    unmount();
    mostrar();

    expect(await screen.findByText("minji prueba")).toBeInTheDocument();
    expect(screen.getByLabelText("Grado")).toHaveValue("1");
    expect(screen.getByLabelText("Competencia")).toHaveValue("lectura");
    expect(api.listarDashboardAula).toHaveBeenLastCalledWith("10", {
      gradoId: "1",
      competencia: "lectura",
    });
  });

  it("restaura el aula guardada para que el grado siga siendo válido", async () => {
    seleccionarAula("20");
    guardarFiltrosDashboard({ gradoId: "2", competencia: "" });
    api.listarAulas.mockResolvedValue([AULA, AULA2]);
    api.listarDashboardAula.mockResolvedValue(DASHBOARD2);

    mostrar();

    expect(await screen.findByText("Luis Costa")).toBeInTheDocument();
    expect(screen.getByLabelText("Aula")).toHaveValue("20");
    expect(screen.getByLabelText("Grado")).toHaveValue("2");
    expect(api.listarDashboardAula).toHaveBeenLastCalledWith("20", {
      gradoId: "2",
    });
  });

  it("no restaura el grado de un aula anterior al volver", async () => {
    api.listarAulas.mockResolvedValue([AULA, AULA2]);
    api.listarDashboardAula
      .mockResolvedValueOnce(DASHBOARD)
      .mockResolvedValueOnce(DASHBOARD_GRADO_UNO)
      .mockResolvedValueOnce(DASHBOARD2)
      .mockResolvedValueOnce(DASHBOARD2);
    const { unmount } = mostrar();

    expect(await screen.findByText("minji prueba")).toBeInTheDocument();
    await userEvent.selectOptions(screen.getByLabelText("Grado"), "1");
    expect(await screen.findByText("Ester Zavala")).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.queryByText("minji prueba")).not.toBeInTheDocument()
    );

    await userEvent.selectOptions(screen.getByLabelText("Aula"), "20");
    expect(await screen.findByText("Luis Costa")).toBeInTheDocument();
    expect(screen.getByLabelText("Grado")).toHaveValue("");

    unmount();
    mostrar();

    expect(await screen.findByText("Luis Costa")).toBeInTheDocument();
    expect(screen.getByLabelText("Aula")).toHaveValue("20");
    expect(screen.getByLabelText("Grado")).toHaveValue("");
    expect(api.listarDashboardAula).toHaveBeenLastCalledWith("20", {});
  });
});
