from django.db import migrations

# tipo de reto: (puntos, ★, ★★, ★★★)
REGLAS = {
    'lectura': (10, 50, 70, 90),
    'escritura': (15, 50, 70, 90),
    'oralidad': (15, 55, 75, 90),
}


def cargar(apps, schema_editor):
    Regla = apps.get_model('configuracion', 'ReglaPuntuacion')
    for tipo, (puntos, una, dos, tres) in REGLAS.items():
        Regla.objects.update_or_create(
            tipo_reto=tipo,
            defaults={'puntos': puntos, 'umbral_1_estrella': una,
                      'umbral_2_estrellas': dos, 'umbral_3_estrellas': tres},
        )


def quitar(apps, schema_editor):
    apps.get_model('configuracion', 'ReglaPuntuacion').objects.all().delete()


class Migration(migrations.Migration):

    dependencies = [
        ('configuracion', '0007_reglapuntuacion'),
    ]

    operations = [
        migrations.RunPython(cargar, quitar),
    ]