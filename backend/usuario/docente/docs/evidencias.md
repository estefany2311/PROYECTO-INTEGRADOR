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
- `repository/EvidenciaRepository.java`: acceso base a la tabla. La consulta de
  pendientes por aula se agrega en T-298.

## 4. Siguientes tareas de US-31

| Tarea | Alcance |
|---|---|
| T-297 | Datos de prueba de evidencias de audio y texto. |
| T-298 | Endpoint de evidencias pendientes del aula. |
| T-299 | Pantalla con la lista de evidencias y filtro por tipo. |

## 5. Pruebas

| Prueba | Cobertura |
|---|---|
| `MigracionEvidenciaTest` | Tabla, alumno y nivel, audio/texto, contenido según el tipo, resultado IA, estado de revisión, cascada e índices (T-296). |
