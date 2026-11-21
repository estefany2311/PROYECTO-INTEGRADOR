# Evidencias de audio y texto - Módulo Docente

**Tarea:** T-296 · **Historia:** US-31 · **Responsable:** Noemi · **Sprint:** 3

## 1. Objetivo

Guardar las evidencias que produce el estudiante (un audio de lectura o un texto de
escritura) junto con el resultado del motor de IA y el estado de revisión del docente.
La tabla la comparten los módulos **docente** y **estudiante**: Karla registra la
evidencia desde la tablet y el docente la revisa en US-31.

## 2. Tabla `evidencia`

Migración: `backend/usuario/docente/src/main/resources/db/migration/V10__evidencia.sql`.

| Columna | Tipo | Regla |
|---|---|---|
| `id` | `BIGSERIAL` | Clave primaria. |
| `estudiante_id` | `BIGINT` | Obligatorio; referencia a `estudiante`. Se borra en cascada con el alumno. |
| `nivel_id` | `SMALLINT` | Obligatorio; referencia a `nivel`. Se borra en cascada con el nivel. |
| `tipo` | `VARCHAR(10)` | `audio` o `texto`. |
| `audio_url` | `VARCHAR(500)` | URL o ruta del audio; obligatoria cuando `tipo = 'audio'`. |
| `texto` | `TEXT` | Texto escrito por el alumno; obligatorio cuando `tipo = 'texto'`. |
| `resultado_ia` | `TEXT` | Resultado o retroalimentación del motor de IA. Puede ser nulo mientras no se evalúa. |
| `estado_revision` | `VARCHAR(15)` | `pendiente` (por defecto), `aprobada`, `rechazada` o `corregida` (V15, US-33). |
| `creado_en` | `TIMESTAMP` | Fecha de registro; por defecto `now()`. |
| `revisado_en` | `TIMESTAMP` | Fecha en que el docente revisó la evidencia. |
| `calificacion_final` | `SMALLINT` | Nota final que deja el docente, de 0 a 100 (V15, US-33). Nula mientras no revisa. |

### Reglas de integridad

- El `CHECK` `chk_evidencia_contenido` exige `audio_url` para las evidencias de audio y
  `texto` para las de texto.
- `estado_revision` solo acepta `pendiente`, `aprobada`, `rechazada` o `corregida`.
- `ON DELETE CASCADE` limpia las evidencias cuando se borra el alumno o el nivel.

### Índices

- `idx_evidencia_estudiante` (`estudiante_id`): evidencias de un alumno.
- `idx_evidencia_revision` (`estado_revision`, `creado_en DESC`): lista de pendientes
  ordenada de la más reciente a la más antigua (T-298).

## 3. Modelo Java

- `model/Evidencia.java`: entidad JPA con las constantes `AUDIO`, `TEXTO`, `PENDIENTE`,
  `APROBADA`, `RECHAZADA` y `CORREGIDA`, y las fábricas `Evidencia.audio(...)` y
  `Evidencia.texto(...)`.
- `repository/EvidenciaRepository.java`: consultas de pendientes por aula, con el
  alumno, su grado y el nivel cargados con `@EntityGraph` (T-298).

## 4. Datos de prueba (T-297)

Migración: `backend/usuario/docente/src/main/resources/db/migration/V11__evidencia_datos_prueba.sql`.

Carga **9 evidencias** del aula **Noemi De La Cruz**, repartidas entre sus 3 alumnos.
Los alumnos y los niveles se resuelven por nombre y por `orden`, sin quemar ids, y
solo se insertan si el aula existe (en el CI la base está vacía).

| Alumno | Nivel | Tipo | Estado | Días atrás |
|---|---|---|---|---|
| minji prueba delacruz | 1 · Primeros pasos | audio | pendiente | 0 |
| minji prueba delacruz | 2 · Palabras nuevas | texto | pendiente | 2 |
| minji prueba delacruz | 3 · Cuento corto | audio | aprobada | 6 |
| Breysi Zavala | 1 · Primeros pasos | audio | pendiente | 1 |
| Breysi Zavala | 2 · Palabras nuevas | texto | pendiente | 3 |
| Breysi Zavala | 5 · Mi relato | texto | rechazada | 7 |
| Ester Zavala | 2 · Palabras nuevas | texto | pendiente | 1 |
| Ester Zavala | 3 · Cuento corto | audio | pendiente | 4 |
| Ester Zavala | 1 · Primeros pasos | audio | aprobada | 9 |

