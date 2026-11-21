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
| `GET /api/docente/aulas/9/evidencias` | 200 · 6 pendientes (3 audio y 3 texto) |
| `GET /api/docente/aulas/9/evidencias?tipo=audio` | 200 · 3 audios pendientes |
| `GET /api/docente/aulas/9/evidencias?tipo=video` | 400 · `El tipo de evidencia no es válido` |
| `GET /api/docente/aulas/9/evidencias` sin `X-Docente-Id` | 400 |
| `GET /api/docente/aulas/9/evidencias/{id}` | 200 · audio o texto, transcripción y puntaje de la IA |
| `GET /api/docente/aulas/9/evidencias/{id de otra aula}` | 404 · `La evidencia no existe en esta aula` |
| `GET /api/docente/aulas/9/evidencias/{id}` sin `X-Docente-Id` | 400 |
| `GET /api/docente/aulas/9/evidencias/{id}/audio` | 200 · `audio/mpeg` · `Accept-Ranges: bytes` |
| `GET .../audio` con `Range: bytes=0-99` | 206 · `Content-Range: bytes 0-99/1024` |
| `GET .../audio` con `Range: bytes=5000-6000` | 416 · `Content-Range: bytes */1024` |
| `GET .../audio` de una evidencia de texto | 400 · `La evidencia no es de audio` |
| `GET .../audio` de un aula ajena | 403 · `El aula no pertenece al docente` |
| `GET .../audio` sin `X-Docente-Id` | 400 |
| `GET /api/docente/aulas/5/estudiantes` | 200 |
| `GET /api/docente/aulas/5/lecturas` | 200 · 3 lecturas · cada una con `fuente` y `urlRecurso` |
| `GET /api/docente/aulas/5/grupos` | 200 |
| `GET /v3/api-docs` | 200 · `API Docente` 1.0.0 · 21 operaciones |
| `GET /swagger-ui.html` | 200 |
| Frontend `/`, `/alumnos`, `/grupos`, `/catalogo`, `/dashboard`, `/evidencias` | 200 y sin errores de consola |
| Leyenda del semáforo en `/dashboard` | 4 estados visibles · color + ícono + texto |
| Aviso "Necesitan refuerzo" en `/dashboard` (aula 9) | visible · 1 alumno rojo · Ester · 50/100 |
| Contador de alumnos en rojo en el menú (aula 9) | badge 1 junto a Dashboard |
| Filtros de grado y competencia en `/dashboard` (aula 9) | Grado 1.º → Ester · Lectura → solo lectura · sin alumnos → aviso de filtros |
| Persistencia de filtros en `/dashboard` | Salir y volver conserva grado y competencia · el aula activa se restaura |
| Detalle del alumno en `/dashboard/aulas/9/estudiantes/{id}` | 200 · resumen por competencia, niveles y últimas evaluaciones · vuelve al dashboard |
| Gráfico de evolución en el detalle del alumno | visible · puntos ordenados por fecha · leyenda por competencia · zonas del semáforo |
| Fortalezas y dificultades en el detalle del alumno | visible · Lectura fortaleza · Oralidad dificultad · Escritura en proceso |
| Lista de evidencias en `/evidencias` (aula 9) | 6 pendientes · 3 audio y 3 texto · botón Escuchar audio y enlace al detalle del alumno |
| Reproducir un audio en `/evidencias` | El botón descarga el audio con `X-Docente-Id` y aparece el reproductor con el audio |
| Reproducir un audio de otro docente | 403 · mensaje y botón Reintentar audio |
| Abrir `/evidencias/9/3` | Audio original + transcripción lado a lado, puntaje 85 y resultado de la IA |
| Palabras marcadas en `/evidencias/9/3` | «brilla» y «juegan» resaltadas · aviso «La IA marcó 2 palabras para revisar» |
| Abrir `/evidencias/9/6` (texto) | Texto original y aviso de que la IA evaluó el texto directamente |
| Aprobar la nota en `/evidencias/9/3` | Estado Aprobada · nota final 85 · mensaje de confirmación · historial actualizado |
| Corregir la nota en `/evidencias/9/3` | Formulario con nota 0–100 y motivo · guarda nota 70 y motivo · estado Corregida |
| Corregir sin nota o sin motivo | Mensajes de validación del formulario · no llama al backend |
| Corregir con una nota fuera de 0–100 | Mensaje de validación del formulario · no llama al backend |
| Evidencia sin evaluación de la IA | Botón Aprobar deshabilitado con el aviso correspondiente |
| `/evidencias/9/999` | 404 · mensaje y botón Reintentar |
| Filtro por tipo en `/evidencias` | Audio → 3 · Texto → 3 · sin resultados → aviso del filtro |
| Build de producción con `npm run preview` | 200 |
| CORS desde `http://localhost:5173` | 200 · `Access-Control-Allow-Origin` correcto · orígenes configurables con `APP_CORS_ORIGENES` |
| Colección `docs/postman-docente-sprint3.json` | 6 grupos · 23 requests · JSON válido |
| Backend `SemaforoServiceTest` | 8/8 · límites, redondeo y sin evaluaciones |
| Backend `RendimientoServiceTest` | 7/7 · filtros combinados, validaciones y `soloRojo` |
| Backend `DashboardAulaControllerTest` | 6/6 · reenvío de filtros combinados y errores |
| Backend `DetalleEstudianteServiceTest` | 10/10 · niveles, promedios, colores, redondeo, últimas diez evidencias y errores |
| Backend `DetalleEstudianteControllerTest` | 6/6 · ruta, respuesta, estado vacío, header y errores HTTP |
| Backend `EvaluacionRepositoryTest` | 2/2 · promedio por aula e historial del alumno |
| Backend `MigracionEvidenciaTest` | 6/6 · tabla, audio/texto, estado de revisión, cascada e índices |
| Backend `MigracionEvidenciaDatosPruebaTest` | 5/5 · datos de audio y texto, estados y niveles por orden |
| Backend `EvidenciaRepositoryTest` | 5/5 · pendientes por fecha y filtro por tipo, detalle por id y aula, evidencia de otra aula, y calificación final con estado `corregida` y motivo |
| Backend `HistorialCalificacionRepositoryTest` | 2/2 · historial por evidencia ordenado y borrado en cascada con la evidencia |
| Backend `EvidenciaServiceTest` | 29/29 · mapeo, filtro por tipo y texto, tipo en blanco, tipo inválido, resultado IA nulo, aula del docente, detalle con transcripción y puntaje, palabras marcadas, aprobar/corregir con motivo, recálculo de estrellas al cambiar la nota, historial de calificaciones, aprobar después de corregir y acumulación de ambas revisiones |
| Backend `EvidenciaControllerTest` | 33/33 · ruta, respuesta de audio y texto, filtro por tipo, detalle, detalle rechazado, detalle sin evaluación, audio completo, 206, 416, aprobar, corregir, validaciones (motivo, rango, nota negativa, 500 caracteres), 404 en corrección, header obligatorio, errores e historial en el detalle |
| Backend `AlmacenAudioTest` | 14/14 · ruta relativa con subcarpeta, URL y query, nombre simple, carpeta, rutas inválidas y tipo MIME |
| Backend `AudioEvidenciaServiceTest` | 6/6 · audio del aula, texto, otra aula, aula inexistente, aula ajena y archivo faltante |
| Backend `MigracionEvidenciaDetalleTest` | 3/3 · columnas de V13, rango 0-100 del puntaje y datos de prueba |
| Backend `MigracionEvidenciaPalabrasTest` | 2/2 · columna de V14 y palabras marcadas en las evidencias de prueba sin ids quemados |
| Backend `MigracionEvidenciaCalificacionTest` | 3/3 · columna `calificacion_final`, rango 0-100 y estado `corregida` en el `CHECK` |
| Backend `MigracionEvidenciaMotivoTest` | 1/1 · columna `motivo_correccion` de V16 |
| Backend `MigracionHistorialCalificacionTest` | 4/4 · tabla, cascada, límites de notas y acciones, e índice del historial (V17) |
| Backend `MigracionPuntajeEstudianteTest` | 6/6 · tabla, una fila por alumno, acumulados desde cero, valores no negativos, actualización y cascada |
| Backend `PuntajeServiceTest` | 16/16 · acumulado nuevo y existente, umbrales de estrellas, puntajes límite 0 y 100, tipo y puntaje inválidos, alumno inexistente y recálculo al cambiar la nota (sube, baja, tope en 0, sin fila y sin cambio de tramo) |
| Backend `ReglaPuntuacionTest` | 3/3 · estrellas por umbral, umbrales crecientes y puntos no negativos |
| Backend `PuntajeEstudianteRepositoryTest` | 5/5 · acumulado por alumno, fila nueva en cero, único por alumno y cascada |
| Backend `MapaEstudianteServiceTest` | 9/9 · niveles y estados, acumulado de puntos y estrellas, respaldo del progreso y 404 |
| Backend `MapaEstudianteControllerTest` | 2/2 · mapa con puntos y estrellas, y 404 |
| Backend `CatalogoServiceTest` | 13/13 · grados del aula, filtros, aula ajena, sin grados y fuente/enlace de la lectura oficial |
| Backend `CatalogoControllerTest` | 4/4 · catálogo con `fuente` y `urlRecurso`, filtros, 403 y 404 |
| Backend `ConfiguracionCorsTest` | 4/4 · origen local, varios orígenes, espacios y lista vacía |
| Backend `OpenApiConfigTest` | 4/4 · servidor, información, esquemas de seguridad y orden de secciones |
| Backend `test` | 334/334 |
| Frontend `DetalleEstudiante.test.jsx` | 10/10 · resumen por competencia, fortalezas/dificultades, competencias parciales, aula activa, sin datos y error con reintento |
| Frontend `GraficoEvolucion.test.jsx` | 5/5 · orden, leyenda, etiqueta accesible y estados vacío/una evaluación |
| Frontend `FortalezasDificultades.test.jsx` | 4/4 · clasificación, sin dificultades, amarillo y estado vacío |
| Frontend `Evidencias.test.jsx` | 13/13 · tarjetas, orden, conteo por tipo, filtro y vuelta a todos los tipos, aula activa, cambio de aula, Actualizar, sin aulas, vacío, error y enlace Revisar evidencia |
| Frontend `DetalleEvidencia.test.jsx` | 18/18 · audio y transcripción lado a lado, palabras marcadas resaltadas (audio y texto), texto sin transcripción, detalle sin `palabrasMarcadas`, avisos sin transcripción/puntaje, actualización con el botón Actualizar, error con reintento, estado rechazada, aprobar, corregir con formulario, validaciones, cancelar, error del backend, error al aprobar y flujo aprobar → corregir |
| Frontend `ReproductorAudio.test.jsx` | 5/5 · descarga al pulsar, header del docente, blob, error con reintento, liberación y nombre genérico |
| Frontend `Catalogo.test.jsx` | 12/12 · lecturas, filtros, búsqueda, modal, asignación, etiqueta Oficial y enlace al recurso |
| Frontend `test`, `lint`, `build` | 122/122 · OK · OK |
