from django.urls import path

from .views import ReporteAulasView, ReporteColegiosView, ReporteMapaCalorView

urlpatterns = [
    path('reportes/colegios/', ReporteColegiosView.as_view(), name='reporte-colegios'),
    path('reportes/colegios/<int:colegio_id>/aulas/', ReporteAulasView.as_view(), name='reporte-aulas'),
    path('reportes/mapa-calor/', ReporteMapaCalorView.as_view(), name='reporte-mapa-calor'),
]