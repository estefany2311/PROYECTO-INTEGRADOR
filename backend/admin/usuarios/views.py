from rest_framework import generics
from .serializers import DocenteCreateSerializer
from .permissions import EsAdmin


class DocenteCreateView(generics.CreateAPIView):
    serializer_class = DocenteCreateSerializer
    permission_classes = [EsAdmin]