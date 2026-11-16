from rest_framework import serializers

from .biblioteca import MAX_PAGINAS
from .models import Lectura, RecursoOficial
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


class RecursoOficialSerializer(serializers.ModelSerializer):
    """T-443: libro oficial con cuántas lecturas se importaron de él."""
    importado = serializers.BooleanField(read_only=True)
    lecturas_importadas = serializers.SerializerMethodField()
    fuente = serializers.SerializerMethodField()

    class Meta:
        model = RecursoOficial
        fields = ['id', 'titulo', 'descripcion', 'grado', 'competencia', 'url_recurso', 'url_pdf',
                  'importado', 'lecturas_importadas', 'fuente', 'sincronizado_en']

    def get_lecturas_importadas(self, obj):
        return Lectura.objects.filter(url_recurso=obj.url_recurso).count()

    def get_fuente(self, obj):
        return RecursoOficial.FUENTE


class RangoPaginasSerializer(serializers.Serializer):
    desde = serializers.IntegerField(min_value=1, error_messages={
        'required': 'Indica desde qué página.', 'invalid': 'La página debe ser un número.',
        'min_value': 'La página debe ser 1 o mayor.'})
    hasta = serializers.IntegerField(min_value=1, error_messages={
        'required': 'Indica hasta qué página.', 'invalid': 'La página debe ser un número.',
        'min_value': 'La página debe ser 1 o mayor.'})

    def validate(self, datos):
        if datos['hasta'] < datos['desde']:
            raise serializers.ValidationError({'hasta': 'La página final no puede ser menor que la inicial.'})
        if datos['hasta'] - datos['desde'] + 1 > MAX_PAGINAS:
            raise serializers.ValidationError({'hasta': f'Puedes sacar hasta {MAX_PAGINAS} páginas a la vez.'})
        return datos


class ImportarLecturaSerializer(serializers.Serializer):
    """Datos que el admin revisa antes de guardar la lectura importada."""
    titulo = serializers.CharField(max_length=150)
    texto = serializers.CharField()
    tipo_reto = serializers.ChoiceField(choices=Lectura.TIPOS_RETO, error_messages={
        'invalid_choice': 'El tipo de reto debe ser lectura, escritura u oralidad.'})
    dificultad = serializers.ChoiceField(choices=Lectura.DIFICULTADES, default='basica', error_messages={
        'invalid_choice': 'La dificultad debe ser básica, intermedia o avanzada.'})
    grado = serializers.IntegerField(min_value=1, max_value=6, required=False)

    # Mismas reglas que una lectura manual
    validate_titulo = LecturaSerializer.validate_titulo
    validate_texto = LecturaSerializer.validate_texto
