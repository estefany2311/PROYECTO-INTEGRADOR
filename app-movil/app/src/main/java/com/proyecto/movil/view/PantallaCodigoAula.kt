package com.proyecto.movil.view

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proyecto.movil.ui.theme.AppMovilTheme

@Composable
fun PantallaCodigoAula(onContinuar: (String) -> Unit) {
    var codigo by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Ingresa el código de tu aula", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = codigo,
            onValueChange = { codigo = it },
            label = { Text("Código de aula") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { if (codigo.isNotBlank()) onContinuar(codigo) },
            enabled = codigo.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Continuar", fontSize = 18.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PantallaCodigoAulaPreview() {
    AppMovilTheme { PantallaCodigoAula(onContinuar = {}) }
}