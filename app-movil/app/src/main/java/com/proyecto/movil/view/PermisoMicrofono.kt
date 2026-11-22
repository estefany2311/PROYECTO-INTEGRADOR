package com.proyecto.movil.view

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.proyecto.movil.ui.theme.AppMovilTheme
import com.proyecto.movil.ui.theme.ColoresAventura

enum class EstadoPermiso {
    SIN_PEDIR,          // todavía no se pidió
    DENEGADO,           // dijo que no, pero se puede volver a preguntar
    DENEGADO_SIEMPRE    // Android ya no muestra la ventana: hay que ir a Ajustes
}

@Composable
fun ConPermisoMicrofono(onAhoraNo: () -> Unit, contenido: @Composable () -> Unit) {
    val contexto = LocalContext.current
    var concedido by rememberSaveable { mutableStateOf(tienePermisoMicrofono(contexto)) }
    var estado by rememberSaveable { mutableStateOf(EstadoPermiso.SIN_PEDIR) }

    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { si ->
        concedido = si
        if (!si) {
            val actividad = contexto.buscarActividad()
            val sePuedeVolverAPreguntar = actividad?.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) ?: true
            estado = if (sePuedeVolverAPreguntar) EstadoPermiso.DENEGADO else EstadoPermiso.DENEGADO_SIEMPRE
        }
    }

    if (concedido) {
        contenido()
    } else {
        ExplicacionMicrofono(
            estado = estado,
            onPermitir = { pedirPermiso.launch(Manifest.permission.RECORD_AUDIO) },
            onAhoraNo = onAhoraNo,
            onAbrirAjustes = {
                contexto.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", contexto.packageName, null))
                )
            },
            // Al volver de Ajustes, el niño (o el profesor) toca "Ya lo activé" y se revisa de nuevo
            onYaLoActive = { concedido = tienePermisoMicrofono(contexto) }
        )
    }
}

fun tienePermisoMicrofono(contexto: Context): Boolean =
    ContextCompat.checkSelfPermission(contexto, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

private tailrec fun Context.buscarActividad(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.buscarActividad()
    else -> null
}

// Sin lógica de permisos: se puede ver en @Preview y probar con cualquier estado
@Composable
fun ExplicacionMicrofono(
    estado: EstadoPermiso,
    onPermitir: () -> Unit,
    onAhoraNo: () -> Unit,
    onAbrirAjustes: () -> Unit,
    onYaLoActive: () -> Unit
) {
    val (titulo, detalle) = when (estado) {
        EstadoPermiso.SIN_PEDIR ->
            "Para escuchar cómo lees, necesito usar el micrófono" to
                "Tu voz solo se usa para revisar tu lectura. Cuando aparezca la ventana, toca «Permitir»."
        EstadoPermiso.DENEGADO ->
            "Sin el micrófono no puedo escucharte" to
                "Toca el botón otra vez y elige «Permitir». Si tienes dudas, pregúntale a tu profesor."
        EstadoPermiso.DENEGADO_SIEMPRE ->
            "El micrófono está apagado para esta app" to
                "Pídele a tu profesor que lo active en Ajustes. Cuando esté listo, toca «Ya lo activé»."
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(ColoresAventura.Cielo)
            .verticalScroll(rememberScrollState())
            .padding(32.dp)
            .testTag("explicacion_microfono"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Koala(tamano = 180.dp, saludando = estado == EstadoPermiso.SIN_PEDIR)
        Spacer(Modifier.height(24.dp))
        Column(
            Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                titulo,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = ColoresAventura.TextoOscuro,
                modifier = Modifier.testTag("titulo_permiso")
            )
            Spacer(Modifier.height(12.dp))
            Text(detalle, fontSize = 20.sp, lineHeight = 28.sp, textAlign = TextAlign.Center, color = ColoresAventura.TextoOscuro)
            Spacer(Modifier.height(28.dp))

            when (estado) {
                EstadoPermiso.SIN_PEDIR -> BotonPrincipal("Sí, usar el micrófono", "boton_permitir", onPermitir)
                EstadoPermiso.DENEGADO -> BotonPrincipal("Intentar otra vez", "boton_permitir", onPermitir)
                EstadoPermiso.DENEGADO_SIEMPRE -> {
                    BotonPrincipal("Abrir Ajustes", "boton_ajustes", onAbrirAjustes)
                    Spacer(Modifier.height(12.dp))
                    BotonSecundario("Ya lo activé", "boton_ya_active", onYaLoActive)
                }
            }
            Spacer(Modifier.height(12.dp))
            BotonSecundario("Ahora no", "boton_ahora_no", onAhoraNo)
        }
    }
}

@Composable
private fun BotonPrincipal(texto: String, tag: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = ColoresAventura.Naranja),
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier.fillMaxWidth().height(64.dp).testTag(tag)
    ) { Text(texto, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White) }
}

@Composable
private fun BotonSecundario(texto: String, tag: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier.fillMaxWidth().height(56.dp).testTag(tag)
    ) { Text(texto, fontSize = 20.sp, color = ColoresAventura.TextoOscuro) }
}

@Preview(name = "Permiso · primera vez", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun PermisoSinPedirPreview() = PermisoDePrueba(EstadoPermiso.SIN_PEDIR)

@Preview(name = "Permiso · dijo que no", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun PermisoDenegadoPreview() = PermisoDePrueba(EstadoPermiso.DENEGADO)

@Preview(name = "Permiso · hay que ir a Ajustes", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun PermisoDenegadoSiemprePreview() = PermisoDePrueba(EstadoPermiso.DENEGADO_SIEMPRE)

@Composable
private fun PermisoDePrueba(estado: EstadoPermiso) {
    AppMovilTheme(dynamicColor = false) {
        ExplicacionMicrofono(estado, onPermitir = {}, onAhoraNo = {}, onAbrirAjustes = {}, onYaLoActive = {})
    }
}
