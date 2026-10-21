from django.core.exceptions import ValidationError
from django.test import TestCase

from .models import Colegio


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
