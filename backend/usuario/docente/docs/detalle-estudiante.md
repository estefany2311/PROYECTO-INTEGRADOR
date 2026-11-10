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

La ruta vive bajo el aula para validar que el aula pertenece al docente. La validación
de que el alumno pertenece a esa aula se completa en T-291.

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
| `ultimasEvidencias` | Hasta 10 evaluaciones, de la más reciente a la más antigua. La tabla de evidencias con audio/texto y estado de revisión llega en US-31 (T-296); por ahora se muestra el historial de evaluaciones. |

## 4. Errores

| Situación | Estado | Mensaje |
|---|---|---|
| El aula no existe | `404` | `El aula no existe` |
| El aula es de otro docente | `403` | `El aula no pertenece al docente` |
| El alumno no existe | `404` | `El alumno no existe` |

## 5. Pruebas

| Prueba | Cobertura |
|---|---|
| `DetalleEstudianteServiceTest` | Niveles, promedios por competencia, color, últimas diez evidencias, sin evaluaciones y errores. |
| `DetalleEstudianteControllerTest` | Ruta, forma de la respuesta y traducción de errores a HTTP. |
| `EvaluacionRepositoryTest` | El historial del alumno llega de la evaluación más reciente a la más antigua. |
