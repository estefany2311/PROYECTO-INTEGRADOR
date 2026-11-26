package com.proyecto.movil.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.proyecto.movil.model.Perfil
import com.proyecto.movil.ui.theme.ColoresAventura
import com.proyecto.movil.view.componentes.BotonAventura
import com.proyecto.movil.view.componentes.EstiloBoton
import com.proyecto.movil.view.componentes.Koala
import com.proyecto.movil.viewmodel.PerfilViewModel
import com.proyecto.movil.viewmodel.PerfilesUiState

// T-441: colores de los avatares (relleno y franja de abajo), uno por alumno en orden
private val ColoresAvatar = listOf(
    ColoresAventura.Naranja to ColoresAventura.NaranjaBorde,
    ColoresAventura.Turquesa to ColoresAventura.TurquesaBorde,
    ColoresAventura.Azul to ColoresAventura.AzulBorde,
    Color(0xFFB86BDB) to Color(0xFF9149B5)
)

@Composable
fun PantallaPerfiles(
    codigoAula: String,
    onPerfilElegido: (Perfil) -> Unit,
    onCambiarCodigo: () -> Unit = {},
    viewModel: PerfilViewModel = viewModel()
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(codigoAula) {
        viewModel.cargarPerfiles(codigoAula)
    }

    Column(modifier = Modifier.fillMaxSize().background(ColoresAventura.Cielo)) {
        // T-441: koala, título y en qué aula está, como en el mockup
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 6.dp)
        ) {
            Koala(tamano = 64.dp, saludando = false, soloCara = true)
            Column {
                Text("¿Quién eres?", fontSize = 30.sp, fontWeight = FontWeight.Black, color = ColoresAventura.TextoOscuro)
                Text("Toca tu nombre para entrar", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ColoresAventura.TextoSuave)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(ColoresAventura.AmarilloSuave)
                .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
        ) {
            Text(
                "Aula $codigoAula",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = ColoresAventura.AmarilloTexto,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onCambiarCodigo, modifier = Modifier.testTag("boton_cambiar_codigo")) {
                Text("Cambiar código", fontSize = 14.sp, fontWeight = FontWeight.Black, color = ColoresAventura.AmarilloTexto)
            }
        }

        when (val s = estado) {
            is PerfilesUiState.Cargando -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ColoresAventura.AmarilloBorde)
                }
            }
            is PerfilesUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "¡Ups! Algo no funcionó.",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = ColoresAventura.TextoOscuro
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Dice qué pasó: aula que no existe, sin internet o error del servidor
                        Text(
                            text = s.mensaje,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColoresAventura.TextoSuave,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        BotonAventura(
                            "Intentar otra vez",
                            onClick = { viewModel.cargarPerfiles(codigoAula) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        BotonAventura(
                            "Cambiar código",
                            onClick = onCambiarCodigo,
                            estilo = EstiloBoton.Secundario,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            is PerfilesUiState.Exito -> {
                if (s.perfiles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Todavía no hay estudiantes en esta aula.\nPídele a tu profesor que te agregue.",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColoresAventura.TextoOscuro,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    CuadriculaPerfiles(perfiles = s.perfiles, onPerfilElegido = onPerfilElegido)
                }
            }
        }
    }
}

// T-441: tarjetas blancas con relieve; el avatar va sobre un círculo de color con su inicial
// (si la imagen no carga, por ejemplo sin internet, se ve la inicial)
@Composable
private fun CuadriculaPerfiles(perfiles: List<Perfil>, onPerfilElegido: (Perfil) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),   // 2 columnas en el teléfono, más en la tablet
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        itemsIndexed(perfiles) { index, perfil ->
            val (color, borde) = ColoresAvatar[index % ColoresAvatar.size]
            val forma = RoundedCornerShape(22.dp)
            // La franja de abajo (Linea) es el relieve de la tarjeta
            Box(
                Modifier
                    .clip(forma)
                    .background(ColoresAventura.Linea)
                    .clickable(role = Role.Button) { onPerfilElegido(perfil) }
                    .padding(start = 2.dp, end = 2.dp, top = 2.dp, bottom = 6.dp)
                    .testTag("perfil_${perfil.id}")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(ColoresAventura.Tarjeta)
                        .padding(top = 14.dp, bottom = 12.dp, start = 8.dp, end = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(borde)
                            .padding(bottom = 5.dp)
                            .clip(CircleShape)
                            .background(color),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            perfil.nombre.take(1).uppercase(),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1D2A3A)
                        )
                        AsyncImage(
                            model = perfil.avatarUrl,
                            contentDescription = "Avatar de ${perfil.nombre}",
                            modifier = Modifier.fillMaxSize().padding(4.dp).clip(CircleShape)
                        )
                    }
                    Text(
                        text = perfil.nombre,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        color = ColoresAventura.TextoOscuro,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