Resumen para las pruebas de la lista (T-299): **6 pendientes** (3 de audio y 3 de
texto), **2 aprobadas** y **1 rechazada**. Las pendientes quedan con `revisado_en`
nulo; las revisadas lo tienen un día después de `creado_en`.

## 5. Endpoint de pendientes (T-298)

```
GET /api/docente/aulas/{aulaId}/evidencias?tipo=audio|texto
Header obligatorio: X-Docente-Id
```

Devuelve **solo** las evidencias en estado `pendiente` del aula, de la más reciente a
la más antigua (`creado_en DESC`, desempate por `id`). El filtro `tipo` es opcional y
no distingue mayúsculas; si se omite, llegan audio y texto.

Respuesta (`200`):

```json
[
  {
    "id": 12,
    "estudianteId": 7,
    "nombres": "Minji",
    "apellidos": "Prueba",
    "gradoId": 5,
    "gradoNombre": "5.º",
    "nivelOrden": 1,
    "nivelNombre": "Primeros pasos",
    "tipo": "audio",
    "audioUrl": "https://cdn.ejemplo.pe/evidencias/minji-nivel1.mp3",
    "texto": null,
    "resultadoIa": "Lee con claridad y buen ritmo. Se sugiere practicar las pausas.",
    "creadoEn": "2026-11-10T09:00:00"
  }
]
```

Errores:

| Caso | Respuesta |
|---|---|
| Aula inexistente | `404` · `El aula no existe` |
| Aula de otro docente | `403` · `El aula no pertenece al docente` |
| `tipo` distinto de `audio`/`texto` | `400` · `El tipo de evidencia no es válido` |
| Sin `X-Docente-Id` | `400` |

## 6. Pantalla de evidencias (T-299)

Ruta: `/evidencias` · entrada **Evidencias** en el menú *Evaluación*.

- **Selector de aula**: comparte el aula activa con el menú y el resto de pantallas
  (`aulaSeleccionada`). Al entrar se restaura la última usada.
- **Filtro por tipo**: `Todos los tipos`, `Audio` o `Texto`. Cambiarlo vuelve a llamar
  al endpoint con `?tipo=`, así el filtro no se recalcula en el navegador.
- **Tarjeta por evidencia**: alumno con iniciales, grado y nivel, etiqueta de tipo,
  contenido (enlace **Escuchar audio** o el texto escrito), **Resultado de la IA**,
  fecha de registro y enlace **Ver detalle del alumno**.
- **Estados**: carga, error con **Reintentar**, aula sin evidencias y filtro sin
  resultados (mensaje distinto para audio y texto).

Archivos del frontend:

| Archivo | Cambio |
|---|---|
| `web-portal/docente/src/pages/Evidencias.jsx` | Pantalla nueva. |
| `web-portal/docente/src/api.js` | `listarEvidenciasPendientes(aulaId, { tipo })`. |
| `web-portal/docente/src/App.jsx` | Ruta `/evidencias`. |
| `web-portal/docente/src/components/Layout.jsx` | Entrada del menú. |
| `web-portal/docente/src/index.css` | Estilos `evidencias-*` y `evidencia-*`. |

## 7. Pruebas (T-296 a T-300)

T-300 cierra US-31 con las pruebas de la lista de evidencias: el backend cubre
el tipo en blanco, el filtro por `texto`, el resultado de IA nulo y la respuesta
de una evidencia de texto; el frontend cubre el orden de las tarjetas, el conteo
por tipo, el botón **Actualizar**, la vuelta a *Todos los tipos*, el aula sin
evidencias y el mensaje cuando la IA aún no generó un resultado.

