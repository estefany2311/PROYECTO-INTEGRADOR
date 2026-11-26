import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { iniciarSesion, salir } from "./auth";
import { listarAulas } from "./api";
import { abrirSesion, cerrarSesion, obtenerSesion } from "./sesion";

const respuesta = (status, cuerpo) => ({
  ok: status >= 200 && status < 300,
  status,
  json: () => Promise.resolve(cuerpo),
});

const SESION = { access: "viejo", refresh: "r", docente: { id: 15, nombre: "Docente" } };

let fetchFalso;

beforeEach(() => {
  cerrarSesion();
  fetchFalso = vi.fn();
  vi.stubGlobal("fetch", fetchFalso);
});

afterEach(() => vi.unstubAllGlobals());

const NUEVA = {
  access: "a",
  refresh: "r",
  docente: { id: 15, usuario: "prueba", nombre: "Docente de Prueba" },
};

describe("Inicio de sesión", () => {
  it("guarda los tokens y los datos de la cuenta docente", async () => {
    fetchFalso.mockResolvedValueOnce(respuesta(200, NUEVA));

    await iniciarSesion("prueba", "clave-de-prueba");

    expect(obtenerSesion()).toEqual(NUEVA);
    expect(fetchFalso.mock.calls[0][0]).toBe("http://localhost:8081/api/auth/login");
    expect(JSON.parse(fetchFalso.mock.calls[0][1].body)).toEqual({
      username: "prueba",
      password: "clave-de-prueba",
    });
  });

  it.each([
    [401, "Usuario o contraseña incorrectos"],
    [403, "Esta cuenta no es de docente"],
    [429, "Demasiados intentos. Espera unos minutos y vuelve a probar"],
  ])("muestra el motivo del rechazo (%i)", async (status, mensaje) => {
    fetchFalso.mockResolvedValueOnce(respuesta(status, { mensaje }));

    await expect(iniciarSesion("prueba", "x")).rejects.toThrow(mensaje);
    expect(obtenerSesion()).toBeNull();
  });

  it("avisa si el servidor está apagado", async () => {
    fetchFalso.mockRejectedValueOnce(new TypeError("Failed to fetch"));

    await expect(iniciarSesion("prueba", "x")).rejects.toThrow(/conectar con el servidor/);
  });
});

describe("Cerrar sesión", () => {
  it("sale del portal y pide anular los tokens", async () => {
    abrirSesion(SESION);
    fetchFalso.mockResolvedValueOnce(respuesta(204, null));

    await salir();

    expect(obtenerSesion()).toBeNull();
    expect(fetchFalso.mock.calls[0][0]).toBe("http://localhost:8081/api/auth/logout");
    expect(JSON.parse(fetchFalso.mock.calls[0][1].body)).toEqual({ refresh: "r" });
  });

  it("sale aunque no haya conexión", async () => {
    abrirSesion(SESION);
    fetchFalso.mockRejectedValueOnce(new TypeError("Failed to fetch"));

    await expect(salir()).resolves.toBeUndefined();
    expect(obtenerSesion()).toBeNull();
  });
});

describe("Peticiones con sesión", () => {
  it("envían el token y nunca el id del docente", async () => {
    abrirSesion(SESION);
    fetchFalso.mockResolvedValueOnce(respuesta(200, []));

    await listarAulas();

    const cabeceras = fetchFalso.mock.calls[0][1].headers;
    expect(cabeceras.Authorization).toBe("Bearer viejo");
    expect(cabeceras).not.toHaveProperty("X-Docente-Id");
  });

  it("si el token venció lo renuevan y repiten la petición", async () => {
    abrirSesion(SESION);
    fetchFalso
      .mockResolvedValueOnce(respuesta(401, { mensaje: "Tu sesión terminó" }))
      .mockResolvedValueOnce(respuesta(200, { access: "nuevo" }))
      .mockResolvedValueOnce(respuesta(200, [{ id: 1 }]));

    await expect(listarAulas()).resolves.toEqual([{ id: 1 }]);

    expect(fetchFalso.mock.calls[2][1].headers.Authorization).toBe("Bearer nuevo");
    expect(obtenerSesion().access).toBe("nuevo");
  });

  it("si ya no se puede renovar cierran la sesión", async () => {
    abrirSesion(SESION);
    fetchFalso
      .mockResolvedValueOnce(respuesta(401, { mensaje: "Tu sesión terminó" }))
      .mockResolvedValueOnce(respuesta(401, {}));

    await expect(listarAulas()).rejects.toThrow("Tu sesión terminó");
    expect(obtenerSesion()).toBeNull();
  });

  it("un fallo pasajero al renovar no cierra la sesión", async () => {
    abrirSesion(SESION);
    fetchFalso
      .mockResolvedValueOnce(respuesta(401, {}))
      .mockResolvedValueOnce(respuesta(500, {}));

    await expect(listarAulas()).rejects.toThrow(/renovar tu sesión/);
    expect(obtenerSesion()).not.toBeNull();
  });
});
