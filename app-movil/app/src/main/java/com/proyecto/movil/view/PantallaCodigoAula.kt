package com.proyecto.movil.view

import androidx.compose.foundation.layout.*
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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

@Preview(showBackground = true)
@Composable
fun PantallaCodigoAulaPreview() {
    AppMovilTheme { PantallaCodigoAula(onContinuar = {}) }
}