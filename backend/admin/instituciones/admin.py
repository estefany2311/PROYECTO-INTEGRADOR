from django.contrib import admin

from .models import AsignacionDocente, Colegio


class AsignacionDocenteInline(admin.TabularInline):
    model = AsignacionDocente
    extra = 0


@admin.register(Colegio)
class ColegioAdmin(admin.ModelAdmin):
    list_display = ('codigo_modular', 'nombre', 'region', 'distrito', 'activo')
    search_fields = ('nombre', 'codigo_modular')
    inlines = [AsignacionDocenteInline]


@admin.register(AsignacionDocente)
class AsignacionDocenteAdmin(admin.ModelAdmin):
    list_display = ('docente', 'colegio', 'asignado_en')
