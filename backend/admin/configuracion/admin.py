from django.contrib import admin

from .models import ConfiguracionEvaluacion, ConfiguracionOrtografia, HistorialConfiguracion, PalabraPermitida


@admin.register(ConfiguracionEvaluacion)
class ConfiguracionEvaluacionAdmin(admin.ModelAdmin):
    list_display = ('grado', 'umbral_precision', 'tolerancia_pausas', 'palabras_por_minuto', 'actualizado_en')



@admin.register(HistorialConfiguracion)
class HistorialConfiguracionAdmin(admin.ModelAdmin):
    list_display = ('fecha', 'usuario', 'grado', 'campo', 'valor_anterior', 'valor_nuevo')
    list_filter = ('grado', 'campo')




@admin.register(ConfiguracionOrtografia)
class ConfiguracionOrtografiaAdmin(admin.ModelAdmin):
    list_display = ('grado', 'sensibilidad', 'ignorar_tildes', 'actualizado_en')


@admin.register(PalabraPermitida)
class PalabraPermitidaAdmin(admin.ModelAdmin):
    list_display = ('palabra', 'creado_por', 'creado_en')
    search_fields = ('palabra',)