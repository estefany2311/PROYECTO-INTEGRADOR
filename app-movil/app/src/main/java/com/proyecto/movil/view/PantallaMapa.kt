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
import com.proyecto.movil.view.componentes.BarraAventura
import com.proyecto.movil.view.componentes.MenuInferior
import com.proyecto.movil.view.componentes.Pestana
import com.proyecto.movil.view.componentes.DialogoSalir
import com.proyecto.movil.view.componentes.RecorridoNiveles
import com.proyecto.movil.viewmodel.MapaUiState
import com.proyecto.movil.viewmodel.MapaViewModel
import com.proyecto.movil.viewmodel.MensajesViewModel
import kotlinx.coroutines.delay

// Inicio después del PIN: barra con los contadores, la pestaña elegida y el menú inferior.
// Aventura = mapa (T-210, US-05); Logros y Perfil usan el mismo mapa ya cargado
@Composable
fun PantallaMapa(
    onSalir: (codigoAula: String?) -> Unit,
    onAbrirNivel: (nivelId: Int) -> Unit = {},
    viewModel: MapaViewModel = viewModel(),
    mensajesViewModel: MensajesViewModel = viewModel()
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

    // Pestaña del menú inferior; rememberSaveable la mantiene al girar la tablet
    var pestana by rememberSaveable { mutableStateOf(Pestana.AVENTURA) }
    val mapa = (estado as? MapaUiState.Exito)?.mapa
    val mensajes by mensajesViewModel.mensajes.collectAsStateWithLifecycle()

    // Cada vez que se abre Logros se piden los mensajes del profesor (puede haber escrito uno nuevo)
    LaunchedEffect(pestana) { if (pestana == Pestana.LOGROS) mensajesViewModel.cargar() }

    // T-240: el mensaje del nivel bloqueado se oculta solo (si se toca otro, el tiempo empieza de nuevo)
    LaunchedEffect(aviso) {
        if (aviso != null) {
            delay(MapaViewModel.DURACION_AVISO_MS)
            viewModel.cerrarAviso()
        }
    }

    Box(Modifier.fillMaxSize().background(ColoresAventura.Cielo)) {
        Column(Modifier.fillMaxSize()) {
            // La racha llega con su endpoint; mientras tanto no se muestra
            BarraAventura(
                nombre = sesion?.nombre ?: "",
                avatarUrl = sesion?.avatarUrl ?: "",
                racha = null,
                estrellas = mapa?.estrellas ?: 0,
                puntos = mapa?.puntos ?: 0
            )
            Box(Modifier.weight(1f)) {
                val contenidoMapa = @Composable {
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
                when (pestana) {
                    Pestana.AVENTURA -> contenidoMapa()
                    // Sin mapa (cargando o error) se muestra lo mismo que en Aventura: carga o reintentar
                    Pestana.LOGROS -> if (mapa != null) ContenidoLogros(mapa, mensajes) else contenidoMapa()
                    // Perfil no depende del mapa: siempre se puede salir
                    Pestana.PERFIL -> ContenidoPerfilAlumno(
                        nombre = sesion?.nombre ?: "",
                        avatarUrl = sesion?.avatarUrl ?: "",
                        completados = mapa?.completados ?: 0,
                        totalNiveles = mapa?.totalNiveles ?: 0,
                        onSalir = { if (!saliendo) preguntandoSalir = true }
                    )
                }
            }
            MenuInferior(actual = pestana, onElegir = { pestana = it })
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