| Prueba | Cobertura |
|---|---|
| `MigracionEvidenciaTest` | Tabla, alumno y nivel, audio/texto, contenido según el tipo, resultado IA, estado de revisión, cascada e índices (T-296). |
| `MigracionEvidenciaDatosPruebaTest` | Audio y texto, los tres alumnos del aula, los tres estados, pendientes sin fecha de revisión y niveles por `orden` (T-297). |
| `EvidenciaRepositoryTest` | Pendientes de la más reciente a la más antigua y filtro por tipo contra PostgreSQL (T-298). |
| `EvidenciaServiceTest` | Mapeo de alumno, grado y nivel, filtro por tipo y texto, tipo en blanco o inválido, lista vacía, resultado IA nulo y validación del aula (T-298, T-300). |
| `EvidenciaControllerTest` | Ruta, forma de la respuesta de audio y texto, filtro por tipo, header obligatorio y errores HTTP (T-298, T-300). |
| `Evidencias.test.jsx` | Tarjetas con alumno, nivel, tipo y resultado de la IA, orden, conteo por tipo, filtro y vuelta a todos los tipos, aula activa, cambio de aula, Actualizar, estado vacío, sin aulas y error con reintento (T-299, T-300). |

## 8. Detalle de la evidencia (T-371, US-32)

El docente necesita **contrastar la evaluación automática**: escuchar el audio o leer el
texto del alumno junto con lo que entendió la IA y el puntaje que le puso.

### Migración V13

`V13__evidencia_transcripcion_y_puntaje_ia.sql` agrega dos columnas a `evidencia`:

| Columna | Tipo | Regla |
|---|---|---|
| `transcripcion` | `TEXT` | Lo que la IA entendió del audio. Nula en las evidencias de texto. |
| `puntaje_ia` | `SMALLINT` | Puntaje global de la IA, de 0 a 100 (`chk_evidencia_puntaje_ia`). Nulo mientras no evalúa. |

Las 9 evidencias de prueba de V11 quedan con su transcripción (las de audio) y su
puntaje, sin quemar ids: se actualizan por `audio_url` o por `texto`.

### Endpoint

```
GET /api/docente/aulas/{aulaId}/evidencias/{evidenciaId}
Header obligatorio: X-Docente-Id
```

Funciona con evidencias en **cualquier estado** (`pendiente`, `aprobada`, `rechazada`),
porque el docente puede volver a consultar una evidencia ya revisada.

Respuesta (`200`):

```json
{
  "id": 30,
  "estudianteId": 7,
  "nombres": "Minji",
  "apellidos": "Prueba",
  "gradoId": 5,
  "gradoNombre": "5.º",
  "nivelOrden": 1,
  "nivelNombre": "Primeros pasos",
  "tipoReto": "lectura",
  "tipo": "audio",
  "audioUrl": "https://cdn.ejemplo.pe/evidencias/minji-nivel1.mp3",
  "texto": null,
  "transcripcion": "El sol brilla. Los niños juegan en el parque.",
  "palabrasMarcadas": ["brilla", "juegan"],
  "puntajeIa": 85,
  "calificacionFinal": null,
  "motivoCorreccion": null,
  "resultadoIa": "Lee con claridad y buen ritmo.",
  "estadoRevision": "pendiente",
  "creadoEn": "2026-11-10T09:00:00",
  "revisadoEn": null,
  "historial": []
}
```

En una evidencia de texto, `audioUrl` y `transcripcion` llegan nulos y `texto` trae lo
que escribió el alumno. `tipoReto` sale del nivel (`lectura`, `escritura` u `oralidad`)
para saber qué competencia evaluó la IA. `palabrasMarcadas` llega como `[]` si la IA no
marcó ninguna palabra. `calificacionFinal` y `motivoCorreccion` llegan nulos mientras el
docente no revisa la evidencia (T-378). `historial` trae las revisiones del docente, de la
más reciente a la más antigua (T-380); llega vacío si la evidencia nunca se revisó.

Errores:

| Caso | Respuesta |
|---|---|
| Aula inexistente | `404` · `El aula no existe` |
| Aula de otro docente | `403` · `El aula no pertenece al docente` |
| Evidencia inexistente o de otra aula | `404` · `La evidencia no existe en esta aula` |
| Sin `X-Docente-Id` | `400` |

### Palabras marcadas por la IA (T-375)

