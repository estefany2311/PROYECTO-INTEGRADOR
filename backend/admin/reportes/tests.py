from unittest import mock

from django.db import connection
from io import StringIO

from django.core.management import call_command
from django.core.management.base import CommandError
from django.test import TestCase

from rest_framework import status
from rest_framework.test import APITestCase

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




    # --- T-266: rendimiento por colegio ---
    def por_colegio(self):
        return {r['nombre']: r for r in consultas.rendimiento_por_colegio()}

    def test_rendimiento_del_colegio_a(self):
        a = self.por_colegio()['IE Ccollpa']
        self.assertEqual((a['alumnos'], a['evaluaciones']), (2, 4))          # Ana y Luis (Inés inactiva)
        self.assertEqual(a['promedios'], {'lectura': 70.0, 'escritura': 70.0, 'oralidad': 40.0})
        self.assertEqual(a['promedio_general'], 62.5)                       # (80 + 70 + 60 + 40) / 4
        self.assertEqual(a['semaforo'], {'verde': 1, 'amarillo': 1, 'rojo': 0, 'sin_evaluar': 0})

    def test_rendimiento_del_colegio_b(self):
        b = self.por_colegio()['IE Huaccana']
        self.assertEqual((b['alumnos'], b['evaluaciones']), (2, 1))
        self.assertEqual(b['promedios'], {'lectura': 30.0, 'escritura': None, 'oralidad': None})
        self.assertEqual(b['semaforo'], {'verde': 0, 'amarillo': 0, 'rojo': 1, 'sin_evaluar': 1})

    def test_colegio_sin_alumnos_sale_en_cero(self):
        Colegio.objects.create(codigo_modular='3333333', nombre='IE Nueva', region='Puno')
        nueva = self.por_colegio()['IE Nueva']
        self.assertEqual((nueva['alumnos'], nueva['promedio_general']), (0, None))

    def test_colegio_inactivo_no_sale(self):
        Colegio.objects.create(codigo_modular='4444444', nombre='IE Cerrada', region='Puno', activo=False)
        self.assertNotIn('IE Cerrada', self.por_colegio())

    def test_filtrar_un_solo_colegio(self):
        filas = consultas.rendimiento_por_colegio(colegio_id=self.colegio_b.id)
        self.assertEqual([f['nombre'] for f in filas], ['IE Huaccana'])




class CargarDatosReportesTests(TestCase):
    """T-265: comando que carga y borra evaluaciones de prueba."""

    def setUp(self):
        with connection.cursor() as c:
            for sql in TABLAS:
                c.execute(sql)
            c.execute('INSERT INTO aula VALUES (201, %s, NULL)', ['Aula prueba'])
            c.execute("""INSERT INTO estudiante (id, nombres, apellidos, aula_id, grado_id, activo) VALUES
                (11, 'A', 'A', 201, 1, TRUE), (12, 'B', 'B', 201, 1, TRUE),
                (13, 'C', 'C', 201, 1, TRUE), (14, 'D', 'D', 201, 1, FALSE)""")
            # Una evaluación "real" de la app, que el comando nunca debe tocar
            c.execute("INSERT INTO evaluacion (estudiante_id, competencia, puntaje, evaluado_en) "
                      "VALUES (11, 'lectura', 99, '2026-10-01 10:00:00')")

    def contar(self, where='TRUE'):
        with connection.cursor() as c:
            c.execute(f'SELECT COUNT(*) FROM evaluacion WHERE {where}')
            return c.fetchone()[0]

    def cargar(self, *args):
        call_command('cargar_datos_reportes', *args, stdout=StringIO())

    def test_carga_9_evaluaciones_por_alumno_activo(self):
        self.cargar()
        self.assertEqual(self.contar("evaluado_en::time = '07:26:05'"), 3 * 9)  # 3 activos x 3 competencias x 3

    def test_el_semaforo_muestra_los_tres_colores(self):
        self.cargar()
        semaforo = consultas.alumnos_por_semaforo(aula_id=201)
        self.assertEqual((semaforo['verde'] + semaforo['amarillo'] + semaforo['rojo']), 3)
        self.assertGreaterEqual(semaforo['rojo'], 1)

    def test_correrlo_dos_veces_no_duplica(self):
        self.cargar()
        self.cargar()
        self.assertEqual(self.contar("evaluado_en::time = '07:26:05'"), 27)

    def test_borrar_quita_solo_los_datos_de_prueba(self):
        self.cargar()
        self.cargar('--borrar')
        self.assertEqual(self.contar(), 1)  # queda solo la evaluación "real"

    @mock.patch('reportes.management.commands.cargar_datos_reportes.tablas_disponibles', return_value=False)
    def test_sin_tablas_muestra_un_error_claro(self, _tablas):
        with self.assertRaises(CommandError):
            self.cargar()




class ReporteColegiosAPITests(APITestCase):
    """T-267: endpoint de reporte por colegio."""
    URL = '/api/reportes/colegios/'

    def setUp(self):
        ConsultasAgregacionTests.setUp(self)  # reutiliza los mismos datos de T-264
        rol_admin, _ = Rol.objects.get_or_create(nombre='ADMIN')
        self.admin = Usuario.objects.create_user(
            username='admin', email='admin@test.pe', password='clave-segura-1', rol=rol_admin)
        self.client.force_authenticate(self.admin)

    def test_admin_ve_resumen_y_colegios(self):
        resp = self.client.get(self.URL)
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertEqual(resp.data['resumen']['alumnos'], 4)  # sin la alumna inactiva
        self.assertEqual(resp.data['resumen']['promedios']['lectura'], 56.7)
        self.assertEqual({c['nombre'] for c in resp.data['colegios']}, {'IE Ccollpa', 'IE Huaccana'})

    def test_filtrar_por_un_colegio(self):
        resp = self.client.get(self.URL, {'colegio': self.colegio_b.id})
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertEqual([c['nombre'] for c in resp.data['colegios']], ['IE Huaccana'])
        self.assertEqual(resp.data['resumen']['semaforo'],
                         {'verde': 0, 'amarillo': 0, 'rojo': 1, 'sin_evaluar': 1})

    def test_colegio_con_letras_devuelve_400(self):
        self.assertEqual(self.client.get(self.URL, {'colegio': 'abc'}).status_code,
                         status.HTTP_400_BAD_REQUEST)

    def test_colegio_que_no_existe_devuelve_404(self):
        self.assertEqual(self.client.get(self.URL, {'colegio': 99999}).status_code,
                         status.HTTP_404_NOT_FOUND)

    def test_sin_token_devuelve_401(self):
        self.client.force_authenticate(None)
        self.assertEqual(self.client.get(self.URL).status_code, status.HTTP_401_UNAUTHORIZED)

    def test_docente_no_puede_ver_el_reporte(self):
        docente = Usuario.objects.get(username='doc_a')
        self.client.force_authenticate(docente)
        self.assertEqual(self.client.get(self.URL).status_code, status.HTTP_403_FORBIDDEN)