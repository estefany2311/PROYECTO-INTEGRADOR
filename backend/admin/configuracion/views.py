from rest_framework import viewsets
from rest_framework.decorators import action
from rest_framework.response import Response

from usuarios.permissions import TienePermiso
from .models import (ConfiguracionEvaluacion, ConfiguracionOrtografia, HistorialConfiguracion,
                     PalabraPermitida, ReglaPuntuacion)
from .serializers import (
    ConfiguracionEvaluacionSerializer,
    ConfiguracionOrtografiaSerializer,
    HistorialConfiguracionSerializer,
    PalabraPermitidaSerializer,
    ReglaPuntuacionSerializer,
)

CAMPOS_AUDITADOS = ['umbral_precision', 'tolerancia_pausas', 'palabras_por_minuto']


class ConfiguracionEvaluacionViewSet(viewsets.ModelViewSet):
    """US-51: rigor de la evaluación de voz por grado. Solo se consulta y se actualiza."""
    permission_classes = [TienePermiso]
    modulo = 'configuracion'
    serializer_class = ConfiguracionEvaluacionSerializer
    queryset = ConfiguracionEvaluacion.objects.select_related('actualizado_por')
    lookup_field = 'grado'
    lookup_value_regex = '[1-6]'  # solo grados válidos; así /historial/ no se confunde con un grado
    # No se crean ni se borran grados: siempre son los 6 cargados por defecto
    http_method_names = ['get', 'put', 'patch']

    def perform_update(self, serializer):
        # T-252: guardar en el historial cada campo que cambió
        antes = {campo: getattr(serializer.instance, campo) for campo in CAMPOS_AUDITADOS}
        config = serializer.save(actualizado_por=self.request.user)
        cambios = [
            HistorialConfiguracion(
                grado=config.grado,
                campo=campo,
                valor_anterior=str(antes[campo]),
                valor_nuevo=str(getattr(config, campo)),
                usuario=self.request.user,
            )
            for campo in CAMPOS_AUDITADOS
            if getattr(config, campo) != antes[campo]
        ]
        HistorialConfiguracion.objects.bulk_create(cambios)

    @action(detail=False, methods=['get'], url_path='historial')
    def historial(self, request):
        """Últimos 50 cambios (se puede filtrar con ?grado=3)."""
        cambios = HistorialConfiguracion.objects.select_related('usuario')
        grado = request.query_params.get('grado', '').strip()
        if grado.isdigit():
            cambios = cambios.filter(grado=int(grado))
        return Response(HistorialConfiguracionSerializer(cambios[:50], many=True).data)




class ConfiguracionOrtografiaViewSet(viewsets.ModelViewSet):
    """US-52: sensibilidad y tildes por grado. Solo se consulta y se actualiza."""
    permission_classes = [TienePermiso]
    modulo = 'configuracion'
    serializer_class = ConfiguracionOrtografiaSerializer
    queryset = ConfiguracionOrtografia.objects.select_related('actualizado_por')
    lookup_field = 'grado'
    lookup_value_regex = '[1-6]'
    http_method_names = ['get', 'put', 'patch']

    def perform_update(self, serializer):
        serializer.save(actualizado_por=self.request.user)


class PalabraPermitidaViewSet(viewsets.ModelViewSet):
    """US-52: palabras que el validador nunca marca como error."""
    permission_classes = [TienePermiso]
    modulo = 'configuracion'
    serializer_class = PalabraPermitidaSerializer
    queryset = PalabraPermitida.objects.select_related('creado_por')
    http_method_names = ['get', 'post', 'delete']
    # Agregar o quitar una palabra es editar la configuración
    acciones_personalizadas = {'create': 'editar', 'destroy': 'editar'}

    def perform_create(self, serializer):
        serializer.save(creado_por=self.request.user)




class ReglaPuntuacionViewSet(viewsets.ModelViewSet):
    """US-53: puntos y umbrales de estrellas por tipo de reto. Solo se consulta y se actualiza."""
    permission_classes = [TienePermiso]
    modulo = 'configuracion'
    serializer_class = ReglaPuntuacionSerializer
    queryset = ReglaPuntuacion.objects.select_related('actualizado_por')
    lookup_field = 'tipo_reto'
    lookup_value_regex = 'lectura|escritura|oralidad'
    http_method_names = ['get', 'put', 'patch']

    def perform_update(self, serializer):
        serializer.save(actualizado_por=self.request.user)