`V14__evidencia_palabras_marcadas.sql` agrega la columna `palabras_marcadas TEXT`. La IA
guarda ahí las palabras que quiere que el docente revise, separadas por comas; el endpoint
las devuelve como lista (`palabrasMarcadas`) y descarta espacios y duplicados. En las
evidencias de audio son palabras de la `transcripcion` (pronunciación); en las de texto,
palabras del `texto` con alguna observación de escritura.

Las 5 evidencias de audio de V11 quedan con sus palabras marcadas por `audio_url`, sin
quemar ids. El portal las resalta en el contenido y avisa cuántas marcó la IA.

### Pruebas

| Prueba | Cobertura |
|---|---|
| `MigracionEvidenciaDetalleTest` | Columnas de V13, rango 0–100 del puntaje y datos de prueba sin ids quemados. |
| `MigracionEvidenciaPalabrasTest` | Columna de V14 y palabras marcadas en las evidencias de prueba sin ids quemados. |
| `EvidenciaRepositoryTest` | Detalle por id y aula, sin importar el estado, y evidencia de otra aula vacía. |
| `EvidenciaServiceTest` | Audio con transcripción, puntaje y palabras marcadas, normalización de la lista, marcas en una evidencia de texto, texto revisado, evidencia de otra aula, aula inexistente y aula ajena. |
| `EvidenciaControllerTest` | Ruta del detalle, respuesta de audio y texto con `palabrasMarcadas`, evidencia rechazada, detalle sin evaluación de la IA, 404, 403 y header obligatorio. |

T-376 cierra las pruebas del detalle: el backend cubre el caso de una evidencia
rechazada y el de una evidencia que la IA todavía no evaluó (`transcripcion`,
`puntajeIa` y `resultadoIa` nulos, `palabrasMarcadas` vacío), y el portal cubre el
botón **Actualizar**, las marcas en una evidencia de texto y un detalle sin
`palabrasMarcadas`.

## 9. Audio seguro de la evidencia (T-372, US-32)

El docente escucha el audio del alumno en el portal, pero el archivo **no** se expone
como una URL pública: se pide al backend y este comprueba que quien lo pide es el
docente dueño del aula.

### Endpoint

```
GET /api/docente/aulas/{aulaId}/evidencias/{evidenciaId}/audio
Header obligatorio: X-Docente-Id
```

El control valida en este orden:

1. El aula existe → si no, `404`.
2. El aula pertenece al docente → si no, `403`.
3. La evidencia existe en esa aula → si no, `404`.
4. La evidencia es de tipo `audio` → si no, `400`.
5. El archivo está en la carpeta configurada → si no, `404`.

### Carpeta de audios

`application.properties`:

```properties
# La misma carpeta que usa el backend estudiante
app.evidencias.directorio=${estudiante.audios.carpeta:${user.home}/plataforma-educativa/audios}
```

En despliegue se sobreescribe con la variable `APP_EVIDENCIAS_DIRECTORIO` (o con
`ESTUDIANTE_AUDIOS_CARPETA`). Los backends docente y estudiante deben apuntar a la
**misma carpeta**; por defecto es `~/plataforma-educativa/audios`, fuera del repositorio.

`audio_url` guarda una **ruta relativa** a esa carpeta, con la subcarpeta del alumno
delante: `est_7/nivel_1_20261117-221100_ab12cd34.wav`. También se acepta una **URL
completa** (`https://cdn.ejemplo.pe/audios/evidencia-30.mp3?v=3`), de la que se usa solo
el nombre. Se rechazan las rutas absolutas, los segmentos `..` y los nombres con
caracteres no permitidos; un archivo que no existe responde `404`. Nunca se sirve un
archivo de fuera de la carpeta.

> **Para Karla (US-13):** el docente lee de la misma carpeta que configura
> `estudiante.audios.carpeta` (`~/plataforma-educativa/audios`). No hay que copiar
> archivos: basta con guardar en `audio_url` la ruta relativa tal como la genera el
> backend estudiante (`est_7/...`).

### Respuestas

| Caso | Respuesta |
|---|---|
| Sin `Range` | `200` · archivo completo · `Content-Type` según la extensión (`audio/mpeg`, `audio/wav`, `audio/mp4`, `audio/aac`, `audio/ogg`, `audio/webm`) · `Accept-Ranges: bytes` · `Cache-Control: private, max-age=0` |
| `Range: bytes=0-1023` | `206` · solo esa parte · `Content-Range: bytes 0-1023/{total}` |
| `Range` fuera del archivo o con formato inválido | `416` · `Content-Range: bytes */{total}` |

