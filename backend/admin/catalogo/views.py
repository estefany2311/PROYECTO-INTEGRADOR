from rest_framework import filters, viewsets

from usuarios.permissions import TienePermiso
from .models import Lectura
from .serializers import LecturaSerializer


class LecturaViewSet(viewsets.ModelViewSet):
    permission_classes = [TienePermiso]
    modulo = 'catalogo'
    serializer_class = LecturaSerializer
    # Editar y desactivar lecturas se hará en una historia posterior
    http_method_names = ['get', 'post']
    filter_backends = [filters.SearchFilter]
    search_fields = ['titulo']

    def get_queryset(self):
        queryset = Lectura.objects.select_related('creado_por')
        params = self.request.query_params

        tipo_reto = params.get('tipo_reto', '').strip()
        if tipo_reto:
            queryset = queryset.filter(tipo_reto=tipo_reto)

        grado = params.get('grado', '').strip()
        if grado.isdigit():
            queryset = queryset.filter(grado=int(grado))

        dificultad = params.get('dificultad', '').strip()
        if dificultad:
            queryset = queryset.filter(dificultad=dificultad)

        return queryset

    def perform_create(self, serializer):
        serializer.save(creado_por=self.request.user)