package com.proyecto.movil.view

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.proyecto.movil.R
import com.proyecto.movil.model.MapaRespuesta
import com.proyecto.movil.model.NivelMapa
import com.proyecto.movil.model.TipoReto
import com.proyecto.movil.ui.theme.AppMovilTheme
import com.proyecto.movil.ui.theme.ColoresAventura
import com.proyecto.movil.viewmodel.MapaUiState
import com.proyecto.movil.viewmodel.MapaViewModel

// Inicio después del PIN: barra del alumno + mapa de aventura (T-210, US-05)
@Composable
fun PantallaMapa(
    onSalir: (codigoAula: String?) -> Unit,
    viewModel: MapaViewModel = viewModel()
) {
    val sesion = viewModel.sesion
    val estado by viewModel.uiState.collectAsState()

    // T-237: "Salir" primero pregunta; rememberSaveable lo mantiene abierto si se gira la tablet
    var preguntandoSalir by rememberSaveable { mutableStateOf(false) }
    // Evita tocar "Sí, salir" dos veces mientras se avisa al backend (hasta 3 s sin conexión)
    var saliendo by remember { mutableStateOf(false) }

    // Se pide el mapa una vez al abrir la pantalla
    LaunchedEffect(Unit) { viewModel.cargarMapa() }

    Column(Modifier.fillMaxSize().background(ColoresAventura.pasto)) {
        BarraEstudiante(
            nombre = sesion?.nombre ?: "",
            avatarUrl = sesion?.avatarUrl ?: "",
            onSalir = { if (!saliendo) preguntandoSalir = true }
        )
        ContenidoMapa(estado = estado, onReintentar = { viewModel.cargarMapa() })
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

/**
 * T-237 (US-04): confirmación antes de cerrar la sesión, con la cara del alumno
 * para que vea que es SU sesión la que se cierra. Así un toque sin querer no lo saca.
 * Botones grandes y con palabras completas: "Sí, salir" / "No, quedarme".
 */
@Composable
fun DialogoSalir(
    nombre: String,
    avatarUrl: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,       // tocar fuera o "atrás" = quedarse
        containerColor = Color.White,
        modifier = Modifier.testTag("dialogo_salir"),
        icon = {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Avatar de $nombre",
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(ColoresAventura.Cielo)
            )
        },
        title = {
            Text(
                "¿Salir, $nombre?",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = ColoresAventura.TextoOscuro
            )
        },
        text = {
            Text(
                "Así otro compañero puede usar la tablet.",
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                color = ColoresAventura.TextoOscuro,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                colors = ButtonDefaults.buttonColors(containerColor = ColoresAventura.Naranja),
                modifier = Modifier.height(56.dp).testTag("boton_confirmar_salir")
            ) {
                Text("Sí, salir", fontSize = 20.sp, color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onCancelar,
                modifier = Modifier.height(56.dp).testTag("boton_cancelar_salir")
            ) {
                Text("No, quedarme", fontSize = 20.sp, color = ColoresAventura.TextoOscuro)
            }
        }
    )
}

// Separado de PantallaMapa para poder verlo en Preview y probarlo sin ViewModel (T-212)
@Composable
fun ContenidoMapa(estado: MapaUiState, onReintentar: () -> Unit) {
    when (estado) {
        is MapaUiState.Cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(64.dp).testTag("cargando_mapa"))
        }
        is MapaUiState.Error -> ErrorMapa(estado.mensaje, onReintentar)
        is MapaUiState.Exito -> RecorridoNiveles(estado.mapa)
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

// T-211: tamaños según la pantalla. Desde 600 dp de ancho se considera tablet
private data class MedidasMapa(
    val nodo: Dp,           // diámetro del círculo de cada nivel
    val alturaFila: Dp,     // espacio vertical de cada nivel (círculo + nombre)
    val anchoEtiqueta: Dp,  // ancho del nombre debajo del círculo
    val texto: TextUnit,
    val numero: TextUnit
) {
    companion object {
        val TELEFONO = MedidasMapa(96.dp, 172.dp, 150.dp, 18.sp, 18.sp)
        val TABLET = MedidasMapa(128.dp, 214.dp, 220.dp, 22.sp, 22.sp)
    }
}

private val ANCHO_TABLET = 600.dp
private val ANCHO_MAXIMO_MAPA = 720.dp   // en tablets grandes el mapa no se estira de borde a borde

// Posición horizontal de cada nivel (fracción del ancho): izquierda, centro, derecha, centro...
private val POSICIONES_X = listOf(0.25f, 0.5f, 0.75f, 0.5f)

// T-210/T-211: resumen de avance arriba y los niveles unidos por un camino que serpentea
@Composable
fun RecorridoNiveles(mapa: MapaRespuesta) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val medidas = if (maxWidth >= ANCHO_TABLET) MedidasMapa.TABLET else MedidasMapa.TELEFONO
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.widthIn(max = ANCHO_MAXIMO_MAPA).fillMaxWidth()) {
                ResumenAvance(mapa.completados, mapa.totalNiveles, mapa.puntos)
                Spacer(Modifier.height(24.dp))
                CaminoDeNiveles(mapa.niveles.sortedBy { it.orden }, medidas)
            }
        }
    }
}

