from rest_framework import serializers

from .models import Lectura


class LecturaSerializer(serializers.ModelSerializer):
    creado_por = serializers.CharField(source='creado_por.username', read_only=True, default=None)

    class Meta:
        model = Lectura
        fields = ['id', 'titulo', 'texto', 'tipo_reto', 'activo', 'creado_por', 'creado_en']
        read_only_fields = ['id', 'activo', 'creado_por', 'creado_en']

    def validate_titulo(self, value):
        value = value.strip()
        if not value:
            raise serializers.ValidationError('El título es obligatorio.')
        return value

    def validate_texto(self, value):
        value = value.strip()
        if len(value) < 20:
            raise serializers.ValidationError('El texto de la lectura debe tener al menos 20 caracteres.')
        return value