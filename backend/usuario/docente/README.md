# Módulo Docente (dentro de backend/usuario)

Responsable: Noemi
Historia: US-23 — Registrar aula multigrado
Paquete sugerido: com.proyecto.usuario.docente

## Documentación

- Swagger UI: http://localhost:8081/swagger-ui.html
- OpenAPI JSON: http://localhost:8081/v3/api-docs
- Postman unificado: `docs/postman-docente-sprint2.json`
- Ambiente de pruebas: `docs/ambiente-pruebas.md`

Las rutas `/api/docente/**` requieren el header `X-Docente-Id` con el id del docente.
La colección Postman incluye US-23, US-24, US-35, US-36 y US-37; ajusta sus variables
(`aulaId`, `estudianteId`, `lecturaId`, `grupoId`, `avatarId`) antes de ejecutarla.

## Ejecutar

```powershell
.\mvnw.cmd spring-boot:run
```
