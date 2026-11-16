# Despliegue del módulo Docente (T-307)

Arquitectura usada:

```
Vercel (React + Vite)  ──►  Render (Spring Boot, Docker)  ──►  Neon (PostgreSQL)
   web-portal/docente           backend/usuario/docente          base compartida
```

- **Frontend:** Vercel, directorio `web-portal/docente`.
- **Backend:** Render con el `Dockerfile` del módulo, directorio `backend/usuario/docente`.
- **Base de datos:** Neon, la misma que ya usa el proyecto. Flyway aplica las
  migraciones V1–V12 al arrancar.

## 1. Backend en Render

1. New → Web Service → conectar el repositorio.
2. Configuración:
   - **Root Directory:** `backend/usuario/docente`
   - **Runtime:** Docker
   - **Health Check Path:** `/api/docente/grados` (no pide header)
3. Variables de entorno:

| Variable | Valor |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<host-neon>/plataforma_educativa?sslmode=require&channel_binding=require` |
| `SPRING_DATASOURCE_USERNAME` | usuario de Neon |
| `SPRING_DATASOURCE_PASSWORD` | contraseña de Neon |
| `SPRING_PROFILES_ACTIVE` | `prod` (evita cargar `application-local.properties`) |
| `APP_CORS_ORIGENES` | `https://<tu-app>.vercel.app` (varios, separados por coma) |
| `APP_OPENAPI_SERVIDOR` | `https://<tu-api>.onrender.com` |

El puerto lo toma de la variable `PORT` que inyecta Render
(`server.port=${PORT:8081}`); en local sigue siendo 8081.

> El `Dockerfile` no se pudo construir en la máquina de desarrollo (Docker no
> está instalado), así que el primer deploy de Render es también la validación
> de la imagen. Si el build falla, revisar el log de Render antes de tocar código.

> No subas credenciales al repositorio: Render las guarda como variables de entorno.
> `application-local.properties` es solo para desarrollo.

## 2. Frontend en Vercel

1. Add New → Project → importar el repositorio.
2. Configuración:
   - **Root Directory:** `web-portal/docente`
   - **Framework Preset:** Vite
   - **Build Command:** `npm run build`
   - **Output Directory:** `dist`
3. Variable de entorno:

| Variable | Valor |
|---|---|
| `VITE_API_URL` | `https://<tu-api>.onrender.com` |

`vercel.json` ya incluye la regla de SPA para que React Router funcione al
recargar `/dashboard`, `/alumnos`, etc.

## 3. CORS

Antes cada controlador tenía `@CrossOrigin` fijo en `localhost:5173`. Ahora la
configuración es central (`ConfiguracionCors`) y se controla con
`APP_CORS_ORIGENES`. Si cambias el dominio de Vercel, actualiza esa variable y
reinicia el servicio.

## 4. Swagger en producción

- UI: `https://<tu-api>.onrender.com/swagger-ui.html`
- JSON: `https://<tu-api>.onrender.com/v3/api-docs`

Usa **Authorize** con `X-Docente-Id` para las rutas del portal y con el token de
la tablet para `GET /api/estudiante/mapa`.

## 5. Verificación después del despliegue

```powershell
$api = "https://<tu-api>.onrender.com"

Invoke-RestMethod "$api/api/docente/grados"
Invoke-RestMethod "$api/api/docente/aulas" -Headers @{ "X-Docente-Id" = "1" }
Invoke-RestMethod "$api/api/docente/aulas/5/lecturas" -Headers @{ "X-Docente-Id" = "1" }
Invoke-RestMethod "$api/v3/api-docs" | Select-Object -ExpandProperty info
```

| Verificación | Resultado esperado |
|---|---|
| `GET /api/docente/grados` | 200 · 6 grados |
| `GET /api/docente/aulas` con header | 200 · aulas del docente |
| `GET /api/docente/aulas/{id}/lecturas` | 200 · lecturas con `fuente` y `urlRecurso` |
| `GET /v3/api-docs` | 200 · `API Docente` 1.0.0 |
| Frontend desplegado | carga `/`, `/dashboard`, `/catalogo` sin 404 al recargar |
| CORS | el navegador no bloquea las llamadas al backend |

## 6. Rollback

- **Render:** Deploys → elegir el deploy anterior → *Redeploy*.
- **Vercel:** Deployments → *Promote to Production* del deploy anterior.
- **Base de datos:** las migraciones son solo de avance. Para revertir un cambio
  de esquema, restaurar el backup de Neon; no editar `flyway_historial_docente`
  a mano.

## 7. CI

`.github/workflows/docente-api.yml` y `docente-web.yml` corren en cada push:

- Backend: PostgreSQL 17 temporal + `./mvnw -B test` (no usa Neon).
- Frontend: `npm ci`, `npm run lint`, `npm run build`.

Si el backend agrega columnas a tablas de Admin, actualizar también
`src/test/resources/db/ci/beforeMigrate.sql`.
