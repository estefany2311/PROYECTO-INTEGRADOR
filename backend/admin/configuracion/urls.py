from rest_framework.routers import DefaultRouter

from .views import ConfiguracionEvaluacionViewSet, ConfiguracionOrtografiaViewSet, PalabraPermitidaViewSet

router = DefaultRouter()
router.register('configuracion/evaluacion', ConfiguracionEvaluacionViewSet, basename='configuracion-evaluacion')
router.register('configuracion/ortografia', ConfiguracionOrtografiaViewSet, basename='configuracion-ortografia')
router.register('configuracion/palabras', PalabraPermitidaViewSet, basename='configuracion-palabras')

urlpatterns = router.urls