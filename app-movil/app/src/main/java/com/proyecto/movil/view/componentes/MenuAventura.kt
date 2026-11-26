package com.proyecto.movil.view.componentes

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.proyecto.movil.R
import com.proyecto.movil.ui.theme.ColoresAventura

// Pestañas del menú inferior. Tareas se agrega cuando el backend docente liste las del alumno
enum class Pestana(val etiqueta: String, @DrawableRes val icono: Int) {
    AVENTURA("Aventura", R.drawable.ic_menu_aventura),
    LOGROS("Logros", R.drawable.ic_menu_logros),
    PERFIL("Perfil", R.drawable.ic_menu_perfil)
}

// Barra de arriba: avatar del niño y sus contadores (racha, estrellas, puntos), como en el mockup.
// racha = null: todavía no se sabe (no se muestra)
@Composable
fun BarraAventura(nombre: String, avatarUrl: String, racha: Int?, estrellas: Int, puntos: Int) {
    Column(Modifier.fillMaxWidth().background(ColoresAventura.Tarjeta)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            // Avatar sobre un círculo naranja con relieve; si la imagen no carga se ve la inicial
            Box(
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(ColoresAventura.NaranjaBorde)
                    .padding(bottom = 3.dp)
                    .clip(CircleShape)
                    .background(ColoresAventura.Naranja),
                contentAlignment = Alignment.Center
            ) {
                Text(nombre.take(1).uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF2B1A00))
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Avatar de $nombre",
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (racha != null) Contador(R.drawable.ic_racha, racha, Color(0xFFC2410C), "Racha de $racha días", "barra_racha")
                Contador(R.drawable.ic_estrella_llena, estrellas, Color(0xFF8A6A00), "$estrellas estrellas", "barra_estrellas")
                Contador(R.drawable.ic_moneda, puntos, ColoresAventura.AzulBorde, "$puntos puntos", "barra_puntos")
            }
        }
        HorizontalDivider(thickness = 2.dp, color = ColoresAventura.Linea)
    }
}

@Composable
private fun Contador(@DrawableRes icono: Int, valor: Int, color: Color, descripcion: String, tag: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = descripcion }.testTag(tag)
    ) {
        Icon(painterResource(icono), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(24.dp))
        Text("$valor", fontSize = 18.sp, fontWeight = FontWeight.Black, color = color)
    }
}

// Menú inferior con tres pestañas grandes; la activa va en una pastilla amarilla
@Composable
fun MenuInferior(actual: Pestana, onElegir: (Pestana) -> Unit) {
    Column(Modifier.fillMaxWidth().background(ColoresAventura.Tarjeta)) {
        HorizontalDivider(thickness = 2.dp, color = ColoresAventura.Linea)
        Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 10.dp)) {
            Pestana.entries.forEach { pestana ->
                val activa = pestana == actual
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .selectable(selected = activa, role = Role.Tab, onClick = { onElegir(pestana) })
                        .testTag("pestana_${pestana.name.lowercase()}")
                ) {
                    val pastilla = if (activa) {
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(ColoresAventura.AmarilloSuave)
                            .border(2.dp, ColoresAventura.Amarillo, RoundedCornerShape(14.dp))
                    } else Modifier
                    Box(pastilla.padding(horizontal = 18.dp, vertical = 5.dp)) {
                        Icon(
                            painterResource(pestana.icono),
                            contentDescription = null,
                            tint = if (activa) ColoresAventura.AmarilloTexto else ColoresAventura.TextoSuave,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Text(
                        pestana.etiqueta,
                        fontSize = 13.sp,
                        fontWeight = if (activa) FontWeight.Black else FontWeight.ExtraBold,
                        color = if (activa) ColoresAventura.AmarilloTexto else ColoresAventura.TextoSuave
                    )
                }
            }
        }
    }
}
