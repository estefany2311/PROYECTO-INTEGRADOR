package com.proyecto.movil.ui.theme

import androidx.compose.ui.graphics.Color


// Colores del módulo estudiante. Desde el sprint 4 (T-440): amarillo suave del portal docente
// como color principal, fondo crema y acentos vivos; cada color de botón tiene su "Borde",
// la franja más oscura de abajo que le da el relieve.
// Van dentro de un object porque Color.kt ya tiene otro TextoOscuro.
object ColoresAventura {
    val Cielo = Color(0xFFFFF8E7)        // fondo de todas las pantallas (crema)
    val Tarjeta = Color(0xFFFFFFFF)
    val Linea = Color(0xFFF2E4BC)        // bordes suaves de tarjetas y separadores

    val Amarillo = Color(0xFFFFD95C)     // principal: botón Continuar, nivel actual, unidad
    val AmarilloBorde = Color(0xFFE8B828)
    val AmarilloTexto = Color(0xFF4A3A00)
    val AmarilloSuave = Color(0xFFFFF0BF)

    val Naranja = Color(0xFFFF9F1C)      // avatar / intentar de nuevo / racha
    val NaranjaBorde = Color(0xFFD98200)
    val Turquesa = Color(0xFF2EC4B6)     // éxito / nivel completado
    val TurquesaBorde = Color(0xFF1E9A8E)
    val TurquesaTexto = Color(0xFF0B5F58)
    val Azul = Color(0xFF4EA8DE)         // T-242: reto de oralidad / puntos
    val AzulBorde = Color(0xFF2F86BD)

    val Gris = Color(0xFFD5CBB0)         // deshabilitado / nivel bloqueado
    val Bloqueado = Color(0xFFECE5D3)
    val BloqueadoIcono = Color(0xFF9C9178)
    val Rojo = Color(0xFFE63946)         // errores
    val TextoOscuro = Color(0xFF2B3A55)
    val TextoSuave = Color(0xFF6B6450)

    val pasto = Color(0xFF87EA82)
}
