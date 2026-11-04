from rest_framework.routers import DefaultRouter

from .views import ConfiguracionEvaluacionViewSet

router = DefaultRouter()
router.register('configuracion/evaluacion', ConfiguracionEvaluacionViewSet, basename='configuracion-evaluacion')

urlpatterns = router.urls