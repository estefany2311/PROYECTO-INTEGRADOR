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
| `GET /api/docente/aulas/9/dashboard?soloRojo=true` | 200 · 1 estudiante rojo · resumen del aula completo |
| `GET /api/docente/aulas/9/dashboard?gradoId=1` | 200 · 1 estudiante (Ester) · 6 grados en el selector · 3 promedios por competencia · general 50 · rojo |
| `GET /api/docente/aulas/9/dashboard?competencia=lectura` | 200 · 3 estudiantes · 1 promedio de lectura · general 73 · amarillo |
| `GET /api/docente/aulas/9/dashboard?gradoId=1&competencia=lectura` | 200 · 1 estudiante (Ester) · 1 promedio de lectura · general 55 · rojo |
| `GET /api/docente/aulas/9/dashboard?gradoId=99` | 400 · `El grado no corresponde a esta aula` |
| `GET /api/docente/aulas/9/dashboard?competencia=matematica` | 400 · `La competencia no es válida` |
| `GET /api/docente/aulas/9/estudiantes/{id}/detalle` | 200 · niveles, 3 competencias y últimas evidencias |
| `GET /api/docente/aulas/9/estudiantes/999/detalle` | 404 · `El alumno no existe en esta aula` |
| `GET /api/docente/aulas/9/estudiantes/{id de otra aula}/detalle` | 404 · `El alumno no existe en esta aula` |
| `GET /api/docente/aulas/9/estudiantes/{id}/detalle` sin `X-Docente-Id` | 400 |
| `GET /api/docente/aulas/5/estudiantes` | 200 |
| `GET /api/docente/aulas/5/lecturas` | 200 · 3 lecturas |
| `GET /api/docente/aulas/5/grupos` | 200 |
| `GET /v3/api-docs` | 200 · `API Docente` 1.0.0 · 15 rutas |
| `GET /swagger-ui.html` | 200 |
| Frontend `/`, `/alumnos`, `/grupos`, `/catalogo`, `/dashboard` | 200 y sin errores de consola |
| Leyenda del semáforo en `/dashboard` | 4 estados visibles · color + ícono + texto |
| Aviso "Necesitan refuerzo" en `/dashboard` (aula 9) | visible · 1 alumno rojo · Ester · 50/100 |
| Contador de alumnos en rojo en el menú (aula 9) | badge 1 junto a Dashboard |
| Filtros de grado y competencia en `/dashboard` (aula 9) | Grado 1.º → Ester · Lectura → solo lectura · sin alumnos → aviso de filtros |
| Persistencia de filtros en `/dashboard` | Salir y volver conserva grado y competencia · el aula activa se restaura |
| Detalle del alumno en `/dashboard/aulas/9/estudiantes/{id}` | 200 · resumen por competencia, niveles y últimas evaluaciones · vuelve al dashboard |
| Gráfico de evolución en el detalle del alumno | visible · puntos ordenados por fecha · leyenda por competencia · zonas del semáforo |
| Fortalezas y dificultades en el detalle del alumno | visible · Lectura fortaleza · Oralidad dificultad · Escritura en proceso |
| Build de producción con `npm run preview` | 200 |
| CORS desde `http://localhost:5173` | 200 · `Access-Control-Allow-Origin` correcto |
| Backend `SemaforoServiceTest` | 8/8 · límites, redondeo y sin evaluaciones |
| Backend `RendimientoServiceTest` | 7/7 · filtros combinados, validaciones y `soloRojo` |
| Backend `DashboardAulaControllerTest` | 6/6 · reenvío de filtros combinados y errores |
| Backend `DetalleEstudianteServiceTest` | 10/10 · niveles, promedios, colores, redondeo, últimas diez evidencias y errores |
| Backend `DetalleEstudianteControllerTest` | 6/6 · ruta, respuesta, estado vacío, header y errores HTTP |
| Backend `EvaluacionRepositoryTest` | 2/2 · promedio por aula e historial del alumno |
| Backend `MigracionEvidenciaTest` | 6/6 · tabla, audio/texto, estado de revisión, cascada e índices |
| Backend `MigracionEvidenciaDatosPruebaTest` | 5/5 · datos de audio y texto, estados y niveles por orden |
| Backend `test` | 191/191 |
| Frontend `DetalleEstudiante.test.jsx` | 10/10 · resumen por competencia, fortalezas/dificultades, competencias parciales, aula activa, sin datos y error con reintento |
| Frontend `GraficoEvolucion.test.jsx` | 5/5 · orden, leyenda, etiqueta accesible y estados vacío/una evaluación |
| Frontend `FortalezasDificultades.test.jsx` | 4/4 · clasificación, sin dificultades, amarillo y estado vacío |
| Frontend `test`, `lint`, `build` | 82/82 · OK · OK |
