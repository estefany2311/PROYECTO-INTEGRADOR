# Pull Request — Sprint 3, módulo Docente

> Texto listo para pegar en GitHub al abrir el PR de `web-noemi` hacia `main`.
> No crea el PR automáticamente: lo abre la responsable de la rama.

## Título

```
Sprint 3 — Módulo Docente: dashboard, detalle, evidencias, lecturas oficiales y US-09
```

## Descripción

### Resumen

Cierra el Sprint 3 del módulo Docente (React + Spring Boot) e incluye las
historias de rendimiento, evidencias, catálogo oficial y puntos/estrellas.

### Historias y tareas incluidas

| Historia | Tareas | Estado |
|---|---|---|
| US-27 · Dashboard de rendimiento | T-278, T-279 | Cerrada |
| US-28 · Filtros del dashboard | T-286, T-287 | Cerrada |
| US-29 · Detalle del alumno | T-290, T-292 | Cerrada |
| US-30 · Alumnos que necesitan refuerzo | T-282, T-284 | Cerrada |
| US-31 · Evidencias pendientes | T-298, T-299, T-300 | Cerrada |
| US-35 · Catálogo de lecturas | T-189, T-190, T-230, T-306* | Cerrada |
| US-36 · Asignación individual | T-192, T-193 | Cerrada |
| US-37 · Grupos | T-195 | Cerrada |
| US-09 · Puntos y estrellas | T-301 a T-305 | Cerrada (backend) |
| Generales | T-199 (Swagger), T-307 (entrega) | Cerrada |

\* El ajuste de `fuente` / `urlRecurso` se hizo junto con Admin y no tenía ID de
tarea en el backlog; quedó como `feat(catalogo)`.

### Cambios técnicos

- **Dashboard y detalle:** endpoints con filtros por grado y competencia, y
  respuesta con niveles, promedios y últimas evaluaciones.
- **Evidencias:** listado de pendientes con filtro por tipo (`audio` / `texto`).
- **Catálogo:** lectura de la tabla `lecturas` de Admin, solo activas y de los
  grados del aula. Se agregaron `fuente` y `urlRecurso` a la respuesta y la
  etiqueta **Oficial** + **Ver recurso** en la pantalla.
- **US-09:** tabla `puntaje_estudiante`, servicio que suma 100 puntos y de 0 a 3
  estrellas por reto, y `GET /api/estudiante/mapa` con `puntos` y `estrellas`.
- **T-307:** Swagger con servidor configurable, esquemas de seguridad y secciones;
  CORS central configurable por variable; colección Postman del Sprint 3;
  `Dockerfile` y guía de despliegue.

### Migraciones (Flyway)

| Versión | Contenido |
|---|---|
| V9 | Evaluaciones y datos de prueba |
| V10 | Evidencias |
| V11 | Datos de prueba de evidencias |
| V12 | `puntaje_estudiante` |

Se aplican solas al arrancar. El CI las corre sobre un PostgreSQL temporal; para
las tablas de Admin usa `src/test/resources/db/ci/beforeMigrate.sql`.

### Pruebas

| Verificación | Resultado |
|---|---|
| Backend `./mvnw -B test` | **246/246** en 42 suites |
| Frontend `npm test -- --run` | **99/99** en 12 archivos |
| Frontend `npm run lint` | OK |
| Frontend `npm run build` | OK |
| CI `docente-api` | PostgreSQL 17 temporal, migraciones desde cero |
| CI `docente-web` | lint + build |

### Cómo probar

```powershell
# Backend
cd backend\usuario\docente
.\mvnw.cmd spring-boot:run

# Frontend
cd web-portal\docente
npm install
npm run dev
```

1. Abrir `http://localhost:5173` y revisar Dashboard, Alumnos, Grupos y Catálogo.
2. En Catálogo, una lectura con `fuente = oficial` debe mostrar **Oficial** y
   **Ver recurso** (si `urlRecurso` no está vacío).
3. Probar `GET /api/estudiante/mapa` con el token de la tablet: debe traer
   `puntos` y `estrellas`.
4. Swagger: `http://localhost:8081/swagger-ui.html` → Authorize con `X-Docente-Id`.
5. Postman: importar `backend/usuario/docente/docs/postman-docente-sprint3.json`.

### Checklist

- [x] Solo se tocó el módulo Docente (`backend/usuario/docente`, `web-portal/docente`).
- [x] No se cambiaron credenciales ni la base de Neon.
- [x] Pruebas de backend y frontend en verde.
- [x] Documentación actualizada (`docs/`).
- [x] Sin cambios pendientes de commit.
- [ ] PR revisado por el equipo.
- [ ] Despliegue verificado (Render + Vercel).

### Notas para revisión

- **Daniela:** el portal solo lee `lecturas`; no crea ni desactiva. Las columnas
  `fuente` y `url_recurso` ya existen en Neon y se replican en el CI.
- **Karla:** el contador visual de estrellas de la cabecera del mapa sigue en
  `movil-karla`; el backend ya entrega `estrellas` (T-303).
- **Despliegue:** ver `backend/usuario/docente/docs/despliegue.md`.
