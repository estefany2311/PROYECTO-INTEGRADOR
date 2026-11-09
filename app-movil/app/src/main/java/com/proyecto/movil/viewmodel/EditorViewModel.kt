package com.proyecto.movil.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * T-323 (US-18): el texto que el niño va escribiendo en el editor.
 * Vive en el ViewModel para que no se pierda al girar la tablet.
 * Llegan después: contador de palabras (T-324), borrador guardado en la tablet (T-325)
 * y marcar errores de ortografía (US-19).
 */
class EditorViewModel : ViewModel() {

    private val _texto = MutableStateFlow("")
    val texto: StateFlow<String> = _texto

    fun escribir(nuevo: String) {
        // Un límite generoso para que un dedo apoyado en una tecla no llene la memoria
        _texto.value = nuevo.take(MAX_CARACTERES)
    }

    companion object {
        const val MAX_CARACTERES = 3_000
    }
}
