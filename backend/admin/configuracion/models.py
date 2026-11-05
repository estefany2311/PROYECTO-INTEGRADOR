from decimal import Decimal

from django.conf import settings
from django.core.validators import MaxValueValidator, MinValueValidator
from django.db import models


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