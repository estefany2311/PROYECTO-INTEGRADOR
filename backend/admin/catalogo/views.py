import io

from django.core.management import call_command
from django.core.management.base import CommandError
from django.db import transaction
from rest_framework import filters, status, viewsets
from rest_framework.decorators import action
from rest_framework.exceptions import ValidationError
from rest_framework.response import Response

from usuarios.permissions import TienePermiso
from . import biblioteca
from .models import Lectura, RecursoOficial
from .serializers import (ImportarLecturaSerializer, LecturaSerializer, RangoPaginasSerializer,
                          RecursoOficialSerializer)
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


class RecursoOficialViewSet(viewsets.ReadOnlyModelViewSet):
    """T-443: libros oficiales (sincronizados en T-441) e importación de lecturas desde su PDF."""
    permission_classes = [TienePermiso]
    modulo = 'catalogo'
    serializer_class = RecursoOficialSerializer
    acciones_personalizadas = {'sincronizar': 'editar'}

    def get_queryset(self):
        queryset = RecursoOficial.objects.all()
        params = self.request.query_params
        if params.get('nuevos', '').strip().lower() == 'true':
            queryset = queryset.filter(lectura__isnull=True)
        grado = params.get('grado', '').strip()
        if grado:
            if not grado.isdigit() or not 1 <= int(grado) <= 6:
                raise ValidationError({'grado': 'El filtro de grado debe ser un número entre 1 y 6.'})
            queryset = queryset.filter(grado=int(grado))
        return queryset

    @action(detail=True, methods=['get'])
    def texto(self, request, pk=None):
        """GET /recursos-oficiales/{id}/texto/?desde=24&hasta=25"""
        recurso = self.get_object()
        rango = RangoPaginasSerializer(data=request.query_params)
        rango.is_valid(raise_exception=True)
        desde, hasta = rango.validated_data['desde'], rango.validated_data['hasta']
        if not recurso.url_pdf:
            return Response({'detail': 'Este recurso no tiene PDF.'}, status=status.HTTP_400_BAD_REQUEST)
        try:
            ruta = biblioteca.descargar_pdf(recurso.url_pdf, recurso.uuid)
            texto, total = biblioteca.extraer_texto(ruta, desde, hasta)
        except ValueError as error:
            return Response({'detail': str(error)}, status=status.HTTP_400_BAD_REQUEST)
        except biblioteca.ErrorBiblioteca as error:
            return Response({'detail': str(error)}, status=status.HTTP_502_BAD_GATEWAY)
        aviso = '' if texto else ('Estas páginas no tienen texto que se pueda copiar (pueden ser imágenes). '
                                  'Escribe el texto a mano.')
        return Response({'desde': desde, 'hasta': hasta, 'paginas_total': total, 'texto': texto, 'aviso': aviso})

    @action(detail=True, methods=['post'])
    def importar(self, request, pk=None):
        """POST /recursos-oficiales/{id}/importar/  → crea la lectura con fuente oficial."""
        recurso = self.get_object()
        datos = ImportarLecturaSerializer(data=request.data)
        datos.is_valid(raise_exception=True)
        grado = recurso.grado or datos.validated_data.get('grado')
        if not grado:
            raise ValidationError({'grado': 'Este recurso no tiene grado: indícalo (1 a 6).'})
        with transaction.atomic():
            lectura = Lectura.objects.create(
                titulo=datos.validated_data['titulo'],
                texto=datos.validated_data['texto'],
                tipo_reto=datos.validated_data['tipo_reto'],
                dificultad=datos.validated_data['dificultad'],
                grado=grado,
                fuente='oficial',
                url_recurso=recurso.url_recurso,
                creado_por=request.user,
            )
            if recurso.lectura_id is None:   # marca el libro como importado
                recurso.lectura = lectura
                recurso.save(update_fields=['lectura'])
        return Response(LecturaSerializer(lectura).data, status=status.HTTP_201_CREATED)

    @action(detail=False, methods=['post'])
    def sincronizar(self, request):
        """POST /recursos-oficiales/sincronizar/  → botón "Actualizar catálogo"."""
        salida = io.StringIO()
        try:
            call_command('sincronizar_recursos', limpiar=True, stdout=salida)
        except CommandError as error:
            return Response({'detail': str(error)}, status=status.HTTP_502_BAD_GATEWAY)
        lineas = salida.getvalue().strip().splitlines()
        return Response({'resultado': lineas[-1] if lineas else 'Listo.',
                         'recursos': RecursoOficial.objects.count()})
