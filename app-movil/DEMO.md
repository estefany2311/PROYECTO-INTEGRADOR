# Demo del módulo Estudiante (Sprint 2)

Guía para preparar la app móvil y los backends el día de la presentación (T-220).

## 1. Qué tiene que estar corriendo

| Servicio | Carpeta | Puerto | Para qué lo usa la app |
|---|---|---|---|
| Backend estudiante | `backend/usuario/estudiante` | 8082 | Perfiles, PIN, cerrar sesión, detalle del nivel |
| Backend docente | `backend/usuario/docente` | 8081 | Mapa de aventura (`GET /api/estudiante/mapa`) |

Cada backend necesita su `src/main/resources/application-local.properties` con la URL, el usuario
y la contraseña de Neon. Ese archivo **no se sube a git** (ver los `.example`).

## 2. Configurar la IP de la PC

La tablet se conecta a la PC por WiFi, así que necesita la IP de la PC **del día de la demo**.

1. En PowerShell: `ipconfig` → "Dirección IPv4" del adaptador Wi-Fi.
2. En `app-movil/local.properties` (no se sube a git):
   ```
   api.base.url=http://TU_IP:8082/
   api.docente.url=http://TU_IP:8081/
   ```
3. Android Studio → **Sync Now**.
4. Comprobar desde el navegador del móvil:
   `http://TU_IP:8082/api/estudiante/perfiles?codigoAula=DEMO123` debe mostrar los alumnos.

La IP cambia al cambiar de red. Si la app muestra "¡Ups! Algo no funcionó", lo primero es revisar `ipconfig`.
Usar el hotspot del celular como red de la demo ayuda a que la IP no cambie.

## 3. Generar el APK de pruebas

1. Android Studio → **Build → Build APK(s)**.
2. El archivo queda en `app-movil/app/build/outputs/apk/debug/app-debug.apk`.
3. Usar siempre el **debug**: la versión release solo permite conectarse al emulador (10.0.2.2).
4. Pasarlo al móvil e instalar (permitir "instalar apps desconocidas").

La IP queda guardada dentro del APK: si cambia, hay que volver a generarlo.
El APK no se sube a git (la carpeta `build/` está ignorada); se comparte comprimido o por Drive.

## 4. Lista antes de presentar

- [ ] Los dos backends levantados (8082 y 8081)
- [ ] PC y móvil en la misma red WiFi
- [ ] Firewall de Windows con los puertos 8081 y 8082 permitidos
- [ ] `local.properties` con la IP de hoy y APK generado después de cambiarla

## 5. Recorrido de la demo

| Paso | Qué mostrar | Historia |
|---|---|---|
| 1 | Código de aula **DEMO123** → "¿Quién eres?" con los avatares | US-02 |
| 2 | Elegir a **Ana** y escribir un PIN incorrecto (0000): círculos rojos, sacudida e intentos restantes | US-03 |
| 3 | PIN correcto **1234** → entra al mapa | US-02 |
| 4 | Mapa con el avance, el camino y los íconos de cada reto; girar la pantalla | US-05 |
| 5 | Tocar un nivel gris con candado → "¡Todavía no! Termina «…»" | US-06 |
| 6 | Tocar el nivel 1 → detalle con el tipo de reto y "Comenzar" | US-07 |
| 7 | "Salir" → diálogo con el avatar → vuelve a "¿Quién eres?" y el token deja de servir | US-04 |

Los retos (lectura, escritura, oralidad) se juegan a partir del Sprint 3: "Comenzar" muestra un aviso.
