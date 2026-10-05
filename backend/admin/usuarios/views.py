from rest_framework import generics
from .models import Usuario
from .serializers import DocenteCreateSerializer, DocenteListSerializer
from .permissions import EsAdmin


class DocenteListCreateView(generics.ListCreateAPIView):
    permission_classes = [EsAdmin]

    def get_queryset(self):
        return (
            Usuario.objects
            .filter(rol__nombre='DOCENTE')
            .select_related('rol')
            .order_by('last_name', 'first_name')
        )

    def get_serializer_class(self):
        if self.request.method == 'POST':
            return DocenteCreateSerializer
        return DocenteListSerializer