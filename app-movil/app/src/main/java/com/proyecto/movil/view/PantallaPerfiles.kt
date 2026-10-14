package com.proyecto.movil.view

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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
                Text("${s.perfiles.size} perfiles encontrados")
                // La cuadrícula con los avatares la conectamos en la T-5
            }
        }
    }
}