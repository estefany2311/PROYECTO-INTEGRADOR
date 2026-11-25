from drf_spectacular.types import OpenApiTypes
from drf_spectacular.utils import OpenApiParameter, extend_schema, extend_schema_view
from rest_framework import viewsets
from rest_framework.decorators import action
from rest_framework.exceptions import NotFound
from rest_framework.response import Response

from usuarios.permissions import TienePermiso
from .models import (ConfiguracionEvaluacion, ConfiguracionOrtografia, HistorialConfiguracion,
                     PalabraPermitida, ReglaDesbloqueo, ReglaPuntuacion)
from .serializers import (
    ConfiguracionEvaluacionSerializer,
    ConfiguracionOrtografiaSerializer,
    HistorialConfiguracionSerializer,
    PalabraPermitidaSerializer,
    ReglaDesbloqueoSerializer,
    ReglaPuntuacionSerializer,
)
from .servicios import ESTRELLAS_POR_DEFECTO, PUNTAJE_POR_DEFECTO, listar_niveles, obtener_nivel

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

# T-370: este ViewSet no tiene serializer de respuesta, así que se describe a mano para Swagger
P_NIVEL = OpenApiParameter('nivel_id', OpenApiTypes.INT, OpenApiParameter.PATH, description='Id del nivel (tabla nivel).')


@extend_schema_view(
    list=extend_schema(tags=['Desbloqueo de niveles'], summary='Niveles del mapa con su regla',
                       operation_id='configuracion_desbloqueo_list', responses=OpenApiTypes.OBJECT),
                       
    retrieve=extend_schema(tags=['Desbloqueo de niveles'], summary='Regla de un nivel',
                           parameters=[P_NIVEL], responses=OpenApiTypes.OBJECT),
    update=extend_schema(tags=['Desbloqueo de niveles'], summary='Guardar la regla de un nivel',
                         parameters=[P_NIVEL], request=ReglaDesbloqueoSerializer, responses=OpenApiTypes.OBJECT),
    partial_update=extend_schema(tags=['Desbloqueo de niveles'], summary='Cambiar parte de la regla de un nivel',
                                 parameters=[P_NIVEL], request=ReglaDesbloqueoSerializer,
                                 responses=OpenApiTypes.OBJECT),
)

class ReglaDesbloqueoViewSet(viewsets.ViewSet):
    """US-54: regla para desbloquear el siguiente nivel del mapa. Solo se consulta y se actualiza.

    Los niveles vienen de la tabla `nivel` (Spring Boot); la regla, de `reglas_desbloqueo`.
    """
    permission_classes = [TienePermiso]
    modulo = 'configuracion'
    lookup_field = 'nivel_id'
    lookup_value_regex = r'\d+'

    def _nivel(self, nivel_id):
        nivel = obtener_nivel(int(nivel_id))
        if nivel is None:
            raise NotFound('El nivel no existe.')
        return nivel

    def _respuesta(self, nivel, regla):
        """Datos del nivel + su regla (o los valores por defecto si aún no tiene)."""
        return {
            'nivel_id': nivel['id'],
            'orden': nivel['orden'],
            'nombre': nivel['nombre'],
            'tipo_reto': nivel['tipo_reto'],
            'activo': nivel['activo'],
            'estrellas_minimas': regla.estrellas_minimas if regla else ESTRELLAS_POR_DEFECTO,
            'puntaje_minimo': regla.puntaje_minimo if regla else PUNTAJE_POR_DEFECTO,
            'personalizada': regla is not None,
            'actualizado_por': regla.actualizado_por.username if regla and regla.actualizado_por else None,
            'actualizado_en': regla.actualizado_en if regla else None,
        }

    def list(self, request):
        reglas = {r.nivel_id: r for r in ReglaDesbloqueo.objects.select_related('actualizado_por')}
        return Response([self._respuesta(n, reglas.get(n['id'])) for n in listar_niveles()])

    def retrieve(self, request, nivel_id=None):
        nivel = self._nivel(nivel_id)
        regla = ReglaDesbloqueo.objects.select_related('actualizado_por').filter(nivel_id=nivel['id']).first()
        return Response(self._respuesta(nivel, regla))

    def update(self, request, nivel_id=None, partial=False):
        nivel = self._nivel(nivel_id)
        regla = ReglaDesbloqueo.objects.filter(nivel_id=nivel['id']).first()
        serializer = ReglaDesbloqueoSerializer(regla, data=request.data, partial=partial, context={'nivel': nivel})
        serializer.is_valid(raise_exception=True)
        # Si el nivel no tenía regla, se crea; si ya tenía, se actualiza
        regla = serializer.save(nivel_id=nivel['id'], actualizado_por=request.user)
        return Response(self._respuesta(nivel, regla))

    def partial_update(self, request, nivel_id=None):
        return self.update(request, nivel_id=nivel_id, partial=True)