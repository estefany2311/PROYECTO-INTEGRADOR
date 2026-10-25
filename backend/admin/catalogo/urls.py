from rest_framework.routers import DefaultRouter

from .views import LecturaViewSet

router = DefaultRouter()
router.register('lecturas', LecturaViewSet, basename='lectura')

urlpatterns = router.urls