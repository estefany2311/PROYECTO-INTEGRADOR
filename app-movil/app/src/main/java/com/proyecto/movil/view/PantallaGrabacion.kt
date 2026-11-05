package com.proyecto.movil.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proyecto.movil.ui.theme.ColoresAventura

/**
 * US-11: pantalla de grabación de la lectura. Se abre con "Estoy listo, grabar" (T-219).
 * T-313: primero se asegura el permiso del micrófono con la explicación de Kiko.
 * T-314 agrega aquí la grabación del audio (WAV, mono, 16 kHz).
 */
@Composable
fun PantallaGrabacion(nivelId: Int, onVolver: () -> Unit) {
    ConPermisoMicrofono(onAhoraNo = onVolver) {
        Column(
            Modifier.fillMaxSize().background(ColoresAventura.Cielo).padding(32.dp).testTag("grabacion_lista"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Koala(tamano = 180.dp)
            Spacer(Modifier.height(24.dp))
            Text(
                "¡Micrófono listo!",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = ColoresAventura.TextoOscuro
            )
            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = onVolver, modifier = Modifier.height(56.dp)) {
                Text("Volver", fontSize = 20.sp, color = ColoresAventura.TextoOscuro)
            }
        }
    }
}
