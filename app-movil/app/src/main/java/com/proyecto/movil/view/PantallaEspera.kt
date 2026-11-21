package com.proyecto.movil.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.proyecto.movil.ui.theme.AppMovilTheme
import com.proyecto.movil.ui.theme.ColoresAventura
import com.proyecto.movil.view.componentes.Koala
import com.proyecto.movil.viewmodel.EsperaUiState
import com.proyecto.movil.viewmodel.EsperaViewModel

// T-418 (US-13): el koala "escucha" mientras la IA transcribe la lectura
@Composable
fun PantallaEspera(
    evidenciaId: Long,
    onVolverAlMapa: () -> Unit,
    esperaViewModel: EsperaViewModel = viewModel()
) {
    val estado by esperaViewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(evidenciaId) { esperaViewModel.seguir(evidenciaId) }
    ContenidoEspera(estado, onVolverAlMapa)
}

// Sin ViewModel: para @Preview y para las pruebas
@Composable
fun ContenidoEspera(estado: EsperaUiState, onVolverAlMapa: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(ColoresAventura.Cielo).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Koala(tamano = 200.dp, saludando = estado !is EsperaUiState.Escuchando)
        Spacer(Modifier.height(24.dp))
        when (estado) {
            EsperaUiState.Escuchando -> {
                Titulo("Estamos escuchando tu lectura…", "titulo_escuchando")
                Spacer(Modifier.height(20.dp))
                CircularProgressIndicator(
                    color = ColoresAventura.Turquesa,
                    strokeWidth = 6.dp,
                    modifier = Modifier.size(64.dp).testTag("indicador_escuchando")
                )
                Spacer(Modifier.height(16.dp))
                Text("Esto puede tardar un ratito", fontSize = 20.sp, color = ColoresAventura.TextoOscuro)
            }
            is EsperaUiState.Lista -> {
                Titulo("¡Te escuchamos!", "titulo_lista")
                Spacer(Modifier.height(16.dp))
                Text("Esto entendimos:", fontSize = 20.sp, color = ColoresAventura.TextoOscuro)
                Spacer(Modifier.height(8.dp))
                Text(
                    estado.transcripcion.ifBlank { "No escuchamos palabras en tu grabación." },
                    fontSize = 24.sp,
                    lineHeight = 34.sp,
                    color = ColoresAventura.TextoOscuro,
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                        .testTag("transcripcion")
                )
            }
            is EsperaUiState.Error -> Aviso(estado.mensaje)
            EsperaUiState.TardaMucho -> Aviso("Está tardando más de lo normal. Tu profesor igual recibirá tu lectura.")
        }
        if (estado !is EsperaUiState.Escuchando) {
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onVolverAlMapa,
                colors = ButtonDefaults.buttonColors(containerColor = ColoresAventura.Turquesa),
                shape = RoundedCornerShape(36.dp),
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().height(72.dp).testTag("boton_volver_mapa_espera")
            ) { Text("Volver al mapa", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White) }
        }
    }
}

@Composable
private fun Titulo(texto: String, etiqueta: String) {
    Text(
        texto,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = ColoresAventura.Turquesa,
        modifier = Modifier.testTag(etiqueta)
    )
}

@Composable
private fun Aviso(mensaje: String) {
    Text(
        mensaje,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = ColoresAventura.TextoOscuro,
        modifier = Modifier.widthIn(max = 720.dp).testTag("aviso_espera")
    )
}

@Preview(name = "Espera · escuchando", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun EsperaEscuchandoPreview() = AppMovilTheme(dynamicColor = false) {
    ContenidoEspera(EsperaUiState.Escuchando) {}
}

@Preview(name = "Espera · lista", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun EsperaListaPreview() = AppMovilTheme(dynamicColor = false) {
    ContenidoEspera(EsperaUiState.Lista("Había una vez un cóndor que vivía en los Andes.")) {}
}
