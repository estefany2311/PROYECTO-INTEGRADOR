package com.proyecto.movil.view.mockups

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proyecto.movil.ui.theme.AppMovilTheme
import com.proyecto.movil.ui.theme.ColoresAventura
import com.proyecto.movil.view.Koala

private val Fondo = ColoresAventura.Cielo
private val Texto = ColoresAventura.TextoOscuro

// Barra de arriba común: volver, nivel y tipo de reto con su color
@Composable
private fun Cabecera(nivel: String, reto: String, color: Color) {
    Row(
        Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("←  Volver", fontSize = 20.sp, color = Texto)
        Spacer(Modifier.weight(1f))
        Text(nivel, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Texto)
        Spacer(Modifier.weight(1f))
        Text(
            reto, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White,
            modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(color).padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

// Botón grande de los mockups (no hace nada: es solo diseño)
@Composable
private fun BotonGrande(texto: String, color: Color, modifier: Modifier = Modifier, alto: Dp = 72.dp, contorno: Boolean = false) {
    Box(
        modifier
            .height(alto)
            .clip(RoundedCornerShape(36.dp))
            .background(if (contorno) Color.White else color)
            .border(if (contorno) 3.dp else 0.dp, color, RoundedCornerShape(36.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(texto, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = if (contorno) color else Color.White)
    }
}

@Composable
private fun Tarjeta(modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color.White).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = contenido
    )
}

private const val TEXTO_LECTURA =
    "Había una vez un koala llamado Kiko que vivía en un árbol muy alto. " +
        "Todas las mañanas leía un libro con sus amigos del bosque."

// ---------- 1. Lectura con letra grande (US-10) ----------
@Preview(name = "1 · Lectura", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun MockupLecture() {
    AppMovilTheme(dynamicColor = false) {
        Column(Modifier.fillMaxSize().background(Fondo)) {
            Cabecera("Nivel 1 · Primeros pasos", "Lectura", ColoresAventura.Turquesa)
            Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Lee el texto en voz alta", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Texto)
                Spacer(Modifier.height(20.dp))
                Tarjeta {
                    // Letra grande e interlineado amplio para primeros grados
                    Text(TEXTO_LECTURA, fontSize = 34.sp, lineHeight = 50.sp, color = Texto)
                }
                Spacer(Modifier.height(16.dp))
                // Control del tamaño de letra (T-311)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BotonGrande("A−", Texto, Modifier.width(96.dp), alto = 56.dp, contorno = true)
                    Spacer(Modifier.width(16.dp))
                    Text("Tamaño de letra", fontSize = 18.sp, color = Texto)
                    Spacer(Modifier.width(16.dp))
                    BotonGrande("A+", Texto, Modifier.width(96.dp), alto = 56.dp, contorno = true)
                }
                Spacer(Modifier.height(40.dp))
                BotonGrande("🎤  Estoy listo, grabar", ColoresAventura.Naranja, Modifier.fillMaxWidth())
            }
        }
    }
}

// ---------- 2. Permiso de micrófono explicado para niños (US-11, T-313) ----------
@Preview(name = "2 · Permiso de micrófono", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun MockupPermisoMicrofono() {
    AppMovilTheme(dynamicColor = false) {
        Column(
            Modifier.fillMaxSize().background(Fondo).padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Koala(tamano = 200.dp)
            Spacer(Modifier.height(24.dp))
            Tarjeta {
                Text(
                    "Para escuchar cómo lees, necesito usar el micrófono",
                    fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Texto, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Tu voz solo se usa para revisar tu lectura. Pide ayuda a tu profesor si aparece un mensaje.",
                    fontSize = 22.sp, color = Texto, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(32.dp))
                BotonGrande("Sí, usar el micrófono", ColoresAventura.Naranja, Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                BotonGrande("Ahora no", Texto, Modifier.fillMaxWidth(), alto = 64.dp, contorno = true)
            }
        }
    }
}

// ---------- 3. Grabando: indicador visual y tiempo (US-11, T-315/T-316) ----------
@Preview(name = "3 · Grabando", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun MockupGrabando() {
    AppMovilTheme(dynamicColor = false) {
        Column(Modifier.fillMaxSize().background(Fondo)) {
            Cabecera("Nivel 1 · Primeros pasos", "Lectura", ColoresAventura.Turquesa)
            Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Tarjeta {
                    Text(TEXTO_LECTURA, fontSize = 30.sp, lineHeight = 44.sp, color = Texto)
                }
                Spacer(Modifier.height(40.dp))
                // Círculo rojo grande: en la app late (animación) mientras graba
                Box(
                    Modifier.size(180.dp).clip(CircleShape).background(ColoresAventura.Rojo.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(120.dp).clip(CircleShape).background(ColoresAventura.Rojo), contentAlignment = Alignment.Center) {
                        Text("🎤", fontSize = 48.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Grabando… 0:12", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = ColoresAventura.Rojo)
                Text("Se detiene sola al llegar a 2:00", fontSize = 18.sp, color = Texto)
                Spacer(Modifier.height(16.dp))
                // Barra del tiempo usado
                Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)).background(Color.White)) {
                    Box(Modifier.fillMaxWidth(0.1f).fillMaxHeight().background(ColoresAventura.Rojo))
                }
                Spacer(Modifier.height(32.dp))
                BotonGrande("■  Terminar", Texto, Modifier.fillMaxWidth())
            }
        }
    }
}

// ---------- 4. Consigna de escritura con ilustración (US-17) ----------
@Preview(name = "4 · Consigna de escritura", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun MockupConsigna() {
    AppMovilTheme(dynamicColor = false) {
        Column(Modifier.fillMaxSize().background(Fondo)) {
            Cabecera("Nivel 2 · Palabras nuevas", "Escritura", ColoresAventura.Naranja)
            Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Tarjeta {
                    // Ilustración de la consigna (en la app vendrá de los datos semilla, T-319)
                    Box(
                        Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(20.dp))
                            .background(ColoresAventura.Naranja.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) { Koala(tamano = 200.dp, saludando = false) }
                    Spacer(Modifier.height(24.dp))
                    Text("Tu reto", fontSize = 22.sp, color = ColoresAventura.Naranja, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Escribe qué le gusta hacer a Kiko el koala por las mañanas.",
                        fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Texto, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Usa al menos 20 palabras", fontSize = 20.sp, color = Texto)
                }
                Spacer(Modifier.height(40.dp))
                BotonGrande("Empezar a escribir", ColoresAventura.Naranja, Modifier.fillMaxWidth())
            }
        }
    }
}

// ---------- 5. Editor con contador de palabras y errores marcados (US-18, US-19) ----------
@Preview(name = "5 · Editor", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun MockupEditor() {
    // Palabras con error ortográfico: subrayadas y en rojo (T-331)
    val texto = buildAnnotatedString {
        append("Kiko se despierta temprano y ")
        withStyle(SpanStyle(color = ColoresAventura.Rojo, textDecoration = TextDecoration.Underline)) { append("bebe") }
        append(" agua. Despues ")
        withStyle(SpanStyle(color = ColoresAventura.Rojo, textDecoration = TextDecoration.Underline)) { append("lle") }
        append(" su libro favorito con sus ")
        withStyle(SpanStyle(color = ColoresAventura.Rojo, textDecoration = TextDecoration.Underline)) { append("amijos") }
        append(" del bosque")
    }
    AppMovilTheme(dynamicColor = false) {
        Column(Modifier.fillMaxSize().background(Fondo)) {
            Cabecera("Nivel 2 · Palabras nuevas", "Escritura", ColoresAventura.Naranja)
            Column(Modifier.padding(32.dp)) {
                Text(
                    "Escribe qué le gusta hacer a Kiko por las mañanas.",
                    fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Texto
                )
                Spacer(Modifier.height(16.dp))
                // Hoja del editor: letra grande
                Box(
                    Modifier.fillMaxWidth().height(520.dp).clip(RoundedCornerShape(24.dp)).background(Color.White)
                        .border(3.dp, ColoresAventura.Naranja, RoundedCornerShape(24.dp)).padding(28.dp)
                ) {
                    Text(texto, fontSize = 30.sp, lineHeight = 46.sp, color = Texto)
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Contador de palabras (T-324)
                    Text("18 de 20 palabras", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Texto)
                    Spacer(Modifier.weight(1f))
                    // Guardado automático (T-325)
                    Text("✓ Guardado", fontSize = 18.sp, color = ColoresAventura.Turquesa, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                Text("Las palabras en rojo pueden tener un error. ¡Revísalas!", fontSize = 18.sp, color = ColoresAventura.Rojo)
                Spacer(Modifier.height(28.dp))
                // Desactivado hasta llegar al mínimo de palabras
                BotonGrande("Enviar mi texto", ColoresAventura.Gris, Modifier.fillMaxWidth())
            }
        }
    }
}

// ---------- 6. Nivel completado y siguiente desbloqueado (US-08) ----------
@Preview(name = "6 · Nivel completado", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun MockupNivelCompletado() {
    AppMovilTheme(dynamicColor = false) {
        Column(
            Modifier.fillMaxSize().background(Fondo).padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("¡Lo lograste!", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Texto)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) { i ->
                    Text("★", fontSize = 72.sp, color = if (i < 2) ColoresAventura.Naranja else ColoresAventura.Gris)
                }
            }
            Text("Ganaste 2 estrellas", fontSize = 24.sp, color = Texto)
            Spacer(Modifier.height(24.dp))
            Koala(tamano = 220.dp)
            Spacer(Modifier.height(24.dp))
            Tarjeta {
                Text("Se abrió un nivel nuevo", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Texto)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Candado gris que se abre y pasa al color del reto (animación T-216)
                    Box(Modifier.size(80.dp).clip(CircleShape).background(ColoresAventura.Gris), contentAlignment = Alignment.Center) {
                        Text("🔒", fontSize = 32.sp)
                    }
                    Text("  →  ", fontSize = 32.sp, color = Texto)
                    Box(Modifier.size(80.dp).clip(CircleShape).background(ColoresAventura.Naranja), contentAlignment = Alignment.Center) {
                        Text("2", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Nivel 2 · Palabras nuevas", fontSize = 20.sp, color = Texto)
            }
            Spacer(Modifier.height(32.dp))
            BotonGrande("Volver al mapa", ColoresAventura.Naranja, Modifier.fillMaxWidth())
        }
    }
}
