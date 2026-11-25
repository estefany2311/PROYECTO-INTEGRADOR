package com.proyecto.movil.view.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proyecto.movil.R
import com.proyecto.movil.ui.theme.AppMovilTheme
import com.proyecto.movil.ui.theme.ColoresAventura
import com.proyecto.movil.viewmodel.MapaViewModel

@Composable
internal fun AvisoNivelBloqueado(mensaje: String, onCerrar: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(16.dp)
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(3.dp, ColoresAventura.Gris, RoundedCornerShape(20.dp))
            .clickable(onClick = onCerrar)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("aviso_nivel")
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(ColoresAventura.Gris),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(R.drawable.ic_candado),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(mensaje, fontSize = 20.sp, color = ColoresAventura.TextoOscuro)
    }
}

@Preview(widthDp = 400, heightDp = 200, showBackground = true)
@Composable
fun AvisoNivelBloqueadoPreview() {
    AppMovilTheme(dynamicColor = false) {
        AvisoNivelBloqueado(MapaViewModel.mensajeNivelBloqueado("Primeros pasos"), onCerrar = {})
    }
}