El soporte de `Range` permite que el reproductor del portal (T-373) empiece a sonar sin
descargar todo el archivo.

### Reproductor en el portal (T-373)

La pantalla de evidencias (`web-portal/docente/src/pages/Evidencias.jsx`) muestra un
botón **Escuchar audio** en cada tarjeta de audio. Al pulsarlo, el componente
`components/ReproductorAudio.jsx`:

1. Descarga el audio con `descargarAudioEvidencia(aulaId, evidenciaId)`
   (`src/api.js`), que envía el header `X-Docente-Id`.
2. Crea un blob local con `URL.createObjectURL` y lo pone en un `<audio controls autoPlay>`.
3. Libera el blob con `URL.revokeObjectURL` al desmontar la tarjeta.

No se usa `audioUrl` directamente en un `<audio src>` porque el endpoint exige el header
del docente y el navegador no lo envía en la reproducción de medios. Tampoco se descargan
todos los audios al abrir la lista: la descarga empieza con el clic. Si el backend responde
`403` o `404`, la tarjeta muestra el mensaje y el botón cambia a **Reintentar audio**.

### Detalle lado a lado en el portal (T-374)

Cada tarjeta de la lista tiene el enlace **Revisar evidencia**, que abre la ruta
`/evidencias/{aulaId}/{evidenciaId}` (`pages/DetalleEvidencia.jsx`). La pantalla consume
el detalle de T-371 y muestra:

| Columna | Contenido |
|---|---|
| **Texto original** | El `texto` que escribió el alumno, si la evidencia lo trae. |
| **Audio original** | Si la evidencia es de audio, el reproductor seguro de T-373. |
| **Transcripción de la IA** | Lo que la IA entendió del audio (`transcripcion`). |

En una evidencia de texto sin transcripción se explica que la IA evaluó el texto
directamente. Debajo, el panel **Evaluación de la IA** muestra el `puntajeIa` con su barra,
el `resultadoIa` y las fechas de registro y revisión. El estado (`pendiente`, `aprobada` o
`rechazada`) aparece en la cabecera y en el panel. En pantallas angostas las columnas se
apilan.

T-375 resalta en amarillo las palabras de `palabrasMarcadas` dentro del texto o de la
transcripción, sin distinguir mayúsculas ni tildes, y muestra el aviso «La IA marcó N
palabras para revisar» encima del contenido.

Errores:

| Caso | Respuesta |
|---|---|
| Aula inexistente | `404` · `El aula no existe` |
| Aula de otro docente | `403` · `El aula no pertenece al docente` |
| Evidencia inexistente o de otra aula | `404` · `La evidencia no existe en esta aula` |
| Evidencia de texto | `400` · `La evidencia no es de audio` |
| Archivo no encontrado o nombre inválido | `404` · `El audio no está disponible` |
| Sin `X-Docente-Id` | `400` |

### Pruebas

| Prueba | Cobertura |
|---|---|
| `AlmacenAudioTest` | Ruta relativa con subcarpeta (`est_7/...`), URL completa y query string; nombre simple; `..`, ruta absoluta, nombre vacío y archivo inexistente; tipo MIME por extensión. |
| `AudioEvidenciaServiceTest` | Audio del aula, evidencia de texto, evidencia de otra aula, aula inexistente, aula ajena y archivo que no está en la carpeta. |
| `EvidenciaControllerTest` | Audio completo, `206` con `Content-Range`, `416`, formato de `Range` inválido, `400` de texto, `403`, `404` y header obligatorio. |
| `ReproductorAudio.test.jsx` | No descarga hasta el clic, descarga con el header y reproduce desde un blob, error con reintento, liberación del blob y nombre genérico. |
| `DetalleEvidencia.test.jsx` | Audio y transcripción lado a lado, palabras marcadas resaltadas (audio y texto), texto sin transcripción, detalle sin `palabrasMarcadas`, avisos sin transcripción/puntaje, actualización con el botón Actualizar, error con reintento y estado rechazada. |

