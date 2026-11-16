package com.proyecto.movil.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.proyecto.movil.model.NivelMapa
import com.proyecto.movil.ui.theme.ColoresAventura
import com.proyecto.movil.view.componentes.AvisoNivelBloqueado
import com.proyecto.movil.view.componentes.BarraEstudiante
import com.proyecto.movil.view.componentes.DialogoSalir
import com.proyecto.movil.view.componentes.RecorridoNiveles
import com.proyecto.movil.viewmodel.MapaUiState
import com.proyecto.movil.viewmodel.MapaViewModel
import kotlinx.coroutines.delay

// Inicio después del PIN: barra del alumno + mapa de aventura (T-210, US-05)
@Composable
fun PantallaMapa(
    onSalir: (codigoAula: String?) -> Unit,
    onAbrirNivel: (nivelId: Int) -> Unit = {},
    viewModel: MapaViewModel = viewModel()
) {
    val sesion = viewModel.sesion
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val aviso by viewModel.aviso.collectAsStateWithLifecycle()

    // T-237: "Salir" primero pregunta; rememberSaveable lo mantiene abierto si se gira la tablet
    var preguntandoSalir by rememberSaveable { mutableStateOf(false) }
    // Evita tocar "Sí, salir" dos veces mientras se avisa al backend (hasta 3 s sin conexión)
    var saliendo by remember { mutableStateOf(false) }

    // Se pide el mapa una vez al abrir la pantalla
    LaunchedEffect(Unit) { viewModel.cargarMapa() }

    // T-240: el mensaje del nivel bloqueado se oculta solo (si se toca otro, el tiempo empieza de nuevo)
    LaunchedEffect(aviso) {
        if (aviso != null) {
            delay(MapaViewModel.DURACION_AVISO_MS)
            viewModel.cerrarAviso()
        }
    }

    Box(Modifier.fillMaxSize().background(ColoresAventura.pasto)) {
        Column(Modifier.fillMaxSize()) {
            BarraEstudiante(
                nombre = sesion?.nombre ?: "",
                avatarUrl = sesion?.avatarUrl ?: "",
                onSalir = { if (!saliendo) preguntandoSalir = true }
            )
            ContenidoMapa(
                estado = estado,
                onReintentar = { viewModel.cargarMapa() },
                // T-215: un nivel disponible abre su detalle; uno bloqueado muestra el aviso (T-240)
                onAnimacionTerminada = viewModel::animacionDesbloqueoMostrada,
                onNivelTocado = { nivel ->
                    if (nivel.disponible) onAbrirNivel(nivel.id) else viewModel.tocarNivel(nivel)
                }
            )
        }
        aviso?.let { mensaje ->
            AvisoNivelBloqueado(
                mensaje = mensaje,
                onCerrar = viewModel::cerrarAviso,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    if (preguntandoSalir) {
        DialogoSalir(
            nombre = sesion?.nombre ?: "",
            avatarUrl = sesion?.avatarUrl ?: "",
            onConfirmar = {
                preguntandoSalir = false
                saliendo = true
                viewModel.salir(onSalir)
            },
            onCancelar = { preguntandoSalir = false }
        )
    }
}

// Separado de PantallaMapa para poder verlo en Preview y probarlo sin ViewModel (T-212)
@Composable
fun ContenidoMapa(
    estado: MapaUiState,
    onReintentar: () -> Unit,
    onNivelTocado: (NivelMapa) -> Unit = {},
    onAnimacionTerminada: () -> Unit = {}
) {
    when (estado) {
        is MapaUiState.Cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(64.dp).testTag("cargando_mapa"))
        }
        is MapaUiState.Error -> ErrorMapa(estado.mensaje, onReintentar)
        is MapaUiState.Exito -> RecorridoNiveles(estado.mapa, onNivelTocado, estado.recienDesbloqueados, onAnimacionTerminada)
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
