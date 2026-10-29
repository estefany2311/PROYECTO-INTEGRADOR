from django.db import migrations

MODULOS = ['colegios', 'docentes', 'usuarios', 'roles', 'catalogo', 'alumnos', 'asignaciones']
ACCIONES = ['ver', 'crear', 'editar', 'desactivar']

# Reportes solo se pueden ver
PERMISOS = [(modulo, accion) for modulo in MODULOS for accion in ACCIONES] + [('reportes', 'ver')]

PERMISOS_DOCENTE = [
    'colegios.ver',
    'catalogo.ver',
    'alumnos.ver', 'alumnos.crear', 'alumnos.editar', 'alumnos.desactivar',
    'asignaciones.ver', 'asignaciones.crear', 'asignaciones.editar',
]


def cargar_permisos(apps, schema_editor):
    Rol = apps.get_model('usuarios', 'Rol')
    Permiso = apps.get_model('usuarios', 'Permiso')
    RolPermiso = apps.get_model('usuarios', 'RolPermiso')

    permisos = {}
    for modulo, accion in PERMISOS:
        codigo = f'{modulo}.{accion}'
        permisos[codigo], _ = Permiso.objects.get_or_create(
            codigo=codigo,
            defaults={'modulo': modulo, 'accion': accion, 'descripcion': f'{accion.capitalize()} {modulo}'},
        )

    admin, _ = Rol.objects.get_or_create(nombre='ADMIN')
    docente, _ = Rol.objects.get_or_create(nombre='DOCENTE')

    for permiso in permisos.values():
        RolPermiso.objects.get_or_create(rol=admin, permiso=permiso)
    for codigo in PERMISOS_DOCENTE:
        RolPermiso.objects.get_or_create(rol=docente, permiso=permisos[codigo])


def quitar_permisos(apps, schema_editor):
    Permiso = apps.get_model('usuarios', 'Permiso')
    codigos = [f'{modulo}.{accion}' for modulo, accion in PERMISOS]
    # Al borrar el permiso se borran sus asignaciones (CASCADE)
    Permiso.objects.filter(codigo__in=codigos).delete()


class Migration(migrations.Migration):
    dependencies = [('usuarios', '0004_permiso_rolpermiso')]
    operations = [migrations.RunPython(cargar_permisos, quitar_permisos)]
