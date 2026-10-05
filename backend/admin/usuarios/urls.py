from django.urls import path
from rest_framework_simplejwt.views import TokenObtainPairView, TokenRefreshView
from .views import DocenteListCreateView

urlpatterns = [
    path('auth/login/', TokenObtainPairView.as_view(), name='login'),
    path('auth/refresh/', TokenRefreshView.as_view(), name='token-refresh'),
    path('docentes/', DocenteListCreateView.as_view(), name='docente-list-create'),
]