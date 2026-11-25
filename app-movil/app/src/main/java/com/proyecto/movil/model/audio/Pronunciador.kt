package com.proyecto.movil.model.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

// T-430 (US-14): dice en voz alta la palabra que el niño toca, con la voz del teléfono (sin internet)
interface IPronunciador {
    fun decir(palabra: String)
}

class PronunciadorTts(private val contexto: Context) : IPronunciador {

    private var tts: TextToSpeech? = null
    private var listo = false
    private var pendiente: String? = null   // la que tocó mientras la voz todavía se preparaba

    override fun decir(palabra: String) {
        if (listo) {
            hablar(palabra)
            return
        }
        pendiente = palabra
        // La voz se prepara la primera vez que se usa
        if (tts == null) tts = TextToSpeech(contexto) { estado -> alPrepararse(estado) }
    }

    private fun alPrepararse(estado: Int) {
        val motor = tts ?: return
        listo = estado == TextToSpeech.SUCCESS
        if (listo) {
            // Español de Perú; si el teléfono no lo tiene, cualquier español
            if (motor.setLanguage(Locale.forLanguageTag("es-PE")) < TextToSpeech.LANG_AVAILABLE) {
                motor.setLanguage(Locale.forLanguageTag("es"))
            }
            motor.setSpeechRate(0.8f)   // un poco más lento para que el niño la escuche bien
            pendiente?.let { hablar(it) }
        }
        pendiente = null
    }

    private fun hablar(palabra: String) {
        tts?.speak(palabra, TextToSpeech.QUEUE_FLUSH, null, "palabra")
    }
}
