from rest_framework import status
from rest_framework.test import APITestCase
from unittest import mock

from django.db import connection
from django.db import IntegrityError
from django.test import TestCase

from usuarios.models import Rol, Usuario
from .models import Lectura, RecursoOficial
 
 
class LecturaModelTests(APITestCase):
 
    def test_str_devuelve_el_titulo(self):
        lectura = Lectura.objects.create(
            titulo='El cóndor y el zorro', texto='Había una vez un cóndor que vivía en la montaña.', tipo_reto='lectura')
        self.assertEqual(str(lectura), 'El cóndor y el zorro')
        self.assertTrue(lectura.activo)
 
 
class LecturaAPITests(APITestCase):
    URL = '/api/lecturas/'
    TEXTO = 'Había una vez un cóndor que vivía en lo alto de la montaña.'
 
    def setUp(self):
        rol_admin, _ = Rol.objects.get_or_create(nombre='ADMIN')
        rol_docente, _ = Rol.objects.get_or_create(nombre='DOCENTE')
        self.admin = Usuario.objects.create_user(
            username='admin', email='admin@test.pe', password='clave-segura-1', rol=rol_admin)
        self.docente = Usuario.objects.create_user(
            username='rquispe', email='rquispe@test.pe', password='clave-segura-1', rol=rol_docente)
 
        Lectura.objects.create(titulo='El cóndor y el zorro', texto=self.TEXTO, tipo_reto='lectura')
        Lectura.objects.create(titulo='Mi chacra en la mañana', texto=self.TEXTO, tipo_reto='escritura')
 
        self.client.force_authenticate(self.admin)
 
    def datos(self, **cambios):
        datos = {'titulo': 'La fiesta del Inti Raymi', 'texto': self.TEXTO, 'tipo_reto': 'oralidad'}
        datos.update(cambios)
        return datos
 
    # --- Lo normal funciona ---
    def test_admin_lista_lecturas(self):
        resp = self.client.get(self.URL)
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertEqual(len(resp.data), 2)
 
    def test_admin_crea_lectura_valida(self):
        resp = self.client.post(self.URL, self.datos(titulo='  La fiesta del Inti Raymi  '), format='json')
        self.assertEqual(resp.status_code, status.HTTP_201_CREATED)
        self.assertEqual(resp.data['titulo'], 'La fiesta del Inti Raymi')
        self.assertEqual(resp.data['creado_por'], 'admin')
        self.assertTrue(resp.data['activo'])
        self.assertEqual(Lectura.objects.get(id=resp.data['id']).creado_por, self.admin)
 
    def test_no_se_puede_enviar_creado_por_ni_activo(self):
        resp = self.client.post(self.URL, self.datos(activo=False, creado_por='otro'), format='json')
        self.assertEqual(resp.status_code, status.HTTP_201_CREATED)
        self.assertTrue(resp.data['activo'])
        self.assertEqual(resp.data['creado_por'], 'admin')
 
    # --- Datos inválidos se rechazan ---
    def test_titulo_vacio_devuelve_400(self):
        resp = self.client.post(self.URL, self.datos(titulo='   '), format='json')
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('titulo', resp.data)
 
    def test_texto_corto_devuelve_400(self):
        resp = self.client.post(self.URL, self.datos(texto='corto'), format='json')
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('El texto de la lectura debe tener al menos 20 caracteres.', resp.data['texto'])
 
    def test_tipo_reto_invalido_devuelve_400(self):
        resp = self.client.post(self.URL, self.datos(tipo_reto='canto'), format='json')
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('tipo_reto', resp.data)
 
    def test_campos_obligatorios_faltantes_devuelven_400(self):
        resp = self.client.post(self.URL, {}, format='json')
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        for campo in ('titulo', 'texto', 'tipo_reto'):
            self.assertIn(campo, resp.data)
 
    # --- Permisos ---
    def test_docente_puede_listar_lecturas(self):
        # DOCENTE tiene el permiso catalogo.ver
        self.client.force_authenticate(self.docente)
        resp = self.client.get(self.URL)
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
 
    def test_docente_no_puede_crear_lectura(self):
        # DOCENTE no tiene el permiso catalogo.crear
        self.client.force_authenticate(self.docente)
        resp = self.client.post(self.URL, self.datos(), format='json')
        self.assertEqual(resp.status_code, status.HTTP_403_FORBIDDEN)
        self.assertEqual(Lectura.objects.count(), 2)
 
    def test_sin_token_devuelve_401(self):
        self.client.force_authenticate(None)
        resp = self.client.get(self.URL)
        self.assertEqual(resp.status_code, status.HTTP_401_UNAUTHORIZED)
 
 
    # --- Búsqueda y filtros ---
    def test_buscar_por_titulo(self):
        resp = self.client.get(self.URL, {'search': 'cóndor'})
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertEqual([l['titulo'] for l in resp.data], ['El cóndor y el zorro'])
 
    def test_filtrar_por_tipo_de_reto(self):
        resp = self.client.get(self.URL, {'tipo_reto': 'escritura'})
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertEqual([l['titulo'] for l in resp.data], ['Mi chacra en la mañana'])

    
        # --- Grado y dificultad (T-223) ---
    def test_grado_fuera_de_rango_devuelve_400(self):
        for grado in (0, 7):
            resp = self.client.post(self.URL, self.datos(grado=grado), format='json')
            self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
            self.assertIn('El grado debe estar entre 1.º y 6.º.', resp.data['grado'])

    def test_dificultad_invalida_devuelve_400(self):
        resp = self.client.post(self.URL, self.datos(dificultad='facil'), format='json')
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('La dificultad debe ser básica, intermedia o avanzada.', resp.data['dificultad'])

    def test_crea_lectura_con_grado_y_dificultad(self):
        resp = self.client.post(self.URL, self.datos(grado=3, dificultad='avanzada'), format='json')
        self.assertEqual(resp.status_code, status.HTTP_201_CREATED)
        self.assertEqual(resp.data['grado'], 3)
        self.assertEqual(resp.data['dificultad'], 'avanzada')

    def test_filtro_de_grado_invalido_devuelve_400(self):
        for valor in ('abc', '0', '9'):
            resp = self.client.get(self.URL, {'grado': valor})
            self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)

    def test_filtro_de_dificultad_invalido_devuelve_400(self):
        resp = self.client.get(self.URL, {'dificultad': 'dificil'})
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)

    def test_combinar_grado_y_dificultad(self):
        Lectura.objects.create(titulo='La llama viajera', texto=self.TEXTO, tipo_reto='oralidad',
                               grado=2, dificultad='basica')
        Lectura.objects.create(titulo='La fiesta del pueblo', texto=self.TEXTO, tipo_reto='lectura',
                               grado=2, dificultad='intermedia')
        resp = self.client.get(self.URL, {'grado': 2, 'dificultad': 'basica'})
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertEqual([l['titulo'] for l in resp.data], ['La llama viajera'])
 
    # --- Filtros por grado y dificultad (T-224) ---
    def crear_lecturas_de_filtro(self):
        Lectura.objects.create(titulo='La llama viajera', texto=self.TEXTO, tipo_reto='oralidad',
                               grado=2, dificultad='basica')
        Lectura.objects.create(titulo='Mi chacra', texto=self.TEXTO, tipo_reto='escritura',
                               grado=2, dificultad='intermedia')
        Lectura.objects.create(titulo='El río que cantaba', texto=self.TEXTO, tipo_reto='oralidad',
                               grado=3, dificultad='avanzada')

    def titulos(self, params):
        resp = self.client.get(self.URL, params)
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        return sorted(l['titulo'] for l in resp.data)

    def test_filtrar_solo_por_grado(self):
        self.crear_lecturas_de_filtro()
        self.assertEqual(self.titulos({'grado': 2}), ['La llama viajera', 'Mi chacra'])

    def test_filtrar_solo_por_dificultad(self):
        self.crear_lecturas_de_filtro()
        self.assertEqual(self.titulos({'dificultad': 'avanzada'}), ['El río que cantaba'])

    def test_combinar_grado_y_tipo_de_reto(self):
        self.crear_lecturas_de_filtro()
        self.assertEqual(self.titulos({'grado': 2, 'tipo_reto': 'oralidad'}), ['La llama viajera'])

    def test_combinar_busqueda_y_grado(self):
        self.crear_lecturas_de_filtro()
        self.assertEqual(self.titulos({'search': 'río', 'grado': 3}), ['El río que cantaba'])

    def test_filtro_sin_resultados_devuelve_lista_vacia(self):
        self.crear_lecturas_de_filtro()
        self.assertEqual(self.titulos({'grado': 6}), [])

    def test_filtros_vacios_devuelven_todas(self):
        self.crear_lecturas_de_filtro()
        self.assertEqual(len(self.titulos({'grado': '', 'dificultad': ''})), 5)



