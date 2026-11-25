package com.proyecto.movil.view.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proyecto.movil.model.PalabraLeida
import com.proyecto.movil.model.limpiarPalabra
import com.proyecto.movil.ui.theme.ColoresAventura

// T-429 (US-14): el texto que leyó, con las palabras por mejorar en rojo y subrayadas (como en T-331).
// T-430: al tocar una de esas palabras, se escucha.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextoConPalabrasPorMejorar(
    palabras: List<PalabraLeida>,
    palabraElegida: String?,
    onEscuchar: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(20.dp)
            .testTag("texto_leido")
    ) {
        palabras.forEach { palabra ->
            if (palabra.mejorar) {
                val elegida = limpiarPalabra(palabra.texto).equals(palabraElegida, ignoreCase = true)
                Text(
                    palabra.texto,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColoresAventura.Rojo,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ColoresAventura.Rojo.copy(alpha = if (elegida) 0.30f else 0.12f))
                        .clickable(role = Role.Button) { onEscuchar(palabra.texto) }
                        .padding(horizontal = 4.dp)
                )
            } else {
                Text(palabra.texto, fontSize = 24.sp, color = ColoresAventura.TextoOscuro)
            }
        }
    }
}

// T-428 (US-14): las palabras que debe practicar, sin repetir; cada una se puede tocar para escucharla (T-430)
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListaPalabrasPorMejorar(
    palabras: List<String>,
    palabraElegida: String?,
    onEscuchar: (String) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().testTag("palabras_por_mejorar")
    ) {
        Text("Palabras por mejorar", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = ColoresAventura.Naranja)
        Spacer(Modifier.height(4.dp))
        Text("Toca una palabra para escucharla", fontSize = 20.sp, color = ColoresAventura.TextoOscuro)
        Spacer(Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            palabras.forEach { palabra ->
                val elegida = palabra.equals(palabraElegida, ignoreCase = true)
                Surface(
                    onClick = { onEscuchar(palabra) },
                    shape = RoundedCornerShape(28.dp),
                    color = if (elegida) ColoresAventura.Naranja else Color.White,
                    border = BorderStroke(3.dp, ColoresAventura.Naranja),
                    modifier = Modifier
                        .heightIn(min = 56.dp)
                        .semantics { contentDescription = "Escuchar $palabra" }
                        .testTag("chip_$palabra")
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                        Text(
                            palabra,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (elegida) Color.White else ColoresAventura.TextoOscuro
                        )
                    }
                }
            }
        }
    }
}
