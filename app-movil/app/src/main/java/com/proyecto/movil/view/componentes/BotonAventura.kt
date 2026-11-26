package com.proyecto.movil.view.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proyecto.movil.ui.theme.ColoresAventura

// T-440: los colores de cada tipo de botón: relleno, franja de abajo (relieve) y texto
enum class EstiloBoton(val fondo: Color, val borde: Color, val texto: Color) {
    Principal(ColoresAventura.Amarillo, ColoresAventura.AmarilloBorde, ColoresAventura.AmarilloTexto),
    Exito(ColoresAventura.Turquesa, ColoresAventura.TurquesaBorde, Color(0xFF08332F)),
    Alterno(ColoresAventura.Naranja, ColoresAventura.NaranjaBorde, Color(0xFF2B1A00)),
    Oralidad(ColoresAventura.Azul, ColoresAventura.AzulBorde, Color(0xFF0B2A40)),
    Secundario(ColoresAventura.Tarjeta, ColoresAventura.Linea, ColoresAventura.TextoOscuro)
}

private val RELIEVE = 6.dp

// T-440: botón "de juguete" con relieve: al tocarlo se hunde. Deshabilitado se ve plano y gris
@Composable
fun BotonAventura(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    estilo: EstiloBoton = EstiloBoton.Principal,
    enabled: Boolean = true,
    contenido: (@Composable RowScope.() -> Unit)? = null
) {
    val toques = remember { MutableInteractionSource() }
    val presionado by toques.collectIsPressedAsState()
    val hundido = if (presionado && enabled) RELIEVE else 0.dp
    val fondo = if (enabled) estilo.fondo else ColoresAventura.Bloqueado
    val borde = if (enabled) estilo.borde else ColoresAventura.Gris
    val colorTexto = if (enabled) estilo.texto else ColoresAventura.BloqueadoIcono
    val forma = RoundedCornerShape(18.dp)

    // La franja de abajo es el fondo de afuera; el botón de adentro baja al presionarlo
    Box(
        modifier = modifier
            .clip(forma)
            .background(borde)
            .clickable(
                interactionSource = toques,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(bottom = RELIEVE)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .offset(y = hundido)
                .clip(forma)
                .background(fondo)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            if (contenido != null) {
                contenido()
            } else {
                Text(
                    texto,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = colorTexto,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
