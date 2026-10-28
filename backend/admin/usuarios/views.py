from django.db import transaction
from django.db.models import Count
from rest_framework import filters, generics
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import Permiso, Rol, RolPermiso, Usuario
from .serializers import (
    ActualizarPermisosRolSerializer,
    CambiarEstadoUsuarioSerializer,
    CambiarRolSerializer,
    DocenteCreateSerializer,
    DocenteListSerializer,
    MiPerfilSerializer,
    PermisoSerializer,
    RolSerializer,
    UsuarioListSerializer,
)
from .permissions import TienePermiso


class DocenteListCreateView(generics.ListCreateAPIView):
    permission_classes = [TienePermiso]
    modulo = 'docentes'

    def get_queryset(self):
        return (
            Usuario.objects
            .filter(rol__nombre='DOCENTE')
            .select_related('rol', 'asignacion_colegio__colegio')
            .order_by('last_name', 'first_name')
        )

    def get_serializer_class(self):
        if self.request.method == 'POST':
            return DocenteCreateSerializer
        return DocenteListSerializer


class MiPerfilView(APIView):
    """Datos del usuario autenticado y sus permisos (para mostrar u ocultar menús)."""
    permission_classes = [IsAuthenticated]

    def get(self, request):
        return Response(MiPerfilSerializer(request.user).data)


class PermisoListView(generics.ListAPIView):
    permission_classes = [TienePermiso]
    modulo = 'roles'
    serializer_class = PermisoSerializer
    queryset = Permiso.objects.all()


def roles_con_permisos():
    return (
        Rol.objects
        .annotate(total_usuarios=Count('usuarios', distinct=True))
        .prefetch_related('permisos_asignados__permiso')
        .order_by('id')
    )


class RolListView(generics.ListAPIView):
    permission_classes = [TienePermiso]
    modulo = 'roles'
    serializer_class = RolSerializer

    def get_queryset(self):
        return roles_con_permisos()


class RolPermisosView(APIView):
    permission_classes = [TienePermiso]
    modulo = 'roles'

    def put(self, request, pk):
        rol = generics.get_object_or_404(Rol, pk=pk)
        serializer = ActualizarPermisosRolSerializer(data=request.data, context={'rol': rol})
        serializer.is_valid(raise_exception=True)
        permisos = serializer.validated_data['permisos']

        # Reemplaza todos los permisos del rol
        with transaction.atomic():
            rol.permisos_asignados.exclude(permiso__in=permisos).delete()
            actuales = set(rol.permisos_asignados.values_list('permiso_id', flat=True))
            RolPermiso.objects.bulk_create([
                RolPermiso(rol=rol, permiso=permiso)
                for permiso in permisos
                if permiso.id not in actuales
            ])

        return Response(RolSerializer(roles_con_permisos().get(pk=rol.pk)).data)


class UsuarioListView(generics.ListAPIView):
    permission_classes = [TienePermiso]
    modulo = 'usuarios'
    serializer_class = UsuarioListSerializer
    filter_backends = [filters.SearchFilter]
    search_fields = ['username', 'first_name', 'last_name', 'email']

    def get_queryset(self):
        return Usuario.objects.select_related('rol').order_by('username')


class UsuarioRolView(APIView):
    permission_classes = [TienePermiso]
    modulo = 'roles'

    def patch(self, request, pk):
        usuario = generics.get_object_or_404(Usuario.objects.select_related('rol'), pk=pk)
        serializer = CambiarRolSerializer(
            data=request.data, context={'usuario': usuario, 'request': request}
        )
        serializer.is_valid(raise_exception=True)
        usuario.rol = serializer.validated_data['rol']
        usuario.save(update_fields=['rol'])
        return Response(UsuarioListSerializer(usuario).data)

class UsuarioEstadoView(APIView):
    """Activa o desactiva una cuenta. Usa el permiso usuarios.desactivar."""
    permission_classes = [TienePermiso]
    modulo = 'usuarios'

    def patch(self, request, pk):
        usuario = generics.get_object_or_404(Usuario.objects.select_related('rol'), pk=pk)
        serializer = CambiarEstadoUsuarioSerializer(
            data=request.data, context={'usuario': usuario, 'request': request}
        )
        serializer.is_valid(raise_exception=True)
        usuario.is_active = serializer.validated_data['is_active']
        usuario.save(update_fields=['is_active'])
        return Response(UsuarioListSerializer(usuario).data)