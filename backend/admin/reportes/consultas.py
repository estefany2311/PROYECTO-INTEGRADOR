"""US-47 · T-264: consultas de agregación para los reportes consolidados.

Las tablas evaluacion, estudiante y aula las crean los backends de Spring Boot
(Flyway) en la base Neon compartida. Django solo las LEE con SQL.

Camino para saber el colegio de un alumno:
estudiante.aula_id → aula.docente_id → colegio_docentes.docente_id → colegio_docentes.colegio_id
"""
from django.db import connection

# Semáforo (los mismos rangos del dashboard del docente, US-27)
UMBRAL_VERDE = 75
UMBRAL_AMARILLO = 50

COMPETENCIAS = ['lectura', 'escritura', 'oralidad']
TABLAS_NECESARIAS = ('evaluacion', 'estudiante', 'aula')


def tablas_disponibles():
    """False si todavía no existen las tablas de Spring Boot (por ejemplo, en una base nueva)."""
    existentes = set(connection.introspection.table_names())
    return all(tabla in existentes for tabla in TABLAS_NECESARIAS)


def color_semaforo(promedio):
    if promedio is None:
        return 'sin_evaluar'
    if promedio >= UMBRAL_VERDE:
        return 'verde'
    if promedio >= UMBRAL_AMARILLO:
        return 'amarillo'
    return 'rojo'


def _filtros(colegio_id=None, aula_id=None, grado=None):
    """Arma el WHERE con los filtros opcionales (siempre solo alumnos activos)."""
    condiciones, parametros = ['e.activo'], []
    if colegio_id is not None:
        condiciones.append('cd.colegio_id = %s')
        parametros.append(colegio_id)
    if aula_id is not None:
        condiciones.append('a.id = %s')
        parametros.append(aula_id)
    if grado is not None:
        condiciones.append('e.grado_id = %s')
        parametros.append(grado)
    return ' AND '.join(condiciones), parametros


def promedio_por_competencia(colegio_id=None, aula_id=None, grado=None):
    """Promedio de puntaje por competencia. Siempre devuelve las 3 competencias."""
    resultado = {c: {'competencia': c, 'promedio': None, 'evaluaciones': 0, 'alumnos': 0} for c in COMPETENCIAS}
    if not tablas_disponibles():
        return list(resultado.values())

    where, parametros = _filtros(colegio_id, aula_id, grado)
    sql = f"""
        SELECT ev.competencia,
               ROUND(AVG(ev.puntaje)::numeric, 1) AS promedio,
               COUNT(*) AS evaluaciones,
               COUNT(DISTINCT ev.estudiante_id) AS alumnos
        FROM evaluacion ev
        JOIN estudiante e ON e.id = ev.estudiante_id
        JOIN aula a ON a.id = e.aula_id
        LEFT JOIN colegio_docentes cd ON cd.docente_id = a.docente_id
        WHERE {where}
        GROUP BY ev.competencia
    """
    with connection.cursor() as cursor:
        cursor.execute(sql, parametros)
        for competencia, promedio, evaluaciones, alumnos in cursor.fetchall():
            if competencia in resultado:
                resultado[competencia].update(
                    promedio=float(promedio), evaluaciones=evaluaciones, alumnos=alumnos)
    return list(resultado.values())


def alumnos_por_semaforo(colegio_id=None, aula_id=None, grado=None):
    """Cuántos alumnos activos hay en cada color, según el promedio de todas sus evaluaciones."""
    conteo = {'verde': 0, 'amarillo': 0, 'rojo': 0, 'sin_evaluar': 0}
    if not tablas_disponibles():
        return conteo

    where, parametros = _filtros(colegio_id, aula_id, grado)
    sql = f"""
        SELECT e.id, AVG(ev.puntaje) AS promedio
        FROM estudiante e
        JOIN aula a ON a.id = e.aula_id
        LEFT JOIN colegio_docentes cd ON cd.docente_id = a.docente_id
        LEFT JOIN evaluacion ev ON ev.estudiante_id = e.id
        WHERE {where}
        GROUP BY e.id
    """
    with connection.cursor() as cursor:
        cursor.execute(sql, parametros)
        for _, promedio in cursor.fetchall():
            conteo[color_semaforo(None if promedio is None else float(promedio))] += 1
    return conteo