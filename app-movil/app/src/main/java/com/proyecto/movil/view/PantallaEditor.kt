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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
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
import com.proyecto.movil.model.reemplazarPalabra
import com.proyecto.movil.ui.theme.AppMovilTheme
import com.proyecto.movil.ui.theme.ColoresAventura
import com.proyecto.movil.viewmodel.ConsignaUiState
import com.proyecto.movil.viewmodel.ConsignaViewModel
import com.proyecto.movil.viewmodel.EditorViewModel
import com.proyecto.movil.viewmodel.EnvioTextoUiState
import com.proyecto.movil.viewmodel.EstadoGuardado
import com.proyecto.movil.view.componentes.SubrayarErrores
import com.proyecto.movil.view.componentes.CabeceraEscritura
import com.proyecto.movil.view.componentes.ErrorConsigna
import com.proyecto.movil.view.componentes.Koala
import com.proyecto.movil.view.componentes.MenuSugerencias

//T-323 (US-18): el niño escribe su texto con letra grande. Se abre con "Empezar a escribir" (T-321)
 //Teclado en español y SIN autocorrector: lo que se evalúa es cómo escribe el niño, así que la tablet
//no debe corregirle las palabras. La primera letra de cada oración va en mayúscula.

@Composable
fun PantallaEditor(
    nivelId: Int,
    onVolver: () -> Unit,
    onVolverAlMapa: () -> Unit,
    consignaViewModel: ConsignaViewModel = viewModel(),
    editorViewModel: EditorViewModel = viewModel()
) {
    val consigna by consignaViewModel.uiState.collectAsStateWithLifecycle()
    val texto by editorViewModel.texto.collectAsStateWithLifecycle()
    val guardado by editorViewModel.guardado.collectAsStateWithLifecycle()
    val errores by editorViewModel.errores.collectAsStateWithLifecycle()
    val sinRevisar by editorViewModel.sinRevisar.collectAsStateWithLifecycle()
    val envio by editorViewModel.envio.collectAsStateWithLifecycle()

    LaunchedEffect(nivelId) {
        consignaViewModel.cargar(nivelId)
        editorViewModel.abrir(nivelId)      // T-325: recupera el borrador si lo había
    }

    ContenidoEditor(
        consigna = consigna,
        texto = texto,
        onEscribir = editorViewModel::escribir,
        onVolver = onVolver,
        onEnviar = editorViewModel::pedirEnviar,
        onReintentar = consignaViewModel::reintentar,
        guardado = guardado,
        errores = errores,
        sinRevisar = sinRevisar,
        envio = envio,
        onConfirmarEnvio = editorViewModel::confirmarEnvio,
        onCancelarEnvio = editorViewModel::cancelarEnvio,
        onVolverAlMapa = onVolverAlMapa,
        onElegirSugerencia = editorViewModel::elegirSugerencia
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
    sinRevisar: Boolean = false,
    envio: EnvioTextoUiState = EnvioTextoUiState.Escribiendo,      // T-435
    onConfirmarEnvio: () -> Unit = {},
    onCancelarEnvio: () -> Unit = {},
    onVolverAlMapa: () -> Unit = {},
    onElegirSugerencia: (ErrorOrtografico, String) -> Unit = { _, _ -> }   // T-366/T-367
) {
    // T-435: antes de enviar se confirma, porque después ya no podrá cambiar su texto
    if (envio is EnvioTextoUiState.Confirmando) DialogoConfirmarEnvio(onConfirmarEnvio, onCancelarEnvio)
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
            is ConsignaUiState.Exito -> when (envio) {
                is EnvioTextoUiState.Enviado -> TextoEnviado(envio, onVolverAlMapa)
                else -> Hoja(consigna.consigna, texto, guardado, errores, sinRevisar, envio, onEscribir, onEnviar, onElegirSugerencia)
            }
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
    envio: EnvioTextoUiState,
    onEscribir: (String) -> Unit,
    onEnviar: () -> Unit,
    onElegirSugerencia: (ErrorOrtografico, String) -> Unit
) {
    // T-366: se guarda dónde está el cursor para saber qué palabra tocó el niño
    var campo by remember { mutableStateOf(TextFieldValue(texto, TextRange(texto.length))) }
    // Si el texto cambió desde afuera (borrador recuperado o sugerencia elegida), el cursor va al final
    val valor = if (campo.text == texto) campo else TextFieldValue(texto, TextRange(texto.length))
    var tocoUnaPalabra by remember { mutableStateOf(false) }
    var cerradoPara by remember { mutableStateOf<ErrorOrtografico?>(null) }
    val errorTocado = palabraTocada(valor, errores).takeIf { tocoUnaPalabra && it != cerradoPara }
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
            value = valor,
            onValueChange = { nuevo ->
                // Solo un toque (mismo texto, otro lugar del cursor) abre el menú; al escribir se cierra
                if (nuevo.text != valor.text) tocoUnaPalabra = false
                else if (nuevo.selection != valor.selection) {
                    tocoUnaPalabra = true
                    cerradoPara = null     // un toque nuevo vuelve a abrir el menú aunque lo haya cerrado
                }
                campo = nuevo
                if (nuevo.text != texto) onEscribir(nuevo.text)
            },
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
        // T-366: al tocar una palabra subrayada aparecen sus sugerencias
        errorTocado?.let { error ->
            Spacer(Modifier.height(12.dp))
            MenuSugerencias(
                error = error,
                onElegir = { sugerencia ->
                    tocoUnaPalabra = false
                    // T-367: el cursor queda justo después de la palabra corregida
                    reemplazarPalabra(valor.text, error, sugerencia)?.let { nuevo ->
                        campo = TextFieldValue(nuevo, TextRange(error.inicio + sugerencia.length))
                    }
                    onElegirSugerencia(error, sugerencia)
                },
                onCerrar = { cerradoPara = error }
            )
        }
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
        // T-435: si no se pudo enviar, el texto sigue en la tablet y puede volver a intentarlo
        if (envio is EnvioTextoUiState.Error) {
            Text(
                envio.mensaje,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = ColoresAventura.Rojo,
                modifier = Modifier
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag("aviso_envio")
            )
            Spacer(Modifier.height(8.dp))
        }
        val enviando = envio is EnvioTextoUiState.Enviando
        // T-324: se activa recién al llegar al mínimo de palabras de la consigna
        Button(
            onClick = onEnviar,
            enabled = llego && !enviando,
            colors = ButtonDefaults.buttonColors(
                containerColor = ColoresAventura.Naranja,
                disabledContainerColor = ColoresAventura.Gris
            ),
            shape = RoundedCornerShape(36.dp),
            modifier = Modifier.fillMaxWidth().height(72.dp).testTag("boton_enviar_texto")
        ) {
            if (enviando) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 4.dp, modifier = Modifier.size(32.dp).testTag("enviando_texto"))
                Spacer(Modifier.width(12.dp))
                Text("Enviando…", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
            } else {
                Text("Enviar mi texto", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            }
        }
    }
    }
}

