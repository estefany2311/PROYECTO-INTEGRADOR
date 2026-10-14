package com.proyecto.movil.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.proyecto.movil.model.Perfil
import com.proyecto.movil.viewmodel.PerfilViewModel
import com.proyecto.movil.viewmodel.PerfilesUiState

@Composable
fun PantallaPerfiles(codigoAula: String, viewModel: PerfilViewModel = viewModel()) {
    val estado by viewModel.uiState.collectAsState()

    LaunchedEffect(codigoAula) {
        viewModel.cargarPerfiles(codigoAula)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("¿Quién eres?", fontSize = 24.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp))

        when (val s = estado) {
            is PerfilesUiState.Cargando -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is PerfilesUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No se pudo cargar: ${s.mensaje}")
                }
            }
            is PerfilesUiState.Exito -> {
                CuadriculaPerfiles(perfiles = s.perfiles)
            }
        }
    }
}

@Composable
private fun CuadriculaPerfiles(perfiles: List<Perfil>) {
    LazyVerticalGrid(columns = GridCells.Fixed(3)) {
        items(perfiles) { perfil ->
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = perfil.avatarUrl,
                    contentDescription = "Avatar de ${perfil.nombre}",
                    modifier = Modifier.size(80.dp).clip(CircleShape)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = perfil.nombre, fontSize = 16.sp)
            }
        }
    }
}