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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.proyecto.movil.R
import com.proyecto.movil.model.PalabraLeida
import com.proyecto.movil.model.ResultadoEvaluacion
import com.proyecto.movil.ui.theme.AppMovilTheme
import com.proyecto.movil.ui.theme.ColoresAventura
import com.proyecto.movil.view.componentes.Koala
import com.proyecto.movil.view.componentes.ListaPalabrasPorMejorar
import com.proyecto.movil.view.componentes.TextoConPalabrasPorMejorar
import com.proyecto.movil.viewmodel.EsperaUiState
import com.proyecto.movil.viewmodel.EsperaViewModel

// T-418 (US-13): el koala "escucha" mientras la IA transcribe la lectura
@Composable
fun PantallaEspera(
    evidenciaId: Long,
    onVolverAlMapa: () -> Unit,
    onIntentarDeNuevo: (Int) -> Unit = {},
    esperaViewModel: EsperaViewModel = viewModel()
) {
    val estado by esperaViewModel.uiState.collectAsStateWithLifecycle()
    val palabraElegida by esperaViewModel.palabraElegida.collectAsStateWithLifecycle()
    LaunchedEffect(evidenciaId) { esperaViewModel.seguir(evidenciaId) }
    ContenidoEspera(estado, palabraElegida, esperaViewModel::escuchar, onVolverAlMapa, onIntentarDeNuevo)
}

// Sin ViewModel: para @Preview y para las pruebas
@Composable
fun ContenidoEspera(estado: EsperaUiState, onVolverAlMapa: () -> Unit) =
    ContenidoEspera(estado, palabraElegida = null, onEscuchar = {}, onVolverAlMapa = onVolverAlMapa)

// T-428/T-430: con la palabra elegida y qué hacer al tocar una palabra por mejorar.
// T-431: onIntentarDeNuevo recibe el nivel para volver a empezar el reto
@Composable
fun ContenidoEspera(
    estado: EsperaUiState,
    palabraElegida: String?,
    onEscuchar: (String) -> Unit,
    onVolverAlMapa: () -> Unit,
    onIntentarDeNuevo: (Int) -> Unit = {}
) {
    Column(
        Modifier.fillMaxSize().background(ColoresAventura.Cielo).verticalScroll(rememberScrollState()).padding(24.dp),
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
            is EsperaUiState.Resultado -> ResultadoConEstrellas(estado.resultado, palabraElegida, onEscuchar)
            is EsperaUiState.Error -> Aviso(estado.mensaje)
            EsperaUiState.TardaMucho -> Aviso("Está tardando más de lo normal. Tu profesor igual recibirá tu lectura.")
        }
        // T-431: con el resultado puede repetir la lectura para mejorar
        if (estado is EsperaUiState.Resultado) {
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = { onIntentarDeNuevo(estado.resultado.nivelId) },
                colors = ButtonDefaults.buttonColors(containerColor = ColoresAventura.Naranja),
                shape = RoundedCornerShape(36.dp),
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().height(72.dp).testTag("boton_intentar_de_nuevo")
            ) { Text("Intentar de nuevo", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White) }
        }
        if (estado !is EsperaUiState.Escuchando) {
            Spacer(Modifier.height(if (estado is EsperaUiState.Resultado) 16.dp else 28.dp))
            Button(
                onClick = onVolverAlMapa,
                colors = ButtonDefaults.buttonColors(containerColor = ColoresAventura.Turquesa),
                shape = RoundedCornerShape(36.dp),
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().height(72.dp).testTag("boton_volver_mapa_espera")
            ) { Text("Volver al mapa", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White) }
        }
    }
}

