from django.db import IntegrityError, transaction
from django.test import TestCase
from rest_framework.test import APITestCase

from .models import Permiso, Rol, RolPermiso, Usuario

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

    def test_cuenta_desactivada_no_puede_iniciar_sesion(self):
        self.docente.is_active = False
        self.docente.save()
        resp = self.client.post(URL_LOGIN, {"username": "docente_test", "password": "Docente12345"}, format="json")
        self.assertEqual(resp.status_code, 401)
        self.assertEqual(resp.data["detail"], "Tu cuenta está desactivada. Contacta al administrador.")

    def test_cuenta_desactivada_con_contrasena_incorrecta_no_revela_nada(self):
        self.docente.is_active = False
        self.docente.save()
        resp = self.client.post(URL_LOGIN, {"username": "docente_test", "password": "otra-clave"}, format="json")
        self.assertEqual(resp.status_code, 401)
        self.assertNotIn("desactivada", str(resp.data))

    def test_token_de_cuenta_desactivada_deja_de_funcionar(self):
        resp = self.client.post(URL_LOGIN, {"username": "docente_test", "password": "Docente12345"}, format="json")
        token = resp.data["access"]
        self.docente.is_active = False
        self.docente.save()
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {token}")
        self.assertEqual(self.client.get("/api/auth/me/").status_code, 401)


class PermisosTests(TestCase):
    # Los permisos y sus asignaciones vienen de la migración 0005_cargar_permisos
    def setUp(self):
        self.admin = Rol.objects.get(nombre="ADMIN")
        self.docente = Rol.objects.get(nombre="DOCENTE")

    def test_existen_permisos_y_admin_tiene_todos(self):
        total = Permiso.objects.count()
        self.assertEqual(total, 29)  # 7 módulos x 4 acciones + reportes.ver
        self.assertEqual(self.admin.permisos_asignados.count(), total)
        self.assertTrue(self.admin.tiene_permiso("colegios.crear"))
        self.assertTrue(self.admin.tiene_permiso("reportes.ver"))

    def test_docente_tiene_alumnos_crear_pero_no_colegios_crear(self):
        self.assertTrue(self.docente.tiene_permiso("alumnos.crear"))
        self.assertTrue(self.docente.tiene_permiso("colegios.ver"))
        self.assertFalse(self.docente.tiene_permiso("colegios.crear"))
        self.assertFalse(self.docente.tiene_permiso("asignaciones.desactivar"))

    def test_no_se_repite_el_mismo_permiso_en_un_rol(self):
        permiso = Permiso.objects.get(codigo="colegios.ver")
        with self.assertRaises(IntegrityError), transaction.atomic():
            RolPermiso.objects.create(rol=self.docente, permiso=permiso)


