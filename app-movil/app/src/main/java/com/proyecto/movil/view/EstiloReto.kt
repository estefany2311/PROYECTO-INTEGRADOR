package com.proyecto.movil.view

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.proyecto.movil.R
import com.proyecto.movil.model.TipoReto
import com.proyecto.movil.ui.theme.ColoresAventura

/**
 * T-242 (US-07): cada tipo de reto tiene su ícono, su color y su nombre, siempre los mismos
 * en el mapa y en el detalle, para que el niño los reconozca aunque todavía no lea bien:
 *   lectura   → libro,  turquesa
 *   escritura → lápiz,  naranja
 *   oralidad  → globo de diálogo, azul
 */
data class EstiloReto(
    val color: Color,
    @DrawableRes val icono: Int?,     // null si el reto es desconocido
    val nombre: String                // "lectura", "escritura", "oralidad"
)

fun estiloDe(reto: TipoReto): EstiloReto = when (reto) {
    TipoReto.LECTURA -> EstiloReto(ColoresAventura.Turquesa, R.drawable.ic_reto_lectura, "lectura")
    TipoReto.ESCRITURA -> EstiloReto(ColoresAventura.Naranja, R.drawable.ic_reto_escritura, "escritura")
    TipoReto.ORALIDAD -> EstiloReto(ColoresAventura.Azul, R.drawable.ic_reto_oralidad, "oralidad")
    TipoReto.DESCONOCIDO -> EstiloReto(ColoresAventura.Gris, null, "aventura")
}
