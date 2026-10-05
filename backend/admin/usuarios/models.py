from django.contrib.auth.models import AbstractUser
from django.db import models


class Rol(models.Model):
    nombre = models.CharField(max_length=30, unique=True)  # ADMIN, DOCENTE

    class Meta:
        db_table = 'roles'
        verbose_name = 'Rol'
        verbose_name_plural = 'Roles'

    def __str__(self):
        return self.nombre


class Usuario(AbstractUser):
    email = models.EmailField(unique=True)
    rol = models.ForeignKey(Rol, on_delete=models.PROTECT, related_name='usuarios', null=True)

    class Meta:
        db_table = 'usuarios'

    def __str__(self):
        return f'{self.email} ({self.rol})'