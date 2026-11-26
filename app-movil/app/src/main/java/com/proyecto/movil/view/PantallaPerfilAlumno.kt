package com.proyecto.movil.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
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
import coil.compose.AsyncImage
import com.proyecto.movil.ui.theme.ColoresAventura
import com.proyecto.movil.view.componentes.BotonAventura
import com.proyecto.movil.view.componentes.EstiloBoton

// Pestaña "Perfil": su avatar, su nombre y el botón "Salir" (antes estaba en la barra de arriba, T-208)
@Composable
fun ContenidoPerfilAlumno(nombre: String, avatarUrl: String, completados: Int, totalNiveles: Int, onSalir: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(ColoresAventura.NaranjaBorde)
                .padding(bottom = 6.dp)
                .clip(CircleShape)
                .background(ColoresAventura.Naranja),
            contentAlignment = Alignment.Center
        ) {
            Text(nombre.take(1).uppercase(), fontSize = 56.sp, fontWeight = FontWeight.Black, color = Color(0xFF2B1A00))
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Avatar de $nombre",
                modifier = Modifier.fillMaxSize().padding(6.dp).clip(CircleShape)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(nombre, fontSize = 30.sp, fontWeight = FontWeight.Black, color = ColoresAventura.TextoOscuro, textAlign = TextAlign.Center)
        Text(
            "Completaste $completados de $totalNiveles niveles",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = ColoresAventura.TextoSuave,
            modifier = Modifier.testTag("perfil_avance")
        )
        Spacer(Modifier.height(32.dp))
        BotonAventura(
            "Salir",
            onClick = onSalir,
            estilo = EstiloBoton.Secundario,
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().testTag("boton_salir")
        )
    }
}
