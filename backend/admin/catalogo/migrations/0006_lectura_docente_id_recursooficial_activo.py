from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('catalogo', '0005_recursolectura'),
    ]

    operations = [
        migrations.AddField(
            model_name='lectura',
            name='docente_id',
            field=models.BigIntegerField(blank=True, db_index=True, null=True),
        ),
        migrations.AddField(
            model_name='recursooficial',
            name='activo',
            field=models.BooleanField(db_default=True, default=True),
        ),
    ]
