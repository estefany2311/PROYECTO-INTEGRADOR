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

## 5. Respuesta del mapa (T-303)

El endpoint que consume la tablet, `GET /api/estudiante/mapa`, ahora devuelve el
acumulado del alumno en la cabecera del mapa:

```json
{
  "estudianteId": 7,
  "nombres": "Minji",
  "apellidos": "Prueba",
  "totalNiveles": 6,
  "completados": 2,
  "puntos": 350,
  "estrellas": 6,
  "niveles": []
}
```

- `puntos` y `estrellas` salen de `puntaje_estudiante` (lo que suma T-302).
- Si el alumno todavía no tiene fila en esa tabla, `puntos` mantiene la suma de
  los puntos del progreso y `estrellas` vale `0`, para no romper el mapa de los
  alumnos que ya tenían avance antes de esta historia.
- El detalle por nivel (`niveles[].puntos`) no cambia.

## 6. Coordinación con Karla (T-304)

La cabecera del mapa se dibuja en la app Android de Karla (`movil-karla`), no en
el portal Docente. El backend ya entrega los datos (sección 5); esta sección deja
registrada la coordinación para que la app muestre el contador.

### Contrato que consume la app

`GET /api/estudiante/mapa` con `Authorization: Bearer <token>` devuelve, entre
otros campos:

| Campo | Tipo | Uso en la cabecera |
|---|---|---|
| `puntos` | entero | Puntos acumulados del alumno. |
| `estrellas` | entero | Estrellas acumuladas (de 0 a 3 por reto completado). |

- Si el alumno todavía no tiene fila en `puntaje_estudiante`, `puntos` mantiene la
  suma del progreso y `estrellas` llega en `0`.
- Los acumulados los actualiza T-302 al completar un reto.

### Cambios acordados en `movil-karla`

| Archivo | Cambio |
|---|---|
| `app-movil/.../model/Mapa.kt` | Agregar `val estrellas: Int = 0` a `MapaRespuesta`. |
| `app-movil/.../view/PantallaMapa.kt` | En `ResumenAvance`, mostrar estrella + `estrellas` y puntos + `puntos` (hoy la estrella acompaña a los puntos). |
| `MapaJsonTest.kt` | Agregar `"estrellas"` al JSON y comprobar `mapa.estrellas`. |
| `PantallaMapaTest.kt` | Construir el mapa con `estrellas` y verificar ambos contadores. |

**Estado:** pendiente en la rama de Karla. El backend de la historia queda cerrado
en `web-noemi` con T-301 a T-303. A Karla se le envió el ejemplo de respuesta y
los cuatro cambios de la tabla; ella implementa el contador en `movil-karla` o
aplica el patch que le compartamos.

## 7. Cierre de US-09

T-305 cierra las pruebas de la historia: la regla de puntuación, la tabla real
contra PostgreSQL y los límites del servicio (puntajes 0 y 100). El contador
visual sigue en la rama `movil-karla` de Karla (sección 6).

## 8. Pruebas

| Prueba | Cobertura |
|---|---|
| `MigracionPuntajeEstudianteTest` | Tabla, una fila por alumno, acumulados desde cero, valores no negativos, fecha de actualización y cascada (T-301). |
| `PuntajeServiceTest` | Acumulado nuevo y existente, umbrales de 0 a 3 estrellas, puntajes límite 0 y 100, normalización del tipo, tipo y puntaje inválidos y alumno inexistente (T-302, T-305). |
| `ReglaPuntuacionTest` | Estrellas en cada umbral, umbrales crecientes y puntos no negativos (T-305). |
| `PuntajeEstudianteRepositoryTest` | Acumulado por alumno con el alumno cargado, fila nueva en cero, único por alumno y cascada al borrar al alumno (T-305). |
| `MapaEstudianteServiceTest` | Niveles y estados, acumulado de puntos y estrellas, respaldo del progreso cuando no hay fila y alumno inexistente (T-197, T-303). |
| `MapaEstudianteControllerTest` | Respuesta del mapa con puntos y estrellas, y 404 del alumno inexistente (T-197, T-303). |
