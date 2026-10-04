# Backend Usuario — Spring Boot (compartido)

Proyecto único compartido entre Noemi (Docente) y Karla (Estudiante/Móvil).
Cada una trabaja solo dentro de su propia subcarpeta para no pisarse el código.

## Historias de este sprint
- US-23: Registrar aula multigrado — Noemi (subcarpeta docente/)
- US-01: Visualizar perfiles con avatar — Karla (subcarpeta estudiante/)

## Cómo levantarlo
1. Crear el proyecto con Spring Initializr (dependencias: Spring Web, Spring Data JPA, PostgreSQL Driver, Validation).
2. Configurar la conexión a PostgreSQL en application.properties con variables de entorno.
3. ./mvnw spring-boot:run

## Regla del equipo
Antes de modificar archivos compartidos (pom.xml, application.properties, clases de configuración general), avisar a la otra persona para evitar conflictos al fusionar.
