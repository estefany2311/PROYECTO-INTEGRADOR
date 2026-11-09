from unittest import mock

from django.db import connection
from django.test import TestCase

from instituciones.models import AsignacionDocente, Colegio
from usuarios.models import Rol, Usuario
from . import consultas

TABLAS = [
    'CREATE TABLE IF NOT EXISTS aula (id BIGINT PRIMARY KEY, nombre VARCHAR(100), docente_id BIGINT)',
    'CREATE TABLE IF NOT EXISTS estudiante (id BIGINT PRIMARY KEY, nombres VARCHAR(100), apellidos VARCHAR(100),'
    ' aula_id BIGINT, grado_id SMALLINT, activo BOOLEAN DEFAULT TRUE)',
    'CREATE TABLE IF NOT EXISTS evaluacion (id BIGSERIAL PRIMARY KEY, estudiante_id BIGINT,'
    ' competencia VARCHAR(20), puntaje SMALLINT, evaluado_en TIMESTAMP DEFAULT NOW())',
]


class ConsultasAgregacionTests(TestCase):
    """T-264: promedio por competencia y alumnos por semáforo."""

    def setUp(self):
        rol_docente, _ = Rol.objects.get_or_create(nombre='DOCENTE')
        docente_a = Usuario.objects.create_user(
            username='doc_a', email='a@test.pe', password='clave-segura-1', rol=rol_docente)
        docente_b = Usuario.objects.create_user(
            username='doc_b', email='b@test.pe', password='clave-segura-1', rol=rol_docente)
        self.colegio_a = Colegio.objects.create(codigo_modular='1111111', nombre='IE Ccollpa', region='Cusco')
        self.colegio_b = Colegio.objects.create(codigo_modular='2222222', nombre='IE Huaccana', region='Apurímac')
        AsignacionDocente.objects.create(colegio=self.colegio_a, docente=docente_a)
        AsignacionDocente.objects.create(colegio=self.colegio_b, docente=docente_b)

        with connection.cursor() as c:
            for sql in TABLAS:
                c.execute(sql)
            c.execute('INSERT INTO aula VALUES (101, %s, %s), (102, %s, %s)',
                      ['Aula A', docente_a.id, 'Aula B', docente_b.id])
            # id, aula, grado, activo
            c.execute("""INSERT INTO estudiante (id, nombres, apellidos, aula_id, grado_id, activo) VALUES
                (1, 'Ana', 'Q', 101, 2, TRUE),    -- promedio 75  → verde
                (2, 'Luis', 'M', 101, 2, TRUE),   -- promedio 50  → amarillo
                (3, 'Rosa', 'T', 102, 4, TRUE),   -- promedio 30  → rojo
                (4, 'Juan', 'C', 102, 4, TRUE),   -- sin notas    → sin evaluar
                (5, 'Inés', 'P', 101, 2, FALSE)   -- inactivo     → no cuenta
            """)
            c.execute("""INSERT INTO evaluacion (estudiante_id, competencia, puntaje) VALUES
                (1, 'lectura', 80), (1, 'escritura', 70),
                (2, 'lectura', 60), (2, 'oralidad', 40),
                (3, 'lectura', 30),
                (5, 'lectura', 100)
            """)

    def por_competencia(self, **filtros):
        return {r['competencia']: r for r in consultas.promedio_por_competencia(**filtros)}

    # --- Promedio por competencia ---
    def test_promedio_general_por_competencia(self):
        r = self.por_competencia()
        self.assertEqual(r['lectura']['promedio'], 56.7)    # (80 + 60 + 30) / 3
        self.assertEqual(r['escritura']['promedio'], 70.0)
        self.assertEqual(r['oralidad']['promedio'], 40.0)
        self.assertEqual(r['lectura']['alumnos'], 3)

    def test_el_alumno_inactivo_no_cuenta(self):
        self.assertEqual(self.por_competencia()['lectura']['evaluaciones'], 3)  # sin el 100 de Inés

    def test_filtro_por_colegio(self):
        r = self.por_competencia(colegio_id=self.colegio_a.id)
        self.assertEqual(r['lectura']['promedio'], 70.0)    # (80 + 60) / 2

    def test_filtro_por_aula_y_por_grado(self):
        self.assertEqual(self.por_competencia(aula_id=102)['lectura']['promedio'], 30.0)
        self.assertEqual(self.por_competencia(grado=2)['oralidad']['promedio'], 40.0)

    def test_siempre_devuelve_las_3_competencias(self):
        r = self.por_competencia(aula_id=102)
        self.assertEqual(set(r), {'lectura', 'escritura', 'oralidad'})
        self.assertIsNone(r['escritura']['promedio'])
        self.assertEqual(r['escritura']['evaluaciones'], 0)

    # --- Semáforo ---
    def test_alumnos_por_semaforo_general(self):
        self.assertEqual(consultas.alumnos_por_semaforo(),
                         {'verde': 1, 'amarillo': 1, 'rojo': 1, 'sin_evaluar': 1})

    def test_semaforo_por_colegio(self):
        self.assertEqual(consultas.alumnos_por_semaforo(colegio_id=self.colegio_b.id),
                         {'verde': 0, 'amarillo': 0, 'rojo': 1, 'sin_evaluar': 1})

    def test_color_semaforo_en_los_bordes(self):
        casos = {None: 'sin_evaluar', 49.9: 'rojo', 50: 'amarillo', 74.9: 'amarillo', 75: 'verde', 100: 'verde'}
        self.assertEqual({p: consultas.color_semaforo(p) for p in casos}, casos)

    # --- Sin tablas de Spring Boot ---
    @mock.patch('reportes.consultas.tablas_disponibles', return_value=False)
    def test_sin_tablas_devuelve_todo_en_cero(self, _tablas):
        self.assertEqual(consultas.alumnos_por_semaforo(),
                         {'verde': 0, 'amarillo': 0, 'rojo': 0, 'sin_evaluar': 0})
        self.assertTrue(all(r['promedio'] is None for r in consultas.promedio_por_competencia()))