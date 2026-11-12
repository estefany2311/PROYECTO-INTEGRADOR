# Puntos y estrellas del estudiante - Módulo Docente

**Tarea:** T-301 · **Historia:** US-09 · **Responsable:** Noemi · **Sprint:** 3

## 1. Objetivo

Guardar los **puntos** y las **estrellas acumuladas** por cada estudiante para que el
mapa de aventura pueda mostrarlos en su cabecera y el alumno vea su progreso
gamificado. La tabla la usan el módulo **docente** (consulta y suma) y el de
**estudiante** (mapa), y el servicio que la actualiza llega en T-302 con las reglas
de puntuación de la US-53.

## 2. Tabla `puntaje_estudiante`

Migración: `backend/usuario/docente/src/main/resources/db/migration/V12__puntaje_estudiante.sql`.

| Columna | Tipo | Regla |
|---|---|---|
| `id` | `BIGSERIAL` | Clave primaria. |
| `estudiante_id` | `BIGINT` | Obligatorio y **único**: una sola fila por alumno. Referencia a `estudiante` y se borra en cascada con él. |
| `puntos` | `INT` | Puntos acumulados; por defecto `0` y nunca negativos. |
| `estrellas` | `INT` | Estrellas acumuladas; por defecto `0` y nunca negativas. |
| `actualizado_en` | `TIMESTAMP` | Última vez que cambió el acumulado; por defecto `now()`. |

### Reglas de integridad

- `UNIQUE (estudiante_id)` garantiza un único acumulado por alumno, así la suma de
  T-302 siempre actualiza la misma fila.
- Los `CHECK` `puntos >= 0` y `estrellas >= 0` impiden acumulados negativos.
- `ON DELETE CASCADE` limpia el puntaje cuando se borra el alumno.

## 3. Modelo Java

- `model/PuntajeEstudiante.java`: entidad JPA con la fábrica
  `PuntajeEstudiante.inicial(estudiante)`, que deja los acumulados en cero.
- `model/ReglaPuntuacion.java`: puntos por reto y umbrales de 1, 2 y 3 estrellas.
  Valida que los umbrales sean crecientes y calcula las estrellas de un puntaje.
- `repository/PuntajeEstudianteRepository.java`: `findByEstudianteId(...)` con el
  alumno cargado con `@EntityGraph`.
- `dto/PuntajeEstudianteResponse.java`: acumulado actualizado del alumno.

## 4. Servicio de suma (T-302)

`service/PuntajeService.java` expone
`sumarAlCompletarReto(estudianteId, tipoReto, puntajeObtenido)`. Al completar un reto:

1. Valida el tipo (`lectura`, `escritura` u `oralidad`) y el puntaje (0 a 100).
2. Busca al alumno; si no existe responde 404.
3. Crea su fila en `puntaje_estudiante` si todavía no tiene.
4. Suma los puntos de la regla y las estrellas que correspondan al puntaje.

### Reglas iniciales (US-53)

Mientras el módulo Admin expone sus reglas (T-260), el servicio usa los valores
iniciales de la US-53 centralizados en `model/ReglaPuntuacion.java`:

| Tipo de reto | Puntos por reto | 1 estrella | 2 estrellas | 3 estrellas |
|---|---|---|---|---|
| lectura | 100 | ≥ 60 | ≥ 80 | ≥ 95 |
| escritura | 100 | ≥ 60 | ≥ 80 | ≥ 95 |
| oralidad | 100 | ≥ 60 | ≥ 80 | ≥ 95 |

Los puntos se suman siempre que el reto se completa, aunque el puntaje no alcance
la primera estrella. Las estrellas se acumulan (de 0 a 3 por reto). Cambiar los
valores solo requiere editar `ReglaPuntuacion` o, más adelante, leerlos del
endpoint de la US-53.

La respuesta es `PuntajeEstudianteResponse(estudianteId, puntos, estrellas)` con el
acumulado ya actualizado.

## 5. Siguientes tareas de US-09

| Tarea | Alcance |
|---|---|
| T-303 | Incluir puntos y estrellas en la respuesta del mapa. |
| T-304 | Contador de estrellas y puntos en la cabecera del mapa (coordinado con Karla). |
| T-305 | Pruebas de la suma de puntos y estrellas. |

## 6. Pruebas

| Prueba | Cobertura |
|---|---|
| `MigracionPuntajeEstudianteTest` | Tabla, una fila por alumno, acumulados desde cero, valores no negativos, fecha de actualización y cascada (T-301). |
| `PuntajeServiceTest` | Acumulado nuevo y existente, umbrales de 0 a 3 estrellas, normalización del tipo, tipo y puntaje inválidos y alumno inexistente (T-302). |
