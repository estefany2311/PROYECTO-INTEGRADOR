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

    def tiene_permiso(self, codigo):
        return self.permisos_asignados.filter(permiso__codigo=codigo).exists()


class Permiso(models.Model):
    ACCIONES = [
        ('ver', 'Ver'),
        ('crear', 'Crear'),
        ('editar', 'Editar'),
        ('desactivar', 'Desactivar'),
    ]

    modulo = models.CharField(max_length=40)  # colegios, docentes, catalogo...
    accion = models.CharField(max_length=20, choices=ACCIONES)
    codigo = models.CharField(max_length=60, unique=True)  # colegios.crear
    descripcion = models.CharField(max_length=150, blank=True)

    class Meta:
        db_table = 'permisos'
        ordering = ['modulo', 'accion']
        verbose_name = 'Permiso'
        verbose_name_plural = 'Permisos'

    def __str__(self):
        return self.codigo


class RolPermiso(models.Model):
    rol = models.ForeignKey(Rol, on_delete=models.CASCADE, related_name='permisos_asignados')
    permiso = models.ForeignKey(Permiso, on_delete=models.CASCADE, related_name='roles_asignados')

    class Meta:
        db_table = 'rol_permisos'
        verbose_name = 'Permiso del rol'
        verbose_name_plural = 'Permisos del rol'
        constraints = [
            models.UniqueConstraint(fields=['rol', 'permiso'], name='rol_permiso_unico'),
        ]

    def __str__(self):
        return f'{self.rol} → {self.permiso}'


class Usuario(AbstractUser):
    email = models.EmailField(unique=True)
    rol = models.ForeignKey(Rol, on_delete=models.PROTECT, related_name='usuarios', null=True)

    class Meta:
        db_table = 'usuarios'

    def __str__(self):
        return f'{self.email} ({self.rol})'