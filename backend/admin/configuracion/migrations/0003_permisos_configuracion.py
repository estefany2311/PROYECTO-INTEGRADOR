from django.db import migrations

CODIGOS = [('configuracion', 'ver'), ('configuracion', 'editar')]


def cargar(apps, schema_editor):
    Rol = apps.get_model('usuarios', 'Rol')
    Permiso = apps.get_model('usuarios', 'Permiso')
    RolPermiso = apps.get_model('usuarios', 'RolPermiso')
    admin, _ = Rol.objects.get_or_create(nombre='ADMIN')
    for modulo, accion in CODIGOS:
        permiso, _ = Permiso.objects.get_or_create(
            codigo=f'{modulo}.{accion}',
            defaults={'modulo': modulo, 'accion': accion, 'descripcion': f'{accion.capitalize()} configuración de la IA'},
        )
        # Solo el administrador configura el motor de IA
        RolPermiso.objects.get_or_create(rol=admin, permiso=permiso)


def quitar(apps, schema_editor):
    Permiso = apps.get_model('usuarios', 'Permiso')
    Permiso.objects.filter(codigo__in=[f'{m}.{a}' for m, a in CODIGOS]).delete()


class Migration(migrations.Migration):

    dependencies = [
        ('configuracion', '0002_valores_por_defecto'),
        ('usuarios', '0005_cargar_permisos'),
    ]

    operations = [
        migrations.RunPython(cargar, quitar),
    ]