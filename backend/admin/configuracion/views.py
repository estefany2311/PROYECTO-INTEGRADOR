from rest_framework import viewsets

from usuarios.permissions import TienePermiso
from .models import ConfiguracionEvaluacion
from .serializers import ConfiguracionEvaluacionSerializer


class ConfiguracionEvaluacionViewSet(viewsets.ModelViewSet):
    """US-51: rigor de la evaluación de voz por grado. Solo se consulta y se actualiza."""
    permission_classes = [TienePermiso]
    modulo = 'configuracion'
    serializer_class = ConfiguracionEvaluacionSerializer
    queryset = ConfiguracionEvaluacion.objects.select_related('actualizado_por')
    lookup_field = 'grado'
    # No se crean ni se borran grados: siempre son los 6 cargados por defecto
    http_method_names = ['get', 'put', 'patch']

    def perform_update(self, serializer):
        serializer.save(actualizado_por=self.request.user)