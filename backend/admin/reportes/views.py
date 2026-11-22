from datetime import date

from django.http import HttpResponse
from django.utils import timezone
from rest_framework.exceptions import NotFound, ValidationError
from rest_framework.response import Response
from rest_framework.views import APIView

from instituciones.models import Colegio
from usuarios.permissions import EsAdmin, TienePermiso
from . import consultas
from . import exportar

def _entero(valor, nombre):
    """Convierte un filtro de la URL a número; si viene mal, responde 400."""
    if valor in (None, ''):
        return None
    if not str(valor).isdigit():
        raise ValidationError({nombre: f'El filtro {nombre} debe ser un número.'})
    return int(valor)


def _fecha(valor, nombre):
    """US-49 · T-354: convierte una fecha de la URL (AAAA-MM-DD); si viene mal, responde 400."""
    if valor in (None, ''):
        return None
    try:
        return date.fromisoformat(str(valor))
    except ValueError:
        raise ValidationError({nombre: f'La fecha {nombre} debe tener el formato AAAA-MM-DD.'})

def filtros_reporte(request):
    """US-49 · T-354: filtros comunes de los reportes (?grado=&desde=&hasta=)."""
    grado = _entero(request.query_params.get('grado'), 'grado')
    if grado is not None and not 1 <= grado <= 6:
        raise ValidationError({'grado': 'El grado debe estar entre 1 y 6.'})
    desde = _fecha(request.query_params.get('desde'), 'desde')
    hasta = _fecha(request.query_params.get('hasta'), 'hasta')
    validar_periodo(desde, hasta)
    return {'grado': grado, 'desde': desde, 'hasta': hasta}


def validar_periodo(desde, hasta):
    """US-49 · T-355: el periodo debe tener sentido (sin fechas futuras y desde ≤ hasta)."""
    hoy = date.today()
    errores = {}
    if desde is not None and desde > hoy:
        errores['desde'] = 'La fecha desde no puede ser posterior a hoy.'
    if hasta is not None and hasta > hoy:
        errores['hasta'] = 'La fecha hasta no puede ser posterior a hoy.'
    if desde is not None and hasta is not None and desde > hasta and 'hasta' not in errores:
        errores['hasta'] = 'La fecha hasta no puede ser anterior a la fecha desde.'
    if errores:
        raise ValidationError(errores)


class ReporteColegiosView(APIView):
    """US-47 · T-267: reporte consolidado por colegio (solo administrador).

    GET /api/reportes/colegios/            → todos los colegios activos
    GET /api/reportes/colegios/?colegio=3  → un solo colegio
    T-354: también ?grado=2&desde=2026-09-01&hasta=2026-11-30 (periodo, fechas incluidas)
    """
    # Debe tener el permiso reportes.ver Y ser administrador
    permission_classes = [TienePermiso, EsAdmin]
    modulo = 'reportes'

    def get(self, request):
        colegio_id = _entero(request.query_params.get('colegio'), 'colegio')
        if colegio_id is not None and not Colegio.objects.filter(id=colegio_id, activo=True).exists():
            raise NotFound('El colegio no existe o está inactivo.')

        filtros = filtros_reporte(request)
        competencias = consultas.promedio_por_competencia(colegio_id=colegio_id, **filtros)
        semaforo = consultas.alumnos_por_semaforo(colegio_id=colegio_id, **filtros)
        resumen = {
            'alumnos': sum(semaforo.values()),
            'evaluaciones': sum(c['evaluaciones'] for c in competencias),
            'promedios': {c['competencia']: c['promedio'] for c in competencias},
            'semaforo': semaforo,
        }
        return Response({
            'filtros': {'colegio': colegio_id, **filtros},
            'resumen': resumen,
            'colegios': consultas.rendimiento_por_colegio(colegio_id=colegio_id, **filtros),
        })




class ReporteAulasView(APIView):
    """US-47 · T-268: aulas de un colegio con su rendimiento (solo administrador).

    GET /api/reportes/colegios/<id>/aulas/
    T-354: también ?grado=&desde=&hasta=
    """
    permission_classes = [TienePermiso, EsAdmin]
    modulo = 'reportes'

    def get(self, request, colegio_id):
        colegio = Colegio.objects.filter(id=colegio_id, activo=True).first()
        if colegio is None:
            raise NotFound('El colegio no existe o está inactivo.')
        filtros = filtros_reporte(request)
        return Response({
            'filtros': filtros,
            'colegio': {'id': colegio.id, 'nombre': colegio.nombre,
                        'codigo_modular': colegio.codigo_modular, 'region': colegio.region},
            'aulas': consultas.rendimiento_por_aula(colegio.id, **filtros),
        })




class ReporteMapaCalorView(APIView):
    """US-48 · T-350: datos del mapa de calor, desempeño por zona y competencia (solo administrador).

    GET /api/reportes/mapa-calor/                       → por región, todos los grados
    GET /api/reportes/mapa-calor/?agrupar=provincia     → por provincia
    GET /api/reportes/mapa-calor/?agrupar=region&grado=3
    T-354: también ?desde=&hasta=
    """
    permission_classes = [TienePermiso, EsAdmin]
    modulo = 'reportes'

    def get(self, request):
        agrupar = request.query_params.get('agrupar') or 'region'
        if agrupar not in consultas.AGRUPACIONES:
            raise ValidationError({'agrupar': 'agrupar debe ser region o provincia.'})

        filtros = filtros_reporte(request)
        return Response({
            'agrupar': agrupar,
            'grado': filtros['grado'],
            'desde': filtros['desde'],
            'hasta': filtros['hasta'],
            'competencias': consultas.COMPETENCIAS,
            'zonas': consultas.desempeno_por_zona(agrupar=agrupar, **filtros),
        })


class ReporteExcelView(APIView):
    """US-50 · T-358: descarga el reporte por colegio en Excel (solo administrador).

    GET /api/reportes/colegios/excel/
    """
    permission_classes = [TienePermiso, EsAdmin]
    modulo = 'reportes'

    def get(self, request):
        filas = consultas.rendimiento_por_colegio()
        respuesta = HttpResponse(
            exportar.excel_colegios(filas, generado=timezone.localtime()),
            content_type='application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        )
        # "attachment" hace que el navegador lo descargue con ese nombre
        respuesta['Content-Disposition'] = f'attachment; filename="reporte-colegios-{date.today():%Y-%m-%d}.xlsx"'
        return respuesta


class ReportePdfView(APIView):
    """US-50 · T-359: descarga el reporte por colegio en PDF (solo administrador).

    GET /api/reportes/colegios/pdf/
    """
    permission_classes = [TienePermiso, EsAdmin]
    modulo = 'reportes'

    def get(self, request):
        filas = consultas.rendimiento_por_colegio()
        respuesta = HttpResponse(exportar.pdf_colegios(filas, generado=timezone.localtime()),
                                 content_type='application/pdf')
        respuesta['Content-Disposition'] = f'attachment; filename="reporte-colegios-{date.today():%Y-%m-%d}.pdf"'
        return respuesta