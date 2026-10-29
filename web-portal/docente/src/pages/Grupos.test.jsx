import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { describe, it, expect, vi, beforeEach } from "vitest";
import Grupos from "./Grupos";
import * as api from "../api";

vi.mock("../api");

const AULA = {
  id: 10,
  nombre: "Aula Rural",
  grados: [{ id: 1, nombre: "1.º" }],
};

const ANA = {
  id: 1,
  nombres: "Ana",
  apellidos: "Quispe",
  gradoId: 1,
  gradoNombre: "1.º",
  activo: true,
};

const LUIS = {
  id: 2,
  nombres: "Luis",
  apellidos: "Mamani",
  gradoId: 1,
  gradoNombre: "1.º",
  activo: false,
};

const GRUPO = {
  id: 3,
  nombre: "Los exploradores",
  descripcion: "Refuerzo",
  alumnos: [ANA],
};

const GRUPO_VACIO = {
  id: 4,
  nombre: "Los artistas",
  descripcion: null,
  alumnos: [],
};

const LECTURA = {
  id: 7,
  titulo: "El zorro y el cuervo",
  gradoId: 1,
  gradoNombre: "1.º",
  dificultad: "basica",
  activo: true,
};

const mostrar = () =>
  render(
    <MemoryRouter>
      <Grupos />
    </MemoryRouter>
  );

describe("Pantalla de grupos (T-196)", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    api.listarAulas.mockResolvedValue([AULA]);
    api.listarGrupos.mockResolvedValue([]);
    api.listarEstudiantes.mockResolvedValue([ANA, LUIS]);
    api.listarLecturas.mockResolvedValue([LECTURA]);
    api.crearGrupo.mockResolvedValue(GRUPO);
    api.asignarLecturaAGrupo.mockResolvedValue({
      grupoId: 3,
      lecturaId: 7,
      asignados: 1,
      yaTenían: 0,
    });
  });

  it("muestra los grupos del aula con sus alumnos", async () => {
    api.listarGrupos.mockResolvedValue([GRUPO]);
    mostrar();

    expect(await screen.findByText("Los exploradores")).toBeInTheDocument();
    expect(screen.getByText("Ana Quispe")).toBeInTheDocument();
    expect(screen.getByText("1 alumno")).toBeInTheDocument();
  });

  it("avisa cuando el aula todavía no tiene grupos", async () => {
    mostrar();

    expect(await screen.findByText(/aún no hay grupos/i)).toBeInTheDocument();
  });

  it("exige el nombre antes de crear un grupo", async () => {
    mostrar();

    await userEvent.click(await screen.findByRole("button", { name: /nuevo grupo/i }));
    await userEvent.click(screen.getByRole("button", { name: "Crear grupo" }));

    expect(await screen.findByText("El nombre del grupo es obligatorio")).toBeInTheDocument();
    expect(api.crearGrupo).not.toHaveBeenCalled();
  });

  it("exige al menos un alumno para crear el grupo", async () => {
    mostrar();

    await userEvent.click(await screen.findByRole("button", { name: /nuevo grupo/i }));
    await userEvent.type(screen.getByLabelText("Nombre del grupo"), "Los exploradores");
    await userEvent.click(screen.getByRole("button", { name: "Crear grupo" }));

    expect(await screen.findByText("Selecciona al menos un alumno")).toBeInTheDocument();
    expect(api.crearGrupo).not.toHaveBeenCalled();

    // al elegir un alumno, el aviso desaparece
    await userEvent.click(screen.getByLabelText(/ana quispe/i));
    expect(screen.queryByText("Selecciona al menos un alumno")).not.toBeInTheDocument();
  });

  it("crea un grupo con descripción y alumnos activos", async () => {
    mostrar();

    await userEvent.click(await screen.findByRole("button", { name: /nuevo grupo/i }));
    await userEvent.type(screen.getByLabelText("Nombre del grupo"), "Los exploradores");
    await userEvent.type(screen.getByLabelText(/descripción/i), "Refuerzo");
    // el alumno inactivo no se puede elegir para el grupo
    expect(screen.queryByLabelText(/luis mamani/i)).not.toBeInTheDocument();
    await userEvent.click(screen.getByLabelText(/ana quispe/i));
    await userEvent.click(screen.getByRole("button", { name: "Crear grupo" }));

    await waitFor(() =>
      expect(api.crearGrupo).toHaveBeenCalledWith("10", {
        nombre: "Los exploradores",
        descripcion: "Refuerzo",
        estudiantesIds: [1],
      })
    );
    expect(await screen.findByText(/grupo «Los exploradores» creado/i)).toBeInTheDocument();
  });

  it("asigna una lectura a todo el grupo", async () => {
    api.listarGrupos.mockResolvedValue([GRUPO]);
    mostrar();

    await userEvent.click(
      await screen.findByRole("button", { name: /asignar lectura al grupo los exploradores/i })
    );
    expect(
      await screen.findByRole("dialog", { name: /asignar lectura al grupo/i })
    ).toBeInTheDocument();
    expect(api.listarLecturas).toHaveBeenCalledWith("10");

    await userEvent.selectOptions(screen.getByLabelText("Lectura"), "7");
    await userEvent.click(screen.getByRole("button", { name: "Asignar lectura" }));

    await waitFor(() =>
      expect(api.asignarLecturaAGrupo).toHaveBeenCalledWith("10", 3, { lecturaId: 7 })
    );
    expect(await screen.findByText(/asignada a 1 alumno/i)).toBeInTheDocument();
  });

  it("no permite asignar una lectura a un grupo vacío", async () => {
    api.listarGrupos.mockResolvedValue([GRUPO_VACIO]);
    mostrar();

    expect(
      await screen.findByRole("button", { name: /asignar lectura al grupo los artistas/i })
    ).toBeDisabled();
    expect(screen.getByText(/grupo vacío/i)).toBeInTheDocument();
  });

  it("muestra el error del servidor al asignar", async () => {
    api.listarGrupos.mockResolvedValue([GRUPO]);
    api.asignarLecturaAGrupo.mockRejectedValue(new Error("La lectura no está activa"));
    mostrar();

    await userEvent.click(
      await screen.findByRole("button", { name: /asignar lectura al grupo los exploradores/i })
    );
    await userEvent.selectOptions(screen.getByLabelText("Lectura"), "7");
    await userEvent.click(screen.getByRole("button", { name: "Asignar lectura" }));

    expect(await screen.findByText("La lectura no está activa")).toBeInTheDocument();
  });

  it("indica cuántos alumnos ya tenían la lectura", async () => {
    api.listarGrupos.mockResolvedValue([GRUPO]);
    api.asignarLecturaAGrupo.mockResolvedValue({
      grupoId: 3,
      lecturaId: 7,
      asignados: 2,
      yaTenían: 1,
    });
    mostrar();

    await userEvent.click(
      await screen.findByRole("button", { name: /asignar lectura al grupo los exploradores/i })
    );
    await userEvent.selectOptions(screen.getByLabelText("Lectura"), "7");
    await userEvent.click(screen.getByRole("button", { name: "Asignar lectura" }));

    expect(await screen.findByText(/2 alumnos.*1 ya la tenían/i)).toBeInTheDocument();
  });
});
