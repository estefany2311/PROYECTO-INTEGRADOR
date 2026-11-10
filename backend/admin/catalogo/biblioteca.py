"""
Cliente de la Biblioteca Escolar Digital (DSpace 7) del Ministerio de Educación del Perú.
Solo lee recursos públicos. Lo usa el comando sincronizar_recursos.
"""
import json
import re
import time
import urllib.error
import urllib.request

BASE = 'https://bibliotecaescolar.minedu.gob.pe'
API = f'{BASE}/server/api'
PAUSA = 0.3           # segundos entre páginas, para no saturar el servidor
TIEMPO_MAXIMO = 15    # segundos de espera por cada consulta

GRADOS = {
    'primer': 1, 'primero': 1, 'segundo': 2, 'tercer': 3, 'tercero': 3,
    'cuarto': 4, 'quinto': 5, 'sexto': 6,
}

IDIOMAS_CASTELLANO = ('es', 'spa', 'cas', 'es_pe', 'es-pe', 'castellano', 'español', 'espanol')

# Material que no es para el estudiante
NO_PARA_ESTUDIANTES = (
    'guía', 'guia', 'docente', 'maestro', 'maestra', 'orientaciones', 'manual',
    'lámina', 'lamina', 'rúbrica', 'rubrica', 'programación', 'programacion',
    'fascículo', 'fasciculo', 'planificación', 'planificacion', 'evaluación diagnóstica','diccionario',   # material de consulta, no es una lectura
)

# Lenguas originarias (con sus distintas formas de escribirse)
LENGUAS_ORIGINARIAS = (
    'quechua', 'kichwa', 'runasimi', 'aimara', 'aymara', 'jaqaru', 'cauqui',
    'ashaninka', 'asháninka', 'asheninka', 'ashéninka', 'awajún', 'awajun', 'aguaruna',
    'shipibo', 'shipibo-konibo', 'matsigenka', 'machiguenga', 'nomatsigenka', 'yine',
    'harakbut', 'wampis', 'shawi', 'kandozi', 'yanesha', 'kukama', 'kakataibo',
    'urarina', 'achuar', 'tikuna', 'ticuna', 'cashinahua', 'kakinte', 'mastanahua',
    'chamikuro', 'ese eja', 'bora', 'murui', 'secoya', 'maijuna', 'arabela',
    'matsés', 'matses', 'sharanahua', 'yaminahua', 'isconahua', 'iquitu',
    'amahuaca', 'nanti', 'omagua', 'ocaina', 'yagua', 'taushiro', 'madija', 'culina', 'chapra', 'matigenka', 'nahua',
)

# Colecciones EIB: se publican en lenguas originarias
COLECCIONES_EIB = (
    'colección de relatos', 'coleccion de relatos', 'nuestros cantos', 'cancionero',
    'retahílas divertidas', 'retahilas divertidas', 'colección de adivinanzas',
    'coleccion de adivinanzas','colección de poesías', 'coleccion de poesias', 'saberes de los pueblos', 'nuestros relatos',
)


class ErrorBiblioteca(Exception):
    """La biblioteca no respondió o respondió algo inesperado."""


def pedir_json(url):
    """Hace un GET a la API y devuelve el JSON como diccionario."""
    pedido = urllib.request.Request(url, headers={
        'Accept': 'application/json',
        'User-Agent': 'LecturaApp-Admin/1.0',
    })
    try:
        with urllib.request.urlopen(pedido, timeout=TIEMPO_MAXIMO) as respuesta:
            return json.loads(respuesta.read().decode('utf-8'))
    except (urllib.error.URLError, TimeoutError, ValueError) as error:
        raise ErrorBiblioteca(f'No se pudo consultar {url}: {error}') from error


# ---------- Lectura de metadatos ----------

def valores(metadata, campo):
    """Todos los valores de un campo, por ejemplo 'dc.subject'."""
    return [dato.get('value', '') for dato in metadata.get(campo, []) if dato.get('value')]


def primero(metadata, campo, defecto=''):
    """El primer valor de un campo, o el valor por defecto."""
    lista = valores(metadata, campo)
    return lista[0] if lista else defecto


def _titulo(metadata, titulo=''):
    return (titulo or primero(metadata, 'dc.title')).lower()


def _texto_completo(metadata):
    """Todos los metadatos juntos en minúsculas, para buscar palabras."""
    return ' '.join(
        dato.get('value', '') for lista in metadata.values() for dato in lista
    ).lower()


def _menciona(texto, palabras):
    """True si alguna palabra aparece completa (evita que 'bora' coincida con 'colaboración')."""
    return any(re.search(rf'(?<!\w){re.escape(p)}(?!\w)', texto) for p in palabras)


# ---------- Filtros ----------

def es_inicial(titulo):
    """Material de educación inicial (3 a 5 años): no es para primaria."""
    texto = titulo.lower()
    return 'inicial' in texto and 'primaria' not in texto


