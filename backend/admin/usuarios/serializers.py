from rest_framework import serializers
from .models import Permiso, Usuario, Rol
from rest_framework.exceptions import AuthenticationFailed
from rest_framework_simplejwt.serializers import TokenObtainPairSerializer

PERMISOS_ROLES_ADMIN = {'roles.ver', 'roles.editar'}


class DocenteCreateSerializer(serializers.ModelSerializer):
    email = serializers.EmailField(required=True)
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
        rol_docente = Rol.objects.get(nombre='DOCENTE')
        return Usuario.objects.create_user(rol=rol_docente, **validated_data)


class DocenteListSerializer(serializers.ModelSerializer):
    rol = serializers.CharField(source='rol.nombre', read_only=True)
    colegio = serializers.SerializerMethodField()

    class Meta:
        model = Usuario
        fields = ['id', 'username', 'first_name', 'last_name', 'email', 'rol', 'is_active', 'date_joined', 'colegio']
        read_only_fields = fields

    def get_colegio(self, obj):
        asignacion = getattr(obj, 'asignacion_colegio', None)
        if asignacion is None:
            return None
        return {'id': asignacion.colegio.id, 'nombre': asignacion.colegio.nombre}


def nombre_completo(usuario):
    return usuario.get_full_name() or usuario.username


class MiPerfilSerializer(serializers.ModelSerializer):
    nombre_completo = serializers.SerializerMethodField()
    rol = serializers.CharField(source='rol.nombre', read_only=True, default=None)
    permisos = serializers.SerializerMethodField()

    class Meta:
        model = Usuario
        fields = ['id', 'username', 'nombre_completo', 'rol', 'permisos']
        read_only_fields = fields

    def get_nombre_completo(self, obj):
        return nombre_completo(obj)

    def get_permisos(self, obj):
        if obj.rol is None:
            return []
        return sorted(obj.rol.permisos_asignados.values_list('permiso__codigo', flat=True))


class PermisoSerializer(serializers.ModelSerializer):
    class Meta:
        model = Permiso
        fields = ['id', 'codigo', 'modulo', 'accion', 'descripcion']
        read_only_fields = fields


class RolSerializer(serializers.ModelSerializer):
    total_usuarios = serializers.IntegerField(read_only=True)
    permisos = serializers.SerializerMethodField()

    class Meta:
        model = Rol
        fields = ['id', 'nombre', 'total_usuarios', 'permisos']
        read_only_fields = fields

    def get_permisos(self, obj):
        return sorted(asignado.permiso.codigo for asignado in obj.permisos_asignados.all())


class ActualizarPermisosRolSerializer(serializers.Serializer):
    permisos = serializers.ListField(child=serializers.CharField(), allow_empty=True)

    def validate_permisos(self, codigos):
        codigos = list(dict.fromkeys(codigo.strip() for codigo in codigos))
        permisos = list(Permiso.objects.filter(codigo__in=codigos))
        inexistentes = sorted(set(codigos) - {p.codigo for p in permisos})
        if inexistentes:
            raise serializers.ValidationError(
                f"Estos permisos no existen: {', '.join(inexistentes)}."
            )

        rol = self.context['rol']
        if rol.nombre == 'ADMIN' and not PERMISOS_ROLES_ADMIN <= set(codigos):
            raise serializers.ValidationError(
                "El rol ADMIN debe conservar los permisos de roles para no perder el acceso."
            )
        return permisos


class UsuarioListSerializer(serializers.ModelSerializer):
    nombre_completo = serializers.SerializerMethodField()
    rol = serializers.SerializerMethodField()

    class Meta:
        model = Usuario
        fields = ['id', 'username', 'nombre_completo', 'email', 'rol', 'is_active']
        read_only_fields = fields

    def get_nombre_completo(self, obj):
        return nombre_completo(obj)

    def get_rol(self, obj):
        if obj.rol is None:
            return None
        return {'id': obj.rol.id, 'nombre': obj.rol.nombre}


class CambiarRolSerializer(serializers.Serializer):
    rol = serializers.PrimaryKeyRelatedField(queryset=Rol.objects.all())

    def validate(self, attrs):
        if self.context['usuario'].pk == self.context['request'].user.pk:
            raise serializers.ValidationError({'rol': "No puedes cambiar tu propio rol."})
        return attrs

class CambiarEstadoUsuarioSerializer(serializers.Serializer):
    is_active = serializers.BooleanField()

    def validate(self, attrs):
        usuario = self.context['usuario']
        if not attrs['is_active']:
            if usuario.pk == self.context['request'].user.pk:
                raise serializers.ValidationError({'is_active': 'No puedes desactivar tu propia cuenta.'})
            es_admin = usuario.rol is not None and usuario.rol.nombre == 'ADMIN'
            quedan_admins = (
                Usuario.objects.filter(rol__nombre='ADMIN', is_active=True).exclude(pk=usuario.pk).exists()
            )
            if es_admin and not quedan_admins:
                raise serializers.ValidationError({'is_active': 'Debe quedar al menos un administrador activo.'})
        return attrs

class LoginSerializer(TokenObtainPairSerializer):
    """Login con JWT que avisa con claridad cuando la cuenta está desactivada."""

    def validate(self, attrs):
        usuario = Usuario.objects.filter(username=attrs.get('username')).first()
        if usuario and not usuario.is_active and usuario.check_password(attrs.get('password', '')):
            raise AuthenticationFailed({
                'detail': 'Tu cuenta está desactivada. Contacta al administrador.',
                'code': 'cuenta_inactiva',
            })
        return super().validate(attrs)