/**
 * T-211: el camino se dibuja con Canvas (una línea de puntos blancos con curvas)
 * en vez de imágenes: no pesa nada y se ve nítido en cualquier tamaño de tablet.
 * Cada círculo se coloca justo sobre su punto del camino.
 */
@Composable
private fun CaminoDeNiveles(niveles: List<NivelMapa>, medidas: MedidasMapa) {
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(medidas.alturaFila * niveles.size)
    ) {
        val ancho = maxWidth
        fun centroX(i: Int): Dp = ancho * POSICIONES_X[i % POSICIONES_X.size]
        fun centroY(i: Int): Dp = medidas.alturaFila * i + medidas.nodo / 2

        Canvas(Modifier.matchParentSize()) {
            if (niveles.size < 2) return@Canvas
            val camino = Path()
            niveles.indices.forEach { i ->
                val x = centroX(i).toPx()
                val y = centroY(i).toPx()
                if (i == 0) {
                    camino.moveTo(x, y)
                } else {
                    // Curva suave: sale hacia abajo del nivel anterior y llega desde arriba al siguiente
                    val xAnterior = centroX(i - 1).toPx()
                    val yMedio = (centroY(i - 1).toPx() + y) / 2
                    camino.cubicTo(xAnterior, yMedio, x, yMedio, x, y)
                }
            }
            drawPath(
                camino,
                color = Color.White,
                style = Stroke(
                    width = 10.dp.toPx(),
                    cap = StrokeCap.Round,
                    // trazo casi nulo + punta redonda = puntitos
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(1f, 22.dp.toPx()))
                )
            )
        }

        niveles.forEachIndexed { i, nivel ->
            NodoNivel(
                nivel = nivel,
                medidas = medidas,
                modifier = Modifier
                    .offset(x = centroX(i) - medidas.anchoEtiqueta / 2, y = medidas.alturaFila * i)
                    .width(medidas.anchoEtiqueta)
            )
        }
    }
}

@Composable
private fun ResumenAvance(completados: Int, total: Int, puntos: Int) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(16.dp)
            .testTag("resumen_mapa")
    ) {
        Text(
            "Completaste $completados de $total niveles",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = ColoresAventura.TextoOscuro
        )
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else completados.toFloat() / total },
            color = ColoresAventura.Turquesa,
            trackColor = ColoresAventura.Cielo,
            modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp))
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            // T-211: estrella en vector (Color.Unspecified conserva su naranja)
            Icon(
                painterResource(R.drawable.ic_estrella),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text("$puntos puntos", fontSize = 20.sp, color = ColoresAventura.TextoOscuro)
        }
    }
}

/**
 * Un nivel: círculo del color de su reto con el ícono del reto,
 * el número en una insignia arriba a la izquierda y el nombre debajo.
 * Los estilos por estado (candado y gris) se agregan en T-239.
 */
