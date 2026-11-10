package com.proyecto.movil.view

import androidx.compose.foundation.ScrollState
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
import androidx.compose.ui.graphics.Brush
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
import com.proyecto.movil.model.LecturaNivel
import com.proyecto.movil.ui.theme.AppMovilTheme
import com.proyecto.movil.ui.theme.ColoresAventura
import com.proyecto.movil.viewmodel.LecturaUiState
import com.proyecto.movil.viewmodel.LecturaViewModel


@Composable
fun PantallaLectura(
    nivelId: Int,
    onVolver: () -> Unit,
    onGrabar: (LecturaNivel) -> Unit,
    viewModel: LecturaViewModel = viewModel()
) {
    val estado by viewModel.uiState.collectAsState()
    val indice by viewModel.indiceTamano.collectAsState()

    LaunchedEffect(nivelId) { viewModel.cargar(nivelId) }

    ContenidoLectura(
        estado = estado,
        onVolver = onVolver,
        onGrabar = onGrabar,
        onReintentar = viewModel::reintentar,
        tamanoLetra = LecturaViewModel.TAMANOS_LETRA[indice],
        puedeAchicar = indice > 0,
        puedeAgrandar = indice < LecturaViewModel.TAMANOS_LETRA.lastIndex,
        onAchicar = viewModel::achicarLetra,
        onAgrandar = viewModel::agrandarLetra
    )
}

// Sin ViewModel: para @Preview y para las pruebas de T-312
@Composable
fun ContenidoLectura(
    estado: LecturaUiState,
    onVolver: () -> Unit,
    onGrabar: (LecturaNivel) -> Unit,
    onReintentar: () -> Unit,
    tamanoLetra: Int = 32,
    puedeAchicar: Boolean = true,
    puedeAgrandar: Boolean = true,
    onAchicar: () -> Unit = {},
    onAgrandar: () -> Unit = {}
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(ColoresAventura.Cielo)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onVolver, modifier = Modifier.testTag("boton_volver_lectura")) {
                Text("←  Volver", fontSize = 20.sp, color = ColoresAventura.TextoOscuro)
            }
            Spacer(Modifier.weight(1f))
            // T-311: tamaño de letra (solo cuando hay texto que leer)
            if (estado is LecturaUiState.Exito) {
                BotonTamano("A−", "Letra más pequeña", puedeAchicar, onAchicar, "boton_letra_menos")
                Spacer(Modifier.width(12.dp))
                BotonTamano("A+", "Letra más grande", puedeAgrandar, onAgrandar, "boton_letra_mas")
            }
        }
        Spacer(Modifier.height(8.dp))

        when (estado) {
            is LecturaUiState.Cargando ->
                CircularProgressIndicator(Modifier.padding(48.dp).size(64.dp).testTag("cargando_lectura"))
            is LecturaUiState.Exito -> TextoParaLeer(estado.lectura, tamanoLetra) { onGrabar(estado.lectura) }
            is LecturaUiState.Error -> MensajeError(estado, onReintentar, onVolver)
        }
    }
}

@Composable
private fun BotonTamano(texto: String, descripcion: String, habilitado: Boolean, onClick: () -> Unit, tag: String) {
    OutlinedButton(
        onClick = onClick,
        enabled = habilitado,
        modifier = Modifier
            .size(width = 72.dp, height = 56.dp)
            .semantics { contentDescription = descripcion }
            .testTag(tag),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(texto, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = if (habilitado) ColoresAventura.TextoOscuro else ColoresAventura.Gris)
    }
}

// El texto ocupa el espacio libre y se desliza solo él; el botón de grabar queda fijo abajo
@Composable
private fun ColumnScope.TextoParaLeer(lectura: LecturaNivel, tamanoLetra: Int, onGrabar: () -> Unit) {
    val desplazamiento = rememberScrollState()
    Column(
        Modifier.widthIn(max = 720.dp).fillMaxWidth().weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Lee el texto en voz alta",
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = ColoresAventura.TextoOscuro
        )
        Spacer(Modifier.height(18.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(desplazamiento)
                    .padding(horizontal = 28.dp, vertical = 24.dp)
                    .testTag("texto_lectura")
            ) {
                // El título no pasa de 34 sp y tiene su propio interlineado: si ocupa dos líneas no se montan
                val tamanoTitulo = minOf(tamanoLetra, 34)
                Text(
                    lectura.titulo,
                    fontSize = tamanoTitulo.sp,
                    lineHeight = (tamanoTitulo * 1.3f).sp,
                    fontWeight = FontWeight.Bold,
                    color = ColoresAventura.Turquesa
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    lectura.texto,
                    fontSize = tamanoLetra.sp,
                    lineHeight = (tamanoLetra * 1.5f).sp,      // interlineado amplio en todos los tamaños
                    color = ColoresAventura.TextoOscuro,
                    modifier = Modifier.testTag("cuerpo_lectura")
                )
            }
            AvisoSigueLeyendo(desplazamiento, Modifier.align(Alignment.BottomCenter))
        }
        Spacer(Modifier.height(24.dp))
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

// T-311: si queda texto más abajo, una franja suave avisa que se puede deslizar
@Composable
private fun AvisoSigueLeyendo(desplazamiento: ScrollState, modifier: Modifier = Modifier) {
    if (!desplazamiento.canScrollForward) return
    Box(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0f), Color.White)))
            .padding(top = 28.dp, bottom = 10.dp)
            .testTag("aviso_sigue_leyendo"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Desliza para seguir leyendo ↓",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = ColoresAventura.TextoOscuro.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun MensajeError(estado: LecturaUiState.Error, onReintentar: () -> Unit, onVolver: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 32.dp)) {
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

private val LECTURA_DE_PRUEBA = LecturaNivel(
    1, 3, "El koala Kiko",
    "Había una vez un koala llamado Kiko que vivía en un árbol muy alto. " +
        "Todas las mañanas leía un libro con sus amigos del bosque. " +
        "Un día encontró un libro nuevo debajo de una hoja grande. " +
        "Tenía dibujos de ríos, montañas y estrellas. Kiko lo leyó en voz alta para todos sus amigos, " +
        "y desde entonces cada tarde se reúnen bajo el árbol para escuchar un cuento.",
    2, "basica", false
)

@Preview(name = "Lectura · 32 sp", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun PantallaLecturaPreview() {
    AppMovilTheme(dynamicColor = false) {
        ContenidoLectura(LecturaUiState.Exito(LECTURA_DE_PRUEBA), onVolver = {}, onGrabar = {}, onReintentar = {})
    }
}

// Texto largo con la letra más grande: aparece el aviso para seguir leyendo
@Preview(name = "Lectura · 44 sp (texto largo)", widthDp = 400, heightDp = 800, showBackground = true)
@Composable
fun PantallaLecturaGrandePreview() {
    AppMovilTheme(dynamicColor = false) {
        ContenidoLectura(
            LecturaUiState.Exito(LECTURA_DE_PRUEBA), onVolver = {}, onGrabar = {}, onReintentar = {},
            tamanoLetra = 44, puedeAgrandar = false
        )
    }
}
