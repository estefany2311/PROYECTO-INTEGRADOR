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




def _numero(valor):
    return None if valor is None else float(valor)


def rendimiento_por_colegio(colegio_id=None):
    """T-266: una fila por colegio activo con alumnos, promedios por competencia y semáforo.

    Un alumno pertenece al colegio del docente de su aula (colegio_docentes).
    """
    from instituciones.models import Colegio

    colegios = Colegio.objects.filter(activo=True)
    if colegio_id is not None:
        colegios = colegios.filter(id=colegio_id)

    vacio = {'alumnos': 0, 'evaluaciones': 0, 'promedio_general': None,
             'promedios': {c: None for c in COMPETENCIAS},
             'semaforo': {'verde': 0, 'amarillo': 0, 'rojo': 0, 'sin_evaluar': 0}}
    resultado = {
        c.id: {'colegio_id': c.id, 'nombre': c.nombre, 'codigo_modular': c.codigo_modular,
               'region': c.region, **{k: (v.copy() if isinstance(v, dict) else v) for k, v in vacio.items()}}
        for c in colegios
    }
    if not resultado or not tablas_disponibles():
        return list(resultado.values())

    sql = """
        WITH alumnos AS (
            SELECT e.id AS estudiante_id, cd.colegio_id
            FROM estudiante e
            JOIN aula a ON a.id = e.aula_id
            JOIN colegio_docentes cd ON cd.docente_id = a.docente_id
            WHERE e.activo AND cd.colegio_id = ANY(%s)
        ),
        promedio_alumno AS (
            SELECT al.colegio_id, al.estudiante_id, AVG(ev.puntaje) AS promedio
            FROM alumnos al
            LEFT JOIN evaluacion ev ON ev.estudiante_id = al.estudiante_id
            GROUP BY al.colegio_id, al.estudiante_id
        ),
        por_competencia AS (
            SELECT al.colegio_id,
                   COUNT(ev.id) AS evaluaciones,
                   ROUND(AVG(ev.puntaje)::numeric, 1) AS general,
                   ROUND((AVG(ev.puntaje) FILTER (WHERE ev.competencia = 'lectura'))::numeric, 1) AS lectura,
                   ROUND((AVG(ev.puntaje) FILTER (WHERE ev.competencia = 'escritura'))::numeric, 1) AS escritura,
                   ROUND((AVG(ev.puntaje) FILTER (WHERE ev.competencia = 'oralidad'))::numeric, 1) AS oralidad
            FROM alumnos al
            LEFT JOIN evaluacion ev ON ev.estudiante_id = al.estudiante_id
            GROUP BY al.colegio_id
        ),
        semaforo AS (
            SELECT colegio_id,
                   COUNT(*) AS alumnos,
                   COUNT(*) FILTER (WHERE promedio >= %s) AS verde,
                   COUNT(*) FILTER (WHERE promedio >= %s AND promedio < %s) AS amarillo,
                   COUNT(*) FILTER (WHERE promedio < %s) AS rojo,
                   COUNT(*) FILTER (WHERE promedio IS NULL) AS sin_evaluar
            FROM promedio_alumno
            GROUP BY colegio_id
        )
        SELECT s.colegio_id, s.alumnos, p.evaluaciones, p.general, p.lectura, p.escritura, p.oralidad,
               s.verde, s.amarillo, s.rojo, s.sin_evaluar
        FROM semaforo s
        JOIN por_competencia p ON p.colegio_id = s.colegio_id
    """
    parametros = [list(resultado), UMBRAL_VERDE, UMBRAL_AMARILLO, UMBRAL_VERDE, UMBRAL_AMARILLO]
    with connection.cursor() as cursor:
        cursor.execute(sql, parametros)
        for (cid, alumnos, evaluaciones, general, lectura, escritura, oralidad,
             verde, amarillo, rojo, sin_evaluar) in cursor.fetchall():
            resultado[cid].update(
                alumnos=alumnos,
                evaluaciones=evaluaciones,
                promedio_general=_numero(general),
                promedios={'lectura': _numero(lectura), 'escritura': _numero(escritura),
                           'oralidad': _numero(oralidad)},
                semaforo={'verde': verde, 'amarillo': amarillo, 'rojo': rojo, 'sin_evaluar': sin_evaluar},
            )
    return list(resultado.values())




