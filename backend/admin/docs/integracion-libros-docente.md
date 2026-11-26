# Integración: el docente saca lecturas de los libros oficiales

**Quién hace qué**

- **Admin.** Trae los libros oficiales de la Biblioteca Escolar Digital y decide cuáles quedan **activos**.
- **Docente.** Abre un libro activo de su grado, elige las páginas, guarda la lectura y la asigna a sus alumnos.
- **Alumno.** Recibe la tarea, igual que antes. La lectura sigue en la tabla `lecturas`.

El portal docente usa estos endpoints del backend Admin (Django) con el **token de la docente**. Es el mismo `access` que devuelve `POST /api/auth/login/`:

```
Authorization: Bearer <access>
```

Así, el portal docente no tiene que leer PDFs: el backend Admin descarga el libro y saca el texto.

---

## 1. Libros que la docente puede usar

```
GET /api/recursos-oficiales/?grado=3
```

- A una cuenta **DOCENTE** solo le llegan los libros **activos**.
- El filtro `grado` es opcional (1 a 6). Úsenlo con los grados del aula.

Respuesta (resumida):

```json
[
  {
    "id": 4,
    "titulo": "Comunicación 3 : texto escolar",
    "grado": 3,
    "url_pdf": "https://.../comunicacion-3.pdf",
    "activo": true,
    "lecturas_importadas": 2
  }
]
```

`url_pdf` sirve para mostrar el libro y que la docente vea en qué páginas está el cuento.

## 2. Sacar el texto de unas páginas

```
GET /api/recursos-oficiales/{id}/texto/?desde=24&hasta=25
```

Se pueden pedir **hasta 10 páginas** a la vez. La primera vez demora unos segundos, porque el backend descarga el PDF; las siguientes son rápidas.

Respuesta:

```json
{
  "desde": 24,
  "hasta": 25,
  "paginas_total": 82,
  "texto": "Había una vez un zorro y un cuy...",
  "aviso": ""
}
```

Cuando las páginas son imágenes, `texto` viene vacío y `aviso` le pide a la docente que escriba el texto a mano. Muestren el `texto` en un cuadro editable para que la docente pueda corregirlo antes de guardar.

Errores:

| Código | Cuándo |
|---|---|
| 400 | Páginas inválidas o fuera del libro (el mensaje viene en `detail` o en el campo) |
| 404 | El libro no existe o está desactivado |
| 502 | La biblioteca no respondió |

## 3. Guardar la lectura

```
POST /api/recursos-oficiales/{id}/importar/
Content-Type: application/json
```

```json
{
  "titulo": "El zorro y el cuy",
  "texto": "Había una vez un zorro y un cuy...",
  "tipo_reto": "lectura",
  "dificultad": "basica"
}
```

- `tipo_reto` puede ser `lectura`, `escritura` u `oralidad`.
- `dificultad` puede ser `basica`, `intermedia` o `avanzada`. Si no se envía, vale `basica`.
- El **grado lo pone el libro**; no hace falta enviarlo.
- El `texto` debe tener al menos 20 caracteres.

Respuesta `201` con la lectura creada. Incluye `id`, que es el que se usa en la asignación:

```json
{
  "id": 57,
  "titulo": "El zorro y el cuy",
  "tipo_reto": "lectura",
  "grado": 3,
  "dificultad": "basica",
  "activo": true,
  "fuente": "oficial",
  "docente_id": 12
}
```

Errores:

| Código | Cuándo |
|---|---|
| 400 | Falta un dato o no es válido. Cada error viene en su campo, por ejemplo `{"texto": ["..."]}` |
| 403 | La cuenta no es DOCENTE. El admin ya no importa |
| 404 | El libro no existe o está desactivado |

---

## Tabla `lecturas`: columna nueva `docente_id`

Migración `catalogo/0006`. Es un `bigint` que puede ser nulo:

- **Con valor:** el id de la cuenta de la docente que sacó la lectura, en la tabla `usuarios`. Es el mismo `id` que devuelve `GET /api/auth/me/`.
- **Nulo:** son lecturas antiguas del admin. Las ven todas las docentes del grado.

## Cambio en el catálogo del portal docente

Hoy el catálogo muestra las lecturas activas de los grados del aula. Hay que agregar que **cada docente vea solo las suyas más las antiguas**:

```sql
SELECT * FROM lecturas
WHERE activo = true
  AND grado IN (:gradosDelAula)
  AND (docente_id IS NULL OR docente_id = :docenteId)
ORDER BY grado, titulo;
```

En la entidad `Lectura` de Spring Boot hay que mapear la columna. Solo se lee; no se crean lecturas desde Spring:

```java
@Column(name = "docente_id")
private Long docenteId;
```

`:docenteId` es el `id` de `GET /api/auth/me/`. Es el mismo número que va en `aula.docente_id`.
