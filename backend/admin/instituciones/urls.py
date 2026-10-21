from rest_framework.routers import DefaultRouter

from .views import ColegioViewSet

router = DefaultRouter()
router.register('colegios', ColegioViewSet, basename='colegio')

urlpatterns = router.urls
