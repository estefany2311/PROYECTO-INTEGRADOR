package com.proyecto.movil.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.proyecto.movil.model.ConsignaNivel
import com.proyecto.movil.ui.theme.AppMovilTheme
import com.proyecto.movil.ui.theme.ColoresAventura
import com.proyecto.movil.viewmodel.ConsignaUiState
import com.proyecto.movil.viewmodel.ConsignaViewModel
import com.proyecto.movil.viewmodel.EditorViewModel

//T-323 (US-18): el niño escribe su texto con letra grande. Se abre con "Empezar a escribir" (T-321)
 //Teclado en español y SIN autocorrector: lo que se evalúa es cómo escribe el niño, así que la tablet
//no debe corregirle las palabras. La primera letra de cada oración va en mayúscula.

@Composable
fun PantallaEditor(
    nivelId: Int,
    onVolver: () -> Unit,
    onEnviar: (String) -> Unit,
    consignaViewModel: ConsignaViewModel = viewModel(),
    editorViewModel: EditorViewModel = viewModel()
) {
    val consigna by consignaViewModel.uiState.collectAsState()
    val texto by editorViewModel.texto.collectAsState()

    LaunchedEffect(nivelId) { consignaViewModel.cargar(nivelId) }

    ContenidoEditor(
        consigna = consigna,
        texto = texto,
        onEscribir = editorViewModel::escribir,
        onVolver = onVolver,
        onEnviar = { onEnviar(texto) },
        onReintentar = consignaViewModel::reintentar
    )
}

// Sin ViewModel: para @Preview y para las pruebas de T-326
@Composable
fun ContenidoEditor(
    consigna: ConsignaUiState,
    texto: String,
    onEscribir: (String) -> Unit,
    onVolver: () -> Unit,
    onEnviar: () -> Unit,
    onReintentar: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(ColoresAventura.Cielo)
            .imePadding()      // con el teclado abierto, el botón de enviar sigue a la vista
    ) {
        CabeceraEscritura((consigna as? ConsignaUiState.Exito)?.consigna?.nombreNivel, onVolver)

        when (consigna) {
            is ConsignaUiState.Cargando -> Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(64.dp).testTag("cargando_editor"))
            }
            // Sin consigna no tiene sentido escribir: se muestra el mismo error que en la pantalla de consigna
            is ConsignaUiState.Error -> Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ErrorConsigna(consigna, onReintentar, onVolver)
            }
            is ConsignaUiState.Exito -> Hoja(consigna.consigna, texto, onEscribir, onEnviar)
        }
    }
}

@Composable
private fun ColumnScope.Hoja(consigna: ConsignaNivel, texto: String, onEscribir: (String) -> Unit, onEnviar: () -> Unit) {
    Column(
        Modifier
            .weight(1f)
            .align(Alignment.CenterHorizontally)
            .widthIn(max = 720.dp)
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        // La consigna queda arriba para que no olvide qué tiene que escribir
        Text(
            consigna.consigna,
            fontSize = 22.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.SemiBold,
            color = ColoresAventura.TextoOscuro,
            modifier = Modifier.testTag("consigna_editor")
        )
        Spacer(Modifier.height(16.dp))

        // La hoja: letra grande, interlineado amplio y borde naranja como en el mockup
        BasicTextField(
            value = texto,
            onValueChange = onEscribir,
            textStyle = TextStyle(fontSize = 30.sp, lineHeight = 46.sp, color = ColoresAventura.TextoOscuro),
            cursorBrush = SolidColor(ColoresAventura.Naranja),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Default,        // "Enter" hace un salto de línea
                hintLocales = LocaleList("es-PE")     // pide al teclado el idioma español (con ñ y tildes)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .border(3.dp, ColoresAventura.Naranja, RoundedCornerShape(24.dp))
                .semantics { contentDescription = "Escribe tu texto aquí" }
                .testTag("campo_texto"),
            decorationBox = { campo ->
                Box(Modifier.fillMaxSize().padding(24.dp)) {
                    if (texto.isEmpty()) {
                        Text(
                            "Toca aquí y empieza a escribir…",
                            fontSize = 30.sp,
                            lineHeight = 46.sp,
                            color = ColoresAventura.Gris,
                            modifier = Modifier.testTag("ayuda_campo_texto")
                        )
                    }
                    campo()
                }
            }
        )
        Spacer(Modifier.height(20.dp))
        // Por ahora se activa al escribir algo; T-324 lo activará al llegar al mínimo de palabras
        Button(
            onClick = onEnviar,
            enabled = texto.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = ColoresAventura.Naranja,
                disabledContainerColor = ColoresAventura.Gris
            ),
            shape = RoundedCornerShape(36.dp),
            modifier = Modifier.fillMaxWidth().height(72.dp).testTag("boton_enviar_texto")
        ) {
            Text("Enviar mi texto", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
        }
    }
}

private val CONSIGNA_EDITOR = ConsignaNivel(
    2, "Palabras nuevas", "Escribe tres oraciones sobre tu animal favorito.",
    "Cuenta cómo es, qué come y dónde vive.", "animal", 15
)

@Preview(name = "Editor · vacío", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun PantallaEditorVacioPreview() {
    AppMovilTheme(dynamicColor = false) {
        ContenidoEditor(ConsignaUiState.Exito(CONSIGNA_EDITOR), "", {}, {}, {}, {})
    }
}

@Preview(name = "Editor · escribiendo", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun PantallaEditorEscribiendoPreview() {
    AppMovilTheme(dynamicColor = false) {
        ContenidoEditor(
            ConsignaUiState.Exito(CONSIGNA_EDITOR),
            "Mi animal favorito es el perro. Se llama Toby y es muy juguetón. Come croquetas y vive en mi casa.",
            {}, {}, {}, {}
        )
    }
}
