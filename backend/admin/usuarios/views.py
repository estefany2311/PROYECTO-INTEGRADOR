from rest_framework import generics, permissions
from .serializers import DocenteCreateSerializer


class DocenteCreateView(generics.CreateAPIView):
    serializer_class = DocenteCreateSerializer
    # TODO (Tarea 3): cambiar a una permission que exija rol ADMIN
    authentication_classes = []   # temporal, hasta la Tarea 3
    permission_classes = [permissions.AllowAny]