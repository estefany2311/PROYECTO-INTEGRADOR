package com.proyecto.movil.view.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.proyecto.movil.ui.theme.AppMovilTheme
import com.proyecto.movil.ui.theme.ColoresAventura

@Composable
fun DialogoSalir(
    nombre: String,
    avatarUrl: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    val pantallaBaja = LocalConfiguration.current.screenHeightDp < ALTURA_PANTALLA_BAJA
    val tamanoAvatar = if (pantallaBaja) 64.dp else 96.dp

    // usePlatformDefaultWidth = false: Android dejaba el diálogo angosto y "No, quedarme" no cabía
    Dialog(
        onDismissRequest = onCancelar,            // tocar fuera o "atrás" = quedarse
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            modifier = Modifier
                .padding(24.dp)                    // margen con los bordes de la pantalla
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .testTag("dialogo_salir")
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = if (pantallaBaja) 16.dp else 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Avatar de $nombre",
                    modifier = Modifier
                        .size(tamanoAvatar)
                        .clip(CircleShape)
                        .background(ColoresAventura.Cielo)
                )
                Spacer(Modifier.height(if (pantallaBaja) 8.dp else 16.dp))
                Text(
                    "¿Salir, $nombre?",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = ColoresAventura.TextoOscuro
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Así otro compañero puede usar la tablet.",
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    color = ColoresAventura.TextoOscuro
                )
                Spacer(Modifier.height(if (pantallaBaja) 16.dp else 24.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onCancelar,
                        contentPadding = PADDING_BOTON,
                        modifier = Modifier.weight(1f).height(56.dp).testTag("boton_cancelar_salir")
                    ) {
                        Text("No, quedarme", fontSize = 18.sp, color = ColoresAventura.TextoOscuro, maxLines = 1)
                    }
                    Button(
                        onClick = onConfirmar,
                        colors = ButtonDefaults.buttonColors(containerColor = ColoresAventura.Naranja),
                        contentPadding = PADDING_BOTON,
                        modifier = Modifier.weight(1f).height(56.dp).testTag("boton_confirmar_salir")
                    ) {
                        Text("Sí, salir", fontSize = 18.sp, color = Color.White, maxLines = 1)
                    }
                }
            }
        }
    }
}

// Menos de 480 dp de alto = teléfono en horizontal
private const val ALTURA_PANTALLA_BAJA = 480

// Menos relleno que el normal (24 dp a cada lado) para que el texto entre en una línea
private val PADDING_BOTON = PaddingValues(horizontal = 12.dp)

@Preview(name = "¿Salir? · vertical", widthDp = 400, heightDp = 800, showBackground = true)
@Preview(name = "¿Salir? · horizontal", widthDp = 800, heightDp = 360, showBackground = true)
@Composable
fun DialogoSalirPreview() {
    AppMovilTheme(dynamicColor = false) {
        DialogoSalir(nombre = "Ana", avatarUrl = "", onConfirmar = {}, onCancelar = {})
    }
}
