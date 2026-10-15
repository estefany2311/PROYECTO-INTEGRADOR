from rest_framework.test import APITestCase

from .models import Rol, Usuario

URL_DOCENTES = "/api/docentes/"
URL_LOGIN = "/api/auth/login/"

DOCENTE_VALIDO = {
    "username": "rquispe",
    "first_name": "Rosa",
    "last_name": "Quispe",
    "email": "rquispe@correo.com",
    "password": "Clave12345",
}


class BaseApiTest(APITestCase):
    def setUp(self):
        rol_admin = Rol.objects.get(nombre="ADMIN")
        rol_docente = Rol.objects.get(nombre="DOCENTE")
        self.admin = Usuario.objects.create_user(
            username="admin_test", password="Admin12345", rol=rol_admin
        )
        self.docente = Usuario.objects.create_user(
            username="docente_test",
            password="Docente12345",
            email="docente_test@correo.com",
            first_name="Doc",
            last_name="Prueba",
            rol=rol_docente,
        )


class CrearDocenteTests(BaseApiTest):
    def test_admin_crea_docente(self):
        self.client.force_authenticate(self.admin)
        resp = self.client.post(URL_DOCENTES, DOCENTE_VALIDO, format="json")
        self.assertEqual(resp.status_code, 201)
        self.assertNotIn("password", resp.data)
        creado = Usuario.objects.get(username="rquispe")
        self.assertEqual(creado.rol.nombre, "DOCENTE")

    def test_contrasena_se_guarda_con_bcrypt(self):
        self.client.force_authenticate(self.admin)
        self.client.post(URL_DOCENTES, DOCENTE_VALIDO, format="json")
        creado = Usuario.objects.get(username="rquispe")
        self.assertTrue(creado.password.startswith("bcrypt$"))
        self.assertNotEqual(creado.password, DOCENTE_VALIDO["password"])

    def test_sin_token_responde_401(self):
        resp = self.client.post(URL_DOCENTES, DOCENTE_VALIDO, format="json")
        self.assertEqual(resp.status_code, 401)

    def test_docente_no_puede_crear_docentes(self):
        self.client.force_authenticate(self.docente)
        resp = self.client.post(URL_DOCENTES, DOCENTE_VALIDO, format="json")
        self.assertEqual(resp.status_code, 403)
        self.assertFalse(Usuario.objects.filter(username="rquispe").exists())

    def test_campo_obligatorio_faltante(self):
        self.client.force_authenticate(self.admin)
        datos = {k: v for k, v in DOCENTE_VALIDO.items() if k != "last_name"}
        resp = self.client.post(URL_DOCENTES, datos, format="json")
        self.assertEqual(resp.status_code, 400)
        self.assertIn("last_name", resp.data)

    def test_correo_con_formato_invalido(self):
        self.client.force_authenticate(self.admin)
        datos = {**DOCENTE_VALIDO, "email": "abc"}
        resp = self.client.post(URL_DOCENTES, datos, format="json")
        self.assertEqual(resp.status_code, 400)
        self.assertIn("email", resp.data)

    def test_correo_repetido_sin_distinguir_mayusculas(self):
        self.client.force_authenticate(self.admin)
        datos = {
            **DOCENTE_VALIDO,
            "username": "otro_usuario",
            "email": "DOCENTE_TEST@correo.com",
        }
        resp = self.client.post(URL_DOCENTES, datos, format="json")
        self.assertEqual(resp.status_code, 400)
        self.assertIn("email", resp.data)

    def test_contrasena_demasiado_corta(self):
        self.client.force_authenticate(self.admin)
        datos = {**DOCENTE_VALIDO, "password": "123"}
        resp = self.client.post(URL_DOCENTES, datos, format="json")
        self.assertEqual(resp.status_code, 400)
        self.assertIn("password", resp.data)

    def test_usuario_repetido(self):
        self.client.force_authenticate(self.admin)
        datos = {**DOCENTE_VALIDO, "username": "docente_test"}
        resp = self.client.post(URL_DOCENTES, datos, format="json")
        self.assertEqual(resp.status_code, 400)
        self.assertIn("username", resp.data)


class ListarDocentesTests(BaseApiTest):
    def test_admin_lista_solo_docentes_sin_password(self):
        self.client.force_authenticate(self.admin)
        resp = self.client.get(URL_DOCENTES)
        self.assertEqual(resp.status_code, 200)
        usernames = [d["username"] for d in resp.data]
        self.assertIn("docente_test", usernames)
        self.assertNotIn("admin_test", usernames)
        for d in resp.data:
            self.assertNotIn("password", d)

    def test_sin_token_responde_401(self):
        self.assertEqual(self.client.get(URL_DOCENTES).status_code, 401)

    def test_docente_no_puede_listar(self):
        self.client.force_authenticate(self.docente)
        self.assertEqual(self.client.get(URL_DOCENTES).status_code, 403)


class LoginTests(BaseApiTest):
    def test_login_correcto_devuelve_tokens(self):
        resp = self.client.post(
            URL_LOGIN,
            {"username": "admin_test", "password": "Admin12345"},
            format="json",
        )
        self.assertEqual(resp.status_code, 200)
        self.assertIn("access", resp.data)
        self.assertIn("refresh", resp.data)

    def test_login_con_contrasena_incorrecta(self):
        resp = self.client.post(
            URL_LOGIN,
            {"username": "admin_test", "password": "incorrecta"},
            format="json",
        )
        self.assertEqual(resp.status_code, 401)

    def test_token_real_permite_listar(self):
        login = self.client.post(
            URL_LOGIN,
            {"username": "admin_test", "password": "Admin12345"},
            format="json",
        )
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {login.data['access']}")
        self.assertEqual(self.client.get(URL_DOCENTES).status_code, 200)