def es_primaria(metadata, titulo=''):
    if es_inicial(_titulo(metadata, titulo)):
        return False
    return 'primaria' in _titulo(metadata, titulo) or 'primaria' in _texto_completo(metadata)


def es_castellano(metadata, titulo=''):
    texto = _titulo(metadata, titulo)
    # 1) Si el título nombra una lengua originaria, no es castellano (aunque el idioma diga 'cas')
    if _menciona(texto, LENGUAS_ORIGINARIAS) or 'lengua originaria' in texto:
        return False
    # 2) Formato de los libros EIB: "... - 1. grado de Primaria - Madija"
    sufijo = re.search(r'grado de primaria\s*-\s*([^-]+)$', texto)
    if sufijo and 'castellano' not in sufijo.group(1):
        return False
    # 3) Colecciones que se publican en lenguas originarias
    if any(coleccion in texto for coleccion in COLECCIONES_EIB) and 'castellano' not in texto:
        return False
    # 4) Recién ahora miramos el idioma registrado
    idiomas = [i.lower() for i in valores(metadata, 'dc.language.iso')]
    if idiomas:
        return any(i in IDIOMAS_CASTELLANO for i in idiomas)
    return True


def es_para_estudiantes(item, metadata):
    texto = ' '.join([item.get('name', '')] + valores(metadata, 'dc.type')).lower()
    return not any(palabra in texto for palabra in NO_PARA_ESTUDIANTES)


def es_util(item):
    """Primaria + castellano + para estudiantes."""
    metadata = item.get('metadata', {})
    titulo = item.get('name', '')
    return (es_primaria(metadata, titulo)
            and es_castellano(metadata, titulo)
            and es_para_estudiantes(item, metadata))


# ---------- Clasificación ----------

def detectar_grado(metadata, titulo=''):
    """Devuelve el grado (1 a 6) o None si no se puede saber."""
    textos = [_titulo(metadata, titulo)] + [v.lower() for v in valores(metadata, 'dc.subject')]
    patrones = (
        r'comunicaci[oó]n\s+([1-6])\b',
        r'\b([1-6])\s*(?:\.|°|º|er|do|ro|to|vo)?\s*grado',
        r'grado\s*([1-6])\b',
    )
    for texto in textos:
        for patron in patrones:
            encontrado = re.search(patron, texto)
            if encontrado:
                return int(encontrado.group(1))
        for palabra, numero in GRADOS.items():
            if re.search(rf'\b{palabra}\s+grado', texto):
                return numero
    return None


def detectar_competencia(metadata, titulo=''):
    """Sugiere lectura, escritura u oralidad. '' si no está claro (el admin lo elige)."""
    texto = _titulo(metadata, titulo) + ' ' + ' '.join(valores(metadata, 'dc.subject')).lower()
    if any(p in texto for p in ('canto', 'canción', 'cancion', 'retahíla', 'retahila',
                                'adivinanza', 'trabalenguas', 'oralidad')):
        return 'oralidad'
    if any(p in texto for p in ('escritura', 'escribir', 'escribo', 'producción de textos')):
        return 'escritura'
    if any(p in texto for p in ('cuento', 'fábula', 'fabula', 'relato', 'leyenda', 'poema', 'mito')):
        return 'lectura'
    return ''


# ---------- Consultas ----------

def recursos(tamano=50):
    """Recorre todos los recursos de la biblioteca, página por página."""
    pagina, total_paginas = 0, 1
    while pagina < total_paginas:
        url = (f'{API}/discover/search/objects?dsoType=ITEM'
               f'&page={pagina}&size={tamano}')
        datos = pedir_json(url)
        resultado = datos.get('_embedded', {}).get('searchResult', {})
        total_paginas = resultado.get('page', {}).get('totalPages', 0)
        for objeto in resultado.get('_embedded', {}).get('objects', []):
            item = objeto.get('_embedded', {}).get('indexableObject')
            if item:
                yield item
        pagina += 1
        time.sleep(PAUSA)


def enlace_recurso(item):
    """Página pública del recurso en la biblioteca."""
    return f"{BASE}/items/{item.get('uuid', '')}"


def enlace_pdf(item):
    """Enlace de descarga del PDF (bundle ORIGINAL). '' si no tiene."""
    try:
        bundles_url = item.get('_links', {}).get('bundles', {}).get('href')
        if not bundles_url:
            return ''
        bundles = pedir_json(bundles_url).get('_embedded', {}).get('bundles', [])
        for bundle in bundles:
            if bundle.get('name') != 'ORIGINAL':
                continue
            archivos_url = bundle.get('_links', {}).get('bitstreams', {}).get('href')
            archivos = pedir_json(archivos_url).get('_embedded', {}).get('bitstreams', [])
            for archivo in archivos:
                enlace = archivo.get('_links', {}).get('content', {}).get('href')
                if enlace:
                    return enlace
    except ErrorBiblioteca:
        pass
    return ''