package com.proyecto.movil.view

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.proyecto.movil.R
import com.proyecto.movil.model.LecturaNivel
import com.proyecto.movil.ui.theme.AppMovilTheme
import com.proyecto.movil.ui.theme.ColoresAventura
import com.proyecto.movil.viewmodel.GrabacionUiState
import com.proyecto.movil.viewmodel.GrabacionViewModel
import com.proyecto.movil.viewmodel.LecturaUiState
import com.proyecto.movil.viewmodel.LecturaViewModel

/**
 * US-11: el niño graba su voz mientras lee el texto. Se abre con "Estoy listo, grabar" (T-219).
 * T-313: primero se asegura el permiso del micrófono con la explicación de Kiko.
 * T-314: graba en WAV, mono, 16 kHz (decisión de T-327). El texto sigue a la vista mientras graba.
 * T-315: indicador visual mientras graba (micrófono que late con la voz, punto rojo y barra de tiempo).
 * Llegan después: límite de 2 minutos (T-316), guardado temporal (T-317)
 * y escuchar/enviar la grabación (US-12, US-13).
 */
@Composable
fun PantallaGrabacion(
    nivelId: Int,
    onVolver: () -> Unit,
    lecturaViewModel: LecturaViewModel = viewModel(),
    grabacionViewModel: GrabacionViewModel = viewModel()
) {
    ConPermisoMicrofono(onAhoraNo = onVolver) {
        val lectura by lecturaViewModel.uiState.collectAsState()
        val grabacion by grabacionViewModel.uiState.collectAsState()

        LaunchedEffect(nivelId) { lecturaViewModel.cargar(nivelId) }

        ContenidoGrabacion(
            lectura = (lectura as? LecturaUiState.Exito)?.lectura,
            estado = grabacion,
            onVolver = onVolver,
            onGrabar = { grabacionViewModel.grabar(nivelId) },
            onTerminar = grabacionViewModel::terminar,
            onGrabarOtraVez = grabacionViewModel::grabarOtraVez
        )
    }
}

// Sin ViewModel: para @Preview y para las pruebas de T-318
@Composable
fun ContenidoGrabacion(
    lectura: LecturaNivel?,
    estado: GrabacionUiState,
    onVolver: () -> Unit,
    onGrabar: () -> Unit,
    onTerminar: () -> Unit,
    onGrabarOtraVez: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(ColoresAventura.Cielo).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mientras graba no se puede volver: primero hay que terminar
        TextButton(
            onClick = onVolver,
            enabled = estado !is GrabacionUiState.Grabando,
            modifier = Modifier.align(Alignment.Start).testTag("boton_volver_grabacion")
        ) { Text("←  Volver", fontSize = 20.sp, color = ColoresAventura.TextoOscuro) }

        // El texto queda a la vista para que lo lea mientras graba
        Column(
            Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 24.dp)
        ) {
            if (lectura != null) {
                Text(lectura.titulo, fontSize = 30.sp, lineHeight = 39.sp, fontWeight = FontWeight.Bold, color = ColoresAventura.Turquesa)
                Spacer(Modifier.height(12.dp))
                Text(lectura.texto, fontSize = 30.sp, lineHeight = 45.sp, color = ColoresAventura.TextoOscuro)
            }
        }
        Spacer(Modifier.height(20.dp))
        ControlesGrabacion(estado, onGrabar, onTerminar, onGrabarOtraVez, onVolver)
    }
}

@Composable
private fun ControlesGrabacion(
    estado: GrabacionUiState,
    onGrabar: () -> Unit,
    onTerminar: () -> Unit,
    onGrabarOtraVez: () -> Unit,
    onVolver: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
        when (estado) {
            is GrabacionUiState.Lista -> {
                BotonMicrofono(ColoresAventura.Rojo, "Empezar a grabar", "boton_empezar_grabar", onGrabar)
                Spacer(Modifier.height(8.dp))
                Text("Toca el micrófono y lee en voz alta", fontSize = 20.sp, color = ColoresAventura.TextoOscuro)
            }
            is GrabacionUiState.Grabando -> {
                IndicadorGrabando(estado.nivelVoz)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PuntoRojo()
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Grabando… ${formatoTiempo(estado.duracionMs)}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColoresAventura.Rojo,
                        modifier = Modifier.testTag("tiempo_grabacion")
                    )
                }
                Spacer(Modifier.height(8.dp))
                BarraTiempo(estado.duracionMs)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onTerminar,
                    colors = ButtonDefaults.buttonColors(containerColor = ColoresAventura.TextoOscuro),
                    shape = RoundedCornerShape(36.dp),
                    modifier = Modifier.fillMaxWidth().height(72.dp).testTag("boton_terminar_grabar")
                ) { Text("Terminar", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White) }
            }
            is GrabacionUiState.Grabada -> {
                Text(
                    "¡Listo! Grabaste ${formatoTiempo(estado.grabacion.duracionMs)}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColoresAventura.Turquesa,
                    modifier = Modifier.testTag("grabacion_terminada")
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onGrabarOtraVez,
                    shape = RoundedCornerShape(32.dp),
                    modifier = Modifier.fillMaxWidth().height(64.dp).testTag("boton_grabar_otra_vez")
                ) { Text("Grabar otra vez", fontSize = 22.sp, color = ColoresAventura.TextoOscuro) }
            }
            is GrabacionUiState.Error -> {
                Text(
                    estado.mensaje,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = ColoresAventura.TextoOscuro,
                    modifier = Modifier.testTag("error_grabacion")
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onGrabarOtraVez, modifier = Modifier.height(56.dp)) {
                    Text("Intentar otra vez", fontSize = 20.sp, color = ColoresAventura.TextoOscuro)
                }
            }
        }
    }
}

