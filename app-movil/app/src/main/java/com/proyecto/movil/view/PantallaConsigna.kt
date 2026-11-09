package com.proyecto.movil.view

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.proyecto.movil.R
import com.proyecto.movil.model.ConsignaNivel
import com.proyecto.movil.ui.theme.AppMovilTheme
import com.proyecto.movil.ui.theme.ColoresAventura
import com.proyecto.movil.viewmodel.ConsignaUiState
import com.proyecto.movil.viewmodel.ConsignaViewModel

//T-321 (US-17): el niño ve qué tiene que escribir antes de abrir el editor.
 //Se abre con "Comenzar" desde el detalle de un nivel de escritura. Sigue el mockup 4 de T-334:

@Composable
fun PantallaConsigna(
    nivelId: Int,
    onVolver: () -> Unit,
    onEmpezar: (ConsignaNivel) -> Unit,
    viewModel: ConsignaViewModel = viewModel()
) {
    val estado by viewModel.uiState.collectAsState()

    LaunchedEffect(nivelId) { viewModel.cargar(nivelId) }

    ContenidoConsigna(estado, onVolver, onEmpezar, viewModel::reintentar)
}

@Composable
fun ContenidoConsigna(
    estado: ConsignaUiState,
    onVolver: () -> Unit,
    onEmpezar: (ConsignaNivel) -> Unit,
    onReintentar: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(ColoresAventura.Cielo)) {
        Cabecera(
            titulo = (estado as? ConsignaUiState.Exito)?.consigna?.nombreNivel,
            onVolver = onVolver
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (estado) {
                is ConsignaUiState.Cargando ->
                    CircularProgressIndicator(Modifier.padding(48.dp).size(64.dp).testTag("cargando_consigna"))
                is ConsignaUiState.Exito -> TarjetaConsigna(estado.consigna) { onEmpezar(estado.consigna) }
                is ConsignaUiState.Error -> ErrorConsigna(estado, onReintentar, onVolver)
            }
        }
    }
}

