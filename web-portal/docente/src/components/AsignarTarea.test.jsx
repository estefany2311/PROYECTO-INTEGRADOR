import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import AsignarTarea from "./AsignarTarea";
import * as api from "../api";

vi.mock("../api");

const LECTURA = {
  id: 5,
  titulo: "El zorro y el cuervo",
  gradoNombre: "1.º",
  tipoReto: "lectura",
};

const ANA = {
  id: 1, nombres: "Ana", apellidos: "Quispe", gradoNombre: "1.º", activo: true,
};
const LUIS = {
  id: 2, nombres: "Luis", apellidos: "Mamani", gradoNombre: "1.º", activo: false,
};

const abrir = (props = {}) => {
  const onAsignada = vi.fn();
  const onCerrar = vi.fn();
  render(
    <AsignarTarea
      aulaId="10"
      lectura={LECTURA}
      alumnos={[ANA, LUIS]}
      onCerrar={onCerrar}
      onAsignada={onAsignada}
      {...props}
    />
  );
  return { onAsignada, onCerrar };
};

describe("Modal de asignación (T-193)", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it("muestra la lectura y solo los alumnos activos", () => {
    abrir();

    expect(screen.getByRole("dialog", { name: /asignar lectura/i })).toBeInTheDocument();
    expect(screen.getByText("El zorro y el cuervo")).toBeInTheDocument();
    expect(screen.getByRole("option", { name: /ana quispe/i })).toBeInTheDocument();
    expect(screen.queryByRole("option", { name: /luis mamani/i })).not.toBeInTheDocument();
  });

  it("exige elegir un alumno antes de asignar", async () => {
    abrir();

    await userEvent.click(screen.getByRole("button", { name: "Asignar tarea" }));

    expect(await screen.findByText(/elige al alumno/i)).toBeInTheDocument();
    expect(api.asignarLectura).not.toHaveBeenCalled();
  });

  it("asigna la lectura al alumno elegido", async () => {
    api.asignarLectura.mockResolvedValue({ id: 1 });
    const { onAsignada } = abrir();

    await userEvent.selectOptions(screen.getByLabelText("Alumno"), "1");
    await userEvent.click(screen.getByRole("button", { name: "Asignar tarea" }));

    await waitFor(() =>
      expect(api.asignarLectura).toHaveBeenCalledWith("10", {
        lecturaId: 5,
        estudianteId: 1,
      })
    );
    expect(onAsignada).toHaveBeenCalledWith(expect.stringContaining("Ana Quispe"));
  });

  it("muestra el error del servidor (tarea repetida)", async () => {
    api.asignarLectura.mockRejectedValue(new Error("Ese alumno ya tiene esta tarea"));
    abrir();

    await userEvent.selectOptions(screen.getByLabelText("Alumno"), "1");
    await userEvent.click(screen.getByRole("button", { name: "Asignar tarea" }));

    expect(await screen.findByText("Ese alumno ya tiene esta tarea")).toBeInTheDocument();
  });

  it("no permite asignar si el aula no tiene alumnos activos", () => {
    abrir({ alumnos: [LUIS] });

    expect(screen.getByText(/no tiene alumnos activos/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Asignar tarea" })).toBeDisabled();
  });

  it("muestra el estado de carga mientras llegan los alumnos", () => {
    abrir({ alumnos: [], cargando: true });

    expect(screen.getByText(/cargando alumnos/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Asignar tarea" })).toBeDisabled();
  });
});
