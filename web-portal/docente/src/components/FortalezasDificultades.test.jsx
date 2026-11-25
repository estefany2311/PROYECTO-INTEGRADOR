import { render, screen, within } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import FortalezasDificultades from "./FortalezasDificultades";

const PUNTAJES = [
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
];

describe("Fortalezas y dificultades (T-294)", () => {
  it("clasifica verde como fortaleza y rojo como dificultad", () => {
    render(<FortalezasDificultades puntajes={PUNTAJES} />);

    const panel = screen.getByRole("region", {
      name: "Fortalezas y dificultades",
    });
    expect(
      within(panel).getByRole("heading", { name: "Fortalezas y dificultades" })
    ).toBeInTheDocument();

    const fortalezas = within(panel).getByRole("region", { name: "Fortalezas" });
    expect(within(fortalezas).getByText("Lectura")).toBeInTheDocument();
    expect(within(fortalezas).getByText("3 evaluaciones")).toBeInTheDocument();
    expect(within(fortalezas).getByText("En nivel esperado")).toBeInTheDocument();
    expect(within(fortalezas).queryByText("Escritura")).not.toBeInTheDocument();
    expect(within(fortalezas).queryByText("Oralidad")).not.toBeInTheDocument();

    const dificultades = within(panel).getByRole("region", {
      name: "Dificultades",
    });
    expect(within(dificultades).getByText("Oralidad")).toBeInTheDocument();
    expect(within(dificultades).getByText("1 evaluación")).toBeInTheDocument();
    expect(
      within(dificultades).getByText("Necesita refuerzo")
    ).toBeInTheDocument();
    expect(within(dificultades).queryByText("Lectura")).not.toBeInTheDocument();

    expect(
      within(panel).getByText(/en proceso \(amarillo\): escritura 70\/100/i)
    ).toBeInTheDocument();
  });

  it("muestra el estado vacío de dificultades cuando todo está en verde", () => {
    render(
      <FortalezasDificultades
        puntajes={[
          PUNTAJES[0],
          { ...PUNTAJES[1], promedio: 85, color: "verde" },
        ]}
      />
    );

    const fortalezas = screen.getByRole("region", { name: "Fortalezas" });
    expect(within(fortalezas).getAllByRole("listitem")).toHaveLength(2);

    const dificultades = screen.getByRole("region", { name: "Dificultades" });
    expect(
      within(dificultades).getByText(/no se identifican dificultades/i)
    ).toBeInTheDocument();
    expect(screen.queryByText(/en proceso \(amarillo\)/i)).not.toBeInTheDocument();
  });

  it("no cuenta el amarillo como fortaleza ni dificultad", () => {
    render(<FortalezasDificultades puntajes={[PUNTAJES[1]]} />);

    const fortalezas = screen.getByRole("region", { name: "Fortalezas" });
    expect(
      within(fortalezas).getByText(/todavía no hay competencias en nivel esperado/i)
    ).toBeInTheDocument();

    const dificultades = screen.getByRole("region", { name: "Dificultades" });
    expect(
      within(dificultades).getByText(/no se identifican dificultades/i)
    ).toBeInTheDocument();

    const panel = screen.getByRole("region", {
      name: "Fortalezas y dificultades",
    });
    expect(
      within(panel).getByText(/en proceso \(amarillo\): escritura 70\/100/i)
    ).toBeInTheDocument();
  });

  it("muestra el estado vacío cuando no hay evaluaciones", () => {
    render(<FortalezasDificultades puntajes={[]} />);

    expect(
      screen.getByText(
        /aún no hay evaluaciones para identificar fortalezas y dificultades/i
      )
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("region", { name: "Fortalezas" })
    ).not.toBeInTheDocument();
  });
});
