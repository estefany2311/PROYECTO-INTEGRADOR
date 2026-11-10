import { render, screen, within } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import GraficoEvolucion from "./GraficoEvolucion";

const EVIDENCIAS = [
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
  {
    id: 2,
    competencia: "lectura",
    puntaje: 80,
    evaluadoEn: "2026-10-25T12:00:00",
  },
];

describe("Gráfico de evolución (T-293)", () => {
  it("muestra el gráfico con la etiqueta accesible y el rango de puntajes", () => {
    render(<GraficoEvolucion evidencias={EVIDENCIAS} nombre="Minji Prueba" />);

    const region = screen.getByRole("region", {
      name: "Evolución de puntajes",
    });
    expect(
      within(region).getByRole("heading", { name: "Evolución de puntajes" })
    ).toBeInTheDocument();

    const grafico = within(region).getByRole("img", {
      name: /gráfico de evolución de minji prueba/i,
    });
    expect(grafico.getAttribute("aria-label")).toMatch(/3 evaluaciones/i);
    expect(grafico.getAttribute("aria-label")).toMatch(
      /puntajes de 65 a 95 sobre 100/i
    );
  });

  it("ordena los puntos de la evaluación más antigua a la más reciente", () => {
    const { container } = render(
      <GraficoEvolucion evidencias={EVIDENCIAS} nombre="Minji Prueba" />
    );

    const puntos = container.querySelectorAll(".punto-evolucion");
    expect(puntos).toHaveLength(3);
    expect(puntos[0]).toHaveAttribute("data-puntaje", "80");
    expect(puntos[0]).toHaveAttribute("data-competencia", "lectura");
    expect(puntos[1]).toHaveAttribute("data-puntaje", "65");
    expect(puntos[1]).toHaveAttribute("data-competencia", "escritura");
    expect(puntos[2]).toHaveAttribute("data-puntaje", "95");
  });

  it("muestra solo las competencias presentes en el gráfico", () => {
    render(<GraficoEvolucion evidencias={EVIDENCIAS} nombre="Minji Prueba" />);

    const leyenda = screen.getByRole("list", {
      name: "Competencias del gráfico",
    });
    const items = within(leyenda).getAllByRole("listitem");

    expect(items).toHaveLength(2);
    expect(within(leyenda).getByText("Lectura")).toBeInTheDocument();
    expect(within(leyenda).getByText("Escritura")).toBeInTheDocument();
    expect(within(leyenda).queryByText("Oralidad")).not.toBeInTheDocument();
  });

  it("muestra el estado vacío cuando no hay evaluaciones", () => {
    render(<GraficoEvolucion evidencias={[]} nombre="Minji Prueba" />);

    expect(
      screen.getByText(/aún no hay evaluaciones para mostrar la evolución/i)
    ).toBeInTheDocument();
    expect(screen.queryByRole("img")).not.toBeInTheDocument();
  });

  it("avisa cuando solo hay una evaluación", () => {
    render(
      <GraficoEvolucion evidencias={[EVIDENCIAS[0]]} nombre="Minji Prueba" />
    );

    const grafico = screen.getByRole("img", {
      name: /una evaluación/i,
    });
    expect(grafico.querySelectorAll(".punto-evolucion")).toHaveLength(1);
    expect(
      screen.getByText(
        /con una sola evaluación todavía no se puede ver una tendencia/i
      )
    ).toBeInTheDocument();
  });
});
