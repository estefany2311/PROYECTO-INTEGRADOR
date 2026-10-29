package com.proyecto.movil.model.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// Lo que se guarda en el dispositivo después de un PIN correcto
data class SesionGuardada(
    val token: String,
    val estudianteId: Long,
    val nombre: String,
    val avatarUrl: String
)

// Interfaz: en las pruebas (T-206) se reemplaza por una versión en memoria
interface IAlmacenSesion {
    fun guardar(sesion: SesionGuardada)
    fun leer(): SesionGuardada?
    fun borrar()

    // El código del aula no es secreto: sirve para volver a "¿Quién eres?" al salir (T-208)
    fun guardarCodigoAula(codigo: String)
    fun leerCodigoAula(): String?
}

/**
 * T-205: el token se cifra con AES-GCM usando una clave del Android Keystore.
 * La clave nunca sale del dispositivo: aunque alguien copie el archivo de preferencias,
 * no puede leer el token.
 */
class AlmacenSesionSeguro(context: Context) : IAlmacenSesion {

    private val prefs = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    override fun guardar(sesion: SesionGuardada) {
        prefs.edit()
            .putString(TOKEN, cifrar(sesion.token))
            .putLong(ESTUDIANTE_ID, sesion.estudianteId)
            .putString(NOMBRE, sesion.nombre)
            .putString(AVATAR, sesion.avatarUrl)
            .apply()
    }

    override fun leer(): SesionGuardada? {
        val token = prefs.getString(TOKEN, null)?.let(::descifrar) ?: return null
        return SesionGuardada(
            token = token,
            estudianteId = prefs.getLong(ESTUDIANTE_ID, 0),
            nombre = prefs.getString(NOMBRE, "") ?: "",
            avatarUrl = prefs.getString(AVATAR, "") ?: ""
        )
    }

    override fun borrar() {
        prefs.edit().remove(TOKEN).remove(ESTUDIANTE_ID).remove(NOMBRE).remove(AVATAR).apply()
    }

    override fun guardarCodigoAula(codigo: String) {
        prefs.edit().putString(CODIGO_AULA, codigo).apply()
    }

    override fun leerCodigoAula(): String? = prefs.getString(CODIGO_AULA, null)

    // Busca la clave en el Keystore; si no existe (primera vez), la crea
    private fun clave(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }

        val generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generador.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generador.generateKey()
    }

    // Guarda el IV (12 bytes) junto al texto cifrado, en Base64
    private fun cifrar(texto: String): String {
        val cipher = Cipher.getInstance(TRANSFORMACION)
        cipher.init(Cipher.ENCRYPT_MODE, clave())
        val cifrado = cipher.doFinal(texto.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + cifrado, Base64.NO_WRAP)
    }

    // Si no se puede descifrar (por ejemplo, la clave se borró) se trata como "sin sesión"
    private fun descifrar(guardado: String): String? = runCatching {
        val bytes = Base64.decode(guardado, Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, IV_BYTES)
        val cifrado = bytes.copyOfRange(IV_BYTES, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMACION)
        cipher.init(Cipher.DECRYPT_MODE, clave(), GCMParameterSpec(128, iv))
        String(cipher.doFinal(cifrado), Charsets.UTF_8)
    }.getOrNull()

    private companion object {
        const val ARCHIVO = "sesion_estudiante"
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "clave_sesion_estudiante"
        const val TRANSFORMACION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TOKEN = "token"
        const val ESTUDIANTE_ID = "estudiante_id"
        const val NOMBRE = "nombre"
        const val AVATAR = "avatar"
        const val CODIGO_AULA = "codigo_aula"
    }
}