"""US-50: archivos para descargar el reporte de colegios (Excel y PDF)."""
from io import BytesIO

from openpyxl import Workbook
from openpyxl.styles import Alignment, Font, PatternFill
from openpyxl.utils import get_column_letter

VERDE_YALLIQ = '0A8378'

# (título de la columna, ancho, cómo sacar el valor de una fila del reporte)
COLUMNAS_COLEGIOS = [
    ('Código modular', 15, lambda f: f['codigo_modular']),
    ('Colegio', 34, lambda f: f['nombre']),
    ('Región', 14, lambda f: f['region']),
    ('Alumnos', 10, lambda f: f['alumnos']),
    ('Evaluaciones', 13, lambda f: f['evaluaciones']),
    ('Lectura', 10, lambda f: f['promedios']['lectura']),
    ('Escritura', 10, lambda f: f['promedios']['escritura']),
    ('Oralidad', 10, lambda f: f['promedios']['oralidad']),
    ('Promedio general', 17, lambda f: f['promedio_general']),
    ('Logrado', 10, lambda f: f['semaforo']['verde']),
    ('En proceso', 11, lambda f: f['semaforo']['amarillo']),
    ('En inicio', 10, lambda f: f['semaforo']['rojo']),
    ('Sin evaluar', 12, lambda f: f['semaforo']['sin_evaluar']),
]


def excel_colegios(filas):
    """T-358: reporte por colegio en Excel (.xlsx). Devuelve los bytes del archivo."""
    libro = Workbook()
    hoja = libro.active
    hoja.title = 'Colegios'

    # Fila de títulos: verde Yalliq con letra blanca
    hoja.append([titulo for titulo, _, _ in COLUMNAS_COLEGIOS])
    for celda in hoja[1]:
        celda.font = Font(bold=True, color='FFFFFF')
        celda.fill = PatternFill('solid', fgColor=VERDE_YALLIQ)
        celda.alignment = Alignment(horizontal='center', vertical='center', wrap_text=True)

    # Una fila por colegio (un promedio sin datos queda como celda vacía)
    for fila in filas:
        hoja.append([valor(fila) for _, _, valor in COLUMNAS_COLEGIOS])

    for numero, (_, ancho, _) in enumerate(COLUMNAS_COLEGIOS, start=1):
        hoja.column_dimensions[get_column_letter(numero)].width = ancho
    hoja.freeze_panes = 'A2'          # los títulos quedan fijos al bajar

    salida = BytesIO()
    libro.save(salida)
    return salida.getvalue()