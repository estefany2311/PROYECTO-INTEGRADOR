from django.contrib import admin

from .models import Lectura


@admin.register(Lectura)
class LecturaAdmin(admin.ModelAdmin):
    list_display = ('titulo', 'tipo_reto', 'activo', 'creado_por', 'creado_en')
    list_filter = ('tipo_reto', 'activo')
    search_fields = ('titulo',)