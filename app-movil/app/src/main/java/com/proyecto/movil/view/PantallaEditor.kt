package com.proyecto.movil.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
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
import com.proyecto.movil.R
import com.proyecto.movil.model.ConsignaNivel
import com.proyecto.movil.model.ErrorOrtografico
import com.proyecto.movil.model.contarPalabras
import com.proyecto.movil.ui.theme.AppMovilTheme
import com.proyecto.movil.ui.theme.ColoresAventura
import com.proyecto.movil.viewmodel.ConsignaUiState
import com.proyecto.movil.viewmodel.ConsignaViewModel
import com.proyecto.movil.viewmodel.EditorViewModel
import com.proyecto.movil.viewmodel.EstadoGuardado

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
    val guardado by editorViewModel.guardado.collectAsState()
    val errores by editorViewModel.errores.collectAsState()
    val sinRevisar by editorViewModel.sinRevisar.collectAsState()

    LaunchedEffect(nivelId) {
        consignaViewModel.cargar(nivelId)
        editorViewModel.abrir(nivelId)      // T-325: recupera el borrador si lo había
    }

    ContenidoEditor(
        consigna = consigna,
        texto = texto,
        onEscribir = editorViewModel::escribir,
        onVolver = onVolver,
        onEnviar = { onEnviar(texto) },
        onReintentar = consignaViewModel::reintentar,
        guardado = guardado,
        errores = errores,
        sinRevisar = sinRevisar
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
    onReintentar: () -> Unit,
    guardado: EstadoGuardado = EstadoGuardado.NADA,
    errores: List<ErrorOrtografico> = emptyList(),
    sinRevisar: Boolean = false
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
            is ConsignaUiState.Exito -> Hoja(consigna.consigna, texto, guardado, errores, sinRevisar, onEscribir, onEnviar)
        }
    }
}

