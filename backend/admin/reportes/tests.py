from unittest import mock
from datetime import date, timedelta
from django.db import connection
from io import StringIO
from io import BytesIO

import openpyxl
from pypdf import PdfReader

from django.core.management import call_command
from django.core.management.base import CommandError
from django.test import TestCase

from rest_framework import status
from rest_framework.test import APITestCase

from instituciones.models import AsignacionDocente, Colegio
from usuarios.models import Rol, Usuario
from . import consultas
from . import exportar

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





class ReporteAulasAPITests(APITestCase):
    """T-268: endpoint de reporte por aula dentro del colegio."""

    def setUp(self):
        ConsultasAgregacionTests.setUp(self)  # mismos datos de T-264
        with connection.cursor() as c:
            # Un aula del colegio A sin alumnos
            docente_a = Usuario.objects.get(username='doc_a')
            c.execute('INSERT INTO aula VALUES (103, %s, %s)', ['Aula C vacía', docente_a.id])
        rol_admin, _ = Rol.objects.get_or_create(nombre='ADMIN')
        self.admin = Usuario.objects.create_user(
            username='admin', email='admin@test.pe', password='clave-segura-1', rol=rol_admin)
        self.client.force_authenticate(self.admin)

    def url(self, colegio_id):
        return f'/api/reportes/colegios/{colegio_id}/aulas/'

    def test_aulas_del_colegio_a(self):
        resp = self.client.get(self.url(self.colegio_a.id))
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertEqual(resp.data['colegio']['nombre'], 'IE Ccollpa')
        aulas = {a['nombre']: a for a in resp.data['aulas']}
        self.assertEqual(set(aulas), {'Aula A', 'Aula C vacía'})
        a = aulas['Aula A']
        self.assertEqual((a['alumnos'], a['evaluaciones'], a['docente']), (2, 4, 'doc_a'))
        self.assertEqual(a['promedios'], {'lectura': 70.0, 'escritura': 70.0, 'oralidad': 40.0})
        self.assertEqual(a['semaforo'], {'verde': 1, 'amarillo': 1, 'rojo': 0, 'sin_evaluar': 0})

    def test_aula_sin_alumnos_sale_en_cero(self):
        aulas = {a['nombre']: a for a in self.client.get(self.url(self.colegio_a.id)).data['aulas']}
        vacia = aulas['Aula C vacía']
        self.assertEqual((vacia['alumnos'], vacia['promedio_general']), (0, None))

    def test_solo_salen_las_aulas_de_ese_colegio(self):
        resp = self.client.get(self.url(self.colegio_b.id))
        self.assertEqual([a['nombre'] for a in resp.data['aulas']], ['Aula B'])

    def test_colegio_que_no_existe_devuelve_404(self):
        self.assertEqual(self.client.get(self.url(99999)).status_code, status.HTTP_404_NOT_FOUND)

    def test_docente_no_puede_ver_el_reporte(self):
        self.client.force_authenticate(Usuario.objects.get(username='doc_a'))
        self.assertEqual(self.client.get(self.url(self.colegio_a.id)).status_code, status.HTTP_403_FORBIDDEN)

    @mock.patch('reportes.consultas.tablas_disponibles', return_value=False)
    def test_sin_tablas_devuelve_lista_vacia(self, _tablas):
        self.assertEqual(self.client.get(self.url(self.colegio_a.id)).data['aulas'], [])


