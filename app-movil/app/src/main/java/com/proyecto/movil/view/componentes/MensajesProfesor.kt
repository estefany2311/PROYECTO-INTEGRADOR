package com.proyecto.movil.view.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proyecto.movil.model.MensajeDocente
import com.proyecto.movil.model.fechaCorta
import com.proyecto.movil.model.nombreReto
import com.proyecto.movil.ui.theme.ColoresAventura

// "Mensajes de tu profesor": una tarjeta por comentario, la más reciente arriba
@Composable
fun MensajesProfesor(mensajes: List<MensajeDocente>, modifier: Modifier = Modifier) {
    Column(modifier.testTag("mensajes_profesor"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Mensajes de tu profesor", fontSize = 20.sp, fontWeight = FontWeight.Black, color = ColoresAventura.TextoOscuro)
        if (mensajes.isEmpty()) {
            Text(
                "Aquí verás lo que tu profesor te escriba sobre tus lecturas y textos.",
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = ColoresAventura.TextoSuave,
                modifier = Modifier.testTag("sin_mensajes")
            )
        } else {
            mensajes.forEach { TarjetaMensaje(it) }
        }
    }
}

@Composable
private fun TarjetaMensaje(mensaje: MensajeDocente) {
    val color = if (mensaje.tipo == "texto") ColoresAventura.Naranja else ColoresAventura.Turquesa
    // Mismo relieve que las otras tarjetas: franja de abajo del color del reto
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(color)
            .padding(bottom = 5.dp)
            .testTag("mensaje_${mensaje.evidenciaId}")
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(ColoresAventura.Tarjeta)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Koala(tamano = 48.dp, saludando = false, soloCara = true, feliz = true)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${mensaje.nombreReto()} · ${mensaje.nivelNombre ?: "Nivel ${mensaje.nivelId}"}",
                        fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = color,
                        modifier = Modifier.weight(1f)
                    )
                    Text(fechaCorta(mensaje.fecha), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ColoresAventura.TextoSuave)
                }
                Text(mensaje.comentario, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = ColoresAventura.TextoOscuro)
            }
        }
    }
}
