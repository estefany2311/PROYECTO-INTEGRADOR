package com.proyecto.movil.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proyecto.movil.ui.theme.AppMovilTheme
import com.proyecto.movil.ui.theme.ColoresAventura
import com.proyecto.movil.ui.theme.Nunito
import com.proyecto.movil.view.componentes.BotonAventura
import com.proyecto.movil.view.componentes.KoalaSaludando

// T-441: como en el mockup: koala, título, campo grande con relieve amarillo y botón "Continuar"
@Composable
fun PantallaCodigoAula(onContinuar: (String) -> Unit) {
    var codigo by remember { mutableStateOf("") }
    val continuar = { if (codigo.isNotBlank()) onContinuar(codigo.trim()) }

    // Se puede deslizar si no cabe (móvil horizontal); si cabe queda centrada
    BoxWithConstraints(Modifier.fillMaxSize().background(ColoresAventura.Cielo)) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .heightIn(min = maxHeight)
            .fillMaxWidth()
            .padding(horizontal = 26.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        KoalaSaludando("¡Hola! Qué gusto verte", tamano = 150.dp, soloCara = true)

        Spacer(Modifier.height(14.dp))
        Text(
            text = "Ingresa el código\nde tu aula",
            fontSize = 28.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Black,
            color = ColoresAventura.TextoOscuro,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Tu profesor te lo puede dar",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = ColoresAventura.TextoSuave
        )

        Spacer(Modifier.height(22.dp))
        Text(
            "CÓDIGO DE AULA",
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = ColoresAventura.TextoSuave,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
        )
        // Campo blanco con borde amarillo; la franja de abajo más gruesa le da relieve
        BasicTextField(
            value = codigo,
            // Solo letras y números, en mayúsculas: así es fácil de copiar de la pizarra
            onValueChange = { nuevo -> codigo = nuevo.uppercase().filter { it.isLetterOrDigit() }.take(12) },
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = Nunito,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 5.sp,
                color = ColoresAventura.TextoOscuro,
                textAlign = TextAlign.Center
            ),
            cursorBrush = SolidColor(ColoresAventura.AmarilloBorde),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { continuar() }),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Código de aula" }
                .testTag("campo_codigo_aula"),
            decorationBox = { campo ->
                Box(
                    Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(ColoresAventura.AmarilloBorde)
                        .padding(bottom = 4.dp)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(ColoresAventura.Tarjeta)
                            .border(3.dp, ColoresAventura.Amarillo, RoundedCornerShape(18.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (codigo.isEmpty()) {
                            Text(
                                "",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColoresAventura.TextoSuave.copy(alpha = 0.6f)
                            )
                        }
                        campo()
                    }
                }
            }
        )

        Spacer(Modifier.height(24.dp))
        BotonAventura(
            texto = "Continuar",
            onClick = continuar,
            enabled = codigo.isNotBlank(),
            modifier = Modifier.fillMaxWidth().testTag("boton_continuar_codigo")
        )
    }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun PantallaCodigoAulaPreview() {
    AppMovilTheme { PantallaCodigoAula(onContinuar = {}) }
}
