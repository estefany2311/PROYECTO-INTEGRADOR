package com.proyecto.movil.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// T-440: colores fijos de la aventura. Antes se usaba el "color dinámico" de Android 12+,
// que toma los colores del fondo de pantalla de la tablet (por eso se veía rosado o lila).
private val EsquemaAventura = lightColorScheme(
    primary = ColoresAventura.Amarillo,
    onPrimary = ColoresAventura.AmarilloTexto,
    primaryContainer = ColoresAventura.AmarilloSuave,
    onPrimaryContainer = ColoresAventura.AmarilloTexto,
    secondary = ColoresAventura.Turquesa,
    onSecondary = ColoresAventura.TextoOscuro,
    tertiary = ColoresAventura.Naranja,
    onTertiary = ColoresAventura.TextoOscuro,
    background = ColoresAventura.Cielo,
    onBackground = ColoresAventura.TextoOscuro,
    surface = ColoresAventura.Cielo,
    onSurface = ColoresAventura.TextoOscuro,
    surfaceVariant = ColoresAventura.AmarilloSuave,
    onSurfaceVariant = ColoresAventura.TextoSuave,
    outline = ColoresAventura.AmarilloBorde,
    outlineVariant = ColoresAventura.Linea,
    error = ColoresAventura.Rojo
)

// darkTheme y dynamicColor se dejan para no cambiar a quien ya los pasa; la app siempre va en claro
@Suppress("UNUSED_PARAMETER")
@Composable
fun AppMovilTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EsquemaAventura,
        typography = Typography,
        content = content
    )
}
