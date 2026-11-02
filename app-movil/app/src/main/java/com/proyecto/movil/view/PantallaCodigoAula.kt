package com.proyecto.movil.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proyecto.movil.ui.theme.AppMovilTheme
import com.proyecto.movil.ui.theme.AzulClaroAvatar
import com.proyecto.movil.ui.theme.TextoOscuro

@Composable
fun PantallaCodigoAula(onContinuar: (String) -> Unit) {
    var codigo by remember { mutableStateOf("") }

    // Con el koala la pantalla es más alta: se puede deslizar si no cabe (móvil horizontal),
    // y si cabe queda centrada (heightIn = alto de la pantalla)
    BoxWithConstraints(Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .heightIn(min = maxHeight)
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Mascota de la plataforma (el koala del portal Admin) dando la bienvenida
        KoalaSaludando("¡Hola! Qué gusto verte", tamano = 170.dp)

        Spacer(modifier = Modifier.height(24.dp))

        // Título más grande y amigable para primaria
        Text(
            text = "Ingresa el código de tu aula",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextoOscuro,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Campo de texto amplio, redondeado y con bordes resaltados
        OutlinedTextField(
            value = codigo,
            onValueChange = { codigo = it },
            placeholder = {
                Text(
                    text = "Código de aula",
                    fontSize = 18.sp,
                    color = TextoOscuro.copy(alpha = 0.5f)
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AzulClaroAvatar,
                unfocusedBorderColor = TextoOscuro.copy(alpha = 0.3f),
                cursorColor = AzulClaroAvatar
            ),
            textStyle = LocalTextStyle.current.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextoOscuro,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Botón grande y colorido (estilo cápsula)
        Button(
            onClick = { if (codigo.isNotBlank()) onContinuar(codigo.trim()) },
            enabled = codigo.isNotBlank(),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = AzulClaroAvatar,
                contentColor = androidx.compose.ui.graphics.Color.White,
                disabledContainerColor = AzulClaroAvatar.copy(alpha = 0.4f),
                disabledContentColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "Continuar",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun PantallaCodigoAulaPreview() {
    AppMovilTheme { PantallaCodigoAula(onContinuar = {}) }
}