// Barra de arriba: volver, nombre del nivel y la etiqueta "Escritura" (como en el mockup)
@Composable
private fun Cabecera(titulo: String?, onVolver: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onVolver, modifier = Modifier.testTag("boton_volver_consigna")) {
            Text("←  Volver", fontSize = 20.sp, color = ColoresAventura.TextoOscuro)
        }
        Text(
            titulo ?: "",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = ColoresAventura.TextoOscuro,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.weight(1f).testTag("titulo_consigna")
        )
        Text(
            "Escritura",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(ColoresAventura.Naranja)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun TarjetaConsigna(consigna: ConsignaNivel, onEmpezar: () -> Unit) {
    Column(Modifier.widthIn(max = 720.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IlustracionConsigna(consigna.ilustracion)
            Spacer(Modifier.height(24.dp))
            Text("Tu reto", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ColoresAventura.Naranja)
            Spacer(Modifier.height(8.dp))
            Text(
                consigna.consigna,
                fontSize = 30.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = ColoresAventura.TextoOscuro,
                modifier = Modifier.testTag("texto_consigna")
            )
            // La pista es opcional: ayuda a empezar sin decirle qué escribir
            consigna.pista?.takeIf { it.isNotBlank() }?.let { pista ->
                Spacer(Modifier.height(16.dp))
                Text(
                    "Pista: $pista",
                    fontSize = 20.sp,
                    lineHeight = 28.sp,
                    textAlign = TextAlign.Center,
                    color = ColoresAventura.TextoOscuro,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ColoresAventura.Cielo)
                        .padding(16.dp)
                        .testTag("pista_consigna")
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Escribe al menos ${consigna.palabrasMinimas} palabras",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = ColoresAventura.TextoOscuro,
                modifier = Modifier.testTag("palabras_minimas")
            )
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onEmpezar,
            colors = ButtonDefaults.buttonColors(containerColor = ColoresAventura.Naranja),
            shape = RoundedCornerShape(36.dp),
            modifier = Modifier.fillMaxWidth().height(72.dp).testTag("boton_empezar_escribir")
        ) {
            Icon(painterResource(R.drawable.ic_reto_escritura), contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Text("Empezar a escribir", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

/**
 * Ilustración de la consigna: Kiko con un dibujo del tema (huella, libro o lápiz).
 * El nombre viene de los datos semilla (T-319); si llega uno que la app no conoce, se usa el lápiz.
 */
@Composable
private fun IlustracionConsigna(clave: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(ColoresAventura.Naranja.copy(alpha = 0.15f))
            .semantics { contentDescription = "Ilustración: ${descripcionIlustracion(clave)}" }
            .testTag("ilustracion_consigna"),
        contentAlignment = Alignment.Center
    ) {
        Koala(tamano = 190.dp, saludando = false)
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .size(84.dp)
                .clip(CircleShape)
                .background(ColoresAventura.Naranja),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(iconoIlustracion(clave)), contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
        }
    }
}

@DrawableRes
fun iconoIlustracion(clave: String): Int = when (clave.lowercase()) {
    "animal" -> R.drawable.ic_ilustracion_animal
    "libro" -> R.drawable.ic_ilustracion_libro
    else -> R.drawable.ic_reto_escritura
}

private fun descripcionIlustracion(clave: String) = when (clave.lowercase()) {
    "animal" -> "huella de un animal"
    "libro" -> "un libro abierto"
    else -> "un lápiz"
}

@Composable
private fun ErrorConsigna(estado: ConsignaUiState.Error, onReintentar: () -> Unit, onVolver: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 32.dp)) {
        Koala(tamano = 160.dp, saludando = false)
        Spacer(Modifier.height(16.dp))
        Text(
            estado.mensaje,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = ColoresAventura.TextoOscuro,
            modifier = Modifier.testTag("error_consigna")
        )
        Spacer(Modifier.height(24.dp))
        if (estado.puedeReintentar) {
            Button(
                onClick = onReintentar,
                colors = ButtonDefaults.buttonColors(containerColor = ColoresAventura.Naranja),
                modifier = Modifier.height(56.dp).testTag("boton_reintentar_consigna")
            ) { Text("Intentar de nuevo", fontSize = 20.sp, color = Color.White) }
        } else {
            OutlinedButton(onClick = onVolver, modifier = Modifier.height(56.dp).testTag("boton_volver_error_consigna")) {
                Text("Volver", fontSize = 20.sp, color = ColoresAventura.TextoOscuro)
            }
        }
    }
}

private val CONSIGNA_DE_PRUEBA = ConsignaNivel(
    2, "Palabras nuevas", "Escribe tres oraciones sobre tu animal favorito.",
    "Cuenta cómo es, qué come y dónde vive.", "animal", 15
)

@Preview(name = "Consigna · animal", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun PantallaConsignaPreview() {
    AppMovilTheme(dynamicColor = false) {
        ContenidoConsigna(ConsignaUiState.Exito(CONSIGNA_DE_PRUEBA), onVolver = {}, onEmpezar = {}, onReintentar = {})
    }
}

@Preview(name = "Consigna · libro (celular)", widthDp = 400, heightDp = 800, showBackground = true)
@Composable
fun PantallaConsignaLibroPreview() {
    AppMovilTheme(dynamicColor = false) {
        ContenidoConsigna(
            ConsignaUiState.Exito(
                ConsignaNivel(5, "Mi relato", "Escribe un relato corto sobre un día que nunca vas a olvidar.",
                    "Piensa qué pasó primero, qué pasó después y cómo terminó.", "libro", 40)
            ),
            onVolver = {}, onEmpezar = {}, onReintentar = {}
        )
    }
}

@Preview(name = "Consigna · sin consigna", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun PantallaConsignaErrorPreview() {
    AppMovilTheme(dynamicColor = false) {
        ContenidoConsigna(
            ConsignaUiState.Error(ConsignaViewModel.MENSAJE_SIN_CONSIGNA, puedeReintentar = false),
            onVolver = {}, onEmpezar = {}, onReintentar = {}
        )
    }
}
