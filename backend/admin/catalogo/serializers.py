from rest_framework import serializers

from .models import Lectura
from .servicios import lectura_esta_asignada


class LecturaSerializer(serializers.ModelSerializer):
    creado_por = serializers.CharField(source='creado_por.username', read_only=True, default=None)
    asignada = serializers.SerializerMethodField()

    grado = serializers.IntegerField(
        default=1,
        min_value=1,
        max_value=6,
        error_messages={
            'min_value': 'El grado debe estar entre 1.º y 6.º.',
            'max_value': 'El grado debe estar entre 1.º y 6.º.',
            'invalid': 'El grado debe ser un número entre 1 y 6.',
        },
    )
    dificultad = serializers.ChoiceField(
        choices=Lectura.DIFICULTADES,
        default='basica',
        error_messages={'invalid_choice': 'La dificultad debe ser básica, intermedia o avanzada.'},
    )

    class Meta:
        model = Lectura
        fields = ['id', 'titulo', 'texto', 'tipo_reto', 'grado', 'dificultad', 'activo', 'fuente', 'url_recurso',
                  'asignada', 'creado_por', 'creado_en']
        read_only_fields = ['id', 'activo', 'fuente', 'url_recurso', 'creado_por', 'creado_en']
    def get_asignada(self, obj):
        return lectura_esta_asignada(obj.id)

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