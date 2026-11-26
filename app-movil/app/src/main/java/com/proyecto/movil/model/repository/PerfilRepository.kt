package com.proyecto.movil.model.repository

import com.proyecto.movil.model.Perfil
import com.proyecto.movil.model.remote.ApiService
import com.proyecto.movil.model.remote.RetrofitClient
import retrofit2.HttpException
import java.io.IOException

interface IPerfilRepository {
    suspend fun obtenerPerfiles(codigoAula: String): Result<List<Perfil>>
}

class PerfilRepository : IPerfilRepository {
    private val api = RetrofitClient.instance.create(ApiService::class.java)

    override suspend fun obtenerPerfiles(codigoAula: String): Result<List<Perfil>> {
        return try {
            Result.success(api.obtenerPerfiles(codigoAula))
        } catch (e: HttpException) {
            // 404: el docente borró el aula o el código está mal escrito
            Result.failure(Exception(if (e.code() == 404) AULA_NO_EXISTE else ERROR_SERVIDOR))
        } catch (e: IOException) {
            Result.failure(Exception(SIN_CONEXION))
        } catch (e: Exception) {
            Result.failure(Exception(ERROR_SERVIDOR))
        }
    }

    companion object {
        const val AULA_NO_EXISTE = "Ese código de aula no existe. Revisa el código con tu profesor."
        const val SIN_CONEXION = "No pudimos conectarnos. Revisa que la tablet tenga internet."
        const val ERROR_SERVIDOR = "Pídele ayuda a tu profesor."
    }
}