def rendimiento_por_aula(colegio_id):
    """T-268: una fila por aula del colegio, con alumnos, promedios por competencia y semáforo."""
    if not tablas_disponibles():
        return []

    sql = """
        WITH aulas AS (
            SELECT a.id, a.nombre, a.docente_id
            FROM aula a
            JOIN colegio_docentes cd ON cd.docente_id = a.docente_id
            WHERE cd.colegio_id = %s
        ),
        alumnos AS (
            SELECT e.id AS estudiante_id, e.aula_id
            FROM estudiante e
            JOIN aulas au ON au.id = e.aula_id
            WHERE e.activo
        ),
        promedio_alumno AS (
            SELECT al.aula_id, al.estudiante_id, AVG(ev.puntaje) AS promedio
            FROM alumnos al
            LEFT JOIN evaluacion ev ON ev.estudiante_id = al.estudiante_id
            GROUP BY al.aula_id, al.estudiante_id
        ),
        por_competencia AS (
            SELECT al.aula_id,
                   COUNT(ev.id) AS evaluaciones,
                   ROUND(AVG(ev.puntaje)::numeric, 1) AS general,
                   ROUND((AVG(ev.puntaje) FILTER (WHERE ev.competencia = 'lectura'))::numeric, 1) AS lectura,
                   ROUND((AVG(ev.puntaje) FILTER (WHERE ev.competencia = 'escritura'))::numeric, 1) AS escritura,
                   ROUND((AVG(ev.puntaje) FILTER (WHERE ev.competencia = 'oralidad'))::numeric, 1) AS oralidad
            FROM alumnos al
            LEFT JOIN evaluacion ev ON ev.estudiante_id = al.estudiante_id
            GROUP BY al.aula_id
        ),
        semaforo AS (
            SELECT aula_id,
                   COUNT(*) AS alumnos,
                   COUNT(*) FILTER (WHERE promedio >= %s) AS verde,
                   COUNT(*) FILTER (WHERE promedio >= %s AND promedio < %s) AS amarillo,
                   COUNT(*) FILTER (WHERE promedio < %s) AS rojo,
                   COUNT(*) FILTER (WHERE promedio IS NULL) AS sin_evaluar
            FROM promedio_alumno
            GROUP BY aula_id
        )
        SELECT au.id, au.nombre, au.docente_id,
               COALESCE(s.alumnos, 0), COALESCE(p.evaluaciones, 0),
               p.general, p.lectura, p.escritura, p.oralidad,
               COALESCE(s.verde, 0), COALESCE(s.amarillo, 0), COALESCE(s.rojo, 0), COALESCE(s.sin_evaluar, 0)
        FROM aulas au
        LEFT JOIN semaforo s ON s.aula_id = au.id
        LEFT JOIN por_competencia p ON p.aula_id = au.id
        ORDER BY au.nombre
    """
    with connection.cursor() as cursor:
        cursor.execute(sql, [colegio_id, UMBRAL_VERDE, UMBRAL_AMARILLO, UMBRAL_VERDE, UMBRAL_AMARILLO])
        filas = cursor.fetchall()

    # Nombres de los docentes (tabla usuarios de Django)
    from usuarios.models import Usuario
    docentes = {u.id: (u.get_full_name() or u.username)
                for u in Usuario.objects.filter(id__in=[f[2] for f in filas])}

    return [
        {
            'aula_id': aula_id,
            'nombre': nombre,
            'docente': docentes.get(docente_id),
            'alumnos': alumnos,
            'evaluaciones': evaluaciones,
            'promedio_general': _numero(general),
            'promedios': {'lectura': _numero(lectura), 'escritura': _numero(escritura),
                          'oralidad': _numero(oralidad)},
            'semaforo': {'verde': verde, 'amarillo': amarillo, 'rojo': rojo, 'sin_evaluar': sin_evaluar},
        }
        for (aula_id, nombre, docente_id, alumnos, evaluaciones, general, lectura, escritura, oralidad,
             verde, amarillo, rojo, sin_evaluar) in filas
    ]