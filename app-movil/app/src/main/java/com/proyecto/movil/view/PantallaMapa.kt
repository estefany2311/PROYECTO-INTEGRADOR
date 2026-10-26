package com.proyecto.movil.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.proyecto.movil.ui.theme.AppMovilTheme
import com.proyecto.movil.ui.theme.ColoresAventura
import com.proyecto.movil.viewmodel.MapaViewModel

// Inicio después del PIN. En T-210 se agrega el mapa de aventura debajo de la barra.
@Composable
fun PantallaMapa(
    onSalir: (codigoAula: String?) -> Unit,
    viewModel: MapaViewModel = viewModel()
) {
    val sesion = viewModel.sesion
    Column(Modifier.fillMaxSize().background(ColoresAventura.pasto)) {
        BarraEstudiante(
            nombre = sesion?.nombre ?: "",
            avatarUrl = sesion?.avatarUrl ?: "",
            onSalir = { viewModel.salir(onSalir) }
        )
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "¡Hola, ${sesion?.nombre ?: ""}!\nTu aventura empieza aquí.",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = ColoresAventura.TextoOscuro
            )
        }
    }
}

// T-208: barra de arriba con el avatar, el nombre y el botón "Salir" (US-04)
@Composable
fun BarraEstudiante(nombre: String, avatarUrl: String, onSalir: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = "Avatar de $nombre",
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(ColoresAventura.Cielo)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            nombre,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = ColoresAventura.TextoOscuro,
            modifier = Modifier.weight(1f)
        )
        // Botón grande (56dp de alto) para que el niño lo toque fácil
        OutlinedButton(
            onClick = onSalir,
            modifier = Modifier.height(56.dp).testTag("boton_salir")
        ) {
            Text("Salir", fontSize = 22.sp, color = ColoresAventura.TextoOscuro)
        }
    }
}

@Preview(widthDp = 800, heightDp = 300, showBackground = true)
@Composable
fun BarraEstudiantePreview() {
    AppMovilTheme(dynamicColor = false) {
        BarraEstudiante(nombre = "Ana", avatarUrl = "", onSalir = {})
    }
}