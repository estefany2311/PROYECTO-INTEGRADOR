# Backend Admin — Django

## Historia de este sprint
- US-43: Crear cuentas de docentes — Daniela

## Cómo levantarlo
1. python -m venv venv
2. Activar el entorno: source venv/bin/activate (Mac/Linux) o venv\Scripts\activate (Windows)
3. pip install django djangorestframework psycopg2-binary
4. django-admin startproject core .
5. Conectar a PostgreSQL en core/settings.py
6. python manage.py runserver

## Ventaja a aprovechar
Django trae un panel de administración automático (django.contrib.admin) que genera formularios de alta, edición y listado con muy poco código. Revisar si cubre directamente lo que pide US-43 antes de programar pantallas desde cero.
