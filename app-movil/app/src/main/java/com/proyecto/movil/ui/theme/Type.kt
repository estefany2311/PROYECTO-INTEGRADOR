package com.proyecto.movil.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.proyecto.movil.R

// T-440: Nunito, letra redonda y gruesa para niños. Va dentro de la app (res/font)
// para que se vea igual sin internet. Licencia SIL Open Font License.
val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
    Font(R.font.nunito_black, FontWeight.Black)
)

private val base = Typography()

// Todos los estilos de Material 3 con Nunito; títulos y botones más gruesos
val Typography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Nunito, fontWeight = FontWeight.Black),
    displayMedium = base.displayMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.Black),
    displaySmall = base.displaySmall.copy(fontFamily = Nunito, fontWeight = FontWeight.Black),
    headlineLarge = base.headlineLarge.copy(fontFamily = Nunito, fontWeight = FontWeight.Black),
    headlineMedium = base.headlineMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.Black),
    headlineSmall = base.headlineSmall.copy(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold),
    titleLarge = base.titleLarge.copy(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold),
    titleMedium = base.titleMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold),
    titleSmall = base.titleSmall.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
    bodyLarge = base.bodyLarge.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold),
    bodyMedium = base.bodyMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold),
    bodySmall = base.bodySmall.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontFamily = Nunito, fontWeight = FontWeight.Black),
    labelMedium = base.labelMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold),
    labelSmall = base.labelSmall.copy(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold)
)
