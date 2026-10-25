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
 