/**
 * T-315: el micrófono rojo con un aro que crece con la voz del niño. Así ve que la tablet lo está escuchando:
 * si habla bajito el aro casi no se mueve, si lee fuerte y claro el aro se agranda.
 */
@Composable
private fun IndicadorGrabando(nivelVoz: Float) {
    // Se suaviza para que no tiemble; con poca voz igual "respira" un poco
    val voz by animateFloatAsState(targetValue = nivelVoz.coerceIn(0f, 1f), animationSpec = tween(120), label = "voz")
    val respira by rememberInfiniteTransition(label = "respira").animateFloat(
        initialValue = 0f, targetValue = 0.08f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "respiracion"
    )
    val escala = 1f + respira + voz * 0.55f
    Box(
        Modifier.size(170.dp).semantics { contentDescription = "Grabando tu voz" }.testTag("indicador_grabando"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(112.dp)
                .graphicsLayer { scaleX = escala; scaleY = escala }
                .clip(CircleShape)
                .background(ColoresAventura.Rojo.copy(alpha = 0.22f))
        )
        Box(Modifier.size(112.dp).clip(CircleShape).background(ColoresAventura.Rojo), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_microfono), contentDescription = null, tint = Color.White, modifier = Modifier.size(56.dp))
        }
    }
}

// T-315: punto rojo que parpadea, como en cualquier grabadora
@Composable
private fun PuntoRojo() {
    val alfa by rememberInfiniteTransition(label = "punto").animateFloat(
        initialValue = 1f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "parpadeo"
    )
    Box(Modifier.size(16.dp).graphicsLayer { alpha = alfa }.clip(CircleShape).background(ColoresAventura.Rojo))
}

// T-315: cuánto tiempo queda de los 2 minutos que dura como máximo una lectura
@Composable
private fun BarraTiempo(duracionMs: Long) {
    val maximo = GrabacionViewModel.DURACION_MAXIMA_MS
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { (duracionMs.toFloat() / maximo).coerceIn(0f, 1f) },
            color = ColoresAventura.Rojo,
            trackColor = Color.White,
            modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).testTag("barra_tiempo")
        )
        Spacer(Modifier.height(4.dp))
        Text("Puedes leer hasta ${formatoTiempo(maximo)}", fontSize = 16.sp, color = ColoresAventura.TextoOscuro)
    }
}

// Botón redondo grande con el micrófono
@Composable
private fun BotonMicrofono(color: Color, descripcion: String, tag: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(112.dp).semantics { contentDescription = descripcion }.testTag(tag)
    ) {
        Icon(painterResource(R.drawable.ic_microfono), contentDescription = null, tint = Color.White, modifier = Modifier.size(56.dp))
    }
}

// 75 000 ms → "1:15"
fun formatoTiempo(ms: Long): String {
    val segundos = ms / 1000
    return "%d:%02d".format(segundos / 60, segundos % 60)
}

private val LECTURA_PRUEBA = LecturaNivel(1, 3, "El cóndor y el zorro", "Había una vez un cóndor que vivía en lo alto de la montaña con su amigo el zorro.", 2, "basica", false)

@Preview(name = "Grabación · lista", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun GrabacionListaPreview() = GrabacionDePrueba(GrabacionUiState.Lista)

@Preview(name = "Grabación · grabando", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun GrabacionGrabandoPreview() = GrabacionDePrueba(GrabacionUiState.Grabando(12_000, 0.4f))

@Composable
private fun GrabacionDePrueba(estado: GrabacionUiState) {
    AppMovilTheme(dynamicColor = false) {
        ContenidoGrabacion(LECTURA_PRUEBA, estado, onVolver = {}, onGrabar = {}, onTerminar = {}, onGrabarOtraVez = {})
    }
}
