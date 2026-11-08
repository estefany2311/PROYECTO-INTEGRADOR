from rest_framework.routers import DefaultRouter

from .views import (ConfiguracionEvaluacionViewSet, ConfiguracionOrtografiaViewSet, PalabraPermitidaViewSet,
                    ReglaPuntuacionViewSet)

router = DefaultRouter()
router.register('configuracion/evaluacion', ConfiguracionEvaluacionViewSet, basename='configuracion-evaluacion')
router.register('configuracion/ortografia', ConfiguracionOrtografiaViewSet, basename='configuracion-ortografia')
router.register('configuracion/palabras', PalabraPermitidaViewSet, basename='configuracion-palabras')
router.register('configuracion/reglas', ReglaPuntuacionViewSet, basename='configuracion-reglas')

urlpatterns = router.urls