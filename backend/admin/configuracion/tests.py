from django.core.exceptions import ValidationError
from django.test import TestCase
from rest_framework import status
from rest_framework.test import APITestCase

from usuarios.models import Rol, Usuario

from .models import ConfiguracionEvaluacion


class ConfiguracionEvaluacionTests(TestCase):

    def test_hay_valores_por_defecto_para_los_6_grados(self):
        self.assertEqual(list(ConfiguracionEvaluacion.objects.values_list('grado', flat=True)), [1, 2, 3, 4, 5, 6])

    def test_primer_grado_es_menos_exigente_que_sexto(self):
        primero = ConfiguracionEvaluacion.objects.get(grado=1)
        sexto = ConfiguracionEvaluacion.objects.get(grado=6)
        self.assertLess(primero.umbral_precision, sexto.umbral_precision)
        self.assertLess(primero.palabras_por_minuto, sexto.palabras_por_minuto)

    def test_precision_fuera_de_rango_no_es_valida(self):
        config = ConfiguracionEvaluacion.objects.get(grado=3)
        config.umbral_precision = 120
        with self.assertRaises(ValidationError):
            config.full_clean()

    def test_str_muestra_grado_y_precision(self):
        self.assertEqual(str(ConfiguracionEvaluacion.objects.get(grado=1)), '1.° grado · precisión 60 %')



class ConfiguracionEvaluacionAPITests(APITestCase):
    """T-248: consultar y actualizar la configuración (solo administrador)."""
    URL = '/api/configuracion/evaluacion/'

    def setUp(self):
        rol_admin, _ = Rol.objects.get_or_create(nombre='ADMIN')
        rol_docente, _ = Rol.objects.get_or_create(nombre='DOCENTE')
        self.admin = Usuario.objects.create_user(
            username='admin', email='admin@test.pe', password='clave-segura-1', rol=rol_admin)
        self.docente = Usuario.objects.create_user(
            username='rquispe', email='rquispe@test.pe', password='clave-segura-1', rol=rol_docente)
        self.client.force_authenticate(self.admin)

    def test_admin_lista_los_6_grados(self):
        resp = self.client.get(self.URL)
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertEqual([c['grado'] for c in resp.data], [1, 2, 3, 4, 5, 6])

    def test_admin_actualiza_un_grado_y_queda_registrado_quien(self):
        resp = self.client.patch(f'{self.URL}3/', {'umbral_precision': 72}, format='json')
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        config = ConfiguracionEvaluacion.objects.get(grado=3)
        self.assertEqual(config.umbral_precision, 72)
        self.assertEqual(config.actualizado_por, self.admin)

    def test_docente_no_puede_ver_ni_editar(self):
        self.client.force_authenticate(self.docente)
        self.assertEqual(self.client.get(self.URL).status_code, status.HTTP_403_FORBIDDEN)
        self.assertEqual(self.client.patch(f'{self.URL}3/', {'umbral_precision': 10}, format='json').status_code,
                         status.HTTP_403_FORBIDDEN)


    
    # --- T-249: rangos y regla por grado ---
    def patch(self, numero, **datos):
        return self.client.patch(f'{self.URL}{numero}/', datos, format='json')

    def test_precision_mayor_a_100_devuelve_400_con_mensaje(self):
        resp = self.patch(6, umbral_precision=120)
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(str(resp.data['umbral_precision'][0]), 'Debe estar entre 0 y 100 %.')

    def test_pausas_fuera_de_rango_devuelve_400(self):
        resp = self.patch(1, tolerancia_pausas='12')
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('tolerancia_pausas', resp.data)

    def test_palabras_por_minuto_muy_bajas_devuelve_400(self):
        resp = self.patch(1, palabras_por_minuto=5)
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('10 y 200', str(resp.data['palabras_por_minuto'][0]))

    def test_grado_no_puede_ser_menos_exigente_que_el_anterior(self):
        resp = self.patch(3, umbral_precision=64)  # 2.° tiene 65
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('menor que en 2.°', str(resp.data['umbral_precision'][0]))

    def test_grado_no_puede_ser_mas_exigente_que_el_siguiente(self):
        resp = self.patch(3, palabras_por_minuto=80)  # 4.° tiene 75
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('mayor que en 4.°', str(resp.data['palabras_por_minuto'][0]))

    def test_valor_valido_dentro_del_rango_se_guarda(self):
        resp = self.patch(3, umbral_precision=68, tolerancia_pausas='2.5')
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        config = ConfiguracionEvaluacion.objects.get(grado=3)
        self.assertEqual((config.umbral_precision, str(config.tolerancia_pausas)), (68, '2.5'))

    def test_no_se_puede_cambiar_el_grado(self):
        self.patch(3, grado=9)
        self.assertTrue(ConfiguracionEvaluacion.objects.filter(grado=3).exists())
        self.assertFalse(ConfiguracionEvaluacion.objects.filter(grado=9).exists())