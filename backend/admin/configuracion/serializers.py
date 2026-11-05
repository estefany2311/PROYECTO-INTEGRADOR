from decimal import Decimal

from rest_framework import serializers

from .models import ConfiguracionEvaluacion, HistorialConfiguracion

# Límites permitidos (los mismos del modelo y de la pantalla)
PRECISION = (0, 100)
PAUSAS = (Decimal('0'), Decimal('10'))
PALABRAS = (10, 200)


def mensajes(minimo, maximo, unidad):
    texto = f'Debe estar entre {minimo} y {maximo} {unidad}.'
    return {'min_value': texto, 'max_value': texto, 'invalid': 'Ingresa un número válido.',
            'required': 'Este campo es obligatorio.', 'null': 'Este campo es obligatorio.'}


class ConfiguracionEvaluacionSerializer(serializers.ModelSerializer):
    actualizado_por = serializers.CharField(source='actualizado_por.username', read_only=True, default=None)

    # T-249: rangos con mensajes claros
    umbral_precision = serializers.IntegerField(
        min_value=PRECISION[0], max_value=PRECISION[1], error_messages=mensajes(*PRECISION, '%%'))
    tolerancia_pausas = serializers.DecimalField(
        max_digits=3, decimal_places=1, min_value=PAUSAS[0], max_value=PAUSAS[1],
        error_messages={**mensajes(0, 10, 'segundos'),
                        'max_decimal_places': 'Usa como máximo un decimal (por ejemplo 2.5).',
                        'max_digits': 'Debe estar entre 0 y 10 segundos.'})
    palabras_por_minuto = serializers.IntegerField(
        min_value=PALABRAS[0], max_value=PALABRAS[1], error_messages=mensajes(*PALABRAS, 'palabras por minuto'))

    class Meta:
        model = ConfiguracionEvaluacion
        fields = ['grado', 'umbral_precision', 'tolerancia_pausas', 'palabras_por_minuto',
                  'actualizado_por', 'actualizado_en']
        # El grado no se cambia: cada fila es un grado fijo (1.° a 6.°)
        read_only_fields = ['grado', 'actualizado_por', 'actualizado_en']

    def validate(self, attrs):
        """T-249: a mayor grado, mayor exigencia (precisión y palabras por minuto)."""
        if self.instance is None:
            return attrs
        grado = self.instance.grado
        anterior = ConfiguracionEvaluacion.objects.filter(grado=grado - 1).first()
        siguiente = ConfiguracionEvaluacion.objects.filter(grado=grado + 1).first()
        errores = {}
        for campo, nombre in (('umbral_precision', 'la precisión'), ('palabras_por_minuto', 'las palabras por minuto')):
            if campo not in attrs:
                continue
            valor = attrs[campo]
            if anterior and valor < getattr(anterior, campo):
                errores[campo] = (f'En {grado}.° grado {nombre} no puede ser menor que en {grado - 1}.° '
                                  f'({getattr(anterior, campo)}).')
            elif siguiente and valor > getattr(siguiente, campo):
                errores[campo] = (f'En {grado}.° grado {nombre} no puede ser mayor que en {grado + 1}.° '
                                  f'({getattr(siguiente, campo)}).')
        if errores:
            raise serializers.ValidationError(errores)
        return attrs



class HistorialConfiguracionSerializer(serializers.ModelSerializer):
    usuario = serializers.CharField(source='usuario.username', read_only=True, default=None)
    campo_texto = serializers.CharField(source='get_campo_display', read_only=True)

    class Meta:
        model = HistorialConfiguracion
        fields = ['id', 'grado', 'campo', 'campo_texto', 'valor_anterior', 'valor_nuevo', 'usuario', 'fecha']