"""US-47 · T-265: datos de prueba de evaluaciones para poblar los reportes.

Agrega 3 evaluaciones por competencia (lectura, escritura, oralidad) a cada alumno activo,
repartidas en las últimas 3 semanas. Los alumnos salen con nivel alto, medio o bajo,
para que el semáforo muestre los tres colores.

Todas las filas cargadas llevan la hora 07:26:05 (marca de T-265); así
--borrar elimina solo estas y nunca las evaluaciones reales de la app.
"""
import random
from datetime import datetime, time, timedelta

from django.core.management.base import BaseCommand, CommandError
from django.db import connection, transaction

from reportes.consultas import COMPETENCIAS, tablas_disponibles

MARCA = time(7, 26, 5)  # T-265
NIVELES = {'alto': (78, 95), 'medio': (55, 74), 'bajo': (30, 49)}


class Command(BaseCommand):
    help = 'Carga evaluaciones de prueba para los reportes (o las borra con --borrar).'

    def add_arguments(self, parser):
        parser.add_argument('--borrar', action='store_true',
                            help='Borra solo las evaluaciones cargadas por este comando.')

    def handle(self, *args, **opciones):
        if not tablas_disponibles():
            raise CommandError('No existen las tablas evaluacion, estudiante y aula. '
                               'Levanta primero los backends de Spring Boot para que Flyway las cree.')
        if opciones['borrar']:
            self.borrar()
        else:
            self.cargar()

    def borrar(self):
        with connection.cursor() as cursor:
            cursor.execute('DELETE FROM evaluacion WHERE evaluado_en::time = %s', [MARCA])
            self.stdout.write(self.style.SUCCESS(f'Se borraron {cursor.rowcount} evaluaciones de prueba.'))

    @transaction.atomic
    def cargar(self):
        with connection.cursor() as cursor:
            cursor.execute('SELECT id FROM estudiante WHERE activo ORDER BY id')
            alumnos = [fila[0] for fila in cursor.fetchall()]
            if not alumnos:
                raise CommandError('No hay alumnos activos en la tabla estudiante.')

            # Si ya se cargaron antes, se reemplazan (así el comando se puede correr varias veces)
            cursor.execute('DELETE FROM evaluacion WHERE evaluado_en::time = %s', [MARCA])

            hoy = datetime.now().date()
            filas = []
            for posicion, alumno in enumerate(alumnos):
                nivel = list(NIVELES)[posicion % 3]       # alto, medio, bajo, alto, ...
                minimo, maximo = NIVELES[nivel]
                azar = random.Random(alumno)              # mismo alumno → mismas notas
                for competencia in COMPETENCIAS:
                    for semana in range(3):
                        fecha = datetime.combine(hoy - timedelta(weeks=semana), MARCA)
                        filas.append((alumno, competencia, azar.randint(minimo, maximo), fecha))

            cursor.executemany(
                'INSERT INTO evaluacion (estudiante_id, competencia, puntaje, evaluado_en) VALUES (%s, %s, %s, %s)',
                filas,
            )
        self.stdout.write(self.style.SUCCESS(
            f'Se cargaron {len(filas)} evaluaciones de prueba para {len(alumnos)} alumnos.'))