## 10. Calificación final y estado corregida (T-377, US-33)

El docente debe **aprobar o corregir la calificación de la IA** para que la evaluación
sea justa. T-377 prepara la tabla `evidencia` para esa revisión; los endpoints llegan
en T-378 (sección 11) y los botones en T-381.

### Migración V15

`V15__evidencia_calificacion_final.sql` hace dos cambios:

| Cambio | Regla |
|---|---|
| Columna `calificacion_final` | `SMALLINT` de 0 a 100 (`chk_evidencia_calificacion_final`). Nula mientras el docente no revisa. |
| Estado `corregida` | El `CHECK` de `estado_revision` pasa a aceptar `pendiente`, `aprobada`, `rechazada` y `corregida`. |

El `CHECK` de V10 se reemplaza buscando su nombre por definición (no se asume el nombre
que le dio Postgres). Se conserva `rechazada` porque las evidencias revisadas en US-31
siguen siendo válidas.

### Modelo Java

`Evidencia` agrega la constante `CORREGIDA`, el campo `calificacionFinal` con su
`get`/`set` y el comentario de la revisión. El estado por defecto sigue siendo
`pendiente` y la calificación final nace nula.

### Pruebas

| Prueba | Cobertura |
|---|---|
| `MigracionEvidenciaCalificacionTest` | Columna `calificacion_final`, rango 0–100 y el `CHECK` del estado con `corregida` sin perder los estados anteriores. |
| `EvidenciaRepositoryTest` | Actualiza una evidencia a `corregida` con nota final y la vuelve a leer desde PostgreSQL. |

## 11. Aprobar y corregir la calificación (T-378, US-33)

El docente acepta la nota de la IA o la cambia por la suya. Ambos endpoints devuelven el
detalle actualizado de la evidencia (el mismo contrato de T-371) y exigen `X-Docente-Id`.

```
POST /api/docente/aulas/{aulaId}/evidencias/{evidenciaId}/aprobar
POST /api/docente/aulas/{aulaId}/evidencias/{evidenciaId}/corregir
Header obligatorio: X-Docente-Id
```

### Aprobar

No lleva cuerpo. Copia `puntajeIa` en `calificacionFinal`, deja el estado en `aprobada`,
borra cualquier motivo anterior y sella `revisadoEn` con la fecha actual.

Si la evidencia todavía no tiene evaluación de la IA (`puntajeIa` nulo) responde
`400` · `La evidencia no tiene evaluación de la IA`: no hay nota que aprobar.

### Corregir

Cuerpo:

```json
{
  "calificacionFinal": 70,
  "motivo": "El audio tiene ruido de fondo."
}
```

| Campo | Regla |
|---|---|
| `calificacionFinal` | Obligatorio, entre 0 y 100. |
| `motivo` | Obligatorio, sin espacios en blanco y de hasta 500 caracteres. |

Guarda la nota y el motivo (recortado), deja el estado en `corregida` y sella
`revisadoEn`. El `puntajeIa` original se conserva para poder comparar la nota de la IA
con la del docente.

### Efecto en puntos y estrellas (T-379)

Si la nota final cambia, el acumulado del alumno se ajusta en la misma transacción. La
nota anterior es `calificacionFinal` si ya existía o, si no, `puntajeIa`, y se aplica la
diferencia de estrellas entre ambos tramos. Los puntos no se tocan: se ganan al completar
el reto. Ver `puntaje-estudiante.md`, sección 8.

### Migración V16

`V16__evidencia_motivo_correccion.sql` agrega `motivo_correccion TEXT` a `evidencia`.
El historial completo de notas (IA vs. docente) llega en T-380 y se detalla en la
sección 12.

### Errores

| Caso | Respuesta |
|---|---|
| Aula inexistente | `404` · `El aula no existe` |
| Aula de otro docente | `403` · `El aula no pertenece al docente` |
| Evidencia inexistente o de otra aula | `404` · `La evidencia no existe en esta aula` |
| Aprobar sin evaluación de la IA | `400` · `La evidencia no tiene evaluación de la IA` |
| Corrección sin motivo | `400` · `Debes indicar el motivo de la corrección` |
| Nota fuera de 0–100 | `400` · mensaje del rango |
| Sin `X-Docente-Id` | `400` |

