from django.conf import settings
from django.core.validators import MaxValueValidator, MinValueValidator
from django.db import models


class Lectura(models.Model):
    TIPOS_RETO = [
        ('lectura', 'Lectura'),
        ('escritura', 'Escritura'),
        ('oralidad', 'Oralidad'),
    ]

    DIFICULTADES = [
        ('basica', 'Básica'),
        ('intermedia', 'Intermedia'),
        ('avanzada', 'Avanzada'),
    ]

    titulo = models.CharField(max_length=150)
    texto = models.TextField()
    tipo_reto = models.CharField(max_length=20, choices=TIPOS_RETO)
    grado = models.PositiveSmallIntegerField(
        default=1,
        validators=[MinValueValidator(1), MaxValueValidator(6)],
    )
    dificultad = models.CharField(max_length=20, choices=DIFICULTADES, default='basica')
    activo = models.BooleanField(default=True)
    creado_por = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='lecturas_creadas',
    )
    creado_en = models.DateTimeField(auto_now_add=True)
    actualizado_en = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = 'lecturas'
        ordering = ['grado', 'titulo']
        verbose_name = 'Lectura'
        verbose_name_plural = 'Lecturas'

    def __str__(self):
        return self.titulo




class RecursoOficial(models.Model):
    """US-39 · T-440: recurso de la Biblioteca Escolar Digital (API oficial), solo primaria.

    Se llena con el comando sincronizar_recursos (T-441). Si "lectura" está vacío,
    el recurso todavía no se importó al catálogo.
    """
    FUENTE = 'Biblioteca Escolar Digital – Ministerio de Educación del Perú'

    uuid = models.CharField(max_length=36, unique=True)
    titulo = models.CharField(max_length=300)
    descripcion = models.TextField(blank=True)
    grado = models.PositiveSmallIntegerField(
        null=True, blank=True, validators=[MinValueValidator(1), MaxValueValidator(6)])
    competencia = models.CharField(max_length=20, choices=Lectura.TIPOS_RETO, blank=True)
    temas = models.JSONField(default=list, blank=True)
    url_recurso = models.URLField(max_length=500)
    url_pdf = models.URLField(max_length=500, blank=True)
    lectura = models.ForeignKey(
        Lectura, on_delete=models.SET_NULL, null=True, blank=True, related_name='recursos_oficiales')
    sincronizado_en = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = 'recursos_oficiales'
        ordering = ['grado', 'titulo']
        verbose_name = 'Recurso oficial'
        verbose_name_plural = 'Recursos oficiales'

    @property
    def importado(self):
        return self.lectura_id is not None

    def __str__(self):
        grado = f'{self.grado}.° grado' if self.grado else 'sin grado'
        return f'{self.titulo} ({grado})'