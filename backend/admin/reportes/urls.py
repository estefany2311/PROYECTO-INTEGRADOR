from django.urls import path

from .views import ReporteColegiosView

urlpatterns = [
    path('reportes/colegios/', ReporteColegiosView.as_view(), name='reporte-colegios'),
]