class CalculosAgregacionTests(TestCase):
    """T-271: casos límite de los cálculos que usan los reportes (mismos datos de T-264)."""

    def setUp(self):
        ConsultasAgregacionTests.setUp(self)

    def ejecutar(self, sql, parametros=None):
        with connection.cursor() as c:
            c.execute(sql, parametros or [])

    def por_colegio(self):
        return {r['nombre']: r for r in consultas.rendimiento_por_colegio()}

    def docente_a_id(self):
        return AsignacionDocente.objects.get(colegio=self.colegio_a).docente_id

    def test_el_color_usa_el_promedio_sin_redondear(self):
        # 49 y 50 → promedio 49.5: es rojo, aunque redondeado parecería 50
        self.ejecutar("INSERT INTO estudiante (id, nombres, apellidos, aula_id, grado_id, activo) "
                      "VALUES (6, 'Pedro', 'R', 101, 2, TRUE)")
        self.ejecutar("INSERT INTO evaluacion (estudiante_id, competencia, puntaje) "
                      "VALUES (6, 'lectura', 49), (6, 'escritura', 50)")
        self.assertEqual(consultas.alumnos_por_semaforo(aula_id=101)['rojo'], 1)
        self.assertEqual(self.por_colegio()['IE Ccollpa']['semaforo']['rojo'], 1)

    def test_el_semaforo_suma_lo_mismo_que_los_alumnos(self):
        for fila in self.por_colegio().values():
            with self.subTest(colegio=fila['nombre']):
                self.assertEqual(sum(fila['semaforo'].values()), fila['alumnos'])
        for aula in consultas.rendimiento_por_aula(self.colegio_a.id):
            self.assertEqual(sum(aula['semaforo'].values()), aula['alumnos'])

    def test_alumno_sin_evaluaciones_cuenta_como_alumno_pero_no_en_el_promedio(self):
        b = self.por_colegio()['IE Huaccana']          # Rosa (30) y Juan (sin notas)
        self.assertEqual(b['alumnos'], 2)
        self.assertEqual(b['promedio_general'], 30.0)  # Juan no baja el promedio a 15

    def test_el_promedio_general_pesa_cada_evaluacion(self):
        # Colegio A: (80 + 70 + 60 + 40) / 4 = 62.5, no (70 + 70 + 40) / 3 = 60
        self.assertEqual(self.por_colegio()['IE Ccollpa']['promedio_general'], 62.5)

    def test_alumnos_de_un_docente_sin_colegio_no_salen_en_ningun_colegio(self):
        # Aula de un docente que no está asignado a ningún colegio (como los datos de prueba)
        self.ejecutar('INSERT INTO aula VALUES (103, %s, %s)', ['Aula suelta', 99999])
        self.ejecutar("INSERT INTO estudiante (id, nombres, apellidos, aula_id, grado_id, activo) "
                      "VALUES (7, 'Sara', 'L', 103, 3, TRUE)")
        self.ejecutar("INSERT INTO evaluacion (estudiante_id, competencia, puntaje) VALUES (7, 'lectura', 90)")
        # En el total general sí cuenta...
        self.assertEqual(consultas.alumnos_por_semaforo()['verde'], 2)
        # ...pero no aparece en ningún colegio ni en sus aulas
        self.assertEqual(sum(f['alumnos'] for f in self.por_colegio().values()), 4)
        nombres = [a['nombre'] for a in consultas.rendimiento_por_aula(self.colegio_a.id)]
        self.assertNotIn('Aula suelta', nombres)

    def test_filtro_por_grado_en_el_semaforo(self):
        self.assertEqual(consultas.alumnos_por_semaforo(grado=4),
                         {'verde': 0, 'amarillo': 0, 'rojo': 1, 'sin_evaluar': 1})

    def test_rendimiento_por_aula_con_su_docente(self):
        aula = consultas.rendimiento_por_aula(self.colegio_a.id)[0]
        self.assertEqual(aula['nombre'], 'Aula A')
        self.assertEqual(aula['docente'], 'doc_a')
        self.assertEqual((aula['alumnos'], aula['evaluaciones']), (2, 4))
        self.assertEqual(aula['promedios'], {'lectura': 70.0, 'escritura': 70.0, 'oralidad': 40.0})

    def test_aula_sin_alumnos_aparece_en_cero(self):
        self.ejecutar('INSERT INTO aula VALUES (104, %s, %s)', ['Aula vacía', self.docente_a_id()])
        vacia = next(a for a in consultas.rendimiento_por_aula(self.colegio_a.id) if a['nombre'] == 'Aula vacía')
        self.assertEqual((vacia['alumnos'], vacia['evaluaciones'], vacia['promedio_general']), (0, 0, None))
        self.assertEqual(vacia['semaforo'], {'verde': 0, 'amarillo': 0, 'rojo': 0, 'sin_evaluar': 0})