class RolesPermisosApiTests(BaseApiTest):
    URL_ME = "/api/auth/me/"
    URL_ROLES = "/api/roles/"
    URL_COLEGIOS = "/api/colegios/"

    def setUp(self):
        super().setUp()
        self.rol_docente = Rol.objects.get(nombre="DOCENTE")
        self.rol_admin = Rol.objects.get(nombre="ADMIN")
        self.client.force_authenticate(self.admin)

    def url_permisos(self, rol):
        return f"{self.URL_ROLES}{rol.id}/permisos/"

    def permisos_de(self, rol):
        return list(rol.permisos_asignados.values_list("permiso__codigo", flat=True))

    def test_me_devuelve_los_permisos_del_rol(self):
        self.client.force_authenticate(self.docente)
        resp = self.client.get(self.URL_ME)
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.data["username"], "docente_test")
        self.assertEqual(resp.data["nombre_completo"], "Doc Prueba")
        self.assertEqual(resp.data["rol"], "DOCENTE")
        self.assertIn("alumnos.crear", resp.data["permisos"])
        self.assertNotIn("colegios.crear", resp.data["permisos"])

    def test_admin_lista_roles_y_docente_recibe_403(self):
        resp = self.client.get(self.URL_ROLES)
        self.assertEqual(resp.status_code, 200)
        admin = next(r for r in resp.data if r["nombre"] == "ADMIN")
        self.assertEqual(len(admin["permisos"]), Permiso.objects.count())
        self.assertEqual(admin["total_usuarios"], 1)

        self.client.force_authenticate(self.docente)
        self.assertEqual(self.client.get(self.URL_ROLES).status_code, 403)

    def test_cambiar_permisos_de_docente_cambia_lo_que_puede_hacer(self):
        colegio = {"codigo_modular": "0512346", "nombre": "IE N° 50123 Ccollpa", "region": "Cusco"}
        self.client.force_authenticate(self.docente)
        self.assertEqual(self.client.post(self.URL_COLEGIOS, colegio, format="json").status_code, 403)

        self.client.force_authenticate(self.admin)
        nuevos = self.permisos_de(self.rol_docente) + ["colegios.crear"]
        resp = self.client.put(self.url_permisos(self.rol_docente), {"permisos": nuevos}, format="json")
        self.assertEqual(resp.status_code, 200)
        self.assertIn("colegios.crear", resp.data["permisos"])

        self.client.force_authenticate(self.docente)
        self.assertEqual(self.client.post(self.URL_COLEGIOS, colegio, format="json").status_code, 201)

    def test_admin_no_puede_quedarse_sin_roles_editar(self):
        sin_editar = [c for c in self.permisos_de(self.rol_admin) if c != "roles.editar"]
        resp = self.client.put(self.url_permisos(self.rol_admin), {"permisos": sin_editar}, format="json")
        self.assertEqual(resp.status_code, 400)
        self.assertIn(
            "El rol ADMIN debe conservar los permisos de roles para no perder el acceso.",
            resp.data["permisos"],
        )
        self.assertTrue(self.rol_admin.tiene_permiso("roles.editar"))

    def test_codigo_de_permiso_inexistente_devuelve_400(self):
        resp = self.client.put(
            self.url_permisos(self.rol_docente), {"permisos": ["colegios.ver", "naves.volar"]}, format="json"
        )
        self.assertEqual(resp.status_code, 400)
        self.assertIn("naves.volar", resp.data["permisos"][0])

    def test_cambiar_rol_de_otro_usuario(self):
        resp = self.client.patch(f"/api/usuarios/{self.docente.id}/rol/", {"rol": self.rol_admin.id}, format="json")
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.data["rol"]["nombre"], "ADMIN")
        self.docente.refresh_from_db()
        self.assertEqual(self.docente.rol, self.rol_admin)

    def test_no_puede_cambiar_su_propio_rol(self):
        resp = self.client.patch(f"/api/usuarios/{self.admin.id}/rol/", {"rol": self.rol_docente.id}, format="json")
        self.assertEqual(resp.status_code, 400)
        self.assertIn("No puedes cambiar tu propio rol.", resp.data["rol"])
        self.admin.refresh_from_db()
        self.assertEqual(self.admin.rol, self.rol_admin)

    def test_buscar_usuarios(self):
        resp = self.client.get("/api/usuarios/", {"search": "docente_test"})
        self.assertEqual(resp.status_code, 200)
        self.assertEqual([u["username"] for u in resp.data], ["docente_test"])
        self.assertEqual(resp.data[0]["rol"]["nombre"], "DOCENTE")


class AccesoDenegadoPorRolTests(BaseApiTest):
    """El docente y un usuario sin rol reciben 403 en lo que no les corresponde (US-45)."""

    def rutas_prohibidas_para_docente(self):
        return [
            ("get", "/api/docentes/", None),
            ("post", "/api/docentes/", DOCENTE_VALIDO),
            ("get", "/api/roles/", None),
            ("get", "/api/permisos/", None),
            ("get", "/api/usuarios/", None),
            ("patch", f"/api/usuarios/{self.admin.id}/rol/", {"rol": 1}),
            ("patch", f"/api/usuarios/{self.admin.id}/estado/", {"is_active": False}),
            ("post", "/api/colegios/", {"codigo_modular": "0734120", "nombre": "IE Prueba", "region": "Puno"}),
            ("post", "/api/lecturas/", {"titulo": "Prueba", "texto": "Texto suficientemente largo para la prueba.",
                                        "tipo_reto": "lectura"}),
        ]

    def test_docente_recibe_403_donde_no_tiene_permiso(self):
        self.client.force_authenticate(self.docente)
        for metodo, url, datos in self.rutas_prohibidas_para_docente():
            with self.subTest(metodo=metodo, url=url):
                resp = getattr(self.client, metodo)(url, datos, format="json")
                self.assertEqual(resp.status_code, 403)
                self.assertEqual(resp.data["detail"], "No tienes permiso para realizar esta acción.")

    def test_docente_si_puede_ver_colegios_y_catalogo(self):
        self.client.force_authenticate(self.docente)
        for url in ("/api/colegios/", "/api/lecturas/"):
            with self.subTest(url=url):
                self.assertEqual(self.client.get(url).status_code, 200)

    def test_usuario_sin_rol_recibe_403_en_todo(self):
        sin_rol = Usuario.objects.create_user(
            username="sin_rol", email="sin_rol@correo.com", password="SinRol12345"
        )
        self.client.force_authenticate(sin_rol)
        for url in ("/api/colegios/", "/api/lecturas/", "/api/docentes/", "/api/roles/", "/api/usuarios/"):
            with self.subTest(url=url):
                self.assertEqual(self.client.get(url).status_code, 403)

    def test_admin_si_puede_entrar_a_todo(self):
        self.client.force_authenticate(self.admin)
        for url in ("/api/colegios/", "/api/lecturas/", "/api/docentes/", "/api/roles/",
                    "/api/permisos/", "/api/usuarios/"):
            with self.subTest(url=url):
               self.assertEqual(self.client.get(url).status_code, 200)