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
- `repository/PuntajeEstudianteRepository.java`: `findByEstudianteId(...)` con el
  alumno cargado con `@EntityGraph`.

## 4. Siguientes tareas de US-09

| Tarea | Alcance |
|---|---|
| T-302 | Servicio que suma puntos y estrellas al completar un reto con las reglas de la US-53. |
| T-303 | Incluir puntos y estrellas en la respuesta del mapa. |
| T-304 | Contador de estrellas y puntos en la cabecera del mapa (coordinado con Karla). |
| T-305 | Pruebas de la suma de puntos y estrellas. |

## 5. Pruebas

| Prueba | Cobertura |
|---|---|
| `MigracionPuntajeEstudianteTest` | Tabla, una fila por alumno, acumulados desde cero, valores no negativos, fecha de actualización y cascada (T-301). |
