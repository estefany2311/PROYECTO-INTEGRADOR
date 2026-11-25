# Sprint 4 — Módulo Docente (US-34, US-38, US-15, US-22)

Resumen técnico de lo que se agregó al backend docente (Spring Boot) y al portal
(React) en el Sprint 4. Las historias US-32 y US-33 están documentadas en
`docs/evidencias.md`.

## Rutas nuevas

### Portal del docente (header `X-Docente-Id`)

| Método y ruta | Historia | Qué hace |
|---|---|---|
| `PUT /api/docente/aulas/{aulaId}/evidencias/{evidenciaId}/comentario` | US-34 (T-384) | Guarda el comentario de retroalimentación (obligatorio, máximo 500 caracteres). |
| `GET /api/docente/aulas/{aulaId}/asignaciones?estado=` | US-38 (T-389) | Seguimiento de tareas: resumen (`pendientes`, `entregadas`, `vencidas`) y lista. `estado` es opcional: `pendiente`, `entregada` o `vencida`. |
| `POST /api/docente/aulas/{aulaId}/asignaciones` | US-38 (T-388) | Ahora acepta `fechaLimite` opcional (no puede estar en el pasado). |
| `POST /api/docente/aulas/{aulaId}/grupos/{grupoId}/asignaciones` | US-38 (T-388) | Igual: `fechaLimite` opcional para todas las tareas del grupo. |

### App del alumno (header `Authorization: Bearer <token>`)

El id del alumno sale del token. Solo ve y entrega lo suyo.

| Método y ruta | Historia | Qué hace |
|---|---|---|
| `GET /api/estudiante/evidencias/{id}/comentario` | US-34 (T-386) | Comentario del docente en una evidencia (`comentario` es `null` si no hay). |
| `GET /api/estudiante/evidencias/{id}/fluidez` | US-15 (T-397) | Puntaje de fluidez 0 a 100, palabras por minuto, pausas largas y vacilaciones. `409` si la lectura todavía se procesa. |
| `POST /api/estudiante/evidencias/{id}/escritura` | US-22 (T-403) | Puntaje de escritura, desglose, estrellas y mensaje. Guarda el puntaje y actualiza los puntos y estrellas del alumno. |
| `POST /api/estudiante/evidencias/{id}/lectura` | US-09 | Cuenta una lectura u oralidad en los puntos y estrellas del alumno. La app lo llama cuando la evidencia pasa a `listo`. Idempotente; `409` si se está procesando. |
| `POST /api/estudiante/asignaciones/{id}/entregar` | US-38 (T-389) | Marca la tarea como entregada (idempotente). |

## Migraciones

| Versión | Contenido |
|---|---|
| V19 | `asignacion`: `fecha_limite` (opcional), `estado` (`pendiente` o `entregada`) y `entregada_en`. |
| V20 | `evidencia.puntaje_fluidez` (0 a 100, opcional). |
| V21 | `puntaje_nivel_estudiante`: mejor intento por alumno y nivel (nota, puntos y estrellas). |

"Vencida" no se guarda: se calcula (pendiente con la fecha límite ya pasada), así
no hay que actualizar filas cuando pasa el tiempo.

## Fórmulas

**Fluidez (US-15)** — `FormulaFluidez`. Parámetros del grado de la US-51
(`configuracion_evaluacion`: palabras por minuto y tolerancia de pausas).

- Ritmo = palabras por minuto leídas / esperadas (tope 100).
- Pausas = 100 − 10 por pausa larga (mayor a la tolerancia) − 3 por vacilación
  (entre la mitad de la tolerancia y la tolerancia).
- Fluidez = 60 % ritmo + 40 % pausas.

**Escritura (US-22)** — `FormulaEscritura`.

- Ortografía (50 %): baja 200 puntos por cada 100 % de palabras con error.
- Longitud (30 %): palabras escritas / palabras mínimas de la consigna del nivel.
- Cumplimiento de la consigna (20 %): proporción de oraciones completas
  (mayúscula inicial y punto final). Es una aproximación por estructura.
- Estrellas: umbrales 60 / 80 / 95 de la US-53 (`PuntajeService`).

## Datos de otros módulos que se leen (solo lectura, SQL directo)

| Tabla | Dueña | Se usa en | Si no existe |
|---|---|---|---|
| `configuracion_evaluacion` | Daniela (US-51) | Fluidez | Valores iniciales por grado |
| `evidencia_palabra` | Karla (V7 de su backend) | Fluidez | `409` "se está procesando" |
| `consigna_nivel` | Karla (V4 de su backend) | Escritura | 10 palabras mínimas |

## Coordinación con Karla

- **Entrega de tareas:** `evidencia` no está ligada a `lectura`, así que la app
  debe llamar a `POST /api/estudiante/asignaciones/{id}/entregar` cuando el
  alumno termine el reto de la lectura asignada.
- **Fluidez:** la app llama a `GET .../fluidez` cuando la evidencia pasa a
  `listo` (T-416).
- **Escritura:** el texto enviado (T-434) debe guardar en `evidencia.palabras_marcadas`
  las palabras con error del corrector (US-19), separadas por comas; de ahí sale
  el conteo de errores.
- **Puntos y estrellas:** el total de `puntaje_estudiante` (US-09) solo lo escribe
  este backend; la app de Karla guarda su progreso y no lo toca (ver la regla
  única de abajo).

## Regla única de puntos y estrellas (acordada con Karla y Daniela)

- **Reglas:** puntos máximos y umbrales de estrellas salen de `reglas_puntuacion`
  (US-53, Daniela). Sin fila para el tipo de reto: 100 puntos y umbrales 60 / 80 / 95.
- **Nota de 0 a 100 de un intento:** la calificación final del docente si existe; si
  no, lectura = precisión de la IA (`puntaje_ia`, de Karla), escritura = puntaje de
  escritura (`puntaje_ia`) y oralidad = fluidez (`puntaje_fluidez`). La fluidez de
  una lectura se muestra pero no cambia su nota.
- **Puntos** = puntos máximos × nota / 100, redondeado. **Estrellas** según los umbrales.
- **Mejor intento por alumno y nivel** (`puntaje_nivel_estudiante`): si el alumno
  mejora se suma solo la diferencia; si el docente corrige la nota se recalcula
  (puede restar). El total nunca queda negativo.
- `POST .../lectura` y `POST .../escritura` recalculan el nivel; `.../lectura`
  recalcula todos los niveles del alumno, así que también cuenta las lecturas listas
  que quedaron sin contar (por ejemplo, las enviadas sin internet).
- El desbloqueo de niveles (`reglas_desbloqueo`, Daniela) lo resuelve el módulo
  estudiante.

**Ojo con los datos anteriores:** el total que ya estaba en `puntaje_estudiante`
(100 puntos fijos por reto) no tiene fila por nivel; al recalcular un nivel por
primera vez se suman sus puntos nuevos encima. Para datos de prueba conviene
reiniciar `puntaje_estudiante`.

## Pruebas

Colección Postman: `docs/postman-docente-sprint4.json`.
