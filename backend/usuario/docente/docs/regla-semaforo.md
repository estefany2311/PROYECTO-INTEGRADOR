# Regla del semáforo de rendimiento - Módulo Docente

**Tarea:** T-274 · **Historia:** US-27 · **Responsable:** Noemi · **Sprint:** 3

## 1. Objetivo

Definir cuándo un estudiante se muestra en **verde**, **amarillo** o **rojo** en el
dashboard del docente, para que el cálculo sea único y se reutilice en el endpoint
(T-278), el servicio (T-277), la pantalla (T-279) y la leyenda accesible (T-280).

## 2. Escala

Todas las evaluaciones se expresan en una escala de **0 a 100 puntos**.

## 3. Regla

| Color | Promedio (redondeado) | Significado | Texto de la leyenda | Ícono sugerido | Hex |
|---|---|---|---|---|---|
| Verde | 80 - 100 | Logro esperado | En nivel esperado | ✅ | `#22C55E` |
| Amarillo | 60 - 79 | En proceso | En proceso | ⚠️ | `#F59E0B` |
| Rojo | 0 - 59 | Requiere refuerzo | Necesita refuerzo | ⛔ | `#F51E38` |
| Gris | Sin evaluaciones | Sin datos | Sin evaluaciones | — | `#9CA3AF` |

Los límites son inclusivos hacia arriba: **80 es verde** y **60 es amarillo**.
El color se decide con el promedio ya redondeado: 79.5 pasa a 80 (verde) y 59.5 pasa a 60 (amarillo).

## 4. Cómo se calcula el promedio

| Nivel | Fórmula |
|---|---|
| Por competencia | Media aritmética de las evaluaciones de lectura, escritura u oralidad. |
| General del estudiante | Media aritmética de las competencias que tengan al menos una evaluación. |
| Redondeo | Entero más cercano (0.5 sube). El color se decide con el valor redondeado. |

- Solo cuentan las evaluaciones del aula del docente y de los filtros activos (grado y competencia).
- Una competencia sin evaluaciones **no** entra en el promedio general: no se cuenta como 0.
- Si el estudiante no tiene ninguna evaluación, se muestra en **gris** y **no** suma al
  contador de "Necesitan refuerzo" (US-30).

## 5. Ejemplos

| Evaluaciones (lectura, escritura, oralidad) | Promedio general | Color |
|---|---|---|
| 90, 85, 80 | 85 | Verde |
| 80, 80, 80 | 80 | Verde |
| 79, 80, 78 | 79 | Amarillo |
| 60, —, — | 60 | Amarillo |
| 55, 50, 60 | 55 | Rojo |
| —, —, — | Sin datos | Gris |

## 6. Dónde se aplica

| Tarea | Uso de la regla |
|---|---|
| T-276 | Consulta del promedio por competencia. |
| T-277 | Servicio que asigna el color. |
| T-278 | El endpoint del dashboard devuelve promedio y color. |
| T-279 | Tarjetas por estudiante en el dashboard. |
| T-280 | Leyenda accesible: color + ícono + texto. |
| T-281 | Pruebas unitarias de límites, redondeo y casos sin evaluaciones. |
| T-282 | Parámetro `soloRojo` del endpoint: filtra las tarjetas rojas. |
| T-283 | Sección destacada con los alumnos en rojo. |
| T-284 | Contador de alumnos en rojo en el menú. |
| T-286 | Parámetros `gradoId` y `competencia` del endpoint: filtran alumnos, resumen y promedios. |

El parámetro `soloRojo=true` del endpoint (T-282) filtra solo la lista de tarjetas;
el resumen y los promedios del aula se calculan con los alumnos activos que pasan los
filtros de grado y competencia (T-286).

El color nunca debe ser la única señal: siempre va acompañado de ícono y texto (T-280).

## 7. Fuera de alcance

- Umbrales configurables por el administrador: en este sprint quedan fijos.
- Semáforo por competencia en el detalle del estudiante (US-29): reutiliza esta misma
  regla, pero se implementa en T-292.

## 8. Implementación de la leyenda (T-280)

La pantalla `/dashboard` muestra una leyenda visible con los cuatro estados. Cada estado
combina color, ícono y texto; los íconos son decorativos (`aria-hidden`) y el texto visible
es la señal principal. La leyenda se anuncia como la región **Leyenda del semáforo**.

## 9. Sección "Necesitan refuerzo" (T-283)

Cuando el aula tiene alumnos en rojo, el dashboard muestra un aviso destacado debajo del
resumen. Incluye el conteo de alumnos, sus nombres, grados y promedios. Si no hay alumnos
en rojo, la sección no se muestra. La sección se alimenta de los estudiantes que ya devuelve
el dashboard, sin duplicar la consulta al endpoint.

## 10. Contador en el menú (T-284)

El menú lateral muestra un contador junto a **Dashboard** con la cantidad de alumnos en rojo
del aula activa. Usa el parámetro `soloRojo=true` de T-282 y se oculta cuando el contador es
cero. La selección de aula se comparte entre el dashboard y el menú; si todavía no hay una
selección, se usa la primera aula del docente.

## 11. Filtros por grado y competencia (T-286)

El endpoint del dashboard acepta dos parámetros opcionales:

- `gradoId`: limita los alumnos, el resumen y los promedios al grado indicado. La lista
  `grados` sigue devolviendo todos los grados del aula para que el selector pueda cambiarse.
- `competencia`: acepta `lectura`, `escritura` u `oralidad`. Solo las evaluaciones de esa
  competencia alimentan el promedio y el color de cada alumno, el resumen y los promedios
  del aula. Si se omite o se envía vacío, no se filtra por competencia.

Los dos filtros se combinan entre sí y con `soloRojo=true`. `soloRojo` sigue limitando
únicamente la lista de tarjetas; el resumen y los promedios se calculan con los filtros de
grado y competencia activos.

Un grado que no pertenece al aula devuelve `400` con `El grado no corresponde a esta aula`.
Una competencia fuera de `lectura`, `escritura` u `oralidad` devuelve `400` con
`La competencia no es válida`.
