package com.proyecto.movil.view

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proyecto.movil.ui.theme.AppMovilTheme
import com.proyecto.movil.ui.theme.ColoresAventura

/*
 * Mascota de la plataforma: el koala leyendo un libro.
 * Es el mismo dibujo del portal Admin (web-portal/admin/src/components/Koala.jsx, de Daniela),
 * pasado a Compose: se dibuja con Canvas sobre una cuadrícula de 200 x 200, igual que el SVG.
 * Animación (la misma del portal): flota de arriba abajo y saluda con la mano.
 */

private val GrisOreja = Color(0xFF9AA6B2)
private val RosaOreja = Color(0xFFF6C6D0)
private val GrisCabeza = Color(0xFFAEB8C2)
private val GrisPanza = Color(0xFFD9DFE5)
private val Ojo = Color(0xFF1D3557)
private val Nariz = Color(0xFF3B4A5C)
private val BrilloNariz = Color(0xFF5A6B7F)
private val Cachete = Color(0xFFF6A9B8)
private val TapaIzquierda = Color(0xFF0E9F92)
private val TapaDerecha = Color(0xFF0A8378)
private val HojaDerecha = Color(0xFFF4FAFF)
private val LineaHoja = Color(0xFFB8C4D6)
private val Estrella = Color(0xFFFF9F1C)

@Composable
fun Koala(modifier: Modifier = Modifier, tamano: Dp = 180.dp, saludando: Boolean = true) {
    val animacion = rememberInfiniteTransition(label = "koala")
    // Flota 8 de 200 unidades hacia arriba y vuelve (3,5 s la vuelta completa)
    val flota by animacion.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(tween(1750, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flota"
    )
    // Saluda: gira la mano -16° y 12°, y descansa un momento
    val mano by animacion.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 1600
                0f at 0
                -16f at 320
                12f at 640
                0f at 960
                0f at 1600
            }
        ),
        label = "mano"
    )

    Canvas(
        modifier
            .size(tamano)
            .graphicsLayer { translationY = flota / 200f * size.height }
            .semantics { contentDescription = "Koala, la mascota de la plataforma" }
    ) {
        dibujarKoala(escala = size.width / 200f, anguloMano = if (saludando) mano else null)
    }
}

// Dibuja el koala. Las coordenadas son las del SVG original (0..200) multiplicadas por la escala
private fun DrawScope.dibujarKoala(escala: Float, anguloMano: Float?) {
    fun p(x: Float, y: Float) = Offset(x * escala, y * escala)
    fun circulo(color: Color, cx: Float, cy: Float, r: Float, alfa: Float = 1f) =
        drawCircle(color, radius = r * escala, center = p(cx, cy), alpha = alfa)
    fun ovalo(color: Color, cx: Float, cy: Float, rx: Float, ry: Float) =
        drawOval(color, topLeft = p(cx - rx, cy - ry), size = Size(2 * rx * escala, 2 * ry * escala))
    fun poligono(color: Color, vararg puntos: Float) {
        val camino = Path().apply {
            moveTo(puntos[0] * escala, puntos[1] * escala)
            for (i in 2 until puntos.size step 2) lineTo(puntos[i] * escala, puntos[i + 1] * escala)
            close()
        }
        drawPath(camino, color)
    }
    fun linea(x1: Float, y1: Float, x2: Float, y2: Float) =
        drawLine(LineaHoja, p(x1, y1), p(x2, y2), strokeWidth = 2f * escala, cap = StrokeCap.Round)

    // Orejas
    circulo(GrisOreja, 48f, 62f, 34f); circulo(RosaOreja, 48f, 62f, 20f)
    circulo(GrisOreja, 152f, 62f, 34f); circulo(RosaOreja, 152f, 62f, 20f)

    // Cuerpo
    ovalo(GrisOreja, 100f, 168f, 52f, 34f)
    ovalo(GrisPanza, 100f, 172f, 32f, 22f)

    // Cabeza
    ovalo(GrisCabeza, 100f, 98f, 60f, 54f)

    // Ojos con brillo
    circulo(Ojo, 76f, 92f, 8f); circulo(Ojo, 124f, 92f, 8f)
    circulo(Color.White, 79f, 89f, 3f); circulo(Color.White, 127f, 89f, 3f)

    // Nariz y sonrisa
    ovalo(Nariz, 100f, 112f, 15f, 18f)
    ovalo(BrilloNariz, 95f, 104f, 4f, 5f)
    val sonrisa = Path().apply {
        moveTo(90f * escala, 134f * escala)
        quadraticTo(100f * escala, 142f * escala, 110f * escala, 134f * escala)
    }
    drawPath(sonrisa, Nariz, style = Stroke(width = 3.5f * escala, cap = StrokeCap.Round))

    // Cachetes
    circulo(Cachete, 62f, 118f, 9f, alfa = 0.7f); circulo(Cachete, 138f, 118f, 9f, alfa = 0.7f)

    // Libro abierto
    poligono(TapaIzquierda, 100f, 160f, 58f, 150f, 58f, 186f, 100f, 196f)
    poligono(TapaDerecha, 100f, 160f, 142f, 150f, 142f, 186f, 100f, 196f)
    poligono(Color.White, 100f, 162f, 64f, 154f, 64f, 182f, 100f, 190f)
    poligono(HojaDerecha, 100f, 162f, 136f, 154f, 136f, 182f, 100f, 190f)
    linea(72f, 162f, 94f, 167f); linea(72f, 170f, 94f, 175f)
    linea(106f, 167f, 128f, 162f); linea(106f, 175f, 128f, 170f)

    // Patitas: la izquierda sujeta el libro; la derecha saluda (o también sujeta el libro)
    ovalo(GrisOreja, 60f, 166f, 11f, 9f)
    if (anguloMano != null) {
        rotate(anguloMano, pivot = p(154f, 140f)) { ovalo(GrisOreja, 164f, 128f, 11f, 13f) }
    } else {
        ovalo(GrisOreja, 140f, 166f, 11f, 9f)
    }

    // Estrellita naranja
    poligono(
        Estrella,
        170f, 30f, 174f, 40f, 185f, 41f, 177f, 48f, 179f, 59f,
        170f, 53f, 161f, 59f, 163f, 48f, 155f, 41f, 166f, 40f
    )
}

// Globo de diálogo blanco con la puntita hacia el koala
@Composable
fun BurbujaKoala(texto: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            texto,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = ColoresAventura.TextoOscuro,
            modifier = Modifier
                .shadow(6.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(horizontal = 18.dp, vertical = 10.dp)
        )
        Canvas(Modifier.size(width = 18.dp, height = 9.dp)) {
            val punta = Path().apply {
                moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2, size.height); close()
            }
            drawPath(punta, Color.White)
        }
    }
}

// El koala con su saludo, como en el inicio del portal Admin
@Composable
fun KoalaSaludando(mensaje: String, modifier: Modifier = Modifier, tamano: Dp = 180.dp) {
    Column(modifier.testTag("koala"), horizontalAlignment = Alignment.CenterHorizontally) {
        BurbujaKoala(mensaje)
        Spacer(Modifier.height(4.dp))
        Koala(tamano = tamano)
    }
}

@Preview(widthDp = 320, heightDp = 320, showBackground = true, backgroundColor = 0xFF1D3557)
@Composable
fun KoalaPreview() {
    AppMovilTheme(dynamicColor = false) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            KoalaSaludando("¡Hola! Qué gusto verte")
        }
    }
}
