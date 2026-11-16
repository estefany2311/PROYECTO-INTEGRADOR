package com.proyecto.movil.view

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.proyecto.movil.model.SesionRespuesta
import com.proyecto.movil.ui.theme.AppMovilTheme
import com.proyecto.movil.ui.theme.ColoresAventura
import com.proyecto.movil.viewmodel.PinUiState
import com.proyecto.movil.viewmodel.PinViewModel
import com.proyecto.movil.viewmodel.TipoError

private const val TECLA_BORRAR = -1
private const val TECLA_VACIA = -2

// Filas del teclado: abajo a la izquierda queda un hueco, como en el mockup
private val FILAS_TECLADO = listOf(
    listOf(1, 2, 3),
    listOf(4, 5, 6),
    listOf(7, 8, 9),
    listOf(TECLA_VACIA, 0, TECLA_BORRAR)
)

@Composable
fun PantallaPin(
    estudianteId: Long,
    nombre: String,
    avatarUrl: String,
    onVolver: () -> Unit,
    onSesionIniciada: (SesionRespuesta) -> Unit,
    viewModel: PinViewModel = viewModel()
) {
    val estado by viewModel.uiState.collectAsState()

    LaunchedEffect(estudianteId) {
        viewModel.inicializar(estudianteId)
    }

    LaunchedEffect(estado.sesion) {
        estado.sesion?.let(onSesionIniciada)
    }

    ContenidoPin(
        nombre = nombre,
        avatarUrl = avatarUrl,
        estado = estado,
        onDigito = viewModel::presionarDigito,
        onBorrar = viewModel::borrar,
        onVolver = onVolver
    )
}

