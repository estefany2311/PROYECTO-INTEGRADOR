from rest_framework import serializers
from .models import Usuario, Rol


class DocenteCreateSerializer(serializers.ModelSerializer):
    email = serializers.EmailField(required=True)  # valida formato de correo
    password = serializers.CharField(write_only=True, min_length=8)

    class Meta:
        model = Usuario
        fields = ['id', 'username', 'first_name', 'last_name', 'email', 'password']
        read_only_fields = ['id']
        extra_kwargs = {
            'first_name': {'required': True, 'allow_blank': False},
            'last_name': {'required': True, 'allow_blank': False},
        }

    def validate_email(self, value):
        value = value.strip().lower()
        if Usuario.objects.filter(email__iexact=value).exists():
            raise serializers.ValidationError("Ya existe un usuario con este correo.")
        return value

    def create(self, validated_data):
        rol_docente = Rol.objects.get(nombre='DOCENTE')  # sembrado por tu migración
        # create_user aplica hash a la contraseña (nunca se guarda en texto plano)
        return Usuario.objects.create_user(rol=rol_docente, **validated_data)