from rest_framework import serializers

from .models import ConfiguracionEvaluacion


class ConfiguracionEvaluacionSerializer(serializers.ModelSerializer):
    actualizado_por = serializers.CharField(source='actualizado_por.username', read_only=True, default=None)

    class Meta:
        model = ConfiguracionEvaluacion
        fields = ['grado', 'umbral_precision', 'tolerancia_pausas', 'palabras_por_minuto',
                  'actualizado_por', 'actualizado_en']
        # El grado no se cambia: cada fila es un grado fijo (1.° a 6.°)
        read_only_fields = ['grado', 'actualizado_por', 'actualizado_en']