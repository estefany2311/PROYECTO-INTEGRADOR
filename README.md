# Proyecto Integrador — Plataforma Educativa Gamificada

Plataforma móvil y web para fortalecer las competencias comunicativas en escuelas rurales multigrado del Perú.

## Arquitectura del sistema

El sistema se divide en dos módulos independientes que comparten una sola base de datos relacional (PostgreSQL):

```
            ADMINISTRACIÓN                        USUARIO / OPERATIVO
  Front-End Web (React) ──► Back-End (Django)    Front-End Móvil (Kotlin) ──┐
                                   \                                         ├──► Back-End (Spring Boot)
                                    \              Front-End Web (React) ───┘
                                     \                     /
                                      ▼                   ▼
                                  Base de Datos Relacional (PostgreSQL)
```

- **Módulo Administración** (Django + React): resuelve la creación de docentes, el catálogo y los reportes. Se beneficia del panel de administración automático de Django.
- **Módulo Usuario/Operativo** (Spring Boot + Kotlin/React): resuelve lo transaccional del día a día (aulas, alumnos, retos, evaluación con IA).
- **Ambos escriben a la misma base de datos**, pero cada uno es dueño de sus propias tablas para no pisarse.

## Estructura del repositorio

| Carpeta | Tecnología | Responsable | Rama |
|---|---|---|---|
| `backend/admin/` | Django (Python) | Daniela | `admin-daniela` |
| `web-portal/admin/` | React | Daniela | `admin-daniela` |
| `backend/usuario/docente/` | Spring Boot (Java) | Noemi | `web-noemi` |
| `backend/usuario/estudiante/` | Spring Boot (Java) | Karla | `movil-karla` |
| `web-portal/docente/` | React | Noemi | `web-noemi` |
| `app-movil/` | Kotlin (Android nativo) | Karla | `movil-karla` |

**Importante:** `backend/usuario/` es un único proyecto Spring Boot compartido entre Noemi y Karla. Cada una trabaja solo dentro de su propia subcarpeta (`docente/` o `estudiante/`). Los archivos de configuración general (`pom.xml`, `application.properties`) se coordinan entre las dos antes de modificarlos.

## Motor de IA (confirmado con el profesor, T-412)

- **Voz a texto (US-13 y US-14):** Whisper (`faster-whisper`, en español) como servicio aparte en `backend/ia/voz/` (Python). Lo llama solo el backend estudiante; la tablet nunca habla con él.
- **Validación ortográfica (US-19):** dentro del backend estudiante (`VerificadorOrtografico`), con la configuración que guarda el módulo Administración.
- Los audios se guardan en una carpeta compartida (`estudiante.audios.carpeta`) que también lee el backend docente para que el profesor escuche la evidencia.

## Estrategia de ramas

Una rama fija por integrante durante todo el proyecto, no una rama por tarea.

```bash
git clone <URL-del-repositorio>
cd proyecto-integrador

# La primera vez (crear tu rama):
git checkout -b movil-karla        # o admin-daniela / web-noemi

# Al iniciar cada sprint nuevo (traer lo último de main):
git checkout movil-karla
git pull origin main

# Cada vez que avanzas una tarea:
git add .
git commit -m "Descripción corta de lo que hiciste"
git push origin movil-karla

# Al terminar tu historia completa:
# Abres un Pull Request en GitHub de tu rama hacia main
```
