from django.conf import settings
from django.db import models


class Lectura(models.Model):
    TIPOS_RETO = [
        ('lectura', 'Lectura'),
        ('escritura', 'Escritura'),
        ('oralidad', 'Oralidad'),
    ]

    titulo = models.CharField(max_length=150)
    texto = models.TextField()
    tipo_reto = models.CharField(max_length=20, choices=TIPOS_RETO)
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
        ordering = ['titulo']
        verbose_name = 'Lectura'
        verbose_name_plural = 'Lecturas'

    def __str__(self):
        return self.titulo