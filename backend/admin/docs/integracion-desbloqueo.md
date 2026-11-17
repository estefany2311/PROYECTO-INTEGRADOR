# Integración: reglas de desbloqueo de niveles (US-54 → US-08)

El módulo Admin guarda en la tabla `reglas_desbloqueo` qué necesita el alumno en cada
nivel del mapa para desbloquear el siguiente. El backend del estudiante la lee al
completar un reto (`POST /api/estudiante/niveles/{id}/completar`).

## Tabla `reglas_desbloqueo` (la crea Django, migración `configuracion/0009`)

| Columna              | Tipo        | Descripción                                                        |
|----------------------|-------------|--------------------------------------------------------------------|
| `id`                 | bigint      | Llave primaria                                                     |
| `nivel_id`           | smallint    | Id de la tabla `nivel` (único: una regla por nivel)                |
| `estrellas_minimas`  | smallint    | 0 a 3. Valor por defecto en la base: 1                             |
| `puntaje_minimo`     | smallint    | 0 o más. Valor por defecto en la base: 0                           |
| `actualizado_por_id` | bigint      | Usuario admin que la cambió (puede ser nulo)                       |
| `actualizado_en`     | timestamptz | Fecha del último cambio                                            |

## Cómo se aplica

La regla del nivel **N** dice qué se necesita **en el nivel N** para desbloquear el **N + 1**:

```
completado = estrellas_obtenidas >= estrellas_minimas
             AND puntos_obtenidos >= puntaje_minimo
```

- Si el nivel **no tiene fila** en la tabla: se usa **1 estrella y 0 puntos** (la regla de hoy).
- En el último nivel, la regla solo marca el nivel como completado.

## Consulta para el backend del estudiante

```sql
SELECT estrellas_minimas, puntaje_minimo
FROM reglas_desbloqueo
WHERE nivel_id = ?;
```

Sin resultados → usar 1 estrella y 0 puntos.

## Reglas para no romper la integración

- La tabla la crea y la modifica **solo Django**. Spring Boot solo la **lee**:
  no crearla con Flyway ni dejar que Hibernate la cambie (`ddl-auto` en `none` o `validate`).
- El admin no puede guardar una regla imposible (T-338): `puntaje_minimo` nunca supera
  los puntos que da el tipo de reto del nivel (tabla `reglas_puntuacion`).
- La pantalla está en el portal Admin: **Configuración IA → Desbloqueo de niveles**.
- API del admin: `GET /api/configuracion/desbloqueo/` y `PATCH /api/configuracion/desbloqueo/{nivel_id}/`.