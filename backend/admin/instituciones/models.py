from django.conf import settings
from django.core.exceptions import ValidationError
from django.core.validators import RegexValidator
from django.db import models


class Colegio(models.Model):
    codigo_modular = models.CharField(
        max_length=7,
        unique=True,
        validators=[RegexValidator(r'^\d{7}$', 'El código modular debe tener 7 dígitos.')],
    )
    nombre = models.CharField(max_length=150)
    region = models.CharField(max_length=60)
    distrito = models.CharField(max_length=80, blank=True)
    activo = models.BooleanField(default=True)
    creado_en = models.DateTimeField(auto_now_add=True)
    actualizado_en = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = 'colegios'
        ordering = ['nombre']
        verbose_name = 'Colegio'
        verbose_name_plural = 'Colegios'

    def __str__(self):
        return f'{self.nombre} ({self.codigo_modular})'


class AsignacionDocente(models.Model):
    # OneToOne: cada docente pertenece a un solo colegio
    colegio = models.ForeignKey(Colegio, on_delete=models.CASCADE, related_name='asignaciones')
    docente = models.OneToOneField(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name='asignacion_colegio',
        limit_choices_to={'rol__nombre': 'DOCENTE'},
    )
    asignado_en = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'colegio_docentes'

    def clean(self):
        rol = getattr(self.docente, 'rol', None) if self.docente_id else None
        if rol is None or rol.nombre != 'DOCENTE':
            raise ValidationError('Solo se pueden asignar usuarios con rol DOCENTE.')

    def __str__(self):
        return f'{self.docente.username} → {self.colegio.nombre}'
