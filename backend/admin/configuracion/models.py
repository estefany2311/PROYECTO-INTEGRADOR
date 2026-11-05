from decimal import Decimal

from django.conf import settings
from django.core.validators import MaxValueValidator, MinValueValidator
from django.db import models
from django.db.models.functions import Lower


class ConfiguracionEvaluacion(models.Model):
    """US-51: rigor de la evaluación de voz (pronunciación y fluidez) por grado."""

    grado = models.PositiveSmallIntegerField(
        unique=True,
        validators=[MinValueValidator(1), MaxValueValidator(6)],
    )
    umbral_precision = models.PositiveSmallIntegerField(
        default=70,
        validators=[MinValueValidator(0), MaxValueValidator(100)],
        help_text='Porcentaje mínimo de palabras bien pronunciadas (0 a 100).',
    )
    tolerancia_pausas = models.DecimalField(
        max_digits=3,
        decimal_places=1,
        default=Decimal('2.0'),
        validators=[MinValueValidator(Decimal('0')), MaxValueValidator(Decimal('10'))],
        help_text='Segundos de pausa permitidos entre palabras (0 a 10).',
    )
    palabras_por_minuto = models.PositiveSmallIntegerField(
        default=60,
        validators=[MinValueValidator(10), MaxValueValidator(200)],
        help_text='Velocidad de lectura esperada para el grado (10 a 200).',
    )
    actualizado_por = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='configuraciones_evaluacion',
    )
    actualizado_en = models.DateTimeField(auto_now=True)

    class Meta:
        # Nombre claro para que el motor de IA (Spring Boot) pueda leerla (T-251)
        db_table = 'configuracion_evaluacion'
        ordering = ['grado']
        verbose_name = 'Configuración de evaluación'
        verbose_name_plural = 'Configuraciones de evaluación'

    def __str__(self):
        return f'{self.grado}.° grado · precisión {self.umbral_precision} %'



class HistorialConfiguracion(models.Model):
    """T-252: cada cambio de la configuración de voz (quién, qué y cuándo)."""

    CAMPOS = [
        ('umbral_precision', 'Precisión mínima'),
        ('tolerancia_pausas', 'Pausas toleradas'),
        ('palabras_por_minuto', 'Palabras por minuto'),
    ]

    grado = models.PositiveSmallIntegerField()
    campo = models.CharField(max_length=40, choices=CAMPOS)
    valor_anterior = models.CharField(max_length=20)
    valor_nuevo = models.CharField(max_length=20)
    usuario = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='cambios_configuracion',
    )
    fecha = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'historial_configuracion'
        ordering = ['-fecha', '-id']
        verbose_name = 'Cambio de configuración'
        verbose_name_plural = 'Historial de configuración'

    def __str__(self):
        return f'{self.grado}.° · {self.get_campo_display()}: {self.valor_anterior} → {self.valor_nuevo}'




class ConfiguracionOrtografia(models.Model):
    """US-52: parámetros de la validación ortográfica por grado."""

    SENSIBILIDADES = [
        ('baja', 'Baja'),
        ('media', 'Media'),
        ('alta', 'Alta'),
    ]

    grado = models.PositiveSmallIntegerField(
        unique=True,
        validators=[MinValueValidator(1), MaxValueValidator(6)],
    )
    sensibilidad = models.CharField(
        max_length=10,
        choices=SENSIBILIDADES,
        default='media',
        help_text='Qué tan estricta es la IA al marcar errores de ortografía.',
    )
    ignorar_tildes = models.BooleanField(
        default=False,
        help_text='Si está activo, las tildes faltantes no se marcan como error (pensado para 1.° y 2.°).',
    )
    actualizado_por = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='configuraciones_ortografia',
    )
    actualizado_en = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = 'configuracion_ortografia'
        ordering = ['grado']
        verbose_name = 'Configuración ortográfica'
        verbose_name_plural = 'Configuraciones ortográficas'

    def __str__(self):
        tildes = ' · sin marcar tildes' if self.ignorar_tildes else ''
        return f'{self.grado}.° grado · sensibilidad {self.get_sensibilidad_display().lower()}{tildes}'


class PalabraPermitida(models.Model):
    """US-52: palabras que el validador nunca marca como error (quechua, nombres de la zona, etc.)."""

    palabra = models.CharField(max_length=60)
    creado_por = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='palabras_permitidas',
    )
    creado_en = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'palabras_permitidas'
        ordering = ['palabra']
        verbose_name = 'Palabra permitida'
        verbose_name_plural = 'Palabras permitidas'
        constraints = [
            # "Chacra" y "chacra" cuentan como la misma palabra
            models.UniqueConstraint(Lower('palabra'), name='palabra_permitida_unica'),
        ]

    def save(self, *args, **kwargs):
        self.palabra = self.palabra.strip()
        super().save(*args, **kwargs)

    def __str__(self):
        return self.palabra