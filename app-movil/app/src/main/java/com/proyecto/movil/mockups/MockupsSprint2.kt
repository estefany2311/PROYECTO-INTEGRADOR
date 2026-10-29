package com.proyecto.movil.view.mockups

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proyecto.movil.ui.theme.AppMovilTheme

// T-201: mockups del sprint 2. Solo se ven en el panel Preview (tamaño tablet).

// Colores del módulo estudiante (vivos y con buen contraste para niños)
private val Cielo = Color(0xFFE3F4FF)
private val Pasto = Color(0xFFDDF5DC)
private val Naranja = Color(0xFFFF9F1C)      // nivel disponible
private val Turquesa = Color(0xFF2EC4B6)     // nivel completado
private val Gris = Color(0xFFB0B7C3)         // nivel bloqueado
private val Azul = Color(0xFF3A86FF)         // reto de lectura
private val Rojo = Color(0xFFE63946)         // errores
private val TextoOscuro = Color(0xFF1D3557)

// ---------- 1. Teclado del PIN (US-02, US-03) ----------
@Preview(name = "1 · PIN", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun MockupPin() {
    AppMovilTheme(dynamicColor = false) {
        Column(
            modifier = Modifier.fillMaxSize().background(Cielo).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Avatar
            Box(Modifier.size(120.dp).clip(CircleShape).background(Naranja))
            Spacer(Modifier.height(12.dp))

            Text("Hola, Ana", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = TextoOscuro)
            Text("Escribe tu clave secreta", fontSize = 22.sp, color = TextoOscuro)
            Spacer(Modifier.height(24.dp))

            // PIN oculto: 2 números escritos (rellenos) y 2 por escribir (vacíos)
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                repeat(4) { i ->
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (i < 2) TextoOscuro else Color.White)
                            .border(3.dp, TextoOscuro, CircleShape)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // Mensaje de PIN incorrecto (US-03)
            Text(
                "¡Uy! Esa no es tu clave. Te quedan 4 intentos.",
                color = Rojo, fontSize = 20.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(24.dp))

            // Teclado numérico grande
            val filas = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "⌫")
            )
            filas.forEach { fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    fila.forEach { tecla -> TeclaMockup(tecla) }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

// Una tecla redonda y grande. La tecla vacía (abajo a la izquierda) queda invisible.
@Composable
private fun TeclaMockup(texto: String) {
    Box(
        modifier = Modifier
            .size(110.dp)
            .clip(CircleShape)
            .background(if (texto.isEmpty()) Color.Transparent else Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text(texto, fontSize = 44.sp, fontWeight = FontWeight.Bold, color = TextoOscuro)
    }
}

// ---------- 2. Mapa de aventura (US-05, US-06) ----------
@Preview(name = "2 · Mapa", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun MockupMapa() {
    AppMovilTheme(dynamicColor = false) {
        Column(Modifier.fillMaxSize().background(Pasto)) {
            BarraSuperiorMockup()

            // Cada nivel en una fila; el ancho cambia para formar el zigzag
            val posiciones = listOf(0.25f, 0.5f, 0.75f, 0.5f, 0.25f, 0.5f)
            val colores = listOf(Turquesa, Turquesa, Naranja, Gris, Gris, Gris)

            posiciones.forEachIndexed { i, x ->
                Box(Modifier.fillMaxWidth().height(170.dp)) {
                    Box(
                        Modifier.fillMaxWidth(x).align(Alignment.CenterStart),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Box(
                            Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(colores[i])
                                .border(6.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val bloqueado = colores[i] == Gris
                            Text(
                                if (bloqueado) "🔒" else "${i + 1}",
                                fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// Barra de arriba: avatar, nombre y botón Salir (US-04)
@Composable
private fun BarraSuperiorMockup() {
    Row(
        Modifier.fillMaxWidth().background(Color.White).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(56.dp).clip(CircleShape).background(Naranja))
        Spacer(Modifier.width(12.dp))
        Text(
            "Ana", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextoOscuro,
            modifier = Modifier.weight(1f)
        )
        OutlinedButton(onClick = {}) { Text("Salir", fontSize = 20.sp) }
    }
}

// ---------- 3. Detalle del nivel (US-07) ----------
@Preview(name = "3 · Nivel", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun MockupNivel() {
    AppMovilTheme(dynamicColor = false) {
        Column(
            Modifier.fillMaxSize().background(Cielo).padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Nivel 3", fontSize = 28.sp, color = TextoOscuro)
            Text(
                "El zorro y las uvas", fontSize = 40.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, color = TextoOscuro
            )
            Spacer(Modifier.height(32.dp))

            // Ícono grande del tipo de reto
            Box(
                Modifier.size(200.dp).clip(RoundedCornerShape(40.dp)).background(Azul),
                contentAlignment = Alignment.Center
            ) {
                Text("📖", fontSize = 96.sp)
            }
            Spacer(Modifier.height(16.dp))

            Text("Reto de LECTURA", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Azul)
            Text("Lee el cuento con calma.", fontSize = 22.sp, color = TextoOscuro)
            Spacer(Modifier.height(40.dp))

            Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(88.dp)) {
                Text("¡Comenzar!", fontSize = 32.sp)
            }
        }
    }
}

// ---------- 4. Lectura (US-10) ----------
@Preview(name = "4 · Lectura", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun MockupLectura() {
    AppMovilTheme(dynamicColor = false) {
        Column(Modifier.fillMaxSize().background(Color(0xFFFFFDF5)).padding(40.dp)) {
            Text("El zorro y las uvas", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = TextoOscuro)
            Spacer(Modifier.height(24.dp))
            Text(
                "Un zorro vio unas uvas moradas en lo alto de una parra. Saltó una vez, " +
                        "saltó dos veces y saltó tres veces, pero no las alcanzó. " +
                        "Cansado, se fue diciendo: «Esas uvas todavía están verdes».",
                fontSize = 36.sp, lineHeight = 54.sp, color = TextoOscuro,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(88.dp)) {
                Text("¡Terminé de leer!", fontSize = 30.sp)
            }
        }
    }
}