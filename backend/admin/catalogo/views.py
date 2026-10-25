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
        tipo_reto = self.request.query_params.get('tipo_reto', '').strip()
        if tipo_reto:
            queryset = queryset.filter(tipo_reto=tipo_reto)
        return queryset

    def perform_create(self, serializer):
        serializer.save(creado_por=self.request.user)