# =====================================================================
# US-48 · Mapa de calor: desempeño por zona y competencia (T-353)
# =====================================================================

class DesempenoPorZonaTests(TestCase):
    """T-349: consulta de desempeño por zona (región o provincia) y competencia."""

    def setUp(self):
        ConsultasAgregacionTests.setUp(self)   # mismos datos de T-264
        self.colegio_a.provincia = 'Quispicanchi'
        self.colegio_a.save()

    def por_zona(self, **filtros):
        return {z['zona']: z for z in consultas.desempeno_por_zona(**filtros)}

    def test_por_region_calcula_los_promedios_de_cada_competencia(self):
        cusco = self.por_zona()['Cusco']
        self.assertEqual((cusco['colegios'], cusco['alumnos'], cusco['evaluaciones']), (1, 2, 4))
        self.assertEqual(cusco['promedios'], {'lectura': 70.0, 'escritura': 70.0, 'oralidad': 40.0})
        self.assertEqual(cusco['promedio_general'], 62.5)     # (80 + 70 + 60 + 40) / 4

    def test_competencia_sin_notas_queda_en_none(self):
        apurimac = self.por_zona()['Apurímac']
        self.assertEqual(apurimac['promedios'], {'lectura': 30.0, 'escritura': None, 'oralidad': None})
        self.assertEqual(apurimac['alumnos'], 2)               # Rosa y Juan (Juan sin notas)

    def test_las_zonas_salen_ordenadas_por_nombre(self):
        self.assertEqual([z['zona'] for z in consultas.desempeno_por_zona()], ['Apurímac', 'Cusco'])

    def test_por_provincia_y_sin_provincia(self):
        zonas = self.por_zona(agrupar='provincia')
        self.assertEqual(set(zonas), {'Cusco / Quispicanchi', 'Apurímac / Sin provincia'})
        self.assertEqual(zonas['Cusco / Quispicanchi']['promedios']['lectura'], 70.0)

    def test_dos_colegios_de_la_misma_zona_se_juntan(self):
        Colegio.objects.create(codigo_modular='5555555', nombre='IE Ccatca', region='Cusco')
        cusco = self.por_zona()['Cusco']
        self.assertEqual(cusco['colegios'], 2)
        self.assertEqual(cusco['promedios']['lectura'], 70.0)  # el colegio nuevo no tiene alumnos

    def test_zona_con_colegios_pero_sin_evaluaciones_sale_vacia(self):
        Colegio.objects.create(codigo_modular='6666666', nombre='IE Puno', region='Puno')
        puno = self.por_zona()['Puno']
        self.assertEqual((puno['colegios'], puno['alumnos'], puno['promedio_general']), (1, 0, None))

    def test_colegio_inactivo_no_cuenta(self):
        Colegio.objects.create(codigo_modular='7777777', nombre='IE Cerrada', region='Tacna', activo=False)
        self.assertNotIn('Tacna', self.por_zona())

    def test_filtro_por_grado(self):
        zonas = self.por_zona(grado=4)          # solo Rosa y Juan (Apurímac)
        self.assertEqual(zonas['Apurímac']['promedios']['lectura'], 30.0)
        self.assertEqual(zonas['Cusco']['alumnos'], 0)
        self.assertIsNone(zonas['Cusco']['promedio_general'])

    def test_agrupar_invalido_lanza_error(self):
        with self.assertRaises(ValueError):
            consultas.desempeno_por_zona(agrupar='distrito')

    @mock.patch('reportes.consultas.tablas_disponibles', return_value=False)
    def test_sin_tablas_de_spring_devuelve_las_zonas_vacias(self, _tablas):
        zonas = self.por_zona()
        self.assertEqual(set(zonas), {'Cusco', 'Apurímac'})
        self.assertTrue(all(z['promedio_general'] is None for z in zonas.values()))