@Composable
private fun NodoNivel(nivel: NivelMapa, medidas: MedidasMapa, modifier: Modifier = Modifier) {
    val color = colorDelReto(nivel.reto)
    Column(
        modifier
            .testTag("nivel_${nivel.orden}")
            .semantics { contentDescription = "Nivel ${nivel.orden}: ${nivel.nombre}" },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(medidas.nodo)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(color)
                    .border(5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val icono = iconoDelReto(nivel.reto)
                if (icono != null) {
                    Icon(
                        painterResource(icono),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(medidas.nodo * 0.45f)
                    )
                }
            }
            // Insignia con el número del nivel
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .size(medidas.nodo * 0.38f)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(3.dp, color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${nivel.orden}",
                    fontSize = medidas.numero,
                    fontWeight = FontWeight.Bold,
                    color = ColoresAventura.TextoOscuro
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            nivel.nombre,
            fontSize = medidas.texto,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = ColoresAventura.TextoOscuro
        )
    }
}

// T-211: ícono de cada reto (vectores en res/drawable). Si el reto es desconocido no se pone ícono
@DrawableRes
private fun iconoDelReto(reto: TipoReto): Int? = when (reto) {
    TipoReto.LECTURA -> R.drawable.ic_reto_lectura
    TipoReto.ESCRITURA -> R.drawable.ic_reto_escritura
    TipoReto.ORALIDAD -> R.drawable.ic_reto_oralidad
    TipoReto.DESCONOCIDO -> null
}

// Cada tipo de reto tiene su color, para que el niño los reconozca
private fun colorDelReto(reto: TipoReto): Color = when (reto) {
    TipoReto.LECTURA -> ColoresAventura.Turquesa
    TipoReto.ESCRITURA -> ColoresAventura.Naranja
    TipoReto.ORALIDAD -> Color(0xFF4EA8DE)
    TipoReto.DESCONOCIDO -> ColoresAventura.Gris
}

// T-208: barra de arriba con el avatar, el nombre y el botón "Salir" (US-04)
@Composable
fun BarraEstudiante(nombre: String, avatarUrl: String, onSalir: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = "Avatar de $nombre",
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(ColoresAventura.Cielo)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            nombre,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = ColoresAventura.TextoOscuro,
            modifier = Modifier.weight(1f)
        )
        // Botón grande (56dp de alto) para que el niño lo toque fácil
        OutlinedButton(
            onClick = onSalir,
            modifier = Modifier.height(56.dp).testTag("boton_salir")
        ) {
            Text("Salir", fontSize = 22.sp, color = ColoresAventura.TextoOscuro)
        }
    }
}

@Preview(widthDp = 800, heightDp = 300, showBackground = true)
@Composable
fun BarraEstudiantePreview() {
    AppMovilTheme(dynamicColor = false) {
        BarraEstudiante(nombre = "Ana", avatarUrl = "", onSalir = {})
    }
}

@Preview(widthDp = 800, heightDp = 600, showBackground = true)
@Composable
fun DialogoSalirPreview() {
    AppMovilTheme(dynamicColor = false) {
        DialogoSalir(nombre = "Ana", avatarUrl = "", onConfirmar = {}, onCancelar = {})
    }
}

@Preview(name = "Mapa · teléfono", widthDp = 360, heightDp = 1200, showBackground = true)
@Preview(name = "Mapa · tablet", widthDp = 1280, heightDp = 1500, showBackground = true)
@Composable
fun RecorridoNivelesPreview() {
    val tipos = listOf("lectura", "escritura", "oralidad")
    val niveles = (1..6).map { n ->
        NivelMapa(n, n, "Nivel $n", tipos[(n - 1) % 3], null, if (n == 1) "desbloqueado" else "bloqueado")
    }
    AppMovilTheme(dynamicColor = false) {
        Box(Modifier.background(ColoresAventura.pasto)) {
            RecorridoNiveles(MapaRespuesta(1, "Ana", "Pérez", 6, 0, 0, niveles))
        }
    }
}
