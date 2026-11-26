package com.proyecto.movil.view

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proyecto.movil.R
import com.proyecto.movil.model.MapaRespuesta
import com.proyecto.movil.model.MensajeDocente
import com.proyecto.movil.model.TipoReto
import com.proyecto.movil.ui.theme.ColoresAventura
import com.proyecto.movil.view.componentes.MensajesProfesor

// Avance por tipo de reto: cuántos niveles de ese tipo completó del total
data class AvanceReto(val reto: TipoReto, val completados: Int, val total: Int)

fun avancePorReto(mapa: MapaRespuesta): List<AvanceReto> =
    listOf(TipoReto.LECTURA, TipoReto.ESCRITURA, TipoReto.ORALIDAD).map { reto ->
        val delTipo = mapa.niveles.filter { it.reto == reto }
        AvanceReto(reto, delTipo.count { it.completado }, delTipo.size)
    }

// Pestaña "Mis logros": contadores y avance salen del mapa ya cargado; los mensajes del profesor, de su endpoint
@Composable
fun ContenidoLogros(mapa: MapaRespuesta, mensajes: List<MensajeDocente> = emptyList()) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Mis logros", fontSize = 28.sp, fontWeight = FontWeight.Black, color = ColoresAventura.TextoOscuro)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Dato(R.drawable.ic_estrella_llena, mapa.estrellas, "estrellas", ColoresAventura.AmarilloSuave, ColoresAventura.Amarillo, Modifier.weight(1f).testTag("logro_estrellas"))
            Dato(R.drawable.ic_moneda, mapa.puntos, "puntos", Color(0xFFE2F1FB), ColoresAventura.Azul, Modifier.weight(1f).testTag("logro_puntos"))
        }

        MensajesProfesor(mensajes)

        Text("Niveles completados", fontSize = 20.sp, fontWeight = FontWeight.Black, color = ColoresAventura.TextoOscuro)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(ColoresAventura.Tarjeta)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            avancePorReto(mapa).filter { it.total > 0 }.forEach { BarraReto(it) }
        }
    }
}

@Composable
private fun Dato(@DrawableRes icono: Int, valor: Int, nombre: String, fondo: Color, borde: Color, modifier: Modifier) {
    // Tarjeta blanca con la franja de abajo del color del dato (relieve)
    Box(modifier.clip(RoundedCornerShape(18.dp)).background(borde).padding(bottom = 6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(ColoresAventura.Tarjeta)
                .padding(12.dp)
        ) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(fondo), contentAlignment = Alignment.Center) {
                Icon(painterResource(icono), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(28.dp))
            }
            Column {
                Text("$valor", fontSize = 24.sp, fontWeight = FontWeight.Black, color = ColoresAventura.TextoOscuro)
                Text(nombre, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = ColoresAventura.TextoSuave)
            }
        }
    }
}

@Composable
private fun BarraReto(avance: AvanceReto) {
    val (nombre, color, icono) = when (avance.reto) {
        TipoReto.LECTURA -> Triple("Lectura", ColoresAventura.Turquesa, R.drawable.ic_reto_lectura)
        TipoReto.ESCRITURA -> Triple("Escritura", ColoresAventura.Naranja, R.drawable.ic_reto_escritura)
        else -> Triple("Oralidad", ColoresAventura.Azul, R.drawable.ic_reto_oralidad)
    }
    val fraccion = if (avance.total == 0) 0f else avance.completados.toFloat() / avance.total
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(color), contentAlignment = Alignment.Center) {
            Icon(painterResource(icono), contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Column(Modifier.weight(1f)) {
            Row {
                Text(nombre, fontSize = 15.sp, fontWeight = FontWeight.Black, color = ColoresAventura.TextoOscuro, modifier = Modifier.weight(1f))
                Text("${avance.completados} de ${avance.total}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = ColoresAventura.TextoSuave)
            }
            Spacer(Modifier.height(5.dp))
            Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(ColoresAventura.Linea)) {
                Box(Modifier.fillMaxWidth(fraccion).fillMaxHeight().clip(RoundedCornerShape(6.dp)).background(color))
            }
        }
    }
}
