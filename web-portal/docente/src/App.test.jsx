import { act, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { describe, it, expect, vi, beforeEach } from "vitest";
import App from "./App";
import * as api from "./api";
import { abrirSesion, cerrarSesion, obtenerSesion } from "./sesion";

vi.mock("./api");

const SESION = {
  access: "a",
  refresh: "r",
  docente: { id: 15, usuario: "prueba", nombre: "Docente de Prueba" },
};

const abrir = (ruta = "/alumnos") =>
  render(
    <MemoryRouter initialEntries={[ruta]}>
      <App />
    </MemoryRouter>
  );

beforeEach(() => {
  vi.resetAllMocks();
  // cerrar sesión avisa al servidor: aquí no hay ninguno
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue({ ok: true, status: 204 }));
  cerrarSesion();
  api.listarAulas.mockResolvedValue([]);
  api.listarGrados.mockResolvedValue([]);
  api.listarAvatares.mockResolvedValue([]);
});

describe("Acceso al portal", () => {
  it("sin sesión cualquier dirección muestra el login", () => {
    abrir("/dashboard");

    expect(screen.getByRole("button", { name: "Iniciar sesión" })).toBeInTheDocument();
    expect(api.listarAulas).not.toHaveBeenCalled();
  });

  it("con sesión muestra el nombre de la cuenta en la cabecera", async () => {
    abrirSesion(SESION);
    abrir();

    expect(await screen.findByText("Docente de Prueba")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Iniciar sesión" })).not.toBeInTheDocument();
  });

  it("cerrar sesión borra la sesión y vuelve al login", async () => {
    abrirSesion(SESION);
    abrir();

    await userEvent.click(await screen.findByRole("button", { name: "Cerrar sesión" }));

    expect(screen.getByRole("button", { name: "Iniciar sesión" })).toBeInTheDocument();
    expect(obtenerSesion()).toBeNull();
    expect(window.sessionStorage.length).toBe(0);
  });

  it("si la sesión termina sola, el portal vuelve al login", async () => {
    abrirSesion(SESION);
    abrir();
    await screen.findByText("Docente de Prueba");

    act(() => cerrarSesion());

    expect(screen.getByRole("button", { name: "Iniciar sesión" })).toBeInTheDocument();
  });
});
