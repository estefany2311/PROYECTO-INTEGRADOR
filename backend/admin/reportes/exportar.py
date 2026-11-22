"""US-50: archivos para descargar el reporte de colegios (Excel y PDF)."""
from io import BytesIO

from openpyxl import Workbook
from openpyxl.styles import Alignment, Font, PatternFill
from openpyxl.utils import get_column_letter

VERDE_YALLIQ = '0A8378'
TITULO = 'Reporte de rendimiento por colegio'


def describir_filtros(filtros=None):
    """T-360: texto de los filtros aplicados, para el encabezado del archivo.

    filtros: {'colegio': nombre o None, 'grado': 2 o None, 'desde': date o None, 'hasta': date o None}
    """
    filtros = filtros or {}
    desde, hasta = filtros.get('desde'), filtros.get('hasta')
    if desde and hasta:
        periodo = f'del {desde:%d/%m/%Y} al {hasta:%d/%m/%Y}'
    elif desde:
        periodo = f'desde el {desde:%d/%m/%Y}'
    elif hasta:
        periodo = f'hasta el {hasta:%d/%m/%Y}'
    else:
        periodo = 'Todo el periodo'
    partes = [
        filtros.get('colegio') or 'Todos los colegios',
        f"{filtros['grado']}.° grado" if filtros.get('grado') else 'Todos los grados',
        periodo,
    ]
    return ' · '.join(partes)


def encabezado(generado, filtros=None):
    """T-360: las líneas de arriba del archivo: título, fecha de generación y filtros."""
    return [
        f'{TITULO} · Yalliq',
        f'Generado el {generado:%d/%m/%Y a las %H:%M}',
        f'Filtros: {describir_filtros(filtros)}',
    ]

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


def excel_colegios(filas, generado, filtros=None):
    """T-358: reporte por colegio en Excel (.xlsx). Devuelve los bytes del archivo.

    T-360: arriba van el título, la fecha de generación y los filtros aplicados.
    """
    libro = Workbook()
    hoja = libro.active
    hoja.title = 'Colegios'

    # T-360: encabezado (filas 1 a 3) y una fila en blanco
    titulo, fecha, texto_filtros = encabezado(generado, filtros)
    hoja.append([titulo])
    hoja['A1'].font = Font(bold=True, size=14, color=VERDE_YALLIQ)
    hoja.append([fecha])
    hoja.append([texto_filtros])
    for celda in ('A2', 'A3'):
        hoja[celda].font = Font(color='5E6B73')
    hoja.append([])

    # Fila de títulos: verde Yalliq con letra blanca
    hoja.append([titulo for titulo, _, _ in COLUMNAS_COLEGIOS])
    fila_titulos = hoja.max_row
    for celda in hoja[fila_titulos]:
        celda.font = Font(bold=True, color='FFFFFF')
        celda.fill = PatternFill('solid', fgColor=VERDE_YALLIQ)
        celda.alignment = Alignment(horizontal='center', vertical='center', wrap_text=True)

    # Una fila por colegio (un promedio sin datos queda como celda vacía)
    for fila in filas:
        hoja.append([valor(fila) for _, _, valor in COLUMNAS_COLEGIOS])

    for numero, (_, ancho, _) in enumerate(COLUMNAS_COLEGIOS, start=1):
        hoja.column_dimensions[get_column_letter(numero)].width = ancho
    hoja.freeze_panes = f'A{fila_titulos + 1}'   # el encabezado y los títulos quedan fijos al bajar

    salida = BytesIO()
    libro.save(salida)
    return salida.getvalue()


# ---------------------------------------------------------------------
# T-359: PDF
# ---------------------------------------------------------------------
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4, landscape
from reportlab.lib.styles import getSampleStyleSheet
from reportlab.lib.units import cm
from reportlab.platypus import Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle

# En el PDF no caben 13 columnas: se juntan los conteos del semáforo en una sola
COLUMNAS_PDF = ['Colegio', 'Región', 'Alumnos', 'Lectura', 'Escritura', 'Oralidad', 'General',
                'Logrado / Proceso / Inicio / Sin evaluar']


def _texto(valor):
    """Un promedio sin datos se muestra como guion."""
    return '—' if valor is None else str(valor)


def pdf_colegios(filas, generado, filtros=None):
    """T-359: reporte por colegio en PDF (A4 horizontal). Devuelve los bytes del archivo.

    T-360: debajo del título van la fecha de generación y los filtros aplicados.
    """
    salida = BytesIO()
    documento = SimpleDocTemplate(
        salida, pagesize=landscape(A4), title='Reporte de colegios · Yalliq',
        leftMargin=1.5 * cm, rightMargin=1.5 * cm, topMargin=1.5 * cm, bottomMargin=1.5 * cm,
    )
    estilos = getSampleStyleSheet()
    estilo_celda = estilos['BodyText']
    estilo_celda.fontSize = 9

    datos = [COLUMNAS_PDF]
    for f in filas:
        s = f['semaforo']
        datos.append([
            Paragraph(f"{f['nombre']}<br/><font size=7 color='#5E6B73'>{f['codigo_modular']}</font>", estilo_celda),
            f['region'],
            f['alumnos'],
            _texto(f['promedios']['lectura']),
            _texto(f['promedios']['escritura']),
            _texto(f['promedios']['oralidad']),
            _texto(f['promedio_general']),
            f"{s['verde']} / {s['amarillo']} / {s['rojo']} / {s['sin_evaluar']}",
        ])

    tabla = Table(datos, repeatRows=1, colWidths=[7 * cm, 3 * cm, 2 * cm, 2 * cm, 2 * cm, 2 * cm, 2 * cm, 6 * cm])
    tabla.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#' + VERDE_YALLIQ)),   # títulos en verde Yalliq
        ('TEXTCOLOR', (0, 0), (-1, 0), colors.white),
        ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
        ('FONTSIZE', (0, 0), (-1, -1), 9),
        ('ALIGN', (2, 0), (-1, -1), 'CENTER'),
        ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
        ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.white, colors.HexColor('#EEF6F4')]),   # filas alternadas
        ('GRID', (0, 0), (-1, -1), 0.5, colors.HexColor('#C9DFDA')),
        ('TOPPADDING', (0, 0), (-1, -1), 5),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 5),
    ]))

    _, fecha, texto_filtros = encabezado(generado, filtros)
    estilo_nota = estilos['Normal'].clone('nota', fontSize=9, textColor=colors.HexColor('#5E6B73'), alignment=1)
    contenido = [
        Paragraph(TITULO, estilos['Title']),
        Paragraph(fecha, estilo_nota),
        Paragraph(texto_filtros, estilo_nota),
        Spacer(1, 0.4 * cm),
        tabla if filas else Paragraph('No hay colegios activos.', estilos['BodyText']),
    ]
    documento.build(contenido)
    return salida.getvalue()