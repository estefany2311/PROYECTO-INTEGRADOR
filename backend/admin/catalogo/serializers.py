import os

from rest_framework import serializers

from .biblioteca import MAX_PAGINAS
from .models import Lectura, RecursoLectura, RecursoOficial
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
                  'asignada', 'creado_por', 'docente_id', 'creado_en']
        read_only_fields = ['id', 'activo', 'fuente', 'url_recurso', 'creado_por', 'docente_id', 'creado_en']
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
        fields = ['id', 'titulo', 'descripcion', 'grado', 'competencia', 'url_recurso', 'url_pdf', 'activo',
                  'importado', 'lecturas_importadas', 'fuente', 'sincronizado_en']

    def get_lecturas_importadas(self, obj):
        return Lectura.objects.filter(url_recurso=obj.url_recurso).count()

    def get_fuente(self, obj):
        return RecursoOficial.FUENTE


class EstadoLibroSerializer(serializers.Serializer):
    activo = serializers.BooleanField(error_messages={
        'required': 'Indica si el libro queda activo (true) o desactivado (false).',
        'invalid': 'El campo activo debe ser true o false.'})


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

    

# ---------------------------------------------------------------------
# US-42 · Recursos de una lectura (T-343)
# ---------------------------------------------------------------------
MB = 1024 * 1024

# extensión: (tipo, nombre para los mensajes, primeros bytes del archivo)
FORMATOS_RECURSO = {
    '.jpg': ('imagen', 'JPG', [b'\xff\xd8\xff']),
    '.jpeg': ('imagen', 'JPG', [b'\xff\xd8\xff']),
    '.png': ('imagen', 'PNG', [b'\x89PNG\r\n\x1a\n']),
    '.mp3': ('audio', 'MP3', [b'ID3', b'\xff\xfb', b'\xff\xf3', b'\xff\xf2']),
}
TAMANO_MAXIMO = {'imagen': 2 * MB, 'audio': 5 * MB}
MAX_RECURSOS_POR_LECTURA = 6


def _megas(bytes_):
    return f'{bytes_ / MB:.1f}'.replace('.', ',')


class RecursoLecturaSerializer(serializers.ModelSerializer):
    subido_por = serializers.CharField(source='subido_por.username', read_only=True, default=None)

    class Meta:
        model = RecursoLectura
        fields = ['id', 'lectura', 'tipo', 'archivo', 'nombre_original', 'tamano', 'descripcion',
                  'subido_por', 'creado_en']
        # Se completan solos a partir del archivo subido
        read_only_fields = ['lectura', 'tipo', 'nombre_original', 'tamano', 'subido_por', 'creado_en']

    def validate_archivo(self, archivo):
        """T-343: solo JPG, PNG o MP3, con contenido real de ese tipo y sin pasar el tamaño máximo."""
        nombre = archivo.name
        extension = os.path.splitext(nombre)[1].lower()
        if extension not in FORMATOS_RECURSO:
            raise serializers.ValidationError(
                f'«{nombre}»: tipo de archivo no permitido. Solo imágenes (JPG, PNG) o audios (MP3).')

        tipo, formato, firmas = FORMATOS_RECURSO[extension]

        # El contenido debe coincidir con la extensión (un .exe renombrado a .png no pasa)
        inicio = archivo.read(8)
        archivo.seek(0)
        if not any(inicio.startswith(firma) for firma in firmas):
            raise serializers.ValidationError(
                f'«{nombre}»: el contenido no corresponde a un archivo {formato}.')

        if archivo.size > TAMANO_MAXIMO[tipo]:
            articulo = 'una imagen' if tipo == 'imagen' else 'un audio'
            raise serializers.ValidationError(
                f'«{nombre}» pesa {_megas(archivo.size)} MB: el máximo para {articulo} '
                f'es {TAMANO_MAXIMO[tipo] // MB} MB.')
        return archivo

    def validate(self, attrs):
        lectura = self.context['lectura']
        if lectura.recursos.count() >= MAX_RECURSOS_POR_LECTURA:
            raise serializers.ValidationError({'archivo': (
                f'La lectura ya tiene {MAX_RECURSOS_POR_LECTURA} recursos: elimina uno antes de subir otro.')})
        return attrs

    def create(self, validated_data):
        archivo = validated_data['archivo']
        extension = os.path.splitext(archivo.name)[1].lower()
        validated_data['tipo'] = FORMATOS_RECURSO[extension][0]
        validated_data['nombre_original'] = archivo.name[:255]
        validated_data['tamano'] = archivo.size
        return super().create(validated_data)
