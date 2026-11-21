# Pull Request — Sprint 4, módulo Docente

> Texto listo para pegar en GitHub al abrir el PR de `web-noemi` hacia `main`.
> No crea el PR automáticamente: lo abre la responsable de la rama, después de
> verificar que todo esté correcto.

## Título

```
Sprint 4 — Módulo Docente: comentario, seguimiento de tareas, fluidez lectora y puntaje de escritura
```

## Descripción

### Resumen

Cierra el Sprint 4 del módulo Docente (React + Spring Boot): retroalimentación del
docente, fecha límite y seguimiento de tareas, y el cálculo de fluidez lectora y
de puntaje de escritura que consume la app del alumno.

### Historias y tareas incluidas

| Historia | Tareas | Estado |
|---|---|---|
| US-32 · Audio o texto junto al resultado de la IA | T-371 a T-376 | Cerrada |
| US-33 · Aprobar o corregir la calificación | T-377 a T-382 | Cerrada |
| US-34 · Comentario de retroalimentación | T-383 a T-387 | Cerrada |
| US-38 · Fecha límite y seguimiento de tareas | T-388 a T-391 | Cerrada |
| US-15 · Fluidez lectora | T-392 a T-398 | Cerrada (backend) |
| US-22 · Puntaje de escritura | T-399 a T-404 | Cerrada (backend) |
| Generales | T-405 (mockups), T-406 (Swagger y entrega) | Ver pendientes |

### Cambios técnicos

- **US-34:** `PUT .../comentario`, caja de comentario en el detalle de la evidencia
  y `GET /api/estudiante/evidencias/{id}/comentario` para la app.
- **US-38:** `fecha_limite`, `estado` y `entregada_en` en `asignacion`; seguimiento
  con estado calculado (pendiente, entregada, vencida); pantalla **Tareas** con un
  color por estado; fecha límite opcional al asignar a un alumno o a un grupo;
  `POST /api/estudiante/asignaciones/{id}/entregar` para la app.
- **US-15:** `FormulaFluidez` (ritmo y pausas con la configuración de la US-51) y
  `GET /api/estudiante/evidencias/{id}/fluidez`.
- **US-22:** `FormulaEscritura`, mensajes de retroalimentación, suma de puntos y
  estrellas y `POST /api/estudiante/evidencias/{id}/escritura`.
- Detalle completo en `docs/sprint4.md`; colección Postman en
  `docs/postman-docente-sprint4.json`.

### Migraciones (Flyway)

| Versión | Contenido |
|---|---|
| V19 | `asignacion`: fecha límite, estado y fecha de entrega |
| V20 | `evidencia.puntaje_fluidez` |

### Pendiente de coordinar con Karla

- Llamar a `.../entregar` cuando el alumno termine una tarea asignada.
- Pedir `.../fluidez` y `.../escritura` desde las pantallas de resultado.
- Guardar las palabras con error del corrector en `evidencia.palabras_marcadas`.
- Decidir quién suma las estrellas al completar el nivel (hoy lo hace
  `POST .../escritura` para los textos).

### Cómo probar

1. `./mvnw test` en `backend/usuario/docente` y `npm test` en `web-portal/docente`.
2. Importar `docs/postman-docente-sprint4.json` y ajustar las variables.
3. En el portal: **Tareas** muestra el seguimiento; el detalle de una evidencia
   tiene la caja **Comentario para el alumno**.
