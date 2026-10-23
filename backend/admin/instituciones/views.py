from django.db import transaction
from django.db.models import Count
from rest_framework import filters, viewsets
from rest_framework.decorators import action
from rest_framework.response import Response

from usuarios.permissions import TienePermiso
from .models import AsignacionDocente, Colegio
from .serializers import AsignarDocentesSerializer, ColegioSerializer


class ColegioViewSet(viewsets.ModelViewSet):
    permission_classes = [TienePermiso]
    modulo = 'colegios'
    acciones_personalizadas = {'docentes': 'editar'}
    serializer_class = ColegioSerializer
    # Los colegios no se eliminan: se desactivan con activo=False
    http_method_names = ['get', 'post', 'put', 'patch']
    filter_backends = [filters.SearchFilter]
    search_fields = ['nombre', 'codigo_modular']

    def get_queryset(self):
        queryset = (
            Colegio.objects
            .prefetch_related('asignaciones__docente')
            .annotate(total_docentes=Count('asignaciones'))
        )
        region = self.request.query_params.get('region', '').strip()
        if region:
            queryset = queryset.filter(region__iexact=region)
        return queryset

    @action(detail=True, methods=['post'], url_path='docentes')
    def asignar_docentes(self, request, pk=None):
        colegio = self.get_object()
        serializer = AsignarDocentesSerializer(data=request.data, context={'colegio': colegio})
        serializer.is_valid(raise_exception=True)
        docentes = serializer.validated_data['docentes']

        # Reemplaza la asignación completa del colegio
        with transaction.atomic():
            colegio.asignaciones.exclude(docente__in=docentes).delete()
            ya_asignados = set(colegio.asignaciones.values_list('docente_id', flat=True))
            AsignacionDocente.objects.bulk_create([
                AsignacionDocente(colegio=colegio, docente=docente)
                for docente in docentes
                if docente.id not in ya_asignados
            ])

        colegio = self.get_queryset().get(pk=colegio.pk)
        return Response(self.get_serializer(colegio).data)
