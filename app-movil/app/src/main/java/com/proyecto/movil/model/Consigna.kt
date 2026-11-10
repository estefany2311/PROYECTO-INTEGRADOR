package com.proyecto.movil.model

//T-321 (US-17): lo que el niño tiene que escribir en un nivel de escritura.

data class ConsignaNivel(
    val nivelId: Int,
    val nombreNivel: String,
    val consigna: String,
    val pista: String?,
    val ilustracion: String,       // "animal", "libro", "lapiz"... (T-319)
    val palabrasMinimas: Int       // lo usa el contador del editor (T-324)
)

// Lo que puede pasar al pedir la consigna de un nivel
sealed class ResultadoConsigna {
    data class Exito(val consigna: ConsignaNivel) : ResultadoConsigna()
    data class Bloqueado(val nivelAnterior: String?) : ResultadoConsigna()   // 403
    object SinConsigna : ResultadoConsigna()                                 // 404: aún no tiene consigna
    object SesionVencida : ResultadoConsigna()                               // 401
    object ErrorConexion : ResultadoConsigna()
}
