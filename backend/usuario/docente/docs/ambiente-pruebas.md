# Ambiente de pruebas — Módulo Docente

## URLs locales

| Servicio | URL |
|---|---|
| Backend | http://localhost:8081 |
| Swagger UI | http://localhost:8081/swagger-ui.html |
| OpenAPI JSON | http://localhost:8081/v3/api-docs |
| Frontend | http://localhost:5173 |

## Levantar el ambiente

Terminal 1, backend:

```powershell
cd backend\usuario\docente
.\mvnw.cmd spring-boot:run
```

Terminal 2, frontend:

```powershell
cd web-portal\docente
npm install
npm run dev
```

El frontend usa `VITE_API_URL`; si no está definida, apunta a `http://localhost:8081`.
Las rutas `/api/docente/**` requieren el header `X-Docente-Id`.

Para validar el build de producción:

```powershell
cd web-portal\docente
npm run build
npm run preview -- --port 4173
```

## Smoke test

| Verificación | Resultado |
|---|---|
| `GET /api/docente/grados` | 200 · 6 grados |
| `GET /api/docente/aulas` | 200 · 2 aulas |
| `GET /api/docente/aulas/5/estudiantes` | 200 |
| `GET /api/docente/aulas/5/lecturas` | 200 · 3 lecturas |
| `GET /api/docente/aulas/5/grupos` | 200 |
| `GET /v3/api-docs` | 200 · `API Docente` 1.0.0 · 13 rutas |
| `GET /swagger-ui.html` | 200 |
| Frontend `/`, `/alumnos`, `/grupos`, `/catalogo` | 200 y sin errores de consola |
| Build de producción con `npm run preview` | 200 |
| CORS desde `http://localhost:5173` | 200 · `Access-Control-Allow-Origin` correcto |
| Backend `clean test` | 141/141 |
| Frontend `test`, `lint`, `build` | 43/43 · OK · OK |
