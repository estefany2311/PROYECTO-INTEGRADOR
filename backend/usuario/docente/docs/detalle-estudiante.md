# Detalle del estudiante - Módulo Docente

**Tarea:** T-290 · **Historia:** US-29 · **Responsable:** Noemi · **Sprint:** 3

## 1. Objetivo

Entregar en una sola llamada la información que necesita la pantalla de detalle del
estudiante (T-292): sus niveles, el puntaje de cada competencia y las últimas
evaluaciones. El gráfico de evolución (T-293) y las fortalezas/dificultades (T-294)
reutilizan la misma respuesta.

## 2. Endpoint

```http
GET /api/docente/aulas/{aulaId}/estudiantes/{estudianteId}/detalle
X-Docente-Id: 1
```

La ruta vive bajo el aula para validar que el aula pertenece al docente. Además, T-291
valida que el alumno pertenezca a esa aula; si no, responde `404` con
`El alumno no existe en esta aula`.

## 3. Respuesta

```json
{
  "estudianteId": 7,
  "nombres": "Minji",
  "apellidos": "Prueba",
  "gradoId": 5,
  "gradoNombre": "5.º",
  "promedioGeneral": 82,
  "colorGeneral": "verde",
  "niveles": [
    {
      "id": 1,
      "orden": 1,
      "nombre": "Primeros pasos",
      "tipoReto": "lectura",
      "descripcion": "Lee palabras y oraciones cortas",
      "estado": "completado",
      "puntos": 10
    }
  ],
  "puntajesPorCompetencia": [
    {
      "competencia": "lectura",
      "promedio": 80.0,
      "evaluaciones": 2,
      "ultimoPuntaje": 70,
      "color": "verde"
    }
  ],
  "ultimasEvidencias": [
    {
      "id": 4,
      "competencia": "lectura",
      "puntaje": 70,
      "evaluadoEn": "2026-11-01T10:00:00"
    }
  ]
}
```

| Campo | Regla |
|---|---|
| `niveles` | Mismos niveles y estados del mapa del alumno (T-197). Si no hay progreso guardado, el primero está desbloqueado y el resto bloqueado. |
| `puntajesPorCompetencia` | Una entrada por competencia con evaluaciones, en orden lectura, escritura y oralidad. `promedio` es la media de todas las evaluaciones de esa competencia. |
| `ultimoPuntaje` | Puntaje de la evaluación más reciente de esa competencia. |
| `color` | Color del semáforo calculado con el promedio de la competencia (`docs/regla-semaforo.md`). |
| `promedioGeneral` | Media de los promedios de las competencias con evaluaciones, redondeada. Es `null` si no hay evaluaciones. |
| `colorGeneral` | Color del semáforo del promedio general; `gris` cuando no hay evaluaciones. |
| `ultimasEvidencias` | Hasta 10 evaluaciones, de la más reciente a la más antigua. La tabla de evidencias con audio/texto y estado de revisión ya existe (T-296, ver `docs/evidencias.md`); el detalle seguirá mostrando el historial de evaluaciones hasta que T-298 exponga las evidencias pendientes. |

## 4. Errores

| Situación | Estado | Mensaje |
|---|---|---|
| El aula no existe | `404` | `El aula no existe` |
| El aula es de otro docente | `403` | `El aula no pertenece al docente` |
| El alumno no existe o pertenece a otra aula | `404` | `El alumno no existe en esta aula` |

## 5. Pantalla de detalle (T-292, T-293 y T-294)

El frontend abre el detalle desde cada tarjeta del dashboard con la ruta
`/dashboard/aulas/{aulaId}/estudiantes/{estudianteId}`. La pantalla muestra:

- El promedio general y su semáforo (`colorGeneral`).
- Una tarjeta por competencia en el orden lectura, escritura y oralidad. Cada tarjeta
  usa el `promedio`, `evaluaciones`, `ultimoPuntaje` y `color` de la respuesta; el
  color va siempre acompañado de ícono y texto.
- La ruta de aprendizaje con el estado de cada nivel (`completado`, `desbloqueado` o
  `bloqueado`).
- Las últimas evaluaciones con competencia, puntaje y fecha.
- El gráfico de evolución (T-293), que ordena las últimas evaluaciones de la más antigua
  a la más reciente. Cada punto se colorea según la competencia, muestra su puntaje y
  usa las zonas del semáforo como fondo; si no hay evaluaciones se muestra un estado
  vacío y, con una sola, un aviso de que todavía no hay tendencia.
- El panel de fortalezas y dificultades (T-294), que reutiliza `puntajesPorCompetencia`:
  las competencias en verde son fortalezas y las rojas son dificultades. Las amarillas
  se listan aparte como **En proceso** y no cuentan en ninguno de los dos grupos; las
  grises se ignoran. Si un grupo queda vacío se muestra un mensaje y, sin evaluaciones,
  el panel completo usa el estado vacío.

Cuando el alumno no tiene evaluaciones, las tres competencias se muestran en gris con
el texto **Sin evaluaciones** y el promedio general aparece como `—`. Si el endpoint
responde con error (`404`, `403` o fallo de red), la pantalla muestra el mensaje y
permite reintentar o volver al dashboard. Las pruebas de la pantalla están en
`web-portal/docente/src/pages/DetalleEstudiante.test.jsx`.

## 6. Pruebas

| Prueba | Cobertura |
|---|---|
| `DetalleEstudianteServiceTest` | Niveles, promedios y color por competencia, redondeo, competencias sin evaluaciones, últimas diez evidencias, sin evaluaciones y errores. |
| `DetalleEstudianteControllerTest` | Ruta, forma de la respuesta, estado vacío, header obligatorio y traducción de errores a HTTP. |
| `EvaluacionRepositoryTest` | El historial del alumno llega de la evaluación más reciente a la más antigua. |
| T-291 | El alumno debe pertenecer al aula indicada; un alumno de otra aula devuelve `404`. |
| `DetalleEstudiante.test.jsx` | Resumen por competencia, fortalezas/dificultades, competencias parciales, aula activa, promedio general, niveles, últimas evaluaciones, estado sin datos y error con reintento (T-292, T-294 y T-295). |
| `GraficoEvolucion.test.jsx` | Orden cronológico, puntos por competencia, leyenda, etiqueta accesible, estado vacío y una sola evaluación (T-293). |
| `FortalezasDificultades.test.jsx` | Clasificación verde/rojo, ausencia de dificultades, competencias en proceso y estado vacío (T-294). |
| T-295 | Pruebas del detalle: color por competencia, redondeo, competencias sin evaluaciones, estado vacío, header obligatorio y competencias parciales en la pantalla. |
