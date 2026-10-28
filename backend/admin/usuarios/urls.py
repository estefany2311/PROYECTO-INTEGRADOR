from django.urls import path
from rest_framework_simplejwt.views import TokenObtainPairView, TokenRefreshView
from .views import (
    DocenteListCreateView,
    MiPerfilView,
    PermisoListView,
    RolListView,
    RolPermisosView,
    UsuarioListView,
    UsuarioRolView,
    UsuarioEstadoView,
)

urlpatterns = [
    path('auth/login/', TokenObtainPairView.as_view(), name='login'),
    path('auth/refresh/', TokenRefreshView.as_view(), name='token-refresh'),
    path('auth/me/', MiPerfilView.as_view(), name='mi-perfil'),
    path('docentes/', DocenteListCreateView.as_view(), name='docente-list-create'),
    path('permisos/', PermisoListView.as_view(), name='permiso-list'),
    path('roles/', RolListView.as_view(), name='rol-list'),
    path('roles/<int:pk>/permisos/', RolPermisosView.as_view(), name='rol-permisos'),
    path('usuarios/', UsuarioListView.as_view(), name='usuario-list'),
    path('usuarios/<int:pk>/rol/', UsuarioRolView.as_view(), name='usuario-rol'),
    path('usuarios/<int:pk>/estado/', UsuarioEstadoView.as_view(), name='usuario-estado'),
]
