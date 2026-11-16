from django.contrib import admin

from .models import (ConfiguracionEvaluacion, ConfiguracionOrtografia, HistorialConfiguracion,
                PalabraPermitida, ReglaPuntuacion)

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



@admin.register(ReglaPuntuacion)
class ReglaPuntuacionAdmin(admin.ModelAdmin):
    list_display = ('tipo_reto', 'puntos', 'umbral_1_estrella', 'umbral_2_estrellas', 'umbral_3_estrellas')