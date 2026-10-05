package com.proyecto.movil.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proyecto.movil.ui.theme.AppMovilTheme

// Datos de ejemplo, solo para ver cómo se vería (en la T-5 vienen del backend)
private val perfilesDeEjemplo = listOf("Ana", "Luis", "Mía", "Pedro", "Sofía", "Iker")

@Composable
fun PantallaPerfiles() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "¿Quién eres?",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        LazyVerticalGrid(columns = GridCells.Fixed(3)) {
            items(perfilesDeEjemplo) { nombre ->
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFBBDEFB))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = nombre, fontSize = 16.sp)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PantallaPerfilesPreview() {
    AppMovilTheme {
        PantallaPerfiles()
    }
}