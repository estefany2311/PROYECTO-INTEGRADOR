from django.core.exceptions import ValidationError
from django.test import TestCase

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