### Pruebas

| Prueba | Cobertura |
|---|---|
| `MigracionEvidenciaMotivoTest` | Columna `motivo_correccion` de V16. |
| `EvidenciaServiceTest` | Aprobar copia el puntaje y limpia el motivo; aprobar sin IA falla con 400; corregir guarda nota y motivo recortado; evidencia de otra aula da 404; el recálculo parte de la nota vigente y no se llama sin evaluación de la IA. |
| `EvidenciaControllerTest` | `POST` aprobar y corregir, validación de motivo y rango, y 400 al aprobar sin evaluación de la IA. |
| `EvidenciaRepositoryTest` | Persiste nota final y motivo de la corrección en PostgreSQL. |

## 12. Historial de calificaciones (T-380, US-33)

La evidencia guarda solo la **última** revisión. Para conservar la comparación entre la
nota de la IA y la del docente, cada aprobación o corrección agrega una fila al historial.

### Tabla `historial_calificacion`

Migración: `V17__historial_calificacion.sql`.

| Columna | Tipo | Regla |
|---|---|---|
| `id` | `BIGSERIAL` | Clave primaria. |
| `evidencia_id` | `BIGINT` | Obligatorio. Referencia a `evidencia` y se borra en cascada con ella. |
| `docente_id` | `BIGINT` | Obligatorio; quién hizo la revisión (igual que en `aula`, sin FK porque los docentes viven en el módulo de acceso). |
| `nota_ia` | `SMALLINT` | Nota que había dado la IA; nula si la evidencia no tenía evaluación. `CHECK` de 0 a 100. |
| `nota_docente` | `SMALLINT` | Nota que quedó tras la revisión. Obligatoria y con `CHECK` de 0 a 100. |
| `motivo` | `TEXT` | Motivo de la corrección; nulo en las aprobaciones. |
| `accion` | `VARCHAR(15)` | `aprobada` o `corregida` (`CHECK`). |
| `creado_en` | `TIMESTAMP` | Momento de la revisión; por defecto `now()`. |

El índice `idx_historial_calificacion_evidencia (evidencia_id, creado_en DESC)` sirve la
consulta del detalle.

### Cuándo se escribe

- `POST .../aprobar` agrega una fila con `accion = aprobada`, `nota_ia` y
  `nota_docente` iguales a `puntajeIa` y `motivo` nulo.
- `POST .../corregir` agrega una fila con `accion = corregida`, `nota_ia` igual a
  `puntajeIa`, `nota_docente` igual a la nota enviada y el `motivo` recortado.

Ambas filas se guardan en la misma transacción que la evidencia, así que si la revisión
falla no queda historial suelto. La evidencia sigue mostrando solo la última revisión.

### En el detalle

`GET .../evidencias/{evidenciaId}` devuelve `historial`, de la revisión más reciente a la
más antigua:

```json
"historial": [
  {
    "id": 12,
    "docenteId": 1,
    "notaIa": 85,
    "notaDocente": 70,
    "motivo": "El audio tiene ruido de fondo.",
    "accion": "corregida",
    "creadoEn": "2026-11-18T10:00:00"
  },
  {
    "id": 11,
    "docenteId": 1,
    "notaIa": 85,
    "notaDocente": 85,
    "motivo": null,
    "accion": "aprobada",
    "creadoEn": "2026-11-17T09:00:00"
  }
]
```

Las evidencias revisadas antes de V17 no tienen filas de historial: la tabla no se
rellena hacia atrás porque no hay un motivo ni un docente registrados en la evidencia.

### Pruebas

| Prueba | Cobertura |
|---|---|
| `MigracionHistorialCalificacionTest` | Tabla, columnas, cascada, `CHECK` de notas y acciones, e índice por evidencia. |
| `HistorialCalificacionRepositoryTest` | Guarda y lee el historial del más reciente al más antiguo, con nota IA, nota docente y motivo; borra en cascada con la evidencia. |
| `EvidenciaServiceTest` | Aprobar y corregir registran la fila correcta; el detalle mapea el historial ordenado. |
| `EvidenciaControllerTest` | El detalle expone el historial en el JSON. |
