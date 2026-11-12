# Catálogo de lecturas — contrato del portal Docente

La tabla `lecturas` pertenece al módulo Admin (Daniela). El portal Docente **solo
la lee**: no crea, edita ni desactiva lecturas.

## Endpoint

```
GET /api/docente/aulas/{aulaId}/lecturas
X-Docente-Id: <id del docente>
```

Parámetros opcionales: `gradoId`, `dificultad`, `tipoReto`.

## Reglas

| Regla | Comportamiento |
|---|---|
| Origen | Lee la tabla `lecturas`, la misma que llena el Catálogo del admin. |
| Grados | Solo trae lecturas de los grados del aula (`aula_grado`). Si el aula es multigrado, trae los de todos y el docente puede filtrar. |
| Activas | La consulta exige `activo = true`: al desactivar una lectura desaparece del catálogo. |
| Asignación | Al asignar sí se permite una lectura de grado menor al del alumno (US-36). La restricción de grado aplica al catálogo, no a la asignación. |
| Ya asignadas | Desactivar una lectura no borra las asignaciones anteriores. |

## Respuesta con fuente y enlace

```json
[
  {
    "id": 12,
    "titulo": "Cuento del Minedu",
    "texto": "Había una vez un río que cantaba...",
    "tipoReto": "lectura",
    "dificultad": "basica",
    "gradoId": 1,
    "gradoNombre": "1.º",
    "activo": true,
    "fuente": "oficial",
    "urlRecurso": "https://repositorio.minedu.gob.pe/cuento.pdf"
  }
]
```

| Campo | Origen | Uso |
|---|---|---|
| `fuente` | `lecturas.fuente` (`varchar(10)`, default `manual`) | `manual` u `oficial`. Solo `oficial` muestra la etiqueta **Oficial**. |
| `urlRecurso` | `lecturas.url_recurso` (`varchar(500)`, default `''`) | Enlace al libro del Minedu. Las manuales lo traen vacío. |

## Pantalla del docente

- **Tarjeta y modal** del catálogo: etiqueta azul **Oficial** cuando
  `fuente = oficial` (sin distinguir mayúsculas).
- **Ver recurso**: se muestra solo si `urlRecurso` no está vacío y abre el enlace
  en otra pestaña (`target="_blank"` + `rel="noopener noreferrer"`).
- Las lecturas manuales no llevan etiqueta ni enlace.

La pantalla del alumno no cambia con este ajuste. Si se agrega, sería solo un crédito
pequeño tipo «Fuente: Ministerio de Educación», sin etiqueta.

## Pruebas

| Prueba | Cobertura |
|---|---|
| `CatalogoServiceTest` | Grados del aula, filtros, aula ajena, aula sin grados, fuente y enlace de la lectura oficial y manuales sin enlace. |
| `CatalogoControllerTest` | Respuesta con `fuente` y `urlRecurso`, reenvío de filtros, 403 y 404. |
| `Catalogo.test.jsx` | Etiqueta Oficial y enlace en tarjeta y modal; lectura manual sin etiqueta; oficial sin enlace solo con etiqueta. |
