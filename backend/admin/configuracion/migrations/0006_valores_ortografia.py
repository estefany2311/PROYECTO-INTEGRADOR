from django.db import migrations

# grado: (sensibilidad, ignorar_tildes)
VALORES = {
    1: ('baja', True),
    2: ('baja', True),
    3: ('media', False),
    4: ('media', False),
    5: ('alta', False),
    6: ('alta', False),
}

# Palabras de la zona que nunca se marcan como error
PALABRAS = ['chacra', 'wawa', 'apu', 'ayllu', 'choclo', 'Inti Raymi']


def cargar(apps, schema_editor):
    Configuracion = apps.get_model('configuracion', 'ConfiguracionOrtografia')
    Palabra = apps.get_model('configuracion', 'PalabraPermitida')
    for grado, (sensibilidad, ignorar_tildes) in VALORES.items():
        Configuracion.objects.update_or_create(
            grado=grado,
            defaults={'sensibilidad': sensibilidad, 'ignorar_tildes': ignorar_tildes},
        )
    for palabra in PALABRAS:
        if not Palabra.objects.filter(palabra__iexact=palabra).exists():
            Palabra.objects.create(palabra=palabra)


def quitar(apps, schema_editor):
    apps.get_model('configuracion', 'ConfiguracionOrtografia').objects.all().delete()
    apps.get_model('configuracion', 'PalabraPermitida').objects.filter(palabra__in=PALABRAS).delete()


class Migration(migrations.Migration):

    dependencies = [
        ('configuracion', '0005_configuracionortografia_palabrapermitida'),
    ]

    operations = [
        migrations.RunPython(cargar, quitar),
    ]