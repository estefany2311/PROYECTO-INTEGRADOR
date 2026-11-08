from django.core.exceptions import ValidationError
from django.test import TestCase
from rest_framework import status
from rest_framework.test import APITestCase

from usuarios.models import Rol, Usuario
from django.db import IntegrityError
from .models import (ConfiguracionEvaluacion, ConfiguracionOrtografia, HistorialConfiguracion,
                PalabraPermitida, ReglaPuntuacion)


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


    
    # --- T-253: permisos ---
    def test_sin_token_devuelve_401(self):
        self.client.force_authenticate(None)
        self.assertEqual(self.client.get(self.URL).status_code, status.HTTP_401_UNAUTHORIZED)

    def test_docente_no_puede_usar_put(self):
        self.client.force_authenticate(self.docente)
        datos = {'umbral_precision': 70, 'tolerancia_pausas': '2.0', 'palabras_por_minuto': 60}
        resp = self.client.put(f'{self.URL}3/', datos, format='json')
        self.assertEqual(resp.status_code, status.HTTP_403_FORBIDDEN)

    def test_no_se_pueden_crear_ni_borrar_grados(self):
        crear = self.client.post(self.URL, {'grado': 7, 'umbral_precision': 90}, format='json')
        borrar = self.client.delete(f'{self.URL}3/')
                # 403: nadie tiene "configuracion.crear" · 405: el método no existe. Ambos bloquean.
        self.assertIn(crear.status_code, (status.HTTP_403_FORBIDDEN, status.HTTP_405_METHOD_NOT_ALLOWED))
        self.assertEqual(borrar.status_code, status.HTTP_405_METHOD_NOT_ALLOWED)
        self.assertEqual(ConfiguracionEvaluacion.objects.count(), 6)

    def test_grado_que_no_existe_devuelve_404(self):
        self.assertEqual(self.client.get(f'{self.URL}7/').status_code, status.HTTP_404_NOT_FOUND)

    # --- T-253: rangos ---
    def test_pausas_con_dos_decimales_devuelve_400(self):
        resp = self.patch(1, tolerancia_pausas='2.75')
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('un decimal', str(resp.data['tolerancia_pausas'][0]))

    def test_texto_en_vez_de_numero_devuelve_400(self):
        resp = self.patch(2, palabras_por_minuto='rapido')
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(str(resp.data['palabras_por_minuto'][0]), 'Ingresa un número válido.')

    def test_put_con_todos_los_campos_validos(self):
        datos = {'umbral_precision': 72, 'tolerancia_pausas': '1.5', 'palabras_por_minuto': 70}
        resp = self.client.put(f'{self.URL}4/', datos, format='json')
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertEqual(ConfiguracionEvaluacion.objects.get(grado=4).palabras_por_minuto, 70)

    # --- T-253: historial (T-252) ---
    def test_cada_cambio_queda_en_el_historial(self):
        self.patch(3, umbral_precision=72, palabras_por_minuto=62)
        cambios = HistorialConfiguracion.objects.filter(grado=3)
        self.assertEqual(cambios.count(), 2)
        precision = cambios.get(campo='umbral_precision')
        self.assertEqual((precision.valor_anterior, precision.valor_nuevo, precision.usuario),
                         ('70', '72', self.admin))

    def test_guardar_sin_cambios_no_crea_historial(self):
        self.patch(3, umbral_precision=70)  # 70 ya es el valor actual
        self.assertFalse(HistorialConfiguracion.objects.exists())

    def test_endpoint_historial_lista_y_filtra_por_grado(self):
        self.patch(2, umbral_precision=66)
        self.patch(5, palabras_por_minuto=88)
        todos = self.client.get(f'{self.URL}historial/')
        del_grado_5 = self.client.get(f'{self.URL}historial/', {'grado': 5})
        self.assertEqual(todos.status_code, status.HTTP_200_OK)
        self.assertEqual(len(todos.data), 2)
        self.assertEqual([c['grado'] for c in del_grado_5.data], [5])
        self.assertEqual(del_grado_5.data[0]['usuario'], 'admin')

    def test_docente_no_puede_ver_el_historial(self):
        self.client.force_authenticate(self.docente)
        resp = self.client.get(f'{self.URL}historial/')
        self.assertEqual(resp.status_code, status.HTTP_403_FORBIDDEN)




