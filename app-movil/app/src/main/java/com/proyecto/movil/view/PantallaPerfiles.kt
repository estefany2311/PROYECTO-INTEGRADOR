package com.proyecto.movil.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import com.proyecto.movil.ui.theme.*
import com.proyecto.movil.viewmodel.PerfilViewModel
import com.proyecto.movil.viewmodel.PerfilesUiState

@Composable
fun PantallaPerfiles(
    codigoAula: String,
    onPerfilElegido: (Perfil) -> Unit,
    viewModel: PerfilViewModel = viewModel()
) {
    val estado by viewModel.uiState.collectAsState()

    LaunchedEffect(codigoAula) {
        viewModel.cargarPerfiles(codigoAula)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "¿Quién eres?",
            fontSize = 28.sp, // Título más grande para niños
            fontWeight = FontWeight.Bold,
            color = TextoOscuro,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when (val s = estado) {
            is PerfilesUiState.Cargando -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is PerfilesUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "¡Ups! Algo no funcionó.",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextoOscuro
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pídele ayuda a tu profesor.",
                            fontSize = 18.sp,
                            color = TextoOscuro
                        )
                    }
                }
            }
            is PerfilesUiState.Exito -> {
                if (s.perfiles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Todavía no hay estudiantes en esta aula.\nPídele a tu profesor que te agregue.",
                            fontSize = 18.sp,
                            color = TextoOscuro,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    CuadriculaPerfiles(perfiles = s.perfiles, onPerfilElegido = onPerfilElegido)
                }
            }
        }
    }
}

@Composable
private fun CuadriculaPerfiles(perfiles: List<Perfil>, onPerfilElegido: (Perfil) -> Unit) {
    // Lista de los colores vibrantes solicitados para las figuras/avatares
    val coloresAvatar = listOf(
        MoradoAvatar,    // #9B22E1
        RojoAvatar,      // #F51E38
        AmarilloAvatar,  // #FFF133
        AzulClaroAvatar  // #2FC5F6
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2), // 2 columnas para elementos más grandes y fáciles de tocar
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        itemsIndexed(perfiles) { index, perfil ->
            val colorFondo = coloresAvatar[index % coloresAvatar.size]

            // Toda la tarjeta (avatar + nombre) se puede tocar para ir al PIN
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth()
                    .clickable { onPerfilElegido(perfil) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp) // Iconos más grandes (antes 80.dp)
                        .clip(CircleShape)
                        .background(colorFondo)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = perfil.avatarUrl,
                        contentDescription = "Avatar de ${perfil.nombre}",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = perfil.nombre,
                    fontSize = 20.sp, // Texto más legible para primaria
                    fontWeight = FontWeight.Bold,
                    color = TextoOscuro
                )
            }
        }
    }
}