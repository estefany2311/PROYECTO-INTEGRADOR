import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import * as api from "../api";
import AccesoAlumno from "./AccesoAlumno";

// Toda la API se simula: ninguna prueba llama al servidor real
vi.mock("../api", () => ({
  listarAvatares: vi.fn(),
  asignarAvatar: vi.fn(),
  generarPin: vi.fn(),
}));

const AVATARES = [
  { id: 1, nombre: "Zorro", url: "https://ejemplo/1.png" },
  { id: 2, nombre: "Búho", url: "https://ejemplo/2.png" },
];

const ALUMNO = {
  id: 1, nombres: "Ana", apellidos: "Quispe", gradoId: 1,
  gradoNombre: "1.º", activo: true, avatarId: null, avatarUrl: null, tienePin: false,
};

// aulaId llega como texto desde el <select> de Alumnos
const abrir = (alumno = ALUMNO) => {
  const onActualizado = vi.fn();
  const onCerrar = vi.fn();
  render(
    <AccesoAlumno aulaId="10" alumno={alumno} onActualizado={onActualizado} onCerrar={onCerrar} />
  );
  return { onActualizado, onCerrar };
};

describe("AccesoAlumno: avatar y PIN (T-187)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    api.listarAvatares.mockResolvedValue(AVATARES);
    api.asignarAvatar.mockResolvedValue(ALUMNO);
    api.generarPin.mockResolvedValue({ estudianteId: 1, pin: "4821" });
  });

  it("carga el catálogo y guarda el avatar elegido", async () => {
    const { onActualizado } = abrir();
    const user = userEvent.setup();

    await user.click(await screen.findByRole("button", { name: "Avatar Búho" }));
    await user.click(screen.getByRole("button", { name: /guardar avatar/i }));

    expect(api.asignarAvatar).toHaveBeenCalledWith("10", 1, 2);
    expect(onActualizado).toHaveBeenCalledWith("Avatar actualizado correctamente");
  });

  it("marca como elegido el avatar que ya tenía el alumno", async () => {
    abrir({ ...ALUMNO, avatarId: 2, avatarUrl: "https://ejemplo/2.png" });

    expect(await screen.findByRole("button", { name: "Avatar Búho" })).toHaveAttribute("aria-pressed", "true");
    expect(screen.getByRole("button", { name: "Avatar Zorro" })).toHaveAttribute("aria-pressed", "false");
  });

  it("genera el PIN, lo muestra una vez y cambia a Restablecer", async () => {
    const { onActualizado } = abrir();
    const user = userEvent.setup();

    await user.click(await screen.findByRole("button", { name: "Generar PIN" }));

    expect(api.generarPin).toHaveBeenCalledWith("10", 1);
    expect(await screen.findByRole("status")).toHaveTextContent("4821");
    expect(screen.getByRole("button", { name: "Restablecer PIN" })).toBeInTheDocument();
    expect(onActualizado).toHaveBeenCalledWith("PIN generado correctamente");
  });

  it("si el alumno ya tiene PIN el botón dice Restablecer", async () => {
    abrir({ ...ALUMNO, tienePin: true });
    expect(await screen.findByRole("button", { name: "Restablecer PIN" })).toBeInTheDocument();
  });

  it("muestra el error del servidor al guardar el avatar", async () => {
    api.asignarAvatar.mockRejectedValue(new Error("El avatar no existe"));
    abrir();
    const user = userEvent.setup();

    await user.click(await screen.findByRole("button", { name: "Avatar Zorro" }));
    await user.click(screen.getByRole("button", { name: /guardar avatar/i }));

    expect(await screen.findByText("El avatar no existe")).toBeInTheDocument();
  });
});
