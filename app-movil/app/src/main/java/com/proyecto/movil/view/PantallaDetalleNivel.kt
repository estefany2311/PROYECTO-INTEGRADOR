package com.proyecto.movil.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.proyecto.movil.model.NivelDetalle
import com.proyecto.movil.model.TipoReto
import com.proyecto.movil.ui.theme.AppMovilTheme
import com.proyecto.movil.ui.theme.ColoresAventura
import com.proyecto.movil.viewmodel.DetalleNivelUiState
import com.proyecto.movil.viewmodel.DetalleNivelViewModel

/**
 * T-215 (US-07): al tocar un nivel disponible en el mapa se abre esta pantalla.
 * Muestra qué tipo de reto es y un botón grande "Comenzar".
 */
@Composable
fun PantallaDetalleNivel(
    nivelId: Int,
    onVolver: () -> Unit,
    onComenzar: (NivelDetalle) -> Unit,
    viewModel: DetalleNivelViewModel = viewModel()
) {
    val estado by viewModel.uiState.collectAsState()

    LaunchedEffect(nivelId) { viewModel.cargar(nivelId) }

    ContenidoDetalleNivel(
        estado = estado,
        onVolver = onVolver,
        onComenzar = onComenzar,
        onReintentar = viewModel::reintentar
    )
}

// Sin ViewModel: para @Preview y para las pruebas de T-243
@Composable
fun ContenidoDetalleNivel(
    estado: DetalleNivelUiState,
    onVolver: () -> Unit,
    onComenzar: (NivelDetalle) -> Unit,
    onReintentar: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(ColoresAventura.pasto)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButton(
            onClick = onVolver,
            modifier = Modifier.align(Alignment.Start).testTag("boton_volver_mapa")
        ) {
            Text("←  Volver al mapa", fontSize = 20.sp, color = ColoresAventura.TextoOscuro)
        }
        Spacer(Modifier.height(16.dp))

        when (estado) {
            is DetalleNivelUiState.Cargando ->
                CircularProgressIndicator(Modifier.padding(48.dp).size(64.dp).testTag("cargando_nivel"))
            is DetalleNivelUiState.Exito -> TarjetaDetalle(estado.detalle) { onComenzar(estado.detalle) }
            is DetalleNivelUiState.Bloqueado -> MensajeNivel(estado.mensaje, "mensaje_nivel_bloqueado") {
                OutlinedButton(onClick = onVolver, modifier = Modifier.height(56.dp)) {
                    Text("Volver al mapa", fontSize = 20.sp, color = ColoresAventura.TextoOscuro)
                }
            }
            is DetalleNivelUiState.Error -> MensajeNivel(estado.mensaje, "error_nivel") {
                Button(
                    onClick = onReintentar,
                    colors = ButtonDefaults.buttonColors(containerColor = ColoresAventura.Naranja),
                    modifier = Modifier.height(56.dp).testTag("boton_reintentar_nivel")
                ) {
                    Text("Intentar de nuevo", fontSize = 20.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun TarjetaDetalle(detalle: NivelDetalle, onComenzar: () -> Unit) {
    Column(
        Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White)
            .padding(28.dp)
            .testTag("detalle_nivel"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Nivel ${detalle.orden}", fontSize = 22.sp, color = ColoresAventura.TextoOscuro)
        Spacer(Modifier.height(4.dp))
        Text(
            detalle.nombre,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = ColoresAventura.TextoOscuro
        )
        Spacer(Modifier.height(12.dp))
        // T-242 agrega aquí el ícono y el color del tipo de reto
        Text(
            "Reto de ${nombreDelReto(detalle.reto)}",
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = ColoresAventura.TextoOscuro,
            modifier = Modifier.testTag("tipo_reto")
        )
        detalle.descripcion?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, fontSize = 20.sp, textAlign = TextAlign.Center, color = ColoresAventura.TextoOscuro)
        }
        if (detalle.completado) {
            Spacer(Modifier.height(12.dp))
            Text(
                "¡Ya lo completaste! Puedes jugarlo otra vez.",
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                color = ColoresAventura.Turquesa,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(28.dp))
        // Botón grande: lo más importante de la pantalla
        Button(
            onClick = onComenzar,
            colors = ButtonDefaults.buttonColors(containerColor = ColoresAventura.Naranja),
            shape = RoundedCornerShape(36.dp),
            modifier = Modifier.fillMaxWidth().height(72.dp).testTag("boton_comenzar")
        ) {
            Text("Comenzar", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun MensajeNivel(mensaje: String, tag: String, boton: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 32.dp)) {
        Text(
            mensaje,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = ColoresAventura.TextoOscuro,
            modifier = Modifier.testTag(tag)
        )
        Spacer(Modifier.height(24.dp))
        boton()
    }
}

// Nombre del reto para el niño
fun nombreDelReto(reto: TipoReto): String = when (reto) {
    TipoReto.LECTURA -> "lectura"
    TipoReto.ESCRITURA -> "escritura"
    TipoReto.ORALIDAD -> "oralidad"
    TipoReto.DESCONOCIDO -> "aventura"
}

@Preview(widthDp = 400, heightDp = 800, showBackground = true)
@Composable
fun DetalleNivelPreview() {
    AppMovilTheme(dynamicColor = false) {
        ContenidoDetalleNivel(
            estado = DetalleNivelUiState.Exito(
                NivelDetalle(1, 1, "Primeros pasos", "lectura", "Lee palabras y oraciones cortas", "desbloqueado")
            ),
            onVolver = {}, onComenzar = {}, onReintentar = {}
        )
    }
}