class ReporteMapaCalorAPITests(APITestCase):
    """T-350: endpoint de datos del mapa de calor (solo administrador)."""
    URL = '/api/reportes/mapa-calor/'

    def setUp(self):
        ConsultasAgregacionTests.setUp(self)   # mismos datos de T-264
        rol_admin, _ = Rol.objects.get_or_create(nombre='ADMIN')
        self.admin = Usuario.objects.create_user(
            username='admin', email='admin@test.pe', password='clave-segura-1', rol=rol_admin)
        self.client.force_authenticate(self.admin)

    def test_por_defecto_agrupa_por_region(self):
        resp = self.client.get(self.URL)
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertEqual((resp.data['agrupar'], resp.data['grado']), ('region', None))
        self.assertEqual(resp.data['competencias'], ['lectura', 'escritura', 'oralidad'])
        self.assertEqual([z['zona'] for z in resp.data['zonas']], ['Apurímac', 'Cusco'])

    def test_agrupar_por_provincia(self):
        resp = self.client.get(self.URL, {'agrupar': 'provincia'})
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertIn('Cusco / Sin provincia', [z['zona'] for z in resp.data['zonas']])

    def test_filtro_por_grado(self):
        resp = self.client.get(self.URL, {'grado': 2})
        cusco = next(z for z in resp.data['zonas'] if z['zona'] == 'Cusco')
        self.assertEqual((resp.data['grado'], cusco['promedios']['lectura']), (2, 70.0))

    def test_agrupar_invalido_devuelve_400(self):
        resp = self.client.get(self.URL, {'agrupar': 'distrito'})
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(str(resp.data['agrupar']), 'agrupar debe ser region o provincia.')

    def test_grado_fuera_de_rango_devuelve_400(self):
        resp = self.client.get(self.URL, {'grado': 9})
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(str(resp.data['grado']), 'El grado debe estar entre 1 y 6.')

    def test_grado_que_no_es_numero_devuelve_400(self):
        resp = self.client.get(self.URL, {'grado': 'tres'})
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)

    def test_sin_sesion_devuelve_401(self):
        self.client.force_authenticate(None)
        self.assertEqual(self.client.get(self.URL).status_code, status.HTTP_401_UNAUTHORIZED)

    def test_docente_no_puede_ver_el_mapa(self):
        self.client.force_authenticate(Usuario.objects.get(username='doc_a'))
        self.assertEqual(self.client.get(self.URL).status_code, status.HTTP_403_FORBIDDEN)




# =====================================================================
# US-49 · Filtros de reportes: periodo, colegio y grado (T-357)
# =====================================================================
FECHAS = [
    # (alumno, competencia, fecha de la evaluación)
    (1, 'lectura', '2026-09-15 10:00'),     # Ana     80
    (3, 'lectura', '2026-09-20 12:00'),     # Rosa    30
    (5, 'lectura', '2026-10-01 08:00'),     # Inés   100 (inactiva)
    (1, 'escritura', '2026-10-20 09:00'),   # Ana     70
    (2, 'lectura', '2026-10-31 23:59'),     # Luis    60 → último minuto de octubre
    (2, 'oralidad', '2026-11-02 08:00'),    # Luis    40
]


def poner_fechas():
    """Cada evaluación de los datos de T-264 recibe una fecha fija."""
    with connection.cursor() as c:
        for estudiante_id, competencia, fecha in FECHAS:
            c.execute('UPDATE evaluacion SET evaluado_en = %s WHERE estudiante_id = %s AND competencia = %s',
                      [fecha, estudiante_id, competencia])


