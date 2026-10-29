from rest_framework import serializers
from rest_framework.validators import UniqueValidator

from usuarios.models import Usuario
from .models import Colegio


class DocenteResumenSerializer(serializers.ModelSerializer):
    class Meta:
        model = Usuario
        fields = ['id', 'username', 'first_name', 'last_name', 'email']
        read_only_fields = fields


class ColegioSerializer(serializers.ModelSerializer):
    codigo_modular = serializers.CharField(
        max_length=7,
        validators=[
            *Colegio._meta.get_field('codigo_modular').validators,
            UniqueValidator(
                queryset=Colegio.objects.all(),
                message="Ya existe un colegio con este código modular.",
            ),
        ],
    )
    docentes = serializers.SerializerMethodField()
    total_docentes = serializers.SerializerMethodField()

    class Meta:
        model = Colegio
        fields = [
            'id', 'codigo_modular', 'nombre', 'region', 'distrito', 'activo',
            'creado_en', 'docentes', 'total_docentes',
        ]
        read_only_fields = ['id', 'creado_en']
        extra_kwargs = {
            'nombre': {'error_messages': {
                'blank': "El nombre del colegio es obligatorio.",
                'required': "El nombre del colegio es obligatorio.",
            }},
            'region': {'error_messages': {
                'blank': "La región es obligatoria.",
                'required': "La región es obligatoria.",
            }},
        }

    def get_docentes(self, obj):
        docentes = [asignacion.docente for asignacion in obj.asignaciones.all()]
        return DocenteResumenSerializer(docentes, many=True).data

    def get_total_docentes(self, obj) -> int:
        # Viene anotado desde el ViewSet; al crear se calcula directamente
        total = getattr(obj, 'total_docentes', None)
        return total if total is not None else obj.asignaciones.count()

    def validate_nombre(self, value):
        value = value.strip()
        if not value:
            raise serializers.ValidationError("El nombre del colegio es obligatorio.")
        return value

    def validate_region(self, value):
        value = value.strip()
        if not value:
            raise serializers.ValidationError("La región es obligatoria.")
        return value

    def validate_distrito(self, value):
        return value.strip()


class AsignarDocentesSerializer(serializers.Serializer):
    docentes = serializers.ListField(child=serializers.IntegerField(), allow_empty=True)

    def validate_docentes(self, ids):
        ids = list(dict.fromkeys(ids))
        docentes = list(
            Usuario.objects
            .filter(id__in=ids, rol__nombre='DOCENTE')
            .select_related('asignacion_colegio__colegio')
        )
        if len(docentes) != len(ids):
            raise serializers.ValidationError("Solo se pueden asignar usuarios con rol DOCENTE.")

        colegio = self.context['colegio']
        for docente in docentes:
            asignacion = getattr(docente, 'asignacion_colegio', None)
            if asignacion is not None and asignacion.colegio_id != colegio.id:
                raise serializers.ValidationError(
                    f"El docente {docente.username} ya pertenece al colegio {asignacion.colegio.nombre}."
                )
        return docentes