class LecturaEdicionYBorradoTests(APITestCase):
    """T-246 (US-41): editar y borrado lógico; una lectura asignada no se elimina."""
    URL = '/api/lecturas/'
    TEXTO = 'Había una vez un cóndor que vivía en lo alto de la montaña.'

    def setUp(self):
        rol_admin, _ = Rol.objects.get_or_create(nombre='ADMIN')
        rol_docente, _ = Rol.objects.get_or_create(nombre='DOCENTE')
        self.admin = Usuario.objects.create_user(
            username='admin', email='admin@test.pe', password='clave-segura-1', rol=rol_admin)
        self.docente = Usuario.objects.create_user(
            username='rquispe', email='rquispe@test.pe', password='clave-segura-1', rol=rol_docente)
        self.lectura = Lectura.objects.create(
            titulo='El cóndor y el zorro', texto=self.TEXTO, tipo_reto='lectura', grado=2)
        self.url = f'{self.URL}{self.lectura.id}/'
        self.client.force_authenticate(self.admin)

    # --- Editar ---
    def test_admin_edita_el_titulo_con_patch(self):
        resp = self.client.patch(self.url, {'titulo': 'El cóndor sabio'}, format='json')
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.lectura.refresh_from_db()
        self.assertEqual(self.lectura.titulo, 'El cóndor sabio')

    def test_admin_edita_todos_los_campos_con_put(self):
        datos = {'titulo': 'La llama viajera', 'texto': self.TEXTO, 'tipo_reto': 'oralidad',
                 'grado': 4, 'dificultad': 'intermedia'}
        resp = self.client.put(self.url, datos, format='json')
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.lectura.refresh_from_db()
        self.assertEqual((self.lectura.grado, self.lectura.dificultad), (4, 'intermedia'))

    def test_editar_con_texto_corto_devuelve_400(self):
        resp = self.client.patch(self.url, {'texto': 'Muy corto'}, format='json')
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('texto', resp.data)

    def test_docente_no_puede_editar(self):
        self.client.force_authenticate(self.docente)
        resp = self.client.patch(self.url, {'titulo': 'Otro'}, format='json')
        self.assertEqual(resp.status_code, status.HTTP_403_FORBIDDEN)

    # --- Borrado lógico ---
    def test_eliminar_desactiva_sin_borrar_de_la_base(self):
        resp = self.client.delete(self.url)
        self.assertEqual(resp.status_code, status.HTTP_204_NO_CONTENT)
        self.assertTrue(Lectura.objects.filter(id=self.lectura.id).exists())
        self.lectura.refresh_from_db()
        self.assertFalse(self.lectura.activo)

    @mock.patch('catalogo.views.lectura_esta_asignada', return_value=True)
    def test_lectura_asignada_no_se_elimina(self, _asignada):
        resp = self.client.delete(self.url)
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('asignada a alumnos', resp.data['detail'])
        self.lectura.refresh_from_db()
        self.assertTrue(self.lectura.activo)

    def test_docente_no_puede_eliminar(self):
        self.client.force_authenticate(self.docente)
        resp = self.client.delete(self.url)
        self.assertEqual(resp.status_code, status.HTTP_403_FORBIDDEN)
        self.lectura.refresh_from_db()
        self.assertTrue(self.lectura.activo)

    # --- Filtro activo y campo asignada ---
    def test_filtro_activo_true_y_false(self):
        Lectura.objects.create(titulo='Lectura vieja', texto=self.TEXTO, tipo_reto='lectura', activo=False)
        activas = [l['titulo'] for l in self.client.get(self.URL, {'activo': 'true'}).data]
        inactivas = [l['titulo'] for l in self.client.get(self.URL, {'activo': 'false'}).data]
        self.assertEqual(activas, ['El cóndor y el zorro'])
        self.assertEqual(inactivas, ['Lectura vieja'])

    def test_filtro_activo_invalido_devuelve_400(self):
        resp = self.client.get(self.URL, {'activo': 'hola'})
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)

    def test_respuesta_incluye_el_campo_asignada(self):
        resp = self.client.get(self.url)
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertFalse(resp.data['asignada'])

    def test_servicio_detecta_una_lectura_en_la_tabla_asignacion(self):
        from .servicios import lectura_esta_asignada
        if 'asignacion' not in connection.introspection.table_names():
            with connection.cursor() as cursor:
                cursor.execute('CREATE TABLE asignacion (id INTEGER PRIMARY KEY, lectura_id INTEGER)')
        with connection.cursor() as cursor:
            cursor.execute('INSERT INTO asignacion (id, lectura_id) VALUES (%s, %s)', [1, self.lectura.id])
        self.assertTrue(lectura_esta_asignada(self.lectura.id))
        otra = Lectura.objects.create(titulo='Sin asignar', texto=self.TEXTO, tipo_reto='lectura')
        self.assertFalse(lectura_esta_asignada(otra.id))




