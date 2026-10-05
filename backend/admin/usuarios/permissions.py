from rest_framework.permissions import BasePermission


class EsAdmin(BasePermission):
    """Permite el acceso solo a usuarios autenticados con rol ADMIN."""
    message = "Solo los administradores pueden realizar esta acción."

    def has_permission(self, request, view):
        user = request.user
        return bool(
            user
            and user.is_authenticated
            and user.rol is not None
            and user.rol.nombre == 'ADMIN'
        )