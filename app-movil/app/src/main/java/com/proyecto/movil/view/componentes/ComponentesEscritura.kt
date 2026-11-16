package com.proyecto.movil.view.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proyecto.movil.ui.theme.ColoresAventura
import com.proyecto.movil.viewmodel.ConsignaUiState

// Barra de arriba: volver, nombre del nivel y la etiqueta "Escritura" (como en el mockup).
// T-323: la usa también el editor
@Composable
fun CabeceraEscritura(titulo: String?, onVolver: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onVolver, modifier = Modifier.testTag("boton_volver_consigna")) {
            Text("←  Volver", fontSize = 20.sp, color = ColoresAventura.TextoOscuro)
        }
        Text(
            titulo ?: "",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = ColoresAventura.TextoOscuro,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.weight(1f).testTag("titulo_consigna")
        )
        Text(
            "Escritura",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(ColoresAventura.Naranja)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

// T-323: la usa también el editor
@Composable
fun ErrorConsigna(estado: ConsignaUiState.Error, onReintentar: () -> Unit, onVolver: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 32.dp)) {
        Koala(tamano = 160.dp, saludando = false)
        Spacer(Modifier.height(16.dp))
        Text(
            estado.mensaje,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = ColoresAventura.TextoOscuro,
            modifier = Modifier.testTag("error_consigna")
        )
        Spacer(Modifier.height(24.dp))
        if (estado.puedeReintentar) {
            Button(
                onClick = onReintentar,
                colors = ButtonDefaults.buttonColors(containerColor = ColoresAventura.Naranja),
                modifier = Modifier.height(56.dp).testTag("boton_reintentar_consigna")
            ) { Text("Intentar de nuevo", fontSize = 20.sp, color = Color.White) }
        } else {
            OutlinedButton(onClick = onVolver, modifier = Modifier.height(56.dp).testTag("boton_volver_error_consigna")) {
                Text("Volver", fontSize = 20.sp, color = ColoresAventura.TextoOscuro)
            }
        }
    }
}