// Sin ViewModel: se puede usar en @Preview y en pruebas de UI pasándole cualquier estado
@Composable
fun ContenidoPin(
    nombre: String,
    avatarUrl: String,
    estado: PinUiState,
    onDigito: (Int) -> Unit,
    onBorrar: () -> Unit,
    onVolver: () -> Unit
) {
    val tecladoActivo = !estado.verificando && !estado.sacudiendo && estado.sesion == null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColoresAventura.Cielo)
            .verticalScroll(rememberScrollState()) // en teléfonos pequeños el teclado no cabe completo
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButton(onClick = onVolver, modifier = Modifier.align(Alignment.Start)) {
            Text("←  No soy yo", fontSize = 20.sp, color = ColoresAventura.TextoOscuro)
        }

        AsyncImage(
            model = avatarUrl,
            contentDescription = "Avatar de $nombre",
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(ColoresAventura.Naranja)
        )
        Spacer(Modifier.height(12.dp))

        Text(
            "Hola, $nombre",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = ColoresAventura.TextoOscuro
        )
        Text("Escribe tu clave secreta", fontSize = 22.sp, color = ColoresAventura.TextoOscuro)
        Spacer(Modifier.height(24.dp))

        CirculosPin(
            cantidadEscrita = estado.pin.length,
            sacudidas = estado.sacudidas,
            enError = estado.sacudiendo
        )
        Spacer(Modifier.height(16.dp))

        // Altura fija para que el teclado no salte cuando aparece o desaparece el mensaje
        Box(Modifier.heightIn(min = 56.dp), contentAlignment = Alignment.Center) {
            when {
                estado.verificando -> CircularProgressIndicator(color = ColoresAventura.Naranja)
                estado.sesion != null -> Text(
                    "¡Muy bien, ${estado.sesion.nombre}!",
                    color = ColoresAventura.Turquesa,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                estado.mensajeError != null -> TarjetaError(
                    titulo = estado.mensajeError,
                    detalle = estado.detalleError,
                    tipo = estado.tipoError
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        FILAS_TECLADO.forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                fila.forEach { tecla ->
                    when (tecla) {
                        TECLA_VACIA -> Spacer(Modifier.size(96.dp))
                        TECLA_BORRAR -> Tecla("⌫", "tecla_borrar", "Borrar", tecladoActivo, onBorrar)
                        else -> Tecla("$tecla", "tecla_$tecla", "$tecla", tecladoActivo) { onDigito(tecla) }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

// T-207: aviso grande con color según el error, para que el niño entienda sin frustrarse
@Composable
private fun TarjetaError(titulo: String, detalle: String?, tipo: TipoError?) {
    val color = when (tipo) {
        TipoError.BLOQUEADO -> ColoresAventura.Naranja
        TipoError.SIN_CONEXION -> ColoresAventura.Gris
        else -> ColoresAventura.Rojo          // PIN incorrecto
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.12f))
            .border(3.dp, color, RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("aviso_pin")
    ) {
        Column {
            Text(
                titulo,
                color = color,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start
            )
            if (detalle != null) {
                Text(detalle, color = ColoresAventura.TextoOscuro, fontSize = 20.sp)
            }
        }
    }
}

// Ida y vuelta cada vez más corta, como cuando alguien dice "no" con la cabeza (en dp)
private val PASOS_SACUDIDA = listOf(-24f, 24f, -16f, 16f, -8f, 8f, 0f)

/**
 * 4 círculos: rellenos los números ya escritos. El PIN nunca se muestra.
 * T-234: cada vez que [sacudidas] cambia, la fila se sacude y la tablet vibra.
 * Mientras [enError], los círculos llenos se ven en rojo; el ViewModel los borra al terminar.
 */
@Composable
private fun CirculosPin(cantidadEscrita: Int, sacudidas: Int = 0, enError: Boolean = false) {
    val desplazamiento = remember { Animatable(0f) }
    val vibracion = LocalHapticFeedback.current

    LaunchedEffect(sacudidas) {
        if (sacudidas == 0) return@LaunchedEffect      // al abrir la pantalla no se sacude
        vibracion.performHapticFeedback(HapticFeedbackType.LongPress)
        // 7 pasos de 70 ms = 490 ms: termina antes de que el ViewModel borre los círculos (600 ms)
        PASOS_SACUDIDA.forEach { x -> desplazamiento.animateTo(x, tween(durationMillis = 70)) }
    }

    val colorLleno = if (enError) ColoresAventura.Rojo else ColoresAventura.TextoOscuro
    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier
            .graphicsLayer { translationX = desplazamiento.value.dp.toPx() }
            .testTag("circulos_pin")
            .semantics {
                contentDescription = "$cantidadEscrita de ${PinViewModel.LARGO_PIN} números escritos"
            }
    ) {
        repeat(PinViewModel.LARGO_PIN) { i ->
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (i < cantidadEscrita) colorLleno else Color.White)
                    .border(3.dp, colorLleno, CircleShape)
            )
        }
    }
}

// Tecla redonda y grande (96dp) para dedos pequeños
@Composable
private fun Tecla(
    texto: String,
    tag: String,
    descripcion: String,
    habilitada: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(enabled = habilitada, onClick = onClick)
            .semantics { contentDescription = descripcion }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            texto,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = if (habilitada) ColoresAventura.TextoOscuro else ColoresAventura.Gris
        )
    }
}

@Preview(name = "PIN · 2 números", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun ContenidoPinPreview() {
    AppMovilTheme(dynamicColor = false) {
        ContenidoPin(
            nombre = "Ana",
            avatarUrl = "",
            estado = PinUiState(pin = "12"),
            onDigito = {},
            onBorrar = {},
            onVolver = {}
        )
    }
}

@Preview(name = "PIN · incorrecto (durante la sacudida)", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun ContenidoPinErrorPreview() {
    AppMovilTheme(dynamicColor = false) {
        ContenidoPin(
            nombre = "Ana",
            avatarUrl = "",
            estado = PinUiState(
                pin = "0000",
                sacudiendo = true,
                mensajeError = PinViewModel.MENSAJE_PIN_INCORRECTO,
                detalleError = PinViewModel.textoIntentos(3),
                tipoError = TipoError.PIN_INCORRECTO
            ),
            onDigito = {},
            onBorrar = {},
            onVolver = {}
        )
    }
}