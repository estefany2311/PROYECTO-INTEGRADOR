from rest_framework.routers import DefaultRouter

from .views import LecturaViewSet, RecursoOficialViewSet

router = DefaultRouter()
router.register('lecturas', LecturaViewSet, basename='lectura')
router.register('recursos-oficiales', RecursoOficialViewSet, basename='recurso-oficial')

urlpatterns = router.urls