from django.core.management.base import BaseCommand, CommandError

from catalogo import biblioteca
from catalogo.models import RecursoOficial


class Command(BaseCommand):
    help = ('Sincroniza los recursos de primaria en castellano para estudiantes '
            'desde la Biblioteca Escolar Digital.')

    def add_arguments(self, parser):
        parser.add_argument('--sin-pdf', action='store_true',
                            help='No busca el enlace del PDF (más rápido).')
        parser.add_argument('--limite', type=int, default=None,
                            help='Revisa solo los primeros N recursos (para pruebas).')
        parser.add_argument('--limpiar', action='store_true',
                            help='Borra los recursos guardados que ya no cumplen el filtro '
                                 '(solo si no se usaron para crear lecturas).')

    def handle(self, *args, **opciones):
        nuevos = actualizados = omitidos = 0
        encontrados = set()

        self.stdout.write('Consultando la biblioteca... (tarda 1 o 2 minutos)')
        try:
            for numero, item in enumerate(biblioteca.recursos(), start=1):
                if opciones['limite'] and numero > opciones['limite']:
                    break
                if not biblioteca.es_util(item):
                    omitidos += 1
                    continue

                meta = item.get('metadata', {})
                titulo = item.get('name', '')
                datos = {
                    'titulo': titulo[:300],
                    'descripcion': biblioteca.primero(meta, 'dc.description.abstract')
                                   or biblioteca.primero(meta, 'dc.description'),
                    'grado': biblioteca.detectar_grado(meta, titulo),
                    'competencia': biblioteca.detectar_competencia(meta, titulo),
                    'temas': biblioteca.valores(meta, 'dc.subject'),
                    'url_recurso': biblioteca.enlace_recurso(item),
                }
                if not opciones['sin_pdf']:
                    datos['url_pdf'] = biblioteca.enlace_pdf(item)

                _, creado = RecursoOficial.objects.update_or_create(
                    uuid=item['uuid'], defaults=datos)
                encontrados.add(item['uuid'])
                if creado:
                    nuevos += 1
                else:
                    actualizados += 1
        except biblioteca.ErrorBiblioteca as error:
            raise CommandError(str(error))

        if opciones['limpiar']:
            sobrantes = (RecursoOficial.objects
                         .exclude(uuid__in=encontrados)
                         .filter(lectura__isnull=True))
            borrados, _ = sobrantes.delete()
            self.stdout.write(f'Borrados (ya no cumplen el filtro): {borrados}')

        self.stdout.write(self.style.SUCCESS(
            f'Listo. Nuevos: {nuevos} · Actualizados: {actualizados} · '
            f'Omitidos (otro nivel, otra lengua o material docente): {omitidos}'))