@Composable
private fun ColumnScope.Hoja(
    consigna: ConsignaNivel,
    texto: String,
    guardado: EstadoGuardado,
    errores: List<ErrorOrtografico>,
    sinRevisar: Boolean,
    onEscribir: (String) -> Unit,
    onEnviar: () -> Unit
) {
    // Con poca altura (celular en horizontal o teclado abierto) la hoja tiene un alto fijo y todo se desliza
    BoxWithConstraints(
        Modifier
            .weight(1f)
            .align(Alignment.CenterHorizontally)
            .widthIn(max = 720.dp)
            .fillMaxWidth()
    ) {
    val compacto = maxHeight < ALTO_COMPACTO
    Column(
        Modifier
            .fillMaxSize()
            .then(if (compacto) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(if (compacto) 16.dp else 24.dp)
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
            // T-331: subraya en rojo las palabras con error
            visualTransformation = remember(errores) { SubrayarErrores(errores) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Default,        // "Enter" hace un salto de línea
                hintLocales = LocaleList("es-PE")     // pide al teclado el idioma español (con ñ y tildes)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (compacto) Modifier.height(ALTO_HOJA_COMPACTA) else Modifier.weight(1f))
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .border(3.dp, ColoresAventura.Naranja, RoundedCornerShape(24.dp))
                .semantics { contentDescription = "Escribe tu texto aquí" }
                .testTag("campo_texto"),
            decorationBox = { campo ->
                Box(Modifier.fillMaxSize().padding(if (compacto) 16.dp else 24.dp)) {
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
        Spacer(Modifier.height(16.dp))
        // T-331: cuántas palabras tiene que revisar
        val porRevisar = remember(texto, errores) {
            errores.count { it.fin <= texto.length && texto.substring(it.inicio, it.fin) == it.palabra }
        }
        if (porRevisar > 0) {
            Text(
                if (porRevisar == 1) "Revisa la palabra marcada en rojo" else "Revisa las $porRevisar palabras marcadas en rojo",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = ColoresAventura.Rojo,
                modifier = Modifier
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag("aviso_errores")
            )
            Spacer(Modifier.height(8.dp))
        }
        // T-332: sin internet se sigue escribiendo (el borrador se guarda en la tablet)
        if (sinRevisar) {
            Text(
                "Sin internet: revisaremos tu ortografía cuando vuelva la conexión",
                fontSize = 18.sp,
                color = ColoresAventura.TextoOscuro,
                modifier = Modifier
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag("aviso_sin_conexion")
            )
            Spacer(Modifier.height(8.dp))
        }
        // T-324: cuántas palabras lleva y cuántas le faltan
        val palabras = remember(texto) { contarPalabras(texto) }
        val llego = palabras >= consigna.palabrasMinimas
        ContadorPalabras(palabras, consigna.palabrasMinimas, guardado)
        Spacer(Modifier.height(20.dp))
        // T-324: se activa recién al llegar al mínimo de palabras de la consigna
        Button(
            onClick = onEnviar,
            enabled = llego,
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
}

private val ALTO_COMPACTO = 560.dp
private val ALTO_HOJA_COMPACTA = 200.dp

@Composable
private fun ContadorPalabras(palabras: Int, minimo: Int, guardado: EstadoGuardado) {
    val llego = palabras >= minimo
    val faltan = (minimo - palabras).coerceAtLeast(0)
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$palabras de $minimo palabras",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = ColoresAventura.TextoOscuro,
                modifier = Modifier.testTag("contador_palabras")
            )
            if (llego) {
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier.size(30.dp).clip(CircleShape).background(ColoresAventura.Turquesa),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.weight(1f))
            IndicadorGuardado(guardado)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (palabras.toFloat() / minimo).coerceIn(0f, 1f) },
            color = if (llego) ColoresAventura.Turquesa else ColoresAventura.Naranja,
            trackColor = Color.White,
            modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).testTag("barra_palabras")
        )
        Spacer(Modifier.height(8.dp))
        // liveRegion: TalkBack lee el aviso cuando cambia
        Text(
            when {
                llego -> "¡Llegaste al mínimo! Ya puedes enviar tu texto."
                faltan == 1 -> "Te falta 1 palabra"
                else -> "Te faltan $faltan palabras"
            },
            fontSize = 18.sp,
            color = ColoresAventura.TextoOscuro,
            modifier = Modifier
                .semantics { liveRegion = LiveRegionMode.Polite }
                .testTag("aviso_palabras")
        )
    }
}

// T-325: como en el mockup, a la derecha del contador. Le da tranquilidad saber que no pierde su texto
@Composable
private fun IndicadorGuardado(guardado: EstadoGuardado) {
    when (guardado) {
        EstadoGuardado.NADA -> Unit
        EstadoGuardado.GUARDANDO -> Text(
            "Guardando…",
            fontSize = 18.sp,
            color = ColoresAventura.TextoOscuro,
            modifier = Modifier.testTag("estado_guardado")
        )
        EstadoGuardado.GUARDADO -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag("estado_guardado")) {
            Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = ColoresAventura.Turquesa, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(4.dp))
            Text("Guardado", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = ColoresAventura.TextoOscuro)
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

@Preview(name = "Editor · llegó al mínimo", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun PantallaEditorListoPreview() {
    AppMovilTheme(dynamicColor = false) {
        ContenidoEditor(
            ConsignaUiState.Exito(CONSIGNA_EDITOR),
            "Mi animal favorito es el perro. Se llama Toby y es muy juguetón. Come croquetas y vive en mi casa con mi familia.",
            {}, {}, {}, {},
            guardado = EstadoGuardado.GUARDADO
        )
    }
}

@Preview(name = "Editor · escribiendo", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun PantallaEditorEscribiendoPreview() {
    AppMovilTheme(dynamicColor = false) {
        ContenidoEditor(
            ConsignaUiState.Exito(CONSIGNA_EDITOR),
            "Mi animal favorito es el perro. Se llama Toby.",
            {}, {}, {}, {}
        )
    }
}

@Preview(name = "Editor · con errores", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun PantallaEditorConErroresPreview() {
    AppMovilTheme(dynamicColor = false) {
        ContenidoEditor(
            ConsignaUiState.Exito(CONSIGNA_EDITOR),
            "Mi perro es muy jugeton y le gusta corer en el arbol.",
            {}, {}, {}, {},
            guardado = EstadoGuardado.GUARDADO,
            errores = listOf(
                ErrorOrtografico("jugeton", 16, 23, "ortografia", null),
                ErrorOrtografico("corer", 35, 40, "ortografia", null),
                ErrorOrtografico("arbol", 47, 52, "tilde", "árbol")
            )
        )
    }
}
