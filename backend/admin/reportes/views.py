from rest_framework.exceptions import NotFound, ValidationError
from rest_framework.response import Response
from rest_framework.views import APIView

from instituciones.models import Colegio
from usuarios.permissions import EsAdmin, TienePermiso
from . import consultas


def _entero(valor, nombre):
    """Convierte un filtro de la URL a número; si viene mal, responde 400."""
    if valor in (None, ''):
        return None
    if not str(valor).isdigit():
        raise ValidationError({nombre: f'El filtro {nombre} debe ser un número.'})
    return int(valor)


class ReporteColegiosView(APIView):
    """US-47 · T-267: reporte consolidado por colegio (solo administrador).

    GET /api/reportes/colegios/            → todos los colegios activos
    GET /api/reportes/colegios/?colegio=3  → un solo colegio
    """
    # Debe tener el permiso reportes.ver Y ser administrador
    permission_classes = [TienePermiso, EsAdmin]
    modulo = 'reportes'

    def get(self, request):
        colegio_id = _entero(request.query_params.get('colegio'), 'colegio')
        if colegio_id is not None and not Colegio.objects.filter(id=colegio_id, activo=True).exists():
            raise NotFound('El colegio no existe o está inactivo.')

        competencias = consultas.promedio_por_competencia(colegio_id=colegio_id)
        semaforo = consultas.alumnos_por_semaforo(colegio_id=colegio_id)
        resumen = {
            'alumnos': sum(semaforo.values()),
            'evaluaciones': sum(c['evaluaciones'] for c in competencias),
            'promedios': {c['competencia']: c['promedio'] for c in competencias},
            'semaforo': semaforo,
        }
        return Response({
            'resumen': resumen,
            'colegios': consultas.rendimiento_por_colegio(colegio_id=colegio_id),
        })