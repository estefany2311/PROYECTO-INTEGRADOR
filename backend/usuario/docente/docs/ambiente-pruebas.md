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
| `GET /api/docente/aulas` | 200 · 3 aulas |
| `GET /api/docente/aulas/9/dashboard` | 200 · 3 estudiantes · 1 verde, 1 amarillo, 1 rojo |
| `GET /api/docente/aulas/5/estudiantes` | 200 |
| `GET /api/docente/aulas/5/lecturas` | 200 · 3 lecturas |
| `GET /api/docente/aulas/5/grupos` | 200 |
| `GET /v3/api-docs` | 200 · `API Docente` 1.0.0 · 14 rutas |
| `GET /swagger-ui.html` | 200 |
| Frontend `/`, `/alumnos`, `/grupos`, `/catalogo`, `/dashboard` | 200 y sin errores de consola |
| Leyenda del semáforo en `/dashboard` | 4 estados visibles · color + ícono + texto |
| Build de producción con `npm run preview` | 200 |
| CORS desde `http://localhost:5173` | 200 · `Access-Control-Allow-Origin` correcto |
| Backend `SemaforoServiceTest` | 8/8 · límites, redondeo y sin evaluaciones |
| Backend `test` | 153/153 |
| Frontend `test`, `lint`, `build` | 49/49 · OK · OK |