// T-426 (US-14): estrellas, cuántas palabras leyó bien, puntos y si abrió un nivel nuevo
@Composable
private fun ResultadoConEstrellas(resultado: ResultadoEvaluacion, palabraElegida: String?, onEscuchar: (String) -> Unit) {
    val estrellas = (resultado.estrellas ?: 0).coerceIn(0, 3)
    Titulo(tituloPara(estrellas), "titulo_resultado")
    Spacer(Modifier.height(16.dp))
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.semantics { contentDescription = "$estrellas de 3 estrellas" }.testTag("estrellas_resultado")
    ) {
        repeat(3) { i ->
            Icon(
                painterResource(R.drawable.ic_estrella),
                contentDescription = null,
                tint = if (i < estrellas) ColoresAventura.Naranja else ColoresAventura.Gris,
                modifier = Modifier.size(if (i == 1) 96.dp else 80.dp)
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    if (resultado.correctas != null && resultado.evaluadas != null) {
        Text(
            "Leíste bien ${resultado.correctas} de ${resultado.evaluadas} palabras",
            fontSize = 24.sp,
            color = ColoresAventura.TextoOscuro,
            modifier = Modifier.testTag("palabras_bien")
        )
    }
    resultado.puntos?.let {
        Spacer(Modifier.height(8.dp))
        Text(
            "+$it puntos",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = ColoresAventura.Turquesa,
            modifier = Modifier.testTag("puntos_resultado")
        )
    }
    // T-432: si superó su mejor intento en este nivel
    resultado.mensajeMejorIntento()?.let {
        Spacer(Modifier.height(8.dp))
        Text(
            it,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = ColoresAventura.TextoOscuro,
            modifier = Modifier.testTag("mejor_intento")
        )
    }
    resultado.nivelDesbloqueadoNombre?.let {
        Spacer(Modifier.height(12.dp))
        Text(
            "¡Desbloqueaste un nivel nuevo: $it!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = ColoresAventura.Naranja,
            modifier = Modifier.testTag("nivel_desbloqueado")
        )
    }
    PalabrasDelResultado(resultado, palabraElegida, onEscuchar)
}

// T-428/T-429/T-430: el texto con las palabras por mejorar resaltadas y la lista para escucharlas
@Composable
private fun PalabrasDelResultado(resultado: ResultadoEvaluacion, palabraElegida: String?, onEscuchar: (String) -> Unit) {
    val palabras = resultado.palabras.orEmpty()
    if (palabras.isEmpty()) return   // backend anterior o lectura sin texto
    val porMejorar = resultado.palabrasPorMejorar()
    Spacer(Modifier.height(20.dp))
    TextoConPalabrasPorMejorar(palabras, palabraElegida, onEscuchar)
    Spacer(Modifier.height(20.dp))
    if (porMejorar.isEmpty()) {
        Text(
            "¡Leíste todas las palabras bien!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = ColoresAventura.Turquesa,
            modifier = Modifier.testTag("todas_bien")
        )
    } else {
        ListaPalabrasPorMejorar(porMejorar, palabraElegida, onEscuchar)
    }
}

private fun tituloPara(estrellas: Int) = when (estrellas) {
    3 -> "¡Excelente lectura!"
    2 -> "¡Muy bien!"
    1 -> "¡Bien hecho!"
    else -> "¡Sigue practicando!"
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

@Preview(name = "Resultado · 2 estrellas", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun ResultadoPreview() = AppMovilTheme(dynamicColor = false) {
    ContenidoEspera(
        EsperaUiState.Resultado(ResultadoEvaluacion(50L, 1, "listo", "Había una vez", 81, 13, 16, 2, 8, 2, "Palabras nuevas", null))
    ) {}
}

@Preview(name = "Resultado · palabras por mejorar", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun ResultadoPalabrasPreview() = AppMovilTheme(dynamicColor = false) {
    val palabras = "Había una vez un cóndor que vivía en los Andes.".split(" ")
        .map { PalabraLeida(it, it == "cóndor" || it == "Andes.") }
    ContenidoEspera(
        EsperaUiState.Resultado(ResultadoEvaluacion(50L, 1, "listo", "Había una vez", 80, 8, 10, 2, 8, null, null, null, palabras)),
        palabraElegida = "cóndor",
        onEscuchar = {},
        onVolverAlMapa = {}
    )
}

@Preview(name = "Espera · lista", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun EsperaListaPreview() = AppMovilTheme(dynamicColor = false) {
    ContenidoEspera(EsperaUiState.Lista("Había una vez un cóndor que vivía en los Andes.")) {}
}