class RecursoOficialModelTests(TestCase):
    """T-440: tabla de recursos oficiales."""

    def crear(self, **datos):
        base = {'uuid': '137b172b-9330-4bb7-a780-2184391d1810', 'titulo': 'El zorro y el cuy',
                'url_recurso': 'https://bibliotecaescolar.minedu.gob.pe/handle/123456789/191'}
        return RecursoOficial.objects.create(**{**base, **datos})

    def test_recurso_nuevo_no_esta_importado(self):
        self.assertFalse(self.crear().importado)

    def test_al_vincular_una_lectura_queda_importado(self):
        lectura = Lectura.objects.create(titulo='El zorro y el cuy', tipo_reto='lectura',
                                         texto='Había una vez un zorro y un cuy que vivían en el campo.')
        self.assertTrue(self.crear(lectura=lectura).importado)

    def test_no_se_repite_el_mismo_uuid(self):
        self.crear()
        with self.assertRaises(IntegrityError):
            self.crear(titulo='Otro título')

    def test_str_muestra_titulo_y_grado(self):
        self.assertEqual(str(self.crear(grado=2)), 'El zorro y el cuy (2.° grado)')
        self.assertEqual(str(self.crear(uuid='otro-uuid')), 'El zorro y el cuy (sin grado)')


# =====================================================================
# T-445 · Pruebas de la sincronización y la importación (API simulada)
# Ninguna prueba se conecta a internet: la biblioteca y el PDF se simulan.
# =====================================================================
from io import StringIO

from django.core.management import call_command
from django.core.management.base import CommandError

from . import biblioteca


def item_biblioteca(uuid, titulo, idioma=None, tipo='Texto escolar', descripcion='Primaria'):
    """Arma un recurso con la misma forma que devuelve la API de DSpace."""
    metadata = {
        'dc.title': [{'value': titulo}],
        'dc.type': [{'value': tipo}],
        'dc.description': [{'value': descripcion}],
    }
    if idioma:
        metadata['dc.language.iso'] = [{'value': idioma}]
    return {'uuid': uuid, 'name': titulo, 'metadata': metadata, '_links': {}}


TEXTO_ESCOLAR_2 = item_biblioteca('uuid-comu-2', 'Comunicación 2 : texto escolar - Primaria multigrado monolingüe', 'cas')
TEXTO_ESCOLAR_5 = item_biblioteca('uuid-comu-5', 'Comunicación 5 : texto escolar - Primaria multigrado monolingüe')
EIB_MADIJA = item_biblioteca('uuid-madija', 'Diccionario visual - 1. grado de Primaria - Madija', 'cas')
EIB_QUECHUA = item_biblioteca('uuid-quechua', 'Willakuykuna - Quechua Collao - 3. grado de Primaria')
EIB_COLECCION = item_biblioteca('uuid-relatos', 'Kengabagetagantsipage : Colección de relatos - 2. grado')
INICIAL = item_biblioteca('uuid-inicial', 'Katari enfermó del susto - Inicial - Castellano', descripcion='Inicial')
DOCENTE = item_biblioteca('uuid-guia', 'Guía docente para el uso del texto escolar de Comunicación - Primaria')
SECUNDARIA = item_biblioteca('uuid-secu', 'Comunicación 2 : texto escolar - Secundaria', descripcion='Secundaria')


class BibliotecaFiltrosTests(TestCase):
    """Qué recursos sirven para la app: primaria, en castellano y para estudiantes."""

    def test_el_texto_escolar_de_primaria_en_castellano_sirve(self):
        self.assertTrue(biblioteca.es_util(TEXTO_ESCOLAR_2))
        self.assertTrue(biblioteca.es_util(TEXTO_ESCOLAR_5))   # sin idioma registrado

    def test_los_libros_eib_se_descartan_aunque_digan_cas(self):
        self.assertFalse(biblioteca.es_util(EIB_MADIJA))       # "- 1. grado de Primaria - Madija"
        self.assertFalse(biblioteca.es_util(EIB_QUECHUA))      # nombra una lengua originaria
        self.assertFalse(biblioteca.es_util(EIB_COLECCION))    # colección EIB

    def test_inicial_secundaria_y_material_docente_se_descartan(self):
        self.assertFalse(biblioteca.es_util(INICIAL))
        self.assertFalse(biblioteca.es_util(SECUNDARIA))
        self.assertFalse(biblioteca.es_util(DOCENTE))

    def test_una_palabra_que_contiene_bora_no_se_confunde_con_la_lengua(self):
        item = item_biblioteca('uuid-x', 'Elaboramos un texto con colaboración - Comunicación 3 - Primaria')
        self.assertTrue(biblioteca.es_castellano(item['metadata'], item['name']))

    def test_detecta_el_grado_por_el_titulo(self):
        self.assertEqual(biblioteca.detectar_grado({}, TEXTO_ESCOLAR_2['name']), 2)
        self.assertEqual(biblioteca.detectar_grado({}, 'Cuentos para tercer grado'), 3)
        self.assertIsNone(biblioteca.detectar_grado({}, 'Cuentos de mi comunidad'))

    def test_sugiere_la_competencia(self):
        self.assertEqual(biblioteca.detectar_competencia({}, 'Cancionero de mi pueblo'), 'oralidad')
        self.assertEqual(biblioteca.detectar_competencia({}, 'Fábulas andinas'), 'lectura')
        self.assertEqual(biblioteca.detectar_competencia({}, 'Comunicación 2 : texto escolar'), '')

    def test_limpiar_texto_quita_numeros_de_pagina_y_marcas_de_imprenta(self):
        sucio = ('8\nLee y responde.\nLos Leones\nocho\n'
                 '01 Comu 2 Ficha inicial (1-8).indd 8 11/18/23 4:24 PM\nEl zo-\nrro   corre.')
        self.assertEqual(biblioteca.limpiar_texto(sucio), 'Lee y responde.\nLos Leones\nEl zorro corre.')

    def test_limpiar_texto_no_borra_frases_con_numeros(self):
        texto = 'Ciento veinte niños corrieron.\nDos amigos'
        self.assertEqual(biblioteca.limpiar_texto(texto), texto)


@mock.patch('catalogo.biblioteca.enlace_pdf', return_value='https://biblioteca.test/libro.pdf')
class SincronizarRecursosTests(TestCase):
    """Comando sincronizar_recursos con la biblioteca simulada."""

    def sincronizar(self, items, *opciones):
        salida = StringIO()
        with mock.patch('catalogo.biblioteca.recursos', return_value=iter(items)):
            call_command('sincronizar_recursos', *opciones, stdout=salida)
        return salida.getvalue()

    def test_guarda_solo_los_recursos_utiles(self, _pdf):
        salida = self.sincronizar([TEXTO_ESCOLAR_2, EIB_MADIJA, INICIAL, DOCENTE, TEXTO_ESCOLAR_5])
        self.assertEqual(set(RecursoOficial.objects.values_list('uuid', flat=True)), {'uuid-comu-2', 'uuid-comu-5'})
        self.assertIn('Nuevos: 2', salida)
        self.assertIn('Omitidos (otro nivel, otra lengua o material docente): 3', salida)

    def test_guarda_grado_enlaces_y_pdf(self, _pdf):
        self.sincronizar([TEXTO_ESCOLAR_2])
        recurso = RecursoOficial.objects.get(uuid='uuid-comu-2')
        self.assertEqual(recurso.grado, 2)
        self.assertEqual(recurso.url_recurso, f'{biblioteca.BASE}/items/uuid-comu-2')
        self.assertEqual(recurso.url_pdf, 'https://biblioteca.test/libro.pdf')

    def test_correrlo_dos_veces_actualiza_sin_duplicar(self, _pdf):
        self.sincronizar([TEXTO_ESCOLAR_2])
        salida = self.sincronizar([TEXTO_ESCOLAR_2])
        self.assertEqual(RecursoOficial.objects.count(), 1)
        self.assertIn('Nuevos: 0 · Actualizados: 1', salida)

    def test_sin_pdf_no_busca_el_enlace(self, pdf):
        self.sincronizar([TEXTO_ESCOLAR_2], '--sin-pdf')
        pdf.assert_not_called()
        self.assertEqual(RecursoOficial.objects.get().url_pdf, '')

    def test_limpiar_borra_lo_que_ya_no_cumple_pero_no_lo_importado(self, _pdf):
        viejo = RecursoOficial.objects.create(uuid='uuid-viejo', titulo='Recurso que ya no está',
                                              url_recurso='https://biblioteca.test/viejo')
        lectura = Lectura.objects.create(titulo='Lectura importada', tipo_reto='lectura',
                                         texto='Había una vez un zorro que vivía en el campo.')
        importado = RecursoOficial.objects.create(uuid='uuid-importado', titulo='Libro ya usado',
                                                  url_recurso='https://biblioteca.test/usado', lectura=lectura)
        salida = self.sincronizar([TEXTO_ESCOLAR_2], '--limpiar')
        self.assertFalse(RecursoOficial.objects.filter(id=viejo.id).exists())
        self.assertTrue(RecursoOficial.objects.filter(id=importado.id).exists())
        self.assertIn('Borrados (ya no cumplen el filtro): 1', salida)

    def test_si_la_biblioteca_falla_muestra_un_error_claro(self, _pdf):
        with mock.patch('catalogo.biblioteca.recursos', side_effect=biblioteca.ErrorBiblioteca('sin conexión')):
            with self.assertRaisesMessage(CommandError, 'sin conexión'):
                call_command('sincronizar_recursos', stdout=StringIO())


