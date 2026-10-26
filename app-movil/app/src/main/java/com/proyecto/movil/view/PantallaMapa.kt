package com.proyecto.movil.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.proyecto.movil.model.MapaRespuesta
import com.proyecto.movil.model.NivelMapa
import com.proyecto.movil.model.TipoReto
import com.proyecto.movil.ui.theme.AppMovilTheme
import com.proyecto.movil.ui.theme.ColoresAventura
import com.proyecto.movil.viewmodel.MapaUiState
import com.proyecto.movil.viewmodel.MapaViewModel

// Inicio después del PIN: barra del alumno + mapa de aventura (T-210, US-05)
@Composable
fun PantallaMapa(
    onSalir: (codigoAula: String?) -> Unit,
    viewModel: MapaViewModel = viewModel()
) {
    val sesion = viewModel.sesion
    val estado by viewModel.uiState.collectAsState()

    // Se pide el mapa una vez al abrir la pantalla
    LaunchedEffect(Unit) { viewModel.cargarMapa() }

    Column(Modifier.fillMaxSize().background(ColoresAventura.pasto)) {
        BarraEstudiante(
            nombre = sesion?.nombre ?: "",
            avatarUrl = sesion?.avatarUrl ?: "",
            onSalir = { viewModel.salir(onSalir) }
        )
        ContenidoMapa(estado = estado, onReintentar = { viewModel.cargarMapa() })
    }
}

// Separado de PantallaMapa para poder verlo en Preview y probarlo sin ViewModel (T-212)
@Composable
fun ContenidoMapa(estado: MapaUiState, onReintentar: () -> Unit) {
    when (estado) {
        is MapaUiState.Cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(64.dp).testTag("cargando_mapa"))
        }
        is MapaUiState.Error -> ErrorMapa(estado.mensaje, onReintentar)
        is MapaUiState.Exito -> RecorridoNiveles(estado.mapa)
    }
}

@Composable
private fun ErrorMapa(mensaje: String, onReintentar: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            mensaje,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = ColoresAventura.TextoOscuro,
            modifier = Modifier.testTag("error_mapa")
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onReintentar,
            colors = ButtonDefaults.buttonColors(containerColor = ColoresAventura.Naranja),
            modifier = Modifier.height(64.dp).testTag("boton_reintentar")
        ) {
            Text("Intentar de nuevo", fontSize = 22.sp, color = Color.White)
        }
    }
}

// T-210: resumen de avance arriba y los niveles en un camino en zigzag
@Composable
fun RecorridoNiveles(mapa: MapaRespuesta) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        ResumenAvance(mapa.completados, mapa.totalNiveles, mapa.puntos)
        Spacer(Modifier.height(24.dp))

        val niveles = mapa.niveles.sortedBy { it.orden }
        niveles.forEachIndexed { indice, nivel ->
            // Izquierda, centro, derecha, centro...: el camino serpentea como en un juego
            val alineacion = when (indice % 4) {
                0 -> Alignment.Start
                2 -> Alignment.End
                else -> Alignment.CenterHorizontally
            }
            NodoNivel(nivel, Modifier.align(alineacion))
            if (indice < niveles.lastIndex) {
                Camino(Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

@Composable
private fun ResumenAvance(completados: Int, total: Int, puntos: Int) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(16.dp)
            .testTag("resumen_mapa")
    ) {
        Text(
            "Completaste $completados de $total niveles",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = ColoresAventura.TextoOscuro
        )
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else completados.toFloat() / total },
            color = ColoresAventura.Turquesa,
            trackColor = ColoresAventura.Cielo,
            modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp))
        )
        Spacer(Modifier.height(8.dp))
        Text("⭐ $puntos puntos", fontSize = 20.sp, color = ColoresAventura.TextoOscuro)
    }
}

// Un nivel: círculo grande con su número y el nombre debajo.
// Los estilos por estado (candado y gris) se agregan en T-239.
@Composable
private fun NodoNivel(nivel: NivelMapa, modifier: Modifier = Modifier) {
    Column(
        modifier
            .width(180.dp)
            .testTag("nivel_${nivel.orden}")
            .semantics { contentDescription = "Nivel ${nivel.orden}: ${nivel.nombre}" },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(colorDelReto(nivel.reto))
                .border(5.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("${nivel.orden}", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            nivel.nombre,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = ColoresAventura.TextoOscuro
        )
    }
}

// Puntitos entre un nivel y el siguiente
@Composable
private fun Camino(modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        repeat(3) {
            Box(Modifier.padding(3.dp).size(10.dp).clip(CircleShape).background(Color.White))
        }
    }
}

// Cada tipo de reto tiene su color, para que el niño los reconozca
private fun colorDelReto(reto: TipoReto): Color = when (reto) {
    TipoReto.LECTURA -> ColoresAventura.Turquesa
    TipoReto.ESCRITURA -> ColoresAventura.Naranja
    TipoReto.ORALIDAD -> Color(0xFF4EA8DE)
    TipoReto.DESCONOCIDO -> ColoresAventura.Gris
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

@Preview(widthDp = 800, heightDp = 1200, showBackground = true)
@Composable
fun RecorridoNivelesPreview() {
    val tipos = listOf("lectura", "escritura", "oralidad")
    val niveles = (1..6).map { n ->
        NivelMapa(n, n, "Nivel $n", tipos[(n - 1) % 3], null, if (n == 1) "desbloqueado" else "bloqueado")
    }
    AppMovilTheme(dynamicColor = false) {
        Box(Modifier.background(ColoresAventura.pasto)) {
            RecorridoNiveles(MapaRespuesta(1, "Ana", "Pérez", 6, 0, 0, niveles))
        }
    }
}
