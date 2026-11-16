package com.proyecto.movil.view

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import com.proyecto.movil.model.ErrorOrtografico
import com.proyecto.movil.ui.theme.ColoresAventura

// T-331 (US-19): la palabra con error se ve roja, subrayada y con un fondo rojo suave
val ESTILO_ERROR = SpanStyle(
    color = ColoresAventura.Rojo,
    textDecoration = TextDecoration.Underline,
    background = ColoresAventura.Rojo.copy(alpha = 0.12f)
)

// Solo marca los errores cuya palabra sigue en ese lugar del texto
fun marcarErrores(texto: String, errores: List<ErrorOrtografico>, estilo: SpanStyle = ESTILO_ERROR): AnnotatedString =
    buildAnnotatedString {
        append(texto)
        errores.forEach { e ->
            val enSuLugar = e.inicio in 0 until e.fin && e.fin <= texto.length && texto.substring(e.inicio, e.fin) == e.palabra
            if (enSuLugar) addStyle(estilo, e.inicio, e.fin)
        }
    }

// Cambia solo cómo se ve el texto: lo que escribe el niño queda igual
class SubrayarErrores(private val errores: List<ErrorOrtografico>) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(marcarErrores(text.text, errores), OffsetMapping.Identity)

    override fun equals(other: Any?) = other is SubrayarErrores && other.errores == errores

    override fun hashCode() = errores.hashCode()
}
