package com.proyecto.movil.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
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
import com.proyecto.movil.viewmodel.LecturaUiState
import com.proyecto.movil.viewmodel.LecturaViewModel

/**
 * T-219 (US-10): el niño ve el texto que tiene que leer, con letra grande, antes de grabar su voz.
 * Se abre con "Comenzar" desde el detalle de un nivel de lectura u oralidad.
 * El tamaño de letra ajustable y el desplazamiento de textos largos llegan en T-311;
 * la grabación (botón "Estoy listo, grabar") en US-11.
 */
@Composable
fun PantallaLectura(
    nivelId: Int,
    onVolver: () -> Unit,
    onGrabar: (LecturaNivel) -> Unit,
    viewModel: LecturaViewModel = viewModel()
) {
    val estado by viewModel.uiState.collectAsState()

    LaunchedEffect(nivelId) { viewModel.cargar(nivelId) }

    ContenidoLectura(estado = estado, onVolver = onVolver, onGrabar = onGrabar, onReintentar = viewModel::reintentar)
}

// Letra pensada para los primeros grados: grande, con buen espacio entre líneas
private val TAMANO_LETRA = 32.sp
private val ALTO_LINEA = 48.sp

// Sin ViewModel: para @Preview y para las pruebas de T-312
@Composable
fun ContenidoLectura(
    estado: LecturaUiState,
    onVolver: () -> Unit,
    onGrabar: (LecturaNivel) -> Unit,
    onReintentar: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(ColoresAventura.Cielo)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButton(onClick = onVolver, modifier = Modifier.align(Alignment.Start).testTag("boton_volver_lectura")) {
            Text("←  Volver", fontSize = 20.sp, color = ColoresAventura.TextoOscuro)
        }
        Spacer(Modifier.height(8.dp))

        when (estado) {
            is LecturaUiState.Cargando ->
                CircularProgressIndicator(Modifier.padding(48.dp).size(64.dp).testTag("cargando_lectura"))
            is LecturaUiState.Exito -> TextoParaLeer(estado.lectura) { onGrabar(estado.lectura) }
            is LecturaUiState.Error -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 32.dp)
            ) {
                Text(
                    estado.mensaje,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = ColoresAventura.TextoOscuro,
                    modifier = Modifier.testTag("error_lectura")
                )
                Spacer(Modifier.height(24.dp))
                if (estado.puedeReintentar) {
                    Button(
                        onClick = onReintentar,
                        colors = ButtonDefaults.buttonColors(containerColor = ColoresAventura.Naranja),
                        modifier = Modifier.height(56.dp).testTag("boton_reintentar_lectura")
                    ) { Text("Intentar de nuevo", fontSize = 20.sp, color = Color.White) }
                } else {
                    OutlinedButton(onClick = onVolver, modifier = Modifier.height(56.dp)) {
                        Text("Volver", fontSize = 20.sp, color = ColoresAventura.TextoOscuro)
                    }
                }
            }
        }
    }
}

@Composable
private fun TextoParaLeer(lectura: LecturaNivel, onGrabar: () -> Unit) {
    Column(Modifier.widthIn(max = 720.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Lee el texto en voz alta",
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = ColoresAventura.TextoOscuro
        )
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .padding(horizontal = 28.dp, vertical = 24.dp)
                .testTag("texto_lectura")
        ) {
            Text(
                lectura.titulo,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = ColoresAventura.Turquesa
            )
            Spacer(Modifier.height(16.dp))
            Text(lectura.texto, fontSize = TAMANO_LETRA, lineHeight = ALTO_LINEA, color = ColoresAventura.TextoOscuro)
        }
        Spacer(Modifier.height(32.dp))
        // Botón grande: lo siguiente es grabar su lectura (US-11)
        Button(
            onClick = onGrabar,
            colors = ButtonDefaults.buttonColors(containerColor = ColoresAventura.Naranja),
            shape = RoundedCornerShape(36.dp),
            modifier = Modifier.fillMaxWidth().height(72.dp).testTag("boton_grabar")
        ) {
            Icon(painterResource(R.drawable.ic_microfono), contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Text("Estoy listo, grabar", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Preview(widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun PantallaLecturaPreview() {
    AppMovilTheme(dynamicColor = false) {
        ContenidoLectura(
            estado = LecturaUiState.Exito(
                LecturaNivel(
                    1, 3, "El koala Kiko",
                    "Había una vez un koala llamado Kiko que vivía en un árbol muy alto. " +
                        "Todas las mañanas leía un libro con sus amigos del bosque.",
                    2, "basica", false
                )
            ),
            onVolver = {}, onGrabar = {}, onReintentar = {}
        )
    }
}
