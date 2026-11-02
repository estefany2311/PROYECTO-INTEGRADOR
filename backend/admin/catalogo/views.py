from rest_framework import filters, status, viewsets
from rest_framework.exceptions import ValidationError
from rest_framework.response import Response

from usuarios.permissions import TienePermiso
from .models import Lectura
from .serializers import LecturaSerializer
from .servicios import lectura_esta_asignada


class LecturaViewSet(viewsets.ModelViewSet):
    permission_classes = [TienePermiso]
    modulo = 'catalogo'
    serializer_class = LecturaSerializer
    http_method_names = ['get', 'post', 'put', 'patch', 'delete']
    # DELETE es un borrado lógico: usa el permiso 'catalogo.desactivar'
    acciones_personalizadas = {'destroy': 'desactivar'}
    filter_backends = [filters.SearchFilter]
    search_fields = ['titulo']

    def get_queryset(self):
        queryset = Lectura.objects.select_related('creado_por')
        params = self.request.query_params

        activo = params.get('activo', '').strip().lower()
        if activo:
            if activo not in ('true', 'false'):
                raise ValidationError({'activo': 'El filtro activo debe ser true o false.'})
            queryset = queryset.filter(activo=(activo == 'true'))

        tipo_reto = params.get('tipo_reto', '').strip()
        if tipo_reto:
            queryset = queryset.filter(tipo_reto=tipo_reto)

        grado = params.get('grado', '').strip()
        if grado:
            if not grado.isdigit() or not 1 <= int(grado) <= 6:
                raise ValidationError({'grado': 'El filtro de grado debe ser un número entre 1 y 6.'})
            queryset = queryset.filter(grado=int(grado))

        dificultad = params.get('dificultad', '').strip()
        if dificultad:
            if dificultad not in dict(Lectura.DIFICULTADES):
                raise ValidationError({'dificultad': 'El filtro de dificultad debe ser basica, intermedia o avanzada.'})
            queryset = queryset.filter(dificultad=dificultad)

        return queryset

    def perform_create(self, serializer):
        serializer.save(creado_por=self.request.user)

    def destroy(self, request, *args, **kwargs):
        lectura = self.get_object()
        if lectura_esta_asignada(lectura.id):
            return Response(
                {'detail': 'La lectura está asignada a alumnos: no se puede eliminar, solo desactivar.'},
                status=status.HTTP_400_BAD_REQUEST,
            )
        lectura.activo = False
        lectura.save(update_fields=['activo'])
        return Response(status=status.HTTP_204_NO_CONTENT)