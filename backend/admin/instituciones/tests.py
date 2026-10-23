from django.core.exceptions import ValidationError
from django.test import TestCase
from rest_framework import status
from rest_framework.test import APITestCase

from usuarios.models import Rol, Usuario
from .models import AsignacionDocente, Colegio


class ColegioModelTests(TestCase):

    def test_crear_colegio_valido(self):
        colegio = Colegio(codigo_modular='0123456', nombre='IE San Martín', region='Lima', distrito='Comas')
        colegio.full_clean()
        colegio.save()

        self.assertEqual(Colegio.objects.count(), 1)
        self.assertTrue(colegio.activo)
        self.assertEqual(str(colegio), 'IE San Martín (0123456)')

    def test_codigo_modular_de_6_digitos_falla(self):
        colegio = Colegio(codigo_modular='012345', nombre='IE Prueba', region='Lima')

        with self.assertRaises(ValidationError) as ctx:
            colegio.full_clean()

        self.assertIn('codigo_modular', ctx.exception.message_dict)
        self.assertIn('El código modular debe tener 7 dígitos.', ctx.exception.message_dict['codigo_modular'])


class ColegioAPITests(APITestCase):
    URL = '/api/colegios/'

    def setUp(self):
        rol_admin, _ = Rol.objects.get_or_create(nombre='ADMIN')
        rol_docente, _ = Rol.objects.get_or_create(nombre='DOCENTE')
        self.admin = Usuario.objects.create_user(
            username='admin', email='admin@test.pe', password='clave-segura-1', rol=rol_admin)
        self.docente1 = Usuario.objects.create_user(
            username='rquispe', email='rquispe@test.pe', password='clave-segura-1', rol=rol_docente)
        self.docente2 = Usuario.objects.create_user(
            username='jmamani', email='jmamani@test.pe', password='clave-segura-1', rol=rol_docente)

        self.ccollpa = Colegio.objects.create(
            codigo_modular='0512346', nombre='IE N° 50123 Ccollpa', region='Cusco', distrito='Ccatca')
        self.huaccana = Colegio.objects.create(
            codigo_modular='0623771', nombre='IE N° 54210 Huaccana', region='Apurímac', distrito='Huaccana')

        self.client.force_authenticate(self.admin)

    def url_docentes(self, colegio):
        return f'{self.URL}{colegio.id}/docentes/'

    def test_admin_lista_y_crea_colegios(self):
        resp = self.client.get(self.URL)
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertEqual(len(resp.data), 2)

        resp = self.client.post(self.URL, {
            'codigo_modular': '0734120', 'nombre': '  IE N° 70345 Chucuito ', 'region': 'Puno', 'distrito': 'Chucuito',
        }, format='json')
        self.assertEqual(resp.status_code, status.HTTP_201_CREATED)
        self.assertEqual(resp.data['nombre'], 'IE N° 70345 Chucuito')
        self.assertEqual(resp.data['total_docentes'], 0)

    def test_docente_puede_listar_colegios(self):
        # DOCENTE tiene el permiso colegios.ver
        self.client.force_authenticate(self.docente1)
        resp = self.client.get(self.URL)
        self.assertEqual(resp.status_code, status.HTTP_200_OK)

    def test_docente_no_puede_crear_colegio(self):
        self.client.force_authenticate(self.docente1)
        resp = self.client.post(self.URL, {
            'codigo_modular': '0734120', 'nombre': 'IE N° 70345 Chucuito', 'region': 'Puno',
        }, format='json')
        self.assertEqual(resp.status_code, status.HTTP_403_FORBIDDEN)
        self.assertFalse(Colegio.objects.filter(codigo_modular='0734120').exists())

    def test_docente_no_puede_asignar_docentes(self):
        self.client.force_authenticate(self.docente1)
        resp = self.client.post(self.url_docentes(self.ccollpa), {'docentes': [self.docente2.id]}, format='json')
        self.assertEqual(resp.status_code, status.HTTP_403_FORBIDDEN)
        self.assertFalse(AsignacionDocente.objects.exists())

    def test_codigo_modular_de_6_digitos_devuelve_400(self):
        resp = self.client.post(self.URL, {
            'codigo_modular': '073412', 'nombre': 'IE Prueba', 'region': 'Puno',
        }, format='json')
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('codigo_modular', resp.data)

    def test_busqueda_por_nombre(self):
        resp = self.client.get(self.URL, {'search': 'Ccollpa'})
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertEqual(len(resp.data), 1)
        self.assertEqual(resp.data[0]['codigo_modular'], '0512346')

    def test_asignar_docentes_actualiza_total(self):
        resp = self.client.post(self.url_docentes(self.ccollpa),
                                {'docentes': [self.docente1.id, self.docente2.id]}, format='json')
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertEqual(resp.data['total_docentes'], 2)
        self.assertEqual({d['username'] for d in resp.data['docentes']}, {'rquispe', 'jmamani'})

        # La asignación se reemplaza: queda solo docente2
        resp = self.client.post(self.url_docentes(self.ccollpa), {'docentes': [self.docente2.id]}, format='json')
        self.assertEqual(resp.data['total_docentes'], 1)
        self.assertFalse(AsignacionDocente.objects.filter(docente=self.docente1).exists())

    def test_docente_con_colegio_no_se_asigna_a_otro(self):
        AsignacionDocente.objects.create(colegio=self.ccollpa, docente=self.docente1)

        resp = self.client.post(self.url_docentes(self.huaccana), {'docentes': [self.docente1.id]}, format='json')
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('El docente rquispe ya pertenece al colegio IE N° 50123 Ccollpa.', resp.data['docentes'])

    def test_asignar_usuario_admin_devuelve_400(self):
        resp = self.client.post(self.url_docentes(self.ccollpa), {'docentes': [self.admin.id]}, format='json')
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('Solo se pueden asignar usuarios con rol DOCENTE.', resp.data['docentes'])

    def test_eliminar_colegio_devuelve_405(self):
        resp = self.client.delete(f'{self.URL}{self.ccollpa.id}/')
        self.assertEqual(resp.status_code, status.HTTP_405_METHOD_NOT_ALLOWED)
        self.assertTrue(Colegio.objects.filter(pk=self.ccollpa.pk).exists())
