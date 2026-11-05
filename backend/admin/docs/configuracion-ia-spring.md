# Configuración del motor de IA · Lectura desde Spring Boot

> US-51 · T-251 · Módulo Admin (Django). Autora: Daniela Vila.

El administrador configura en el portal web **qué tan exigente es la evaluación de voz** en cada grado.
Django guarda esa configuración en la base **Neon compartida**, y el motor de IA (Spring Boot)
la **lee directamente** de esa tabla.

## Tabla `configuracion_evaluacion`

Una fila por grado (siempre son 6: de 1.° a 6.°).

| Columna | Tipo | Significado | Rango |
|---|---|---|---|
| `id` | bigint | Clave primaria | — |
| `grado` | smallint (único) | Grado de primaria | 1 a 6 |
| `umbral_precision` | smallint | % mínimo de palabras bien pronunciadas | 0 a 100 |
| `tolerancia_pausas` | numeric(3,1) | Segundos de pausa permitidos entre palabras | 0.0 a 10.0 |
| `palabras_por_minuto` | smallint | Velocidad de lectura esperada | 10 a 200 |
| `actualizado_por_id` | bigint (nulo) | Usuario admin que hizo el último cambio | — |
| `actualizado_en` | timestamp | Fecha del último cambio | — |

**Valores iniciales**

| Grado | Precisión | Pausas | Palabras/min |
|---|---|---|---|
| 1.° | 60 % | 3.0 s | 30 |
| 2.° | 65 % | 2.5 s | 45 |
| 3.° | 70 % | 2.0 s | 60 |
| 4.° | 75 % | 2.0 s | 75 |
| 5.° | 80 % | 1.5 s | 85 |
| 6.° | 85 % | 1.5 s | 95 |

## Reglas (importante)

1. **Solo Django escribe** en esta tabla, desde la pantalla *Configuración IA → Evaluación de voz*.
   Spring Boot **solo la lee** (`SELECT`).
2. **No** crear esta tabla en Flyway ni modificarla con `ddl-auto`. La crea y la migra Django
   (`configuracion/migrations`).
3. Leer la configuración **cada vez que se evalúa un audio** (o con caché corta, de 1 a 5 minutos),
   para que un cambio del administrador se aplique sin reiniciar el servicio.
4. Si por algún motivo no existe la fila del grado, usar los valores iniciales de la tabla de arriba.

## Consulta SQL

```sql
SELECT umbral_precision, tolerancia_pausas, palabras_por_minuto
FROM configuracion_evaluacion
WHERE grado = ?;
```

## Ejemplo en Spring Boot (JPA, solo lectura)

```java
@Entity
@Immutable                       // org.hibernate.annotations.Immutable: Hibernate nunca escribe
@Table(name = "configuracion_evaluacion")
public class ConfiguracionEvaluacion {
    @Id
    private Long id;
    private Integer grado;
    @Column(name = "umbral_precision")
    private Integer umbralPrecision;
    @Column(name = "tolerancia_pausas")
    private BigDecimal toleranciaPausas;
    @Column(name = "palabras_por_minuto")
    private Integer palabrasPorMinuto;
    // getters
}

public interface ConfiguracionEvaluacionRepository extends Repository<ConfiguracionEvaluacion, Long> {
    Optional<ConfiguracionEvaluacion> findByGrado(Integer grado);
}
```

## También por la API de Django (solo administrador)

- `GET /api/configuracion/evaluacion/` → los 6 grados
- `GET /api/configuracion/evaluacion/{grado}/` → un grado

Requiere el token JWT de un usuario con el permiso `configuracion.ver`. Spring Boot no necesita usarla,
porque lee la tabla directamente.