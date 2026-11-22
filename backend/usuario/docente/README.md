# Módulo Docente (dentro de backend/usuario)

Responsable: Noemi
Historia: US-23 — Registrar aula multigrado
Paquete sugerido: com.proyecto.usuario.docente

## Documentación

- Swagger UI: http://localhost:8081/swagger-ui.html
- OpenAPI JSON: http://localhost:8081/v3/api-docs
- Postman del Sprint 3: `docs/postman-docente-sprint3.json`
- Postman del Sprint 2: `docs/postman-docente-sprint2.json`
- Ambiente de pruebas: `docs/ambiente-pruebas.md`
- Regla del semáforo (US-27): `docs/regla-semaforo.md`
- Catálogo de lecturas (US-35): `docs/catalogo-lecturas.md`
- Despliegue (T-307): `docs/despliegue.md`
- Texto del PR del Sprint 3: `docs/pull-request-sprint3.md`

Las rutas `/api/docente/**` requieren el header `X-Docente-Id` con el id del docente.
La colección del Sprint 3 incluye aulas, alumnos, catálogo, asignaciones, grupos,
dashboard, detalle, evidencias y el mapa de la tablet; ajusta sus variables
(`aulaId`, `estudianteId`, `lecturaId`, `grupoId`, `avatarId`, `tokenEstudiante`)
antes de ejecutarla.

## Ejecutar

```powershell
.\mvnw.cmd spring-boot:run
```
