from django.db import connection

# Lo que usa la app del estudiante cuando un nivel no tiene regla (US-08)
ESTRELLAS_POR_DEFECTO = 1
PUNTAJE_POR_DEFECTO = 0

COLUMNAS_NIVEL = 'id, orden, nombre, tipo_reto, activo'


def _filas(cursor):
    columnas = [c[0] for c in cursor.description]
    return [dict(zip(columnas, fila)) for fila in cursor.fetchall()]


def tabla_nivel_existe():
    """La tabla `nivel` la crea Spring Boot (módulo docente, Flyway V7)."""
    return 'nivel' in connection.introspection.table_names()


def listar_niveles():
    """Niveles del mapa ordenados por su recorrido (1, 2, 3...)."""
    if not tabla_nivel_existe():
        return []
    with connection.cursor() as cursor:
        cursor.execute(f'SELECT {COLUMNAS_NIVEL} FROM nivel ORDER BY orden')
        return _filas(cursor)


def obtener_nivel(nivel_id):
    """Un nivel por su id, o None si no existe."""
    if not tabla_nivel_existe():
        return None
    with connection.cursor() as cursor:
        cursor.execute(f'SELECT {COLUMNAS_NIVEL} FROM nivel WHERE id = %s', [nivel_id])
        filas = _filas(cursor)
    return filas[0] if filas else None