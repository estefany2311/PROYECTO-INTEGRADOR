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


ACCIONES_POR_METODO = {
    'GET': 'ver',
    'HEAD': 'ver',
    'OPTIONS': 'ver',
    'POST': 'crear',
    'PUT': 'editar',
    'PATCH': 'editar',
}


def codigos_permiso(request):
    """Códigos de permiso del rol del usuario, guardados en caché durante la petición."""
    if not hasattr(request, '_codigos_permiso'):
        rol = request.user.rol
        request._codigos_permiso = set(
            rol.permisos_asignados.values_list('permiso__codigo', flat=True)
        )
    return request._codigos_permiso


def es_desactivacion(request):
    """Un PATCH que solo cambia el estado (activar o desactivar) usa el permiso 'desactivar'."""
    datos = request.data if hasattr(request.data, 'keys') else {}
    campos = set(datos.keys())
    return bool(campos) and campos <= {'activo', 'is_active'}

class TienePermiso(BasePermission):
    """
    Verifica el permiso '<modulo>.<accion>' del rol del usuario.

    La vista define `modulo` y, si lo necesita, `acciones_personalizadas`
    ({nombre_del_action: accion}) para sus @action.
    """
    message = "No tienes permiso para realizar esta acción."

    def has_permission(self, request, view):
        user = request.user
        if not (user and user.is_authenticated and user.rol is not None):
            return False
        return f'{view.modulo}.{self.accion(request, view)}' in codigos_permiso(request)

    def accion(self, request, view):
        personalizadas = getattr(view, 'acciones_personalizadas', {})
        nombre_action = getattr(view, 'action', None)
        if nombre_action:
            # Se acepta el nombre del método o su url_path (p. ej. 'docentes')
            url_path = getattr(getattr(view, nombre_action, None), 'url_path', None)
            for clave in (nombre_action, url_path):
                if clave in personalizadas:
                    return personalizadas[clave]
        if request.method == 'PATCH' and es_desactivacion(request):
            return 'desactivar'
        return ACCIONES_POR_METODO.get(request.method, 'editar')
