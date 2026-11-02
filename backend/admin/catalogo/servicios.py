from django.db import connection


def lectura_esta_asignada(lectura_id):
    """True si la lectura ya está asignada a algún alumno (tabla 'asignacion' del módulo docente)."""
    if 'asignacion' not in connection.introspection.table_names():
        return False
    with connection.cursor() as cursor:
        cursor.execute('SELECT 1 FROM asignacion WHERE lectura_id = %s LIMIT 1', [lectura_id])
        return cursor.fetchone() is not None