class FiltrosConsultasTests(TestCase):
    """T-354: las consultas respetan el periodo y el grado."""
    OCTUBRE = {'desde': date(2026, 10, 1), 'hasta': date(2026, 10, 31)}

    def setUp(self):
        ConsultasAgregacionTests.setUp(self)   # mismos datos de T-264
        poner_fechas()

    def por_competencia(self, **filtros):
        return {r['competencia']: r['promedio'] for r in consultas.promedio_por_competencia(**filtros)}

    def test_periodo_de_octubre(self):
        self.assertEqual(self.por_competencia(**self.OCTUBRE),
                         {'lectura': 60.0, 'escritura': 70.0, 'oralidad': None})

    def test_hasta_incluye_todo_el_ultimo_dia(self):
        self.assertEqual(self.por_competencia(hasta=date(2026, 10, 31))['lectura'], 56.7)   # (80 + 30 + 60) / 3
        self.assertEqual(self.por_competencia(hasta=date(2026, 10, 30))['lectura'], 55.0)   # sin el 60 de Luis

    def test_solo_desde(self):
        self.assertEqual(self.por_competencia(desde=date(2026, 11, 1)),
                         {'lectura': None, 'escritura': None, 'oralidad': 40.0})

    def test_sin_filtros_da_lo_mismo_que_antes(self):
        self.assertEqual(self.por_competencia(), {'lectura': 56.7, 'escritura': 70.0, 'oralidad': 40.0})

    def test_semaforo_del_periodo_cuenta_a_los_que_no_tienen_notas(self):
        self.assertEqual(consultas.alumnos_por_semaforo(**self.OCTUBRE),
                         {'verde': 0, 'amarillo': 2, 'rojo': 0, 'sin_evaluar': 2})   # Ana 70, Luis 60

    def test_rendimiento_por_colegio_con_grado(self):
        filas = {f['nombre']: f for f in consultas.rendimiento_por_colegio(grado=4)}
        self.assertEqual(filas['IE Ccollpa']['alumnos'], 0)                  # sus alumnos son de 2.°
        self.assertEqual((filas['IE Huaccana']['alumnos'], filas['IE Huaccana']['promedios']['lectura']), (2, 30.0))

    def test_rendimiento_por_aula_con_periodo(self):
        aula = consultas.rendimiento_por_aula(self.colegio_a.id, desde=date(2026, 9, 1), hasta=date(2026, 9, 30))[0]
        self.assertEqual((aula['evaluaciones'], aula['promedios']['lectura']), (1, 80.0))   # solo Ana
        self.assertEqual(aula['semaforo'], {'verde': 1, 'amarillo': 0, 'rojo': 0, 'sin_evaluar': 1})

    def test_mapa_de_calor_con_periodo(self):
        zonas = {z['zona']: z for z in consultas.desempeno_por_zona(desde=date(2026, 9, 1), hasta=date(2026, 9, 30))}
        self.assertEqual(zonas['Cusco']['promedios']['lectura'], 80.0)
        self.assertEqual(zonas['Apurímac']['promedios']['lectura'], 30.0)


