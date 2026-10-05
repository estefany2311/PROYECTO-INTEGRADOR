from django.urls import path
from .views import DocenteCreateView

urlpatterns = [
    path('docentes/', DocenteCreateView.as_view(), name='docente-create'),
]