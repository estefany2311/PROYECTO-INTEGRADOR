# Login docente

El portal usa las cuentas que crea el administrador. Cada docente ve solo sus
aulas: `aula.docente_id` es el `id` de su cuenta en la tabla `usuarios`.

El login lo resuelve **este backend**. No hace falta encender el backend de
Admin.

## Rutas

| Método y ruta | Cuerpo | Respuesta |
|---|---|---|
| `POST /api/auth/login` | `{"username", "password"}` | `{access, refresh, docente: {id, usuario, nombre}}` |
| `POST /api/auth/refresh` | `{"refresh"}` | `{access}` |
| `POST /api/auth/logout` | `{"refresh"}` | `204`, anula la sesión |

Las rutas `/api/docente/**` piden `Authorization: Bearer <access>`.
`FiltroSesionDocente` comprueba el token y escribe `X-Docente-Id` con el id de la
cuenta. **El `X-Docente-Id` que envíe el cliente se descarta siempre.** Siguen
siendo públicos `/api/docente/grados` y `/api/docente/avatares`. Las rutas
`/api/estudiante/**` (tablet) no cambian.

| Respuesta | Cuándo |
|---|---|
| 401 | Credenciales incorrectas, cuenta desactivada, o sesión vencida o cerrada |
| 403 | La cuenta no es de docente, o el aula es de otro docente |
| 429 | 5 contraseñas incorrectas seguidas: ese usuario espera 5 minutos |

## Reglas

- Solo entran cuentas activas con rol `DOCENTE`. Se comprueba en cada petición:
  una cuenta desactivada deja de entrar de inmediato.
- Acceso: 1 hora. Renovación: 1 día (no se alarga).
- Los tokens son valores aleatorios. En `sesion_docente` (migración V22) solo se
  guarda su huella SHA-256. No hay claves secretas que configurar.
- Se admiten los dos formatos de contraseña de Django: `bcrypt$...` y
  `pbkdf2_sha256$...`.
- El freno de intentos vive en memoria: se reinicia con el backend.
- Cambiar la contraseña no cierra las sesiones ya abiertas (vencen en un día
  como máximo). Desactivar la cuenta sí las corta.

## Dependencia del módulo Admin

Este backend **solo lee** `usuarios` (`id`, `username`, `password`, `is_active`,
`first_name`, `last_name`, `rol_id`) y `roles` (`id`, `nombre`). Las cuentas se
siguen creando y desactivando en Admin. Si Admin cambia esas columnas, el nombre
del rol, el formato de las contraseñas o agrega reglas a su login, hay que
actualizar este.

## Despliegue

HTTPS es obligatorio: la contraseña viaja en la petición de login.

## Postman

Las colecciones anteriores envían `X-Docente-Id`, que ya no sirve. Inicia sesión
con `POST /api/auth/login` y agrega `Authorization: Bearer <access>`. En Swagger
UI usa el botón **Authorize**.