class FiltrosReportesAPITests(APITestCase):
    """T-354 y T-355: parámetros ?grado=&desde=&hasta= y validación del rango de fechas."""
    URL = '/api/reportes/colegios/'

    def setUp(self):
        ConsultasAgregacionTests.setUp(self)
        poner_fechas()
        rol_admin, _ = Rol.objects.get_or_create(nombre='ADMIN')
        self.admin = Usuario.objects.create_user(
            username='admin', email='admin@test.pe', password='clave-segura-1', rol=rol_admin)
        self.client.force_authenticate(self.admin)

    # --- T-354: los filtros llegan a las consultas ---
    def test_reporte_de_colegios_con_grado_y_periodo(self):
        resp = self.client.get(self.URL, {'grado': 2, 'desde': '2026-10-01', 'hasta': '2026-10-31'})
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        datos = resp.json()
        self.assertEqual(datos['filtros'], {'colegio': None, 'grado': 2, 'desde': '2026-10-01', 'hasta': '2026-10-31'})
        self.assertEqual(datos['resumen']['promedios'], {'lectura': 60.0, 'escritura': 70.0, 'oralidad': None})
        self.assertEqual(datos['resumen']['alumnos'], 2)                     # solo Ana y Luis (2.°)

    def test_reporte_de_aulas_con_periodo(self):
        resp = self.client.get(f'/api/reportes/colegios/{self.colegio_a.id}/aulas/',
                               {'desde': '2026-09-01', 'hasta': '2026-09-30'})
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertEqual(resp.json()['aulas'][0]['promedios']['lectura'], 80.0)

    def test_mapa_de_calor_con_periodo(self):
        resp = self.client.get('/api/reportes/mapa-calor/', {'desde': '2026-09-01', 'hasta': '2026-09-30'})
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertEqual((resp.json()['desde'], resp.json()['hasta']), ('2026-09-01', '2026-09-30'))

    def test_un_solo_dia_es_valido(self):
        resp = self.client.get(self.URL, {'desde': '2026-10-31', 'hasta': '2026-10-31'})
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertEqual(resp.json()['resumen']['promedios']['lectura'], 60.0)   # la de las 23:59

    # --- T-355: validación del rango ---
    def test_formato_de_fecha_invalido_devuelve_400(self):
        resp = self.client.get(self.URL, {'desde': '31/10/2026'})
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(str(resp.data['desde']), 'La fecha desde debe tener el formato AAAA-MM-DD.')

    def test_desde_despues_de_hasta_devuelve_400(self):
        resp = self.client.get(self.URL, {'desde': '2026-11-30', 'hasta': '2026-09-01'})
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(str(resp.data['hasta']), 'La fecha hasta no puede ser anterior a la fecha desde.')

    def test_fecha_futura_devuelve_400(self):
        manana = (date.today() + timedelta(days=1)).isoformat()
        resp = self.client.get(self.URL, {'desde': manana})
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(str(resp.data['desde']), 'La fecha desde no puede ser posterior a hoy.')

    def test_las_dos_fechas_futuras_muestran_los_dos_errores(self):
        manana = (date.today() + timedelta(days=1)).isoformat()
        resp = self.client.get(self.URL, {'desde': manana, 'hasta': manana})
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(set(resp.data), {'desde', 'hasta'})

    def test_grado_invalido_en_el_reporte_de_colegios(self):
        resp = self.client.get(self.URL, {'grado': 7})
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(str(resp.data['grado']), 'El grado debe estar entre 1 y 6.')




# =====================================================================
# US-50 · Exportar el reporte a Excel y PDF (T-363)
# =====================================================================

class DescribirFiltrosTests(TestCase):
    """T-360: texto de los filtros del encabezado."""

    def test_sin_filtros(self):
        self.assertEqual(exportar.describir_filtros(), 'Todos los colegios · Todos los grados · Todo el periodo')

    def test_con_todos_los_filtros(self):
        texto = exportar.describir_filtros(
            {'colegio': 'IE Ccollpa', 'grado': 2, 'desde': date(2026, 9, 1), 'hasta': date(2026, 11, 21)})
        self.assertEqual(texto, 'IE Ccollpa · 2.° grado · del 01/09/2026 al 21/11/2026')

    def test_solo_una_fecha(self):
        self.assertTrue(exportar.describir_filtros({'desde': date(2026, 9, 1)}).endswith('desde el 01/09/2026'))
        self.assertTrue(exportar.describir_filtros({'hasta': date(2026, 10, 31)}).endswith('hasta el 31/10/2026'))