// T-366: el error subrayado donde está el cursor (solo si la palabra sigue en ese lugar)
internal fun palabraTocada(valor: TextFieldValue, errores: List<ErrorOrtografico>): ErrorOrtografico? {
    if (!valor.selection.collapsed) return null
    val cursor = valor.selection.start
    val texto = valor.text
    return errores.firstOrNull { e ->
        cursor in e.inicio..e.fin && e.fin <= texto.length && texto.substring(e.inicio, e.fin) == e.palabra
    }
}

// T-435: confirmación antes de enviar
@Composable
private fun DialogoConfirmarEnvio(onConfirmar: () -> Unit, onCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("¿Enviar tu texto?", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = ColoresAventura.TextoOscuro) },
        text = {
            Text(
                "Cuando lo envíes ya no podrás cambiarlo. Tu profesor lo va a leer.",
                fontSize = 20.sp,
                color = ColoresAventura.TextoOscuro
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                colors = ButtonDefaults.buttonColors(containerColor = ColoresAventura.Naranja),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.height(56.dp).testTag("boton_confirmar_envio")
            ) { Text("Sí, enviar", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onCancelar, modifier = Modifier.height(56.dp).testTag("boton_cancelar_envio")) {
                Text("Seguir escribiendo", fontSize = 20.sp, color = ColoresAventura.TextoOscuro)
            }
        },
        modifier = Modifier.testTag("dialogo_enviar_texto")
    )
}

// T-435: después de enviar, el koala lo felicita con su puntaje (si el backend docente respondió)
@Composable
private fun ColumnScope.TextoEnviado(enviado: EnvioTextoUiState.Enviado, onVolverAlMapa: () -> Unit) {
    Column(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Koala(tamano = 180.dp, saludando = true)
        Spacer(Modifier.height(20.dp))
        Text(
            "¡Enviaste tu texto!",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = ColoresAventura.Turquesa,
            modifier = Modifier.testTag("titulo_texto_enviado")
        )
        enviado.escritura?.let { escritura ->
            val estrellas = escritura.estrellas.coerceIn(0, 3)
            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.semantics { contentDescription = "$estrellas de 3 estrellas" }.testTag("estrellas_texto")
            ) {
                repeat(3) { i ->
                    Icon(
                        painterResource(R.drawable.ic_estrella),
                        contentDescription = null,
                        tint = if (i < estrellas) ColoresAventura.Naranja else ColoresAventura.Gris,
                        modifier = Modifier.size(if (i == 1) 88.dp else 72.dp)
                    )
                }
            }
            escritura.mensaje?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    it,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                    color = ColoresAventura.TextoOscuro,
                    modifier = Modifier.widthIn(max = 640.dp).testTag("mensaje_texto")
                )
            }
        }
        // Sin puntaje todavía: el profesor igual recibió el texto
        if (enviado.escritura == null) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Tu profesor ya puede leerlo.",
                fontSize = 22.sp,
                color = ColoresAventura.TextoOscuro,
                modifier = Modifier.testTag("mensaje_texto")
            )
        }
        enviado.reto?.takeIf { it.siguienteRecienDesbloqueado }?.siguienteNivelNombre?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                "¡Desbloqueaste un nivel nuevo: $it!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = ColoresAventura.Naranja,
                modifier = Modifier.testTag("nivel_desbloqueado_texto")
            )
        }
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onVolverAlMapa,
            colors = ButtonDefaults.buttonColors(containerColor = ColoresAventura.Turquesa),
            shape = RoundedCornerShape(36.dp),
            modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().height(72.dp).testTag("boton_volver_mapa_texto")
        ) { Text("Volver al mapa", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White) }
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