class ConfiguracionOrtografiaModelTests(TestCase):
    """T-254: modelos de la validación ortográfica."""

    def test_valores_por_defecto_del_modelo(self):
        config = ConfiguracionOrtografia(grado=3)  # sin guardar: solo revisa los valores del modelo
        self.assertEqual(config.sensibilidad, 'media')
        self.assertFalse(config.ignorar_tildes)

    def test_str_muestra_grado_sensibilidad_y_tildes(self):
        config = ConfiguracionOrtografia.objects.get(grado=1)
        self.assertEqual(str(config), '1.° grado · sensibilidad baja · sin marcar tildes')

    def test_sensibilidad_invalida_no_es_valida(self):
        config = ConfiguracionOrtografia(grado=2, sensibilidad='extrema')
        with self.assertRaises(ValidationError):
            config.full_clean()

    def test_grado_fuera_de_rango_no_es_valido(self):
        with self.assertRaises(ValidationError):
            ConfiguracionOrtografia(grado=7).full_clean()

    def test_palabra_se_guarda_sin_espacios(self):
        palabra = PalabraPermitida.objects.create(palabra='  kiwicha  ')
        self.assertEqual(palabra.palabra, 'kiwicha')

    def test_no_se_repite_la_misma_palabra_aunque_cambien_mayusculas(self):
        PalabraPermitida.objects.create(palabra='Quinua')
        with self.assertRaises(IntegrityError):
            PalabraPermitida.objects.create(palabra='quinua')

    
    # --- T-255: datos iniciales ---
    def test_hay_valores_ortograficos_para_los_6_grados(self):
        valores = list(ConfiguracionOrtografia.objects.values_list('grado', 'sensibilidad', 'ignorar_tildes'))
        self.assertEqual(valores, [
            (1, 'baja', True), (2, 'baja', True),
            (3, 'media', False), (4, 'media', False),
            (5, 'alta', False), (6, 'alta', False),
        ])

    def test_hay_palabras_permitidas_iniciales(self):
        self.assertTrue(PalabraPermitida.objects.filter(palabra='chacra').exists())
        self.assertEqual(PalabraPermitida.objects.count(), 6)




