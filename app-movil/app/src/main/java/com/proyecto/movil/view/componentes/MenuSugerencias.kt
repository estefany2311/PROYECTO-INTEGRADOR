package com.proyecto.movil.view.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proyecto.movil.model.ErrorOrtografico
import com.proyecto.movil.ui.theme.ColoresAventura

// T-366 (US-20): al tocar una palabra subrayada, las sugerencias del corrector (hasta 3, T-365)
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MenuSugerencias(
    error: ErrorOrtografico,
    onElegir: (String) -> Unit,
    onCerrar: () -> Unit
) {
    val opciones = error.opciones()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(2.dp, ColoresAventura.Rojo, RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("menu_sugerencias")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (opciones.isEmpty()) "Revisa cómo se escribe \"${error.palabra}\"" else "¿Quisiste decir…?",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = ColoresAventura.TextoOscuro,
                modifier = Modifier.weight(1f).testTag("titulo_sugerencias")
            )
            TextButton(onClick = onCerrar, modifier = Modifier.testTag("cerrar_sugerencias")) {
                Text("Cerrar", fontSize = 18.sp, color = ColoresAventura.TextoOscuro)
            }
        }
        if (opciones.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                opciones.forEach { opcion ->
                    Surface(
                        onClick = { onElegir(opcion) },
                        shape = RoundedCornerShape(28.dp),
                        color = Color.White,
                        border = BorderStroke(3.dp, ColoresAventura.Turquesa),
                        modifier = Modifier
                            .heightIn(min = 56.dp)
                            .semantics { contentDescription = "Cambiar por $opcion" }
                            .testTag("sugerencia_$opcion")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                            Text(opcion, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = ColoresAventura.TextoOscuro)
                        }
                    }
                }
            }
        }
    }
}
