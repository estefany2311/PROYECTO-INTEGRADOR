from django.contrib import admin

from .models import ConfiguracionEvaluacion


@admin.register(ConfiguracionEvaluacion)
class ConfiguracionEvaluacionAdmin(admin.ModelAdmin):
    list_display = ('grado', 'umbral_precision', 'tolerancia_pausas', 'palabras_por_minuto', 'actualizado_en')