class ExportarReporteAPITests(APITestCase):
    """T-358 a T-361: los archivos se descargan, tienen el encabezado y respetan los filtros."""
    EXCEL = '/api/reportes/colegios/excel/'
    PDF = '/api/reportes/colegios/pdf/'

    def setUp(self):
        ConsultasAgregacionTests.setUp(self)   # mismos datos de T-264 (IE Ccollpa y IE Huaccana)
        rol_admin, _ = Rol.objects.get_or_create(nombre='ADMIN')
        self.admin = Usuario.objects.create_user(
            username='admin', email='admin@test.pe', password='clave-segura-1', rol=rol_admin)
        self.client.force_authenticate(self.admin)

    def hoja(self, resp):
        """Abre el Excel de la respuesta y devuelve sus filas como listas."""
        libro = openpyxl.load_workbook(BytesIO(resp.content))
        return [list(fila) for fila in libro.active.iter_rows(values_only=True)]

    def texto_pdf(self, resp):
        return '\n'.join(pagina.extract_text() for pagina in PdfReader(BytesIO(resp.content)).pages)

    # --- T-358: Excel ---
    def test_descarga_el_excel_con_su_nombre(self):
        resp = self.client.get(self.EXCEL)
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertEqual(resp['Content-Type'],
                         'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet')
        self.assertIn(f'reporte-colegios-{date.today():%Y-%m-%d}.xlsx', resp['Content-Disposition'])

    def test_el_excel_tiene_encabezado_titulos_y_una_fila_por_colegio(self):
        filas = self.hoja(self.client.get(self.EXCEL))
        self.assertEqual(filas[0][0], 'Reporte de rendimiento por colegio · Yalliq')
        self.assertTrue(filas[1][0].startswith('Generado el '))
        self.assertEqual(filas[2][0], 'Filtros: Todos los colegios · Todos los grados · Todo el periodo')
        self.assertEqual(filas[4][:3], ['Código modular', 'Colegio', 'Región'])
        self.assertEqual(filas[5], ['1111111', 'IE Ccollpa', 'Cusco', 2, 4, 70, 70, 40, 62.5, 1, 1, 0, 0])
        self.assertEqual(len(filas), 7)                       # 4 de encabezado + títulos + 2 colegios

    def test_promedio_sin_datos_queda_vacio_en_el_excel(self):
        huaccana = self.hoja(self.client.get(self.EXCEL))[6]
        self.assertEqual(huaccana[1], 'IE Huaccana')
        self.assertEqual((huaccana[6], huaccana[7]), (None, None))   # escritura y oralidad sin notas

    # --- T-359: PDF ---
    def test_descarga_el_pdf(self):
        resp = self.client.get(self.PDF)
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertEqual(resp['Content-Type'], 'application/pdf')
        self.assertTrue(resp.content.startswith(b'%PDF'))
        self.assertIn(f'reporte-colegios-{date.today():%Y-%m-%d}.pdf', resp['Content-Disposition'])

    def test_el_pdf_tiene_el_encabezado_y_los_colegios(self):
        texto = self.texto_pdf(self.client.get(self.PDF))
        self.assertIn('Reporte de rendimiento por colegio', texto)
        self.assertIn('Generado el', texto)
        self.assertIn('Todos los colegios', texto)
        self.assertIn('IE Ccollpa', texto)
        self.assertIn('IE Huaccana', texto)

    # --- T-361: respeta los filtros ---
    def test_el_excel_respeta_colegio_y_grado(self):
        filas = self.hoja(self.client.get(self.EXCEL, {'colegio': self.colegio_a.id, 'grado': 2}))
        self.assertEqual(filas[2][0], 'Filtros: IE Ccollpa · 2.° grado · Todo el periodo')
        self.assertEqual([f[1] for f in filas[5:]], ['IE Ccollpa'])   # solo el colegio elegido

    def test_el_pdf_respeta_el_colegio(self):
        texto = self.texto_pdf(self.client.get(self.PDF, {'colegio': self.colegio_b.id}))
        self.assertIn('IE Huaccana', texto)
        self.assertNotIn('IE Ccollpa', texto)

    def test_el_periodo_aparece_en_el_encabezado(self):
        filas = self.hoja(self.client.get(self.EXCEL, {'desde': '2026-09-01', 'hasta': '2026-10-31'}))
        self.assertTrue(filas[2][0].endswith('del 01/09/2026 al 31/10/2026'))

    def test_fechas_al_reves_devuelven_400(self):
        resp = self.client.get(self.PDF, {'desde': '2026-11-30', 'hasta': '2026-09-01'})
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)

    def test_colegio_que_no_existe_devuelve_404(self):
        self.assertEqual(self.client.get(self.EXCEL, {'colegio': 9999}).status_code, status.HTTP_404_NOT_FOUND)

    # --- Permisos ---
    def test_docente_no_puede_exportar(self):
        self.client.force_authenticate(Usuario.objects.get(username='doc_a'))
        self.assertEqual(self.client.get(self.EXCEL).status_code, status.HTTP_403_FORBIDDEN)
        self.assertEqual(self.client.get(self.PDF).status_code, status.HTTP_403_FORBIDDEN)