class ConfiguracionOrtografiaAPITests(APITestCase):
    """T-258 (US-52): endpoint de ortografía y palabras permitidas."""
    URL = '/api/configuracion/ortografia/'
    URL_PALABRAS = '/api/configuracion/palabras/'

    def setUp(self):
        rol_admin, _ = Rol.objects.get_or_create(nombre='ADMIN')
        rol_docente, _ = Rol.objects.get_or_create(nombre='DOCENTE')
        self.admin = Usuario.objects.create_user(
            username='admin', email='admin@test.pe', password='clave-segura-1', rol=rol_admin)
        self.docente = Usuario.objects.create_user(
            username='rquispe', email='rquispe@test.pe', password='clave-segura-1', rol=rol_docente)
        self.client.force_authenticate(self.admin)

    # --- Ortografía por grado ---
    def test_admin_lista_los_6_grados(self):
        resp = self.client.get(self.URL)
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertEqual([c['grado'] for c in resp.data], [1, 2, 3, 4, 5, 6])

    def test_admin_cambia_sensibilidad_y_tildes(self):
        resp = self.client.patch(f'{self.URL}3/', {'sensibilidad': 'alta', 'ignorar_tildes': True}, format='json')
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        config = ConfiguracionOrtografia.objects.get(grado=3)
        self.assertEqual((config.sensibilidad, config.ignorar_tildes), ('alta', True))
        self.assertEqual(config.actualizado_por, self.admin)

    def test_sensibilidad_invalida_devuelve_400_con_mensaje(self):
        resp = self.client.patch(f'{self.URL}3/', {'sensibilidad': 'extrema'}, format='json')
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(str(resp.data['sensibilidad'][0]), 'La sensibilidad debe ser baja, media o alta.')

    def test_no_se_pueden_crear_ni_borrar_grados(self):
        crear = self.client.post(self.URL, {'grado': 7, 'sensibilidad': 'alta'}, format='json')
        borrar = self.client.delete(f'{self.URL}3/')
        self.assertIn(crear.status_code, (status.HTTP_403_FORBIDDEN, status.HTTP_405_METHOD_NOT_ALLOWED))
        self.assertEqual(borrar.status_code, status.HTTP_405_METHOD_NOT_ALLOWED)
        self.assertEqual(ConfiguracionOrtografia.objects.count(), 6)

    def test_grado_que_no_existe_devuelve_404(self):
        self.assertEqual(self.client.get(f'{self.URL}7/').status_code, status.HTTP_404_NOT_FOUND)

    # --- Palabras permitidas ---
    def test_admin_lista_las_palabras_iniciales(self):
        resp = self.client.get(self.URL_PALABRAS)
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertIn('chacra', [p['palabra'] for p in resp.data])

    def test_admin_agrega_una_palabra(self):
        resp = self.client.post(self.URL_PALABRAS, {'palabra': 'quinua'}, format='json')
        self.assertEqual(resp.status_code, status.HTTP_201_CREATED)
        palabra = PalabraPermitida.objects.get(palabra='quinua')
        self.assertEqual(palabra.creado_por, self.admin)

    def test_espacios_de_mas_se_limpian(self):
        resp = self.client.post(self.URL_PALABRAS, {'palabra': '  Machu   Picchu  '}, format='json')
        self.assertEqual(resp.status_code, status.HTTP_201_CREATED)
        self.assertEqual(resp.data['palabra'], 'Machu Picchu')

    def test_palabra_repetida_devuelve_400_aunque_cambien_mayusculas(self):
        resp = self.client.post(self.URL_PALABRAS, {'palabra': 'CHACRA'}, format='json')
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(str(resp.data['palabra'][0]), 'Esa palabra ya está en la lista.')

    def test_palabra_muy_corta_devuelve_400(self):
        resp = self.client.post(self.URL_PALABRAS, {'palabra': 'a'}, format='json')
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('al menos 2 letras', str(resp.data['palabra'][0]))

    def test_palabra_con_numeros_devuelve_400(self):
        resp = self.client.post(self.URL_PALABRAS, {'palabra': 'chacra2'}, format='json')
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('Solo se permiten letras', str(resp.data['palabra'][0]))

    def test_admin_quita_una_palabra(self):
        palabra = PalabraPermitida.objects.get(palabra='wawa')
        resp = self.client.delete(f'{self.URL_PALABRAS}{palabra.id}/')
        self.assertEqual(resp.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(PalabraPermitida.objects.filter(id=palabra.id).exists())

    # --- Permisos ---
    def test_sin_token_devuelve_401(self):
        self.client.force_authenticate(None)
        self.assertEqual(self.client.get(self.URL).status_code, status.HTTP_401_UNAUTHORIZED)
        self.assertEqual(self.client.get(self.URL_PALABRAS).status_code, status.HTTP_401_UNAUTHORIZED)

    def test_docente_no_puede_ver_ni_cambiar_la_ortografia(self):
        self.client.force_authenticate(self.docente)
        self.assertEqual(self.client.get(self.URL).status_code, status.HTTP_403_FORBIDDEN)
        resp = self.client.patch(f'{self.URL}3/', {'sensibilidad': 'baja'}, format='json')
        self.assertEqual(resp.status_code, status.HTTP_403_FORBIDDEN)

    def test_docente_no_puede_agregar_ni_quitar_palabras(self):
        self.client.force_authenticate(self.docente)
        palabra = PalabraPermitida.objects.get(palabra='apu')
        agregar = self.client.post(self.URL_PALABRAS, {'palabra': 'quinua'}, format='json')
        quitar = self.client.delete(f'{self.URL_PALABRAS}{palabra.id}/')
        self.assertEqual(agregar.status_code, status.HTTP_403_FORBIDDEN)
        self.assertEqual(quitar.status_code, status.HTTP_403_FORBIDDEN)
        self.assertTrue(PalabraPermitida.objects.filter(id=palabra.id).exists())




class ReglaPuntuacionModelTests(TestCase):
    """T-259: modelo de reglas de puntuación y sus datos iniciales."""

    def test_hay_una_regla_por_tipo_de_reto(self):
        tipos = set(ReglaPuntuacion.objects.values_list('tipo_reto', flat=True))
        self.assertEqual(tipos, {'lectura', 'escritura', 'oralidad'})

    def test_valores_iniciales_de_lectura(self):
        regla = ReglaPuntuacion.objects.get(tipo_reto='lectura')
        self.assertEqual(
            (regla.puntos, regla.umbral_1_estrella, regla.umbral_2_estrellas, regla.umbral_3_estrellas),
            (10, 50, 70, 90),
        )

    def test_estrellas_segun_el_porcentaje(self):
        regla = ReglaPuntuacion.objects.get(tipo_reto='lectura')  # 50 / 70 / 90
        self.assertEqual([regla.estrellas_para(p) for p in (30, 50, 75, 90, 100)], [0, 1, 2, 3, 3])

    def test_puntos_fuera_de_rango_no_son_validos(self):
        regla = ReglaPuntuacion.objects.get(tipo_reto='oralidad')
        regla.puntos = 0
        with self.assertRaises(ValidationError):
            regla.full_clean()

    def test_str_muestra_tipo_puntos_y_umbrales(self):
        self.assertEqual(str(ReglaPuntuacion.objects.get(tipo_reto='escritura')),
                         'Escritura · 15 pts · ★50 ★★70 ★★★90')




class ReglaPuntuacionAPITests(APITestCase):
    """T-260: consultar y actualizar las reglas (solo administrador)."""
    URL = '/api/configuracion/reglas/'

    def setUp(self):
        rol_admin, _ = Rol.objects.get_or_create(nombre='ADMIN')
        rol_docente, _ = Rol.objects.get_or_create(nombre='DOCENTE')
        self.admin = Usuario.objects.create_user(
            username='admin', email='admin@test.pe', password='clave-segura-1', rol=rol_admin)
        self.docente = Usuario.objects.create_user(
            username='rquispe', email='rquispe@test.pe', password='clave-segura-1', rol=rol_docente)
        self.client.force_authenticate(self.admin)

    def test_admin_lista_las_3_reglas(self):
        resp = self.client.get(self.URL)
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertEqual({r['tipo_reto'] for r in resp.data}, {'lectura', 'escritura', 'oralidad'})

    def test_admin_actualiza_los_puntos_y_queda_registrado_quien(self):
        resp = self.client.patch(f'{self.URL}lectura/', {'puntos': 12}, format='json')
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        regla = ReglaPuntuacion.objects.get(tipo_reto='lectura')
        self.assertEqual((regla.puntos, regla.actualizado_por), (12, self.admin))

    def test_docente_no_puede_ver_ni_editar(self):
        self.client.force_authenticate(self.docente)
        self.assertEqual(self.client.get(self.URL).status_code, status.HTTP_403_FORBIDDEN)
        resp = self.client.patch(f'{self.URL}lectura/', {'puntos': 99}, format='json')
        self.assertEqual(resp.status_code, status.HTTP_403_FORBIDDEN)