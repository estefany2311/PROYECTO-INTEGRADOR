package com.proyecto.movil.view.componentes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proyecto.movil.R
import com.proyecto.movil.model.EstadoNivel
import com.proyecto.movil.model.MapaRespuesta
import com.proyecto.movil.model.NivelMapa
import com.proyecto.movil.ui.theme.AppMovilTheme
import com.proyecto.movil.ui.theme.ColoresAventura
import kotlinx.coroutines.delay

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
fun RecorridoNiveles(
    mapa: MapaRespuesta,
    onNivelTocado: (NivelMapa) -> Unit = {},
    recienDesbloqueados: Set<Int> = emptySet(),
    onAnimacionTerminada: () -> Unit = {}
) {
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
                ResumenAvance(mapa.completados, mapa.totalNiveles, mapa.puntos, mapa.estrellas)
                Spacer(Modifier.height(24.dp))
                CaminoDeNiveles(mapa.niveles.sortedBy { it.orden }, medidas, onNivelTocado, recienDesbloqueados, onAnimacionTerminada)
            }
        }
    }
}

@Composable
private fun CaminoDeNiveles(
    niveles: List<NivelMapa>,
    medidas: MedidasMapa,
    onNivelTocado: (NivelMapa) -> Unit,
    recienDesbloqueados: Set<Int> = emptySet(),
    onAnimacionTerminada: () -> Unit = {}
) {
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
                onClick = { onNivelTocado(nivel) },
                recienDesbloqueado = nivel.id in recienDesbloqueados,
                onAnimacionTerminada = onAnimacionTerminada,
                modifier = Modifier
                    .offset(x = centroX(i) - medidas.anchoEtiqueta / 2, y = medidas.alturaFila * i)
                    .width(medidas.anchoEtiqueta)
            )
        }
    }
}

@Composable
private fun ResumenAvance(completados: Int, total: Int, puntos: Int, estrellas: Int) {
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
        // T-304 (US-09): estrellas y puntos acumulados, cada uno con su ícono
        Row(verticalAlignment = Alignment.CenterVertically) {
            // T-211: estrella en vector (Color.Unspecified conserva su naranja)
            Icon(
                painterResource(R.drawable.ic_estrella),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                if (estrellas == 1) "1 estrella" else "$estrellas estrellas",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = ColoresAventura.TextoOscuro,
                modifier = Modifier.testTag("contador_estrellas")
            )
            Spacer(Modifier.width(24.dp))
            Icon(
                painterResource(R.drawable.ic_puntos),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                if (puntos == 1) "1 punto" else "$puntos puntos",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = ColoresAventura.TextoOscuro,
                modifier = Modifier.testTag("contador_puntos")
            )
        }
    }
}

@Composable
private fun NodoNivel(
    nivel: NivelMapa,
    medidas: MedidasMapa,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    recienDesbloqueado: Boolean = false,
    onAnimacionTerminada: () -> Unit = {}
) {
    // T-216: un nivel recién desbloqueado empieza viéndose como bloqueado (gris con candado),
    // el candado se sacude, y luego el círculo pasa al color de su reto con un pequeño salto.
    var mostrarCandado by remember(nivel.id, recienDesbloqueado) { mutableStateOf(recienDesbloqueado) }
    val escala = remember { Animatable(1f) }
    val giroCandado = remember { Animatable(0f) }
    LaunchedEffect(nivel.id, recienDesbloqueado) {
        if (!recienDesbloqueado) return@LaunchedEffect
        kotlinx.coroutines.delay(400)
        listOf(-14f, 14f, -10f, 10f, 0f).forEach { giroCandado.animateTo(it, tween(80)) }
        mostrarCandado = false
        escala.animateTo(1.25f, tween(180))
        escala.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        onAnimacionTerminada()
    }

    val bloqueado = nivel.bloqueado || mostrarCandado
    val color by animateColorAsState(
        targetValue = if (bloqueado) ColoresAventura.Gris else estiloDe(nivel.reto).color,
        animationSpec = tween(500),
        label = "colorNivel"
    )
    val colorTexto = if (bloqueado) ColoresAventura.TextoOscuro.copy(alpha = 0.55f) else ColoresAventura.TextoOscuro
    Column(
        modifier
            .testTag("nivel_${nivel.orden}")
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)   // T-240
            .semantics {
                contentDescription = "Nivel ${nivel.orden}: ${nivel.nombre}"
                // TalkBack lee además el estado: "bloqueado", "disponible" o "completado"
                stateDescription = textoEstado(nivel.estadoNivel)
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(medidas.nodo)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = escala.value; scaleY = escala.value }
                    .clip(CircleShape)
                    .background(color)
                    .border(5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // T-239: candado en los bloqueados; el ícono del reto en los demás
                val icono = if (bloqueado) R.drawable.ic_candado else estiloDe(nivel.reto).icono
                if (icono != null) {
                    Icon(
                        painterResource(icono),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(medidas.nodo * 0.45f)
                            .graphicsLayer { rotationZ = if (mostrarCandado) giroCandado.value else 0f }
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
                    color = colorTexto
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
            color = colorTexto
        )
    }
}

// T-239: lo que lee TalkBack después del nombre del nivel
private fun textoEstado(estado: EstadoNivel): String = when (estado) {
    EstadoNivel.BLOQUEADO -> "bloqueado"
    EstadoNivel.DESBLOQUEADO -> "disponible"
    EstadoNivel.COMPLETADO -> "completado"
}

@Preview(name = "Mapa · teléfono", widthDp = 360, heightDp = 1200, showBackground = true)
@Preview(name = "Mapa · tablet", widthDp = 1280, heightDp = 1500, showBackground = true)
@Composable
fun RecorridoNivelesPreview() {
    val tipos = listOf("lectura", "escritura", "oralidad")
    val niveles = (1..6).map { n ->
        // 1 completado, 2 disponible, 3 a 6 bloqueados (T-239)
        val estado = when (n) { 1 -> "completado"; 2 -> "desbloqueado"; else -> "bloqueado" }
        NivelMapa(n, n, "Nivel $n", tipos[(n - 1) % 3], null, estado)
    }
    AppMovilTheme(dynamicColor = false) {
        Box(Modifier.background(ColoresAventura.pasto)) {
            RecorridoNiveles(MapaRespuesta(1, "Ana", "Pérez", 6, 1, 10, niveles))
        }
    }
}
