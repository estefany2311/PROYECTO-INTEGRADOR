package com.proyecto.movil.model.remote
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // IP local de mi PC, para probar en el teléfono físico por USB/WiFi
    private const val BASE_URL = "http://10.173.207.104:8082/"

    // Lo asigna Dependencias.iniciar(): devuelve el token de la sesión actual o null
    @Volatile
    var proveedorToken: () -> String? = { null }

    // BASIC = solo método, URL y código de respuesta, sin cuerpos: el PIN y el token
    // ya no aparecen en Logcat. En la versión final (release) no se registra nada.
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC   // sin cuerpos: el PIN y el token no aparecen en Logcat
        redactHeader("Authorization")
    }
    private val okHttpClient = OkHttpClient.Builder()
        // Agrega "Authorization: Bearer <token>" si hay sesión guardada
        .addInterceptor { chain ->
            val token = proveedorToken()
            val peticion = if (token != null) {
                chain.request().newBuilder().header("Authorization", "Bearer $token").build()
            } else {
                chain.request()
            }
            chain.proceed(peticion)
        }
        .addInterceptor(loggingInterceptor)
        .build()

    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}