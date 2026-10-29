from rest_framework import status
from rest_framework.test import APITestCase
 
from usuarios.models import Rol, Usuario
from .models import Lectura
 
 
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
 
    def test_editar_y_eliminar_no_estan_permitidos(self):
        lectura = Lectura.objects.first()
        url = f'{self.URL}{lectura.id}/'
        self.assertEqual(self.client.patch(url, {'titulo': 'Otro'}, format='json').status_code,
                         status.HTTP_405_METHOD_NOT_ALLOWED)
        self.assertEqual(self.client.delete(url).status_code, status.HTTP_405_METHOD_NOT_ALLOWED)
 
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