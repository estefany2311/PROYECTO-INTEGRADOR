package com.proyecto.movil.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.proyecto.movil.Dependencias
import com.proyecto.movil.ui.theme.ColoresAventura

// Inicio después del PIN. En T-210 se convierte en el mapa de aventura.
@Composable
fun PantallaMapa() {
    val nombre = Dependencias.almacenSesion.leer()?.nombre ?: ""
    Box(
        modifier = Modifier.fillMaxSize().background(ColoresAventura.pasto),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "¡Hola, $nombre!\nTu aventura empieza aquí.",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}