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
| `estado_revision` | `VARCHAR(15)` | `pendiente` (por defecto), `aprobada` o `rechazada`. |
| `creado_en` | `TIMESTAMP` | Fecha de registro; por defecto `now()`. |
| `revisado_en` | `TIMESTAMP` | Fecha en que el docente revisó la evidencia. |

### Reglas de integridad

- El `CHECK` `chk_evidencia_contenido` exige `audio_url` para las evidencias de audio y
  `texto` para las de texto.
- `estado_revision` solo acepta `pendiente`, `aprobada` o `rechazada`.
- `ON DELETE CASCADE` limpia las evidencias cuando se borra el alumno o el nivel.

### Índices

- `idx_evidencia_estudiante` (`estudiante_id`): evidencias de un alumno.
- `idx_evidencia_revision` (`estado_revision`, `creado_en DESC`): lista de pendientes
  ordenada de la más reciente a la más antigua (T-298).

## 3. Modelo Java

- `model/Evidencia.java`: entidad JPA con las constantes `AUDIO`, `TEXTO`, `PENDIENTE`,
  `APROBADA` y `RECHAZADA`, y las fábricas `Evidencia.audio(...)` y `Evidencia.texto(...)`.
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
  "puntajeIa": 85,
  "resultadoIa": "Lee con claridad y buen ritmo.",
  "estadoRevision": "pendiente",
  "creadoEn": "2026-11-10T09:00:00",
  "revisadoEn": null
}
```

En una evidencia de texto, `audioUrl` y `transcripcion` llegan nulos y `texto` trae lo
que escribió el alumno. `tipoReto` sale del nivel (`lectura`, `escritura` u `oralidad`)
para saber qué competencia evaluó la IA.

Errores:

| Caso | Respuesta |
|---|---|
| Aula inexistente | `404` · `El aula no existe` |
| Aula de otro docente | `403` · `El aula no pertenece al docente` |
| Evidencia inexistente o de otra aula | `404` · `La evidencia no existe en esta aula` |
| Sin `X-Docente-Id` | `400` |

### Pruebas

| Prueba | Cobertura |
|---|---|
| `MigracionEvidenciaDetalleTest` | Columnas de V13, rango 0–100 del puntaje y datos de prueba sin ids quemados. |
| `EvidenciaRepositoryTest` | Detalle por id y aula, sin importar el estado, y evidencia de otra aula vacía. |
| `EvidenciaServiceTest` | Audio con transcripción y puntaje, texto revisado, evidencia de otra aula, aula inexistente y aula ajena. |
| `EvidenciaControllerTest` | Ruta del detalle, respuesta de audio y texto, 404, 403 y header obligatorio. |

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
app.evidencias.directorio=./evidencias
```

En despliegue se sobreescribe con la variable `APP_EVIDENCIAS_DIRECTORIO`. La carpeta
está en `.gitignore`: los audios no se versionan.

`audio_url` puede guardarse como **nombre de archivo** (`evidencia-30.mp3`) o como
**URL completa** (`https://cdn.ejemplo.pe/audios/evidencia-30.mp3?v=3`). En ambos casos
se usa solo el último segmento, sin la query string, y se resuelve dentro de la carpeta.
Un nombre con `..`, una ruta absoluta o un archivo que no existe responde `404`; nunca
se sirve un archivo de fuera de la carpeta.

> **Para Karla (T-413 / T-414):** al guardar el audio deje el archivo en
> `app.evidencias.directorio` y guarde en `audio_url` el nombre del archivo o su URL.
> El docente solo necesita que el nombre final coincida con el archivo.

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
| `AlmacenAudioTest` | Nombre desde archivo, URL y query string; resolución dentro de la carpeta; `..`, nombre vacío y archivo inexistente; rutas absolutas que no escapan; tipo MIME por extensión. |
| `AudioEvidenciaServiceTest` | Audio del aula, evidencia de texto, evidencia de otra aula, aula inexistente, aula ajena y archivo que no está en la carpeta. |
| `EvidenciaControllerTest` | Audio completo, `206` con `Content-Range`, `416`, formato de `Range` inválido, `400` de texto, `403`, `404` y header obligatorio. |
| `ReproductorAudio.test.jsx` | No descarga hasta el clic, descarga con el header y reproduce desde un blob, error con reintento, liberación del blob y nombre genérico. |