class RecursoOficialAPITests(APITestCase):
    """Endpoints de T-443: listar libros, sacar texto del PDF, importar y sincronizar."""
    URL = '/api/recursos-oficiales/'
    TEXTO = 'Había una vez un zorro y un cuy que vivían en el campo.'

    def setUp(self):
        rol_admin, _ = Rol.objects.get_or_create(nombre='ADMIN')
        rol_docente, _ = Rol.objects.get_or_create(nombre='DOCENTE')
        self.admin = Usuario.objects.create_user(
            username='admin', email='admin@test.pe', password='clave-segura-1', rol=rol_admin)
        self.docente = Usuario.objects.create_user(
            username='rquispe', email='rquispe@test.pe', password='clave-segura-1', rol=rol_docente)
        self.libro = RecursoOficial.objects.create(
            uuid='uuid-comu-2', titulo='Comunicación 2 : texto escolar', grado=2,
            url_recurso='https://biblioteca.test/items/uuid-comu-2', url_pdf='https://biblioteca.test/comu2.pdf')
        self.sin_grado = RecursoOficial.objects.create(
            uuid='uuid-sin-grado', titulo='Cuentos del Perú', url_recurso='https://biblioteca.test/items/cuentos')
        self.client.force_authenticate(self.admin)

    def url(self, recurso, accion):
        return f'{self.URL}{recurso.id}/{accion}/'

    def datos(self, **cambios):
        return {'titulo': 'El zorro y el cuy', 'texto': self.TEXTO, 'tipo_reto': 'lectura', **cambios}

    # --- Listar ---
    def test_lista_los_libros_con_su_fuente(self):
        resp = self.client.get(self.URL)
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertEqual(len(resp.data), 2)
        self.assertEqual(resp.data[0]['fuente'], RecursoOficial.FUENTE)

    def test_nuevos_solo_muestra_los_no_importados(self):
        self.client.post(self.url(self.libro, 'importar'), self.datos(), format='json')
        resp = self.client.get(self.URL, {'nuevos': 'true'})
        self.assertEqual([r['id'] for r in resp.data], [self.sin_grado.id])

    def test_filtro_de_grado_invalido_devuelve_400(self):
        self.assertEqual(self.client.get(self.URL, {'grado': '9'}).status_code, status.HTTP_400_BAD_REQUEST)

    # --- Sacar texto del PDF ---
    @mock.patch('catalogo.biblioteca.extraer_texto', return_value=('Había una vez un zorro.', 82))
    @mock.patch('catalogo.biblioteca.descargar_pdf', return_value='cache_pdf/uuid-comu-2.pdf')
    def test_saca_el_texto_de_las_paginas(self, descargar, extraer):
        resp = self.client.get(self.url(self.libro, 'texto'), {'desde': 10, 'hasta': 11})
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertEqual(resp.data['texto'], 'Había una vez un zorro.')
        self.assertEqual(resp.data['paginas_total'], 82)
        self.assertEqual(resp.data['aviso'], '')
        descargar.assert_called_once_with(self.libro.url_pdf, 'uuid-comu-2')
        extraer.assert_called_once_with('cache_pdf/uuid-comu-2.pdf', 10, 11)

    @mock.patch('catalogo.biblioteca.extraer_texto', return_value=('', 82))
    @mock.patch('catalogo.biblioteca.descargar_pdf', return_value='x.pdf')
    def test_paginas_sin_texto_devuelven_un_aviso(self, _descargar, _extraer):
        resp = self.client.get(self.url(self.libro, 'texto'), {'desde': 1, 'hasta': 1})
        self.assertIn('no tienen texto', resp.data['aviso'])

    def test_rango_de_paginas_invalido_devuelve_400(self):
        casos = [{}, {'desde': 5, 'hasta': 3}, {'desde': 1, 'hasta': 11}, {'desde': 0, 'hasta': 2}, {'desde': 'a', 'hasta': 2}]
        for params in casos:
            with self.subTest(params=params):
                resp = self.client.get(self.url(self.libro, 'texto'), params)
                self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)

    @mock.patch('catalogo.biblioteca.extraer_texto', side_effect=ValueError('El libro tiene 82 páginas: elige páginas entre 1 y 82.'))
    @mock.patch('catalogo.biblioteca.descargar_pdf', return_value='x.pdf')
    def test_paginas_fuera_del_libro_devuelven_400(self, _descargar, _extraer):
        resp = self.client.get(self.url(self.libro, 'texto'), {'desde': 90, 'hasta': 91})
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('82 páginas', resp.data['detail'])

    @mock.patch('catalogo.biblioteca.descargar_pdf', side_effect=biblioteca.ErrorBiblioteca('No se pudo descargar el PDF'))
    def test_si_el_pdf_no_descarga_devuelve_502(self, _descargar):
        resp = self.client.get(self.url(self.libro, 'texto'), {'desde': 1, 'hasta': 2})
        self.assertEqual(resp.status_code, status.HTTP_502_BAD_GATEWAY)

    def test_recurso_sin_pdf_devuelve_400(self):
        resp = self.client.get(self.url(self.sin_grado, 'texto'), {'desde': 1, 'hasta': 2})
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)

    # --- Importar ---
    def test_importar_crea_una_lectura_oficial_del_grado_del_libro(self):
        resp = self.client.post(self.url(self.libro, 'importar'), self.datos(dificultad='intermedia'), format='json')
        self.assertEqual(resp.status_code, status.HTTP_201_CREATED)
        lectura = Lectura.objects.get(id=resp.data['id'])
        self.assertEqual((lectura.fuente, lectura.grado, lectura.dificultad), ('oficial', 2, 'intermedia'))
        self.assertEqual(lectura.url_recurso, self.libro.url_recurso)
        self.assertEqual(lectura.creado_por, self.admin)
        self.libro.refresh_from_db()
        self.assertEqual(self.libro.lectura, lectura)      # el libro queda marcado como importado

    def test_de_un_libro_salen_varias_lecturas(self):
        primera = self.client.post(self.url(self.libro, 'importar'), self.datos(), format='json').data
        self.client.post(self.url(self.libro, 'importar'), self.datos(titulo='Otra lectura', tipo_reto='oralidad'), format='json')
        self.libro.refresh_from_db()
        self.assertEqual(self.libro.lectura_id, primera['id'])   # no se pisa la primera
        resp = self.client.get(self.URL)
        libro = next(r for r in resp.data if r['id'] == self.libro.id)
        self.assertEqual(libro['lecturas_importadas'], 2)

    def test_el_grado_del_libro_no_se_puede_cambiar(self):
        resp = self.client.post(self.url(self.libro, 'importar'), self.datos(grado=5), format='json')
        self.assertEqual(resp.data['grado'], 2)

    def test_recurso_sin_grado_pide_el_grado(self):
        resp = self.client.post(self.url(self.sin_grado, 'importar'), self.datos(), format='json')
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('grado', resp.data)
        resp = self.client.post(self.url(self.sin_grado, 'importar'), self.datos(grado=4), format='json')
        self.assertEqual(resp.status_code, status.HTTP_201_CREATED)
        self.assertEqual(resp.data['grado'], 4)

    def test_importar_con_datos_invalidos_devuelve_400(self):
        casos = {'tipo_reto': self.datos(tipo_reto='canto'), 'texto': self.datos(texto='corto'),
                 'titulo': self.datos(titulo='   '), 'dificultad': self.datos(dificultad='facil')}
        for campo, datos in casos.items():
            with self.subTest(campo=campo):
                resp = self.client.post(self.url(self.libro, 'importar'), datos, format='json')
                self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
                self.assertIn(campo, resp.data)
        self.assertEqual(Lectura.objects.count(), 0)

    # --- Sincronizar (botón "Actualizar catálogo") ---
    @mock.patch('catalogo.views.call_command')
    def test_sincronizar_devuelve_el_resultado(self, comando):
        comando.side_effect = lambda *a, **k: k['stdout'].write('Consultando...\nListo. Nuevos: 6 · Actualizados: 0\n')
        resp = self.client.post(f'{self.URL}sincronizar/')
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertEqual(resp.data['resultado'], 'Listo. Nuevos: 6 · Actualizados: 0')
        self.assertTrue(comando.call_args.kwargs['limpiar'])

    @mock.patch('catalogo.views.call_command', side_effect=CommandError('No se pudo consultar la biblioteca'))
    def test_sincronizar_con_la_biblioteca_caida_devuelve_502(self, _comando):
        resp = self.client.post(f'{self.URL}sincronizar/')
        self.assertEqual(resp.status_code, status.HTTP_502_BAD_GATEWAY)

    # --- Permisos ---
    def test_docente_no_puede_importar_ni_sincronizar(self):
        self.client.force_authenticate(self.docente)
        self.assertEqual(self.client.post(self.url(self.libro, 'importar'), self.datos(), format='json').status_code,
                         status.HTTP_403_FORBIDDEN)
        self.assertEqual(self.client.post(f'{self.URL}sincronizar/').status_code, status.HTTP_403_FORBIDDEN)

    def test_sin_token_devuelve_401(self):
        self.client.force_authenticate(None)
        self.assertEqual(self.client.get(self.URL).status_code, status.HTTP_401_UNAUTHORIZED)
