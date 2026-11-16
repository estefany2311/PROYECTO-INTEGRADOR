from decimal import Decimal

from django.db import migrations

# grado, % precisión, segundos de pausa, palabras por minuto
VALORES = [
    (1, 60, '3.0', 30),
    (2, 65, '2.5', 45),
    (3, 70, '2.0', 60),
    (4, 75, '2.0', 75),
    (5, 80, '1.5', 85),
    (6, 85, '1.5', 95),
]


def cargar(apps, schema_editor):
    Configuracion = apps.get_model('configuracion', 'ConfiguracionEvaluacion')
    for grado, precision, pausas, ppm in VALORES:
        Configuracion.objects.update_or_create(
            grado=grado,
            defaults={
                'umbral_precision': precision,
                'tolerancia_pausas': Decimal(pausas),
                'palabras_por_minuto': ppm,
            },
        )


def quitar(apps, schema_editor):
    apps.get_model('configuracion', 'ConfiguracionEvaluacion').objects.all().delete()


class Migration(migrations.Migration):

    dependencies = [
        ('configuracion', '0001_initial'),
    ]

    operations = [
        migrations.RunPython